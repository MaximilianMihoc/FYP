package ie.dit.max.behaviouralbiometricphonelock;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
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

public class ChangePassword extends AppCompatActivity
{
    Firebase ref;

    EditText oldPassword;
    EditText newPassword;
    EditText newPasswordConfirmed;
    Button changePassword;

    SharedPreferences sharedpreferences;
    private String userID;
    private String userEmail;

    String oldPasswordStr;
    String newPasswordStr;
    String newPasswordConfirmedStr;

    ArrayList<Observation> trainScrollFlingObservations;
    ArrayList<Observation> scrollFlingObservations;

    private static SVM scrollFlingSVM;

    private double threshold;
    private int guestsObservationsNeeded;

    boolean userBehaviourCheck;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);
        Firebase.setAndroidContext(this);

        ref = new Firebase("https://fyp-max.firebaseio.com");
        sharedpreferences = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        if(sharedpreferences.contains("UserID")) userID = sharedpreferences.getString("UserID", "");
        if(sharedpreferences.contains("UserEmail")) userEmail = sharedpreferences.getString("UserEmail", "");

        trainScrollFlingObservations = new ArrayList<>();
        scrollFlingObservations = new ArrayList<>();

        userBehaviourCheck = true;

        oldPassword = (EditText) findViewById(R.id.oldPassword);
        newPassword = (EditText) findViewById(R.id.newPassword1);
        newPasswordConfirmed = (EditText) findViewById(R.id.newPassword2);
        changePassword = (Button) findViewById(R.id.changePassword);

        /* Build the train model for this user starting with settings. */
        getUserSettings();

        changePassword.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                oldPasswordStr = oldPassword.getText().toString();
                newPasswordStr = newPassword.getText().toString();
                newPasswordConfirmedStr = newPasswordConfirmed.getText().toString();

                ref.authWithPassword(userEmail, oldPasswordStr, new Firebase.AuthResultHandler()
                {
                    @Override
                    public void onAuthenticated(AuthData authData)
                    {
                        if(newPasswordStr.trim().length() < 5)
                        {
                            Toast.makeText(getApplicationContext(), "Please Enter a New Password of at least 5 characters.", Toast.LENGTH_SHORT).show();
                        }
                        else if(!newPasswordStr.equals(newPasswordConfirmedStr))
                        {
                            Toast.makeText(getApplicationContext(), "Password and Confirmation Password do not match.", Toast.LENGTH_SHORT).show();
                        }
                        else
                        {
                            // get testDataForPasswordChange for user and test it.
                            Firebase scrollFlingRef = new Firebase("https://fyp-max.firebaseio.com/testDataForPasswordChange/" + userID);
                            scrollFlingRef.addListenerForSingleValueEvent(new ValueEventListener()
                            {
                                @Override
                                public void onDataChange(DataSnapshot snapshot)
                                {
                                    if (snapshot.getValue() == null)
                                    {
                                        userBehaviourCheck = false;
                                    }
                                    else
                                    {
                                        // Scroll Fling Test Data
                                        DataSnapshot dpScroll = snapshot.child("scrollFling");
                                        for (DataSnapshot obsSnapshot : dpScroll.getChildren())
                                        {
                                            Observation obs = obsSnapshot.getValue(Observation.class);
                                            scrollFlingObservations.add(obs);
                                        }

                                        if (scrollFlingObservations.size() > 0)
                                        {
                                            //create train and test Martices
                                            Mat testDataMat = buildTrainOrTestMatForScrollFling(scrollFlingObservations);
                                            Mat resultMat = new Mat(scrollFlingObservations.size(), 1, CvType.CV_32S);

                                            System.out.println("Scroll Fling Test Data Mat: ");
                                            //displayMatrix(buildTrainOrTestMatForScrollFling(scrollFlingObservations));

                                            // SVM
                                            scrollFlingSVM.predict(testDataMat, resultMat, 0);
                                            int counter = countOwnerResults(resultMat);

                                            System.out.println("Counter: " + counter);

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
                                        ref.changePassword(userEmail, oldPasswordStr, newPasswordStr, new Firebase.ResultHandler()
                                        {
                                            @Override
                                            public void onSuccess()
                                            {
                                                Toast.makeText(getApplicationContext(), "Password successfully changed", Toast.LENGTH_SHORT).show();

                                                Intent trainIntent = new Intent(ChangePassword.this, OptionsScreen.class);
                                                startActivity(trainIntent);
                                            }

                                            @Override
                                            public void onError(FirebaseError firebaseError)
                                            {
                                                Toast.makeText(getApplicationContext(), "Password could not be changed.", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                    else
                                    {
                                        Toast.makeText(getApplicationContext(), "Password could not be changed.", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onCancelled(FirebaseError firebaseError)
                                {
                                    System.out.println("The read failed: " + firebaseError.getMessage());
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

    private int countOwnerResults(Mat mat)
    {
        int counter = 0;
        for (int i = 0; i < mat.rows(); i++)
        {
            if (mat.get(i, 0)[0] == 1) counter++;
        }

        return counter;
    }

    private void getUserSettings()
    {
        final Firebase settingsRef = new Firebase("https://fyp-max.firebaseio.com/settings/" + userID);
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
                } else
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

            }
        });
    }

    private void getTrainDataFromFirebase()
    {
        final Firebase scrollFlingRef = new Firebase("https://fyp-max.firebaseio.com/trainData");
        scrollFlingRef.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot snapshot)
            {
                if (snapshot.getValue() == null)
                {
                    System.out.println("No Scroll Fling data available. ");
                    Toast toast = Toast.makeText(getApplicationContext(), "No Test data Provided", Toast.LENGTH_SHORT);
                    toast.show();
                } else
                {
                    for (DataSnapshot usrSnapshot : snapshot.getChildren())
                    {
                        //System.out.println("usrSnapshot: " + usrSnapshot.child("scrollFling"));

                        if (!usrSnapshot.getKey().equals(userID))
                        {
                            // Scroll/Fling:
                            DataSnapshot dpScroll = usrSnapshot.child("scrollFling");
                            int countGuestObs = 0;
                            for (DataSnapshot obsSnapshot : dpScroll.getChildren())
                            {
                                Observation obs = obsSnapshot.getValue(Observation.class);
                                obs.setJudgement(0);
                                if (countGuestObs < guestsObservationsNeeded)
                                    trainScrollFlingObservations.add(obs);
                                countGuestObs++;
                            }

                        } else  // get data from the actual user
                        {
                            DataSnapshot scrollSnapshot = usrSnapshot.child("scrollFling");
                            for (DataSnapshot obsSnapshot : scrollSnapshot.getChildren())
                            {
                                Observation obs = obsSnapshot.getValue(Observation.class);
                                trainScrollFlingObservations.add(obs);
                            }

                            // Built the SVM model for Scroll/Fling Observations if training data exists.
                            if (trainScrollFlingObservations.size() > 0)
                            {
                                scrollFlingSVM = createAndTrainScrollFlingSVMClassifier(trainScrollFlingObservations);

                            } else
                            {
                                System.out.println("No Scroll Fling data available. ");
                                // display a Toast letting the user know that there is no training data available.
                                Toast toast = Toast.makeText(getApplicationContext(), "No Training data Provided", Toast.LENGTH_SHORT);
                                toast.show();
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError)
            {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });
    }

    private SVM createAndTrainScrollFlingSVMClassifier(ArrayList<Observation> arrayListObservations)
    {
        //initialise scrollFlingSVM
        SVM tempSVM = SVM.create();
        tempSVM.setKernel(SVM.CHI2);

        tempSVM.setType(SVM.C_SVC);
        tempSVM.setC(10.55);
        tempSVM.setGamma(0.15);

        Mat trainScrollFlingMat = buildTrainOrTestMatForScrollFling(arrayListObservations);
        Mat labelsScrollFlingMat = buildLabelsMat(arrayListObservations);

        //System.out.println("Train Matrix is:\n");
        //displayMatrix(trainScrollFlingMat);

        tempSVM.train(trainScrollFlingMat, Ml.ROW_SAMPLE, labelsScrollFlingMat);

        return tempSVM;
    }

    private Mat buildLabelsMat(ArrayList<Observation> listObservations)
    {
        Mat labelsTempMat = new Mat(listObservations.size(), 1, CvType.CV_32S);

        for(int i = 0; i < listObservations.size(); i++)
        {
            labelsTempMat.put(i, 0, listObservations.get(i).getJudgement());
        }

        return labelsTempMat;
    }

    private Mat buildTrainOrTestMatForScrollFling(ArrayList<Observation> listObservations)
    {
        Mat tempMat = new Mat(listObservations.size(), ScrollFling.numberOfFeatures, CvType.CV_32FC1);

        for(int i = 0; i < listObservations.size(); i++)
        {
            ScrollFling scrollFlingObs = new ScrollFling(listObservations.get(i).getTouch());
            int j = 0;

            // linear accelerations are part of the observation - get average
            tempMat.put(i, j++, listObservations.get(i).getAverageLinearAcceleration());

            // angular Velocity are part of the observation - get average
            tempMat.put(i, j++, listObservations.get(i).getAverageAngularVelocity());

            tempMat.put(i, j++, scrollFlingObs.getMidStrokeAreaCovered());

            // Angle between start and end vectors
            tempMat.put(i, j++, scrollFlingObs.getAngleBetweenStartAndEndVectorsInRad());

            tempMat.put(i, j++, scrollFlingObs.getDirectEndToEndDistance());

            // Mean Direction
            //tempMat.put(i, j++, scrollFlingObs.getMeanDirectionOfStroke());

            // Stop x
            tempMat.put(i, j++, scrollFlingObs.getScaledEndPoint().x);

            // Start x
            tempMat.put(i, j++, scrollFlingObs.getScaledStartPoint().x);

            // Stroke Duration
            tempMat.put(i, j++, scrollFlingObs.getScaledDuration()/10);

            // Start y
            tempMat.put(i, j++, scrollFlingObs.getScaledStartPoint().y);

            // Stop y
            tempMat.put(i, j, scrollFlingObs.getScaledEndPoint().y);
        }
        return tempMat;
    }
}
