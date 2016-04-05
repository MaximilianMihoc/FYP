package ie.dit.max.behaviouralbiometricphonelock;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.ml.Ml;
import org.opencv.ml.SVM;

import java.util.ArrayList;

/**
 * In this Activity, the user will be able to change the password.
 * Before changing the password, the user is required to use tha application for a while.
 * It is required that the user uses the password for at least 20 interactions.
 * The interactions are saved in the database into an object "testDataForPasswordChange" that is deleted every time when the user is or it gets logged out.
 *
 * In order to change the password, the old password also needs to be provided.
 *
 * @author Maximilian Mihoc.
 * @version 1.0
 * @since 5th March 2016
 */
public class ChangePassword extends AppCompatActivity
{
    private static final String DEBUG_TAG = "Change Password Activity";
    private static SVM scrollFlingSVM;
    private Firebase ref;
    private EditText oldPassword;
    private EditText newPassword;
    private EditText newPasswordConfirmed;
    private String userID;
    private String userEmail;
    private String oldPasswordStr;
    private String newPasswordStr;
    private String newPasswordConfirmedStr;
    private ArrayList<Observation> trainScrollFlingObservations;
    private ArrayList<Observation> scrollFlingObservations;
    private double threshold;
    private int guestsObservationsNeeded;
    private boolean userBehaviourCheck;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);
        Firebase.setAndroidContext(this);

        ref = new Firebase(DBVar.mainURL);
        SharedPreferences sharedpreferences = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        if(sharedpreferences.contains("UserID")) userID = sharedpreferences.getString("UserID", "");
        if(sharedpreferences.contains("UserEmail")) userEmail = sharedpreferences.getString("UserEmail", "");

        trainScrollFlingObservations = new ArrayList<>();
        scrollFlingObservations = new ArrayList<>();

        // this variable is used to determine if the interactions checked were Owner's interactions or not
        userBehaviourCheck = true;

        oldPassword = (EditText) findViewById(R.id.oldPassword);
        newPassword = (EditText) findViewById(R.id.newPassword1);
        newPasswordConfirmed = (EditText) findViewById(R.id.newPassword2);
        Button changePassword = (Button) findViewById(R.id.changePassword);

        /* Get User Settings and Build the train model for the current user */
        getUserSettings();

        //On click Listener on the ChangePassword button
        changePassword.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                oldPasswordStr = oldPassword.getText().toString();
                newPasswordStr = newPassword.getText().toString();
                newPasswordConfirmedStr = newPasswordConfirmed.getText().toString();

                // check if the old password is correct
                ref.authWithPassword(userEmail, oldPasswordStr, new Firebase.AuthResultHandler()
                {
                    @Override
                    public void onAuthenticated(AuthData authData)
                    {
                        // at this stage, the old password was checked and it was correct.
                        // Check the new password and show warnings to the user.
                        if (newPasswordStr.trim().length() < 5)
                        {
                            Toast.makeText(getApplicationContext(), "Please Enter a New Password of at least 5 characters.", Toast.LENGTH_SHORT).show();
                        } else if (!newPasswordStr.equals(newPasswordConfirmedStr))
                        {
                            Toast.makeText(getApplicationContext(), "Password and Confirmation Password do not match.", Toast.LENGTH_SHORT).show();
                        } else
                        {
                            // get testDataForPasswordChange for user and test it using the SVM classifier
                            // the password will be changed only if the test data provided is believed to be Owner's data.
                            // The average prediction value needs to be greater than the Threshold.
                            Firebase scrollFlingRef = new Firebase(DBVar.mainURL + "/testDataForPasswordChange/" + userID);
                            scrollFlingRef.addListenerForSingleValueEvent(new ValueEventListener()
                            {
                                @Override
                                public void onDataChange(DataSnapshot snapshot)
                                {
                                    if (snapshot.getValue() == null)
                                    {
                                        // users interactions do not exist, password won't be changed
                                        userBehaviourCheck = false;
                                    } else
                                    {
                                        // get all interactions from the database needed to change password and put them into a list
                                        DataSnapshot dpScroll = snapshot.child("scrollFling");
                                        for (DataSnapshot obsSnapshot : dpScroll.getChildren())
                                        {
                                            Observation obs = obsSnapshot.getValue(Observation.class);
                                            scrollFlingObservations.add(obs);
                                        }

                                        // if the list is empty, password will not be changed.
                                        if (scrollFlingObservations.size() > 0)
                                        {
                                            // having data necessary to change the password
                                            // test and result Martices are created
                                            Mat testDataMat = Classifier.buildTrainOrTestMatForScrollFling(scrollFlingObservations);
                                            Mat resultMat = new Mat(scrollFlingObservations.size(), 1, CvType.CV_32S);

                                            // predict the Observations against the train model SVM
                                            scrollFlingSVM.predict(testDataMat, resultMat, 0);
                                            // count the number of Owner's predicted interactions
                                            int counter = Classifier.countOwnerResults(resultMat);

                                            // check percentage and see if it is owners or not.
                                            userBehaviourCheck = Math.round((counter * 100) / scrollFlingObservations.size()) >= threshold;
                                        } else
                                        {
                                            userBehaviourCheck = false;
                                        }
                                    }

                                    // Change password
                                    if (userBehaviourCheck)
                                    {
                                        // The interactions were believed to be owner's interactions, password gets changed.
                                        ref.changePassword(userEmail, oldPasswordStr, newPasswordStr, new Firebase.ResultHandler()
                                        {
                                            @Override
                                            public void onSuccess()
                                            {
                                                Toast.makeText(getApplicationContext(), "Password successfully changed", Toast.LENGTH_SHORT).show();

                                                Intent intent = new Intent(ChangePassword.this, OptionsScreen.class);
                                                startActivity(intent);
                                            }

                                            @Override
                                            public void onError(FirebaseError firebaseError)
                                            {
                                                Toast.makeText(getApplicationContext(), "Password could not be changed.", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    } else
                                    {
                                        Toast.makeText(getApplicationContext(), "Password could not be changed. Log out and try again.", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onCancelled(FirebaseError firebaseError)
                                {
                                    System.out.println(DEBUG_TAG + "The read failed: " + firebaseError.getMessage());
                                }
                            });
                        }
                    }

                    @Override
                    public void onAuthenticationError(FirebaseError firebaseError)
                    {
                        Toast toast = Toast.makeText(getApplicationContext(), "Old Password Incorrect", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                });


            }
        });
    }

    /**
     * Method getUserSettings
     * This method is used to get the User Settings from the database if they are defined.
     * If they are not defined, the application will use the default settings.
     *
     */
    private void getUserSettings()
    {
        final Firebase settingsRef = new Firebase(DBVar.mainURL + "/settings/" + userID);
        settingsRef.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.getValue() == null)
                {
                    // Default values
                    threshold = 70;
                    guestsObservationsNeeded = 4;
                }
                else
                {
                    UserSettings userSettings = dataSnapshot.getValue(UserSettings.class);
                    threshold = userSettings.getThreshold();
                    guestsObservationsNeeded = userSettings.getNrObsFromAnotherUser() - 1;
                }

                /* Get user training data from Firebase - Owner and Guest data */
                getTrainDataFromFirebase();
            }

            @Override
            public void onCancelled(FirebaseError firebaseError)
            {
                System.out.println(DEBUG_TAG + "The read failed: " + firebaseError.getMessage());
            }
        });
    }

    /**
     * Method getTrainDataFromFirebase
     * This method returns the train data from database and it creates the training Model to be used by the Classifier
     *
     */
    private void getTrainDataFromFirebase()
    {
        final Firebase scrollFlingRef = new Firebase(DBVar.mainURL + "/trainData");
        scrollFlingRef.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot snapshot)
            {
                if (snapshot.getValue() == null)
                {
                    System.out.println(DEBUG_TAG + "No Scroll Fling data available. ");
                    Toast toast = Toast.makeText(getApplicationContext(), "No Test data Provided", Toast.LENGTH_SHORT);
                    toast.show();
                }
                else
                {
                    /*
                    * Data is returned from the database in a Hash map.
                    * The map contains the training data for all the user in a key value pair format.
                    * The next loop will iterate through the map and check the keys.
                    * When the key is Owner's key, train data is stored as owner's data and is stored as guest's data otherwise.
                    *
                    * */
                    for (DataSnapshot usrSnapshot : snapshot.getChildren())
                    {
                        if (!usrSnapshot.getKey().equals(userID))
                        {
                            // Guests data
                            DataSnapshot dpScroll = usrSnapshot.child("scrollFling");
                            int countGuestObs = 0;
                            for (DataSnapshot obsSnapshot : dpScroll.getChildren())
                            {
                                Observation obs = obsSnapshot.getValue(Observation.class);
                                // judgements is set to 0 because for guest data
                                obs.setJudgement(0);

                                // only use a defined number of observations from guest users.
                                if (countGuestObs < guestsObservationsNeeded)
                                    trainScrollFlingObservations.add(obs);
                                countGuestObs++;
                            }

                        }
                        else  // get data from the actual user
                        {
                            DataSnapshot scrollSnapshot = usrSnapshot.child("scrollFling");
                            for (DataSnapshot obsSnapshot : scrollSnapshot.getChildren())
                            {
                                Observation obs = obsSnapshot.getValue(Observation.class);
                                trainScrollFlingObservations.add(obs);
                            }
                        }
                    }

                    // Built the SVM model for Scroll/Fling Observations if training data exists.
                    if (trainScrollFlingObservations.size() > 0)
                    {
                        // create the SVM classifier from the list of observations created
                        scrollFlingSVM = Classifier.createAndTrainScrollFlingSVMClassifier(trainScrollFlingObservations);

                    } else
                    {
                        System.out.println("No Scroll Fling data available. ");
                        // display a Toast letting the user know that there is no training data available.
                        Toast toast = Toast.makeText(getApplicationContext(), "No Training data Provided", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError)
            {
                System.out.println(DEBUG_TAG + "The read failed: " + firebaseError.getMessage());
            }
        });
    }
}
