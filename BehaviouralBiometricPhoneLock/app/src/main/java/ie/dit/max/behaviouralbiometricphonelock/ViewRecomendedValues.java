package ie.dit.max.behaviouralbiometricphonelock;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.ml.SVM;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * In this activity, the user can see the performance of the classification model
 *  and it can see different values recommended by the system for the threshold and nr of observations
 *  from other users used in the train model.
 *
 *  The values calculated and displayed to the user includes the accuracy of the model for his/hers data (interactions)
 *  and the errors calculated when the system was tested with data from other users.
 *
 *  Smaller average Error, make the system recognise intruders faster.
 *  Greater Accuracy, makes the system recognise owner's interactions better.
 *
 * @author Maximilian Mihoc.
 * @version 1.0
 * @since 22th March 2016
 *
 */
public class ViewRecomendedValues extends AppCompatActivity
{
    private static final String DEBUG_TAG = "View Recommended Values Activity";

    private ArrayList<Observation> trainScrollFlingObservations;
    private ArrayList<Observation> scrollFlingObservations;
    private HashMap<String, ArrayList<Observation>> trainDataMapScrollFling;
    private HashMap<String, ArrayList<Observation>> testDataMapScrollFling;
    private String userID;
    private SVM scrollFlingSVM;
    private String[] userKeys;
    private String[] userNames;
    private TextView scrollSVMTextView;
    private TextView displayMinMaxValues;
    private TextView currentUserSettings;
    private ProgressBar progressBarScrollSVM;
    private ProgressBar loadingPanel;

    private UserSettings userSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_recomended_values);

        Firebase.setAndroidContext(this);

        // get User details
        SharedPreferences sharedpreferences = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        if(sharedpreferences.contains("UserID")) userID = sharedpreferences.getString("UserID", "");

        scrollFlingObservations = new ArrayList<>();
        trainScrollFlingObservations = new ArrayList<>();
        trainDataMapScrollFling = new HashMap<>();
        testDataMapScrollFling = new HashMap<>();

        scrollSVMTextView = (TextView) findViewById(R.id.predictions);
        displayMinMaxValues = (TextView) findViewById(R.id.displayMinMaxValues);
        currentUserSettings = (TextView) findViewById(R.id.textView);
        progressBarScrollSVM = (ProgressBar) findViewById(R.id.progressBar);
        loadingPanel = (ProgressBar) findViewById(R.id.loadingPanel);

        // populate users arrays from Database
        populateUserArrays();
    }

    /**
     * Method populateUserArrays
     * Gets all the users from the database and populates 2 arrays.
     * One array contains userIDs and the other contains UserNames
     **/
    private void populateUserArrays()
    {
        Firebase userRef = new Firebase(DBVar.mainURL + "/users");
        userRef.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot snapshot)
            {
                userKeys = new String[(int)snapshot.getChildrenCount()];
                userNames = new String[(int)snapshot.getChildrenCount()];
                int i = 0;
                for (DataSnapshot usrSnapshot : snapshot.getChildren())
                {
                    User u = usrSnapshot.getValue(User.class);
                    userKeys[i] = u.getUserID();
                    userNames[i++] = u.getUserName();
                }
                // get user settings for the current user
                getUserSettings();
            }

            @Override
            public void onCancelled(FirebaseError firebaseError)
            {
                System.out.println(DEBUG_TAG + "The read failed: " + firebaseError.getMessage());
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
                    userSettings = new UserSettings();
                    userSettings.setThreshold(70);
                    userSettings.setNrObsFromAnotherUser(5);
                    userSettings.setSaveTestData(true);
                }
                else
                {
                    userSettings = dataSnapshot.getValue(UserSettings.class);
                }

                // display User Settings to screen so that they can be seen by the user
                currentUserSettings.setText("Current Setting " +
                        "\nThreshold value: " + userSettings.getThreshold() +
                        "\nNumber Of Observations: " + userSettings.getNrObsFromAnotherUser());

                // get train data for all users from database
                getTrainDataFromUsersFirebase();
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
    private void getTrainDataFromUsersFirebase()
    {
        final Firebase scrollFlingRef = new Firebase(DBVar.mainURL + "/trainData");
        scrollFlingRef.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot snapshot)
            {
                if (snapshot.getValue() == null)
                {
                    System.out.println(DEBUG_TAG + "No data available for this user. ");
                    Toast toast = Toast.makeText(getApplicationContext(), "No Train data Provided", Toast.LENGTH_SHORT);
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
                            int i = 0;
                            // Guests data
                            DataSnapshot dpScroll = usrSnapshot.child("scrollFling");
                            for (DataSnapshot obsSnapshot : dpScroll.getChildren())
                            {
                                Observation obs = obsSnapshot.getValue(Observation.class);
                                obs.setJudgement(0);
                                if (i < userSettings.getNrObsFromAnotherUser()) trainScrollFlingObservations.add(obs);
                                i++;
                            }
                        }
                        else // Current user Data
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
                        scrollFlingSVM = Classifier.createAndTrainScrollFlingSVMClassifier(trainScrollFlingObservations);
                    }
                    else
                    {
                        // display a Toast letting the user know that there is no training data available.
                        Toast toast = Toast.makeText(getApplicationContext(), "No Training data Provided", Toast.LENGTH_SHORT);
                        toast.show();
                    }

                    /* Get test data from Firebase and display predictions. */
                    getTestDataFromFirebaseAndTestSystem(userID);
                    getTrainDataForAllUsersFromFirebase();

                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError)
            {
                System.out.println(DEBUG_TAG + "The read failed: " + firebaseError.getMessage());
            }
        });
    }

    /**
     *
     * Method getTestDataFromFirebaseAndTestSystem
     * This method gets the test data collected for current user in the database
     *      and displays the predictions in a progress bar.
     *
     * @param userID String
     */
    private void getTestDataFromFirebaseAndTestSystem(String userID)
    {
        //get Scroll Fling Observations from Firebase
        Firebase scrollFlingRef = new Firebase(DBVar.mainURL + "/testData/" + userID);
        scrollFlingRef.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot snapshot)
            {
                if (snapshot.getValue() == null)
                {
                    Toast toast = Toast.makeText(getApplicationContext(), "No Test data Provided", Toast.LENGTH_SHORT);
                    toast.show();
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
                        Mat testDataMat = Classifier.buildTrainOrTestMatForScrollFling(scrollFlingObservations);
                        Mat resultMat = new Mat(scrollFlingObservations.size(), 1, CvType.CV_32S);

                        // SVM
                        scrollFlingSVM.predict(testDataMat, resultMat, 0);
                        int counter = Classifier.countOwnerResults(resultMat);

                        if(Math.round((counter * 100) / scrollFlingObservations.size()) < 50)
                            scrollSVMTextView.setText("Test Data Validation for current Settings\n" + counter + " / " + scrollFlingObservations.size()
                                    + " -> " + Math.round((counter * 100) / scrollFlingObservations.size()) + "%" +
                                    "Settings values need to be changed to better values. " +
                                    "\nA model Re-train is also recommended." +
                                    "\nTrain Data can be also deleted before training.");
                        else
                            scrollSVMTextView.setText("Test Data Validation for current Settings\n" + counter + " / " + scrollFlingObservations.size()
                                    + " -> " + Math.round((counter * 100) / scrollFlingObservations.size()) + "%");

                        progressBarScrollSVM.setMax(scrollFlingObservations.size());
                        progressBarScrollSVM.setProgress(counter);
                    }
                    else
                    {
                        Toast toast = Toast.makeText(getApplicationContext(), "No Test data Provided for Scroll/Fling", Toast.LENGTH_SHORT);
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

    /**
     * Method getTestDataForAllUsersFromFirebaseAndTestSystem
     * This method gets the test data collected for all the users in the database
     *
     *  For each user, the test data is checked against the trained model for the current user
     *      in order to calculate the errors and accuracy of the model with different parameters.
     *
     */
    private void getTestDataForAllUsersFromFirebaseAndTestSystem()
    {
        Firebase scrollFlingRef = new Firebase(DBVar.mainURL + "/testData");
        scrollFlingRef.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot snapshot)
            {
                if (snapshot.getValue() == null)
                {
                    Toast toast = Toast.makeText(getApplicationContext(), "No Test data Provided", Toast.LENGTH_SHORT);
                    toast.show();
                }
                else
                {
                    // Scroll Fling Test Data
                    for (DataSnapshot usrSnapshot : snapshot.getChildren())
                    {
                        ArrayList<Observation> tempArrayObs = new ArrayList<>();
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

                    // This loop builds the train model 90 times and checks the errors and average accuracy of the model each time.
                    // Based on the calculated values, the parameters obtained when the minimum error happened and the parameters for the
                    // maximum average returned for Owner predictions are saved. These values are then displayed to the user in order to be
                    // used in the settings screen to create better classifiers for the system.
                    for (int i = 10; i <= 100; i++)
                    {
                        retValuesValidation = computeValidationForOneUserAgainstAllOthers(userID, i);

                        if (retValuesValidation.average < min)
                        {
                            min = retValuesValidation.average;
                            bestValueForObsNumbers = i;
                            percentOnMinAvg = retValuesValidation.ownerPercent;
                        }

                        if (retValuesValidation.ownerPercent > max && retValuesValidation.average < 50)
                        {
                            max = retValuesValidation.ownerPercent;
                            nrObsAtMaxOwnerPercent = i;
                        }
                    }

                    // calculate the average for the max Owner Percent
                    ReturnValues tempRV = computeValidationForOneUserAgainstAllOthers(userID, nrObsAtMaxOwnerPercent);

                    String minMaxValues = "\n\n";
                    minMaxValues += "For " + bestValueForObsNumbers + " observations, the Minimum Average Error " + min + " was computed, with a Percent accuracy of " + percentOnMinAvg + "%";
                    minMaxValues += "\n\nFor " + nrObsAtMaxOwnerPercent + " observations, the Average error is " + tempRV.average + "  with a Percent accuracy of " + tempRV.ownerPercent + "%";

                    // display results for current values
                    ReturnValues rvCurrent = computeValidationForOneUserAgainstAllOthers(userID, userSettings.getNrObsFromAnotherUser());
                    minMaxValues += "\n\nCurrent Settings\n" + "For " + userSettings.getNrObsFromAnotherUser() + " observations, Average Error is " + rvCurrent.average;

                    displayMinMaxValues.setText(minMaxValues);
                    loadingPanel.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError)
            {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });
    }

    /**
     * This class contains the values returned by the computeValidationForOneUserAgainstAllOthers method
     * in order to calculate min and max values that will be returned to the screen.
     *
     */
    final class ReturnValues
    {
        float average;
        float ownerPercent;
    }

    /**
     * Method getTrainDataForAllUsersFromFirebase
     *
     * Get train data for all users from the database and place it into a HashMap
     *
     */
    private void getTrainDataForAllUsersFromFirebase()
    {
        final Firebase scrollFlingRef = new Firebase(DBVar.mainURL + "/trainData");
        scrollFlingRef.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot snapshot)
            {
                if (snapshot.getValue() == null)
                {
                    Toast toast = Toast.makeText(getApplicationContext(), "No Train data Provided", Toast.LENGTH_SHORT);
                    toast.show();
                }
                else
                {
                    for (DataSnapshot usrSnapshot : snapshot.getChildren())
                    {
                        ArrayList<Observation> tempArrayObs = new ArrayList<>();
                        DataSnapshot dpScroll = usrSnapshot.child("scrollFling");
                        for (DataSnapshot obsSnapshot : dpScroll.getChildren())
                        {
                            Observation obs = obsSnapshot.getValue(Observation.class);
                            tempArrayObs.add(obs);
                        }
                        //add train data to HashMap
                        trainDataMapScrollFling.put(usrSnapshot.getKey(), tempArrayObs);
                    }
                    getTestDataForAllUsersFromFirebaseAndTestSystem();
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError)
            {
                System.out.println(DEBUG_TAG + "The read failed: " + firebaseError.getMessage());
            }
        });
    }

    /**
     * Method computeValidationForOneUserAgainstAllOthers
     * This method is used to create train models for the user specified in the forUserID parameter using the number of observations
     *      specified by the numberObsNeededFromOtherUsers parameter for getting data from other users.
     *
     * For each user in the database, the test data is checked against the train model and error and prediction values are returned.
     *
     * @param forUserID String
     * @param numberObsNeededFromOtherUsers int
     * @return ReturnValues
     */
    private ReturnValues computeValidationForOneUserAgainstAllOthers(String forUserID, int numberObsNeededFromOtherUsers)
    {
        ArrayList<Observation> trainScrollFlingDataForUserID = new ArrayList<>();

        // Build the train data for the user with userId = forUserID
        for( HashMap.Entry<String, ArrayList<Observation>> trainDataEntry : trainDataMapScrollFling.entrySet())
        {
            if(trainDataEntry.getKey().equals(forUserID))
            {
                ArrayList<Observation> tempList = Classifier.changeJudgements(trainDataEntry.getValue(), 1);
                trainScrollFlingDataForUserID.addAll(tempList);
            }
            else
            {
                ArrayList<Observation> tempList = Classifier.changeJudgements(trainDataEntry.getValue(), 0);
                if(tempList.size() > 0)
                {
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
        SVM tempScrollFlingSVM = Classifier.createAndTrainScrollFlingSVMClassifier(trainScrollFlingDataForUserID);

        int numberOfUsersWithTestData = 0;
        float sumOfPercentages = 0;
        float ownerPercent = 0;

        ReturnValues rV = new ReturnValues();

        // test the new SVM model with the test data
        for(int i = 0; i < userKeys.length; i++)
        {
            ArrayList<Observation> testData = testDataMapScrollFling.get(userKeys[i]);

            if(testData != null)
            {
                Mat testDataMat = Classifier.buildTrainOrTestMatForScrollFling(testData);
                Mat resultMat = new Mat(testData.size(), 1, CvType.CV_32S);

                tempScrollFlingSVM.predict(testDataMat, resultMat, 0);
                int counter = Classifier.countOwnerResults(resultMat);

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

            }
        }

        rV.average = sumOfPercentages/numberOfUsersWithTestData;
        rV.ownerPercent = ownerPercent;

        return rV;
    }

}
