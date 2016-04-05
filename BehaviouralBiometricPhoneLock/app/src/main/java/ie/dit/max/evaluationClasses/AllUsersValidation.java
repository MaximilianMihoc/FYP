package ie.dit.max.evaluationClasses;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.TermCriteria;
import org.opencv.ml.KNearest;
import org.opencv.ml.Ml;
import org.opencv.ml.RTrees;
import org.opencv.ml.SVM;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import ie.dit.max.behaviouralbiometricphonelock.Classifier;
import ie.dit.max.behaviouralbiometricphonelock.Observation;
import ie.dit.max.behaviouralbiometricphonelock.R;
import ie.dit.max.behaviouralbiometricphonelock.ScrollFling;
import ie.dit.max.behaviouralbiometricphonelock.User;
import ie.dit.max.behaviouralbiometricphonelock.UserSettings;

public class AllUsersValidation extends AppCompatActivity
{
    private final class ReturnValues
    {
        float average;
        float ownerPercent;
    }

    private Firebase ref;
    private static final String DEBUG_TAG = "Classifiers";

    private HashMap<String, ArrayList<Observation>> trainDataMapScrollFling;
    private Map<String, ArrayList<Observation>> testDataMapScrollFling;

    private String userID;
    private String userName;

    private String[] userKeys;
    private String[] userNames;

    private TextView validationText;
    private TextView displayMinMaxValues;
    private Button buttonChange;

    private Classifier classifier;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_users_validation);
        Firebase.setAndroidContext(this);
        ref = new Firebase("https://fyp-max.firebaseio.com");

        SharedPreferences sharedpreferences = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        if(sharedpreferences.contains("ValidateDataForUserID")) userID = sharedpreferences.getString("ValidateDataForUserID", "");
        if(sharedpreferences.contains("ValidateDataForUserName")) userName = sharedpreferences.getString("ValidateDataForUserName", "");

        classifier = new Classifier();
        validationText = (TextView) findViewById(R.id.validationText);
        displayMinMaxValues = (TextView) findViewById(R.id.displayMinMaxValues);
        buttonChange = (Button) findViewById(R.id.buttonChange);

        trainDataMapScrollFling = new HashMap<>();
        testDataMapScrollFling = new HashMap<>();

        populateUserArrays();

        buttonChange.setText("Check Errors");
        buttonChange.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // check Errors for number of observations from settings
                final Firebase settingsRef = new Firebase("https://fyp-max.firebaseio.com/settings/" + userID);
                settingsRef.addListenerForSingleValueEvent(new ValueEventListener()
                {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        if (dataSnapshot.getValue() == null)
                        {
                            computeValidationForOneUserAgainstAllOthers(userID, userName, 4, true);
                        }
                        else
                        {
                            UserSettings userSettings = dataSnapshot.getValue(UserSettings.class);
                            computeValidationForOneUserAgainstAllOthers(userID, userName, userSettings.getNrObsFromAnotherUser(), true);
                        }

                    }

                    @Override
                    public void onCancelled(FirebaseError firebaseError)
                    {

                    }
                });
            }
        });

    }

    private void populateUserArrays()
    {
        Firebase userRef = new Firebase("https://fyp-max.firebaseio.com/users");

        userRef.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot snapshot)
            {
                userKeys = new String[(int) snapshot.getChildrenCount()];
                userNames = new String[(int) snapshot.getChildrenCount()];
                int i = 0;
                for (DataSnapshot usrSnapshot : snapshot.getChildren())
                {
                    User u = usrSnapshot.getValue(User.class);
                    userKeys[i] = u.getUserID();
                    userNames[i++] = u.getUserName(); //"User " + i; //
                }

                getTrainDataFromUsersFirebase();

            }

            @Override
            public void onCancelled(FirebaseError firebaseError)
            {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });
    }

    private void getTrainDataFromUsersFirebase()
    {
        final Firebase scrollFlingRef = new Firebase("https://fyp-max.firebaseio.com/trainData");
        scrollFlingRef.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot snapshot)
            {
                if (snapshot.getValue() == null)
                {
                    System.out.println("No data available for this user. ");
                    Toast toast = Toast.makeText(getApplicationContext(), "No Train data Provided", Toast.LENGTH_SHORT);
                    toast.show();
                } else
                {
                    for (DataSnapshot usrSnapshot : snapshot.getChildren())
                    {

                        ArrayList<Observation> tempArrayObs = new ArrayList<>();
                        // Scroll/Fling:
                        DataSnapshot dpScroll = usrSnapshot.child("scrollFling");
                        for (DataSnapshot obsSnapshot : dpScroll.getChildren())
                        {
                            Observation obs = obsSnapshot.getValue(Observation.class);
                            tempArrayObs.add(obs);
                        }
                        //add train data to HashMap
                        trainDataMapScrollFling.put(usrSnapshot.getKey(), tempArrayObs);

                    }

                    /* Get test data from firebase and return predictions. */
                    getTestDataFromFirebaseAndTestSystem();

                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError)
            {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });
    }

    // get all train data from Firebase
    private void getTestDataFromFirebaseAndTestSystem()
    {
        Firebase scrollFlingRef = new Firebase("https://fyp-max.firebaseio.com/testData");
        scrollFlingRef.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot snapshot)
            {
                if (snapshot.getValue() == null)
                {
                    System.out.println("No data available for this User ");
                    Toast toast = Toast.makeText(getApplicationContext(), "No Test data Provided", Toast.LENGTH_SHORT);
                    toast.show();
                } else
                {
                    // Scroll Fling Test Data
                    for (DataSnapshot usrSnapshot : snapshot.getChildren())
                    {
                        ArrayList<Observation> tempArrayObs = new ArrayList<>();
                        // Scroll/Fling:
                        DataSnapshot dpScroll = usrSnapshot.child("scrollFling");
                        for (DataSnapshot obsSnapshot : dpScroll.getChildren())
                        {
                            Observation obs = obsSnapshot.getValue(Observation.class);
                            tempArrayObs.add(obs);
                        }
                        //add test data to HashMap
                        testDataMapScrollFling.put(usrSnapshot.getKey(), tempArrayObs);
                    }

                    //calculate the best number of Observations needed from other users.
                    float min = 100, max = 0, percentOnMinAvg = 0;
                    int bestValueForObsNumbers = 1;
                    int nrObsAtMaxOwnerPercent = 1;


                    ReturnValues retValuesValidation;

                    for (int i = 10; i <= 100; i++)
                    {
                        retValuesValidation = computeValidationForOneUserAgainstAllOthers(userID, userName, i, false);
                        //System.out.println("Average: " + retValuesValidation.average);

                        if (retValuesValidation.average < min) //&& retValuesValidation.ownerPercent >= 70)
                        {
                            min = retValuesValidation.average;
                            bestValueForObsNumbers = i;
                            percentOnMinAvg = retValuesValidation.ownerPercent;
                        }

                        if (retValuesValidation.ownerPercent > max && retValuesValidation.average < 50) //average error less than 50%
                        {
                            max = retValuesValidation.ownerPercent;
                            nrObsAtMaxOwnerPercent = i;
                        }
                    }

                    // calculate the average for the max Owner Percent
                    ReturnValues tempRV = computeValidationForOneUserAgainstAllOthers(userID, userName, nrObsAtMaxOwnerPercent, false);

                    // display results for best value here
                    computeValidationForOneUserAgainstAllOthers(userID, userName, bestValueForObsNumbers, true);

                    String minMaxValues = "\n\n";
                    minMaxValues += "Min Average Error " + min + " for " + bestValueForObsNumbers + " observations with Percent accuracy: " + percentOnMinAvg;
                    minMaxValues += "\n\nAverage error " + tempRV.average + " for " + nrObsAtMaxOwnerPercent + " observations when Owner accuracy is: " + tempRV.ownerPercent;

                    displayMinMaxValues.setText(minMaxValues);

                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError)
            {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });
    }

    // this method builds a confidence matrix and displays it on console
    private void buildAndDisplayConfidenceMatrix()
    {
        for(int i = 0; i < userKeys.length; i++)
        {
            computeValidationForOneUserAgainstAllOthers(userKeys[i], userNames[i], 4, false);
        }

    }

    private ReturnValues computeValidationForOneUserAgainstAllOthers(String forUserID, String forUserName, int numberObsNeededFromOtherUsers, boolean displayOutput)
    {
        ArrayList<Observation> trainScrollFlingDataForUserID = new ArrayList<>();

        // Build the train data for the user with userId = forUserID
        for( HashMap.Entry<String, ArrayList<Observation>> trainDataEntry : trainDataMapScrollFling.entrySet())
        {
            if(trainDataEntry.getKey().equals(forUserID))
            {
                ArrayList<Observation> tempList = classifier.changeJudgements(trainDataEntry.getValue(), 1);
                trainScrollFlingDataForUserID.addAll(tempList);
            }
            else
            {
                ArrayList<Observation> tempList = classifier.changeJudgements(trainDataEntry.getValue(), 0);
                if(tempList.size() > 0)
                {
                    //trainScrollFlingDataForUserID.addAll(tempList);
                    // add 10 observations from each other user in the train list
                    for (int i = 0; i < tempList.size(); i++)
                    {
                        if (i < numberObsNeededFromOtherUsers)
                        {
                            trainScrollFlingDataForUserID.add(tempList.get(i));
                        }
                    }
                }
            }
        }

        //create and train the SVM model
        SVM tempScrollFlingSVM = classifier.createAndTrainScrollFlingSVMClassifier(trainScrollFlingDataForUserID);

        int numberOfUsersWithTestData = 0;
        float sumOfPercentages = 0;
        float ownerPercent = 0;

        ReturnValues rV = new ReturnValues();

        String output = "For " + forUserName + "\n # of Observations From Other users needed: " + (numberObsNeededFromOtherUsers + 1) +"\n";
        String confusionMatrixRow = "For usr: ";

        // test the new SVM model with the test data
        for(int i = 0; i < userKeys.length; i++)
        {
            ArrayList<Observation> testData = testDataMapScrollFling.get(userKeys[i]);

            if(testData != null)
            {
                Mat testDataMat = classifier.buildTrainOrTestMatForScrollFling(testData);
                Mat resultMat = new Mat(testData.size(), 1, CvType.CV_32S);

                tempScrollFlingSVM.predict(testDataMat, resultMat, 0);
                int counter = classifier.countOwnerResults(resultMat);

                if(!userKeys[i].equals(forUserID))
                {
                    sumOfPercentages += (counter * 100) / testData.size();
                    numberOfUsersWithTestData++;
                }
                else
                {
                    if(ownerPercent < ((counter * 100) / testData.size()))
                    {
                        ownerPercent = (counter * 100) / testData.size();
                    }
                }

                if (i < userKeys.length - 1)
                    confusionMatrixRow += counter + ",";
                else
                    confusionMatrixRow += counter + "";

                if(displayOutput)
                    output += (i+1) + ". " + userNames[i] + ": SVM Scroll/Fling -> " + counter + " / " + testData.size() + " -> " + Math.round((counter * 100) / testData.size()) + "%\n";
            }
        }

        if(displayOutput) validationText.setText(output);

        //System.out.println(confusionMatrixRow);

        rV.average = sumOfPercentages/numberOfUsersWithTestData;
        rV.ownerPercent = ownerPercent;

        return rV;
    }
}
