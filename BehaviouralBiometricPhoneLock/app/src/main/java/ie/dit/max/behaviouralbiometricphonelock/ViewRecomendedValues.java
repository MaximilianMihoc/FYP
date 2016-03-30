package ie.dit.max.behaviouralbiometricphonelock;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.ml.Ml;
import org.opencv.ml.SVM;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ViewRecomendedValues extends AppCompatActivity
{
    Firebase ref;
    ArrayList<Observation> trainScrollFlingObservations;
    ArrayList<Observation> scrollFlingObservations;

    HashMap<String, ArrayList<Observation>> trainDataMapScrollFling;
    HashMap<String, ArrayList<Observation>> testDataMapScrollFling;

    private String userID;
    private String userName;
    private SVM scrollFlingSVM;

    String[] userKeys;
    String[] userNames;

    TextView scrollSVMTextView;
    TextView displayMinMaxValues;
    TextView currentUserSettings;

    ProgressBar progressBarScrollSVM;
    ProgressBar loadingPanel;

    SharedPreferences sharedpreferences;
    private UserSettings userSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_recomended_values);
        Firebase.setAndroidContext(this);
        ref = new Firebase("https://fyp-max.firebaseio.com");

        sharedpreferences = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        if(sharedpreferences.contains("UserID")) userID = sharedpreferences.getString("UserID", "");
        if(sharedpreferences.contains("ValidateDataForUserName")) userName = sharedpreferences.getString("ValidateDataForUserName", "");

        scrollFlingObservations = new ArrayList<>();
        trainScrollFlingObservations = new ArrayList<>();
        trainDataMapScrollFling = new HashMap<>();
        testDataMapScrollFling = new HashMap<>();

        scrollSVMTextView = (TextView) findViewById(R.id.predictions);
        displayMinMaxValues = (TextView) findViewById(R.id.displayMinMaxValues);
        currentUserSettings = (TextView) findViewById(R.id.textView);

        progressBarScrollSVM = (ProgressBar) findViewById(R.id.progressBar);
        loadingPanel = (ProgressBar) findViewById(R.id.loadingPanel);

        populateUserArrays();
    }

    private void populateUserArrays()
    {
        Firebase userRef = new Firebase("https://fyp-max.firebaseio.com/users");
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
                getUserSettings();
            }

            @Override
            public void onCancelled(FirebaseError firebaseError)
            {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });
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
                    userSettings = new UserSettings();
                    userSettings.setThreshold(70);
                    userSettings.setNrObsFromAnotherUser(5);
                    userSettings.setSaveTestData(true);
                }
                else
                {
                    userSettings = dataSnapshot.getValue(UserSettings.class);
                }

                currentUserSettings.setText("Current Setting " +
                        "\nThreshold value: " + userSettings.getThreshold() +
                        "\nNumber Of Observations: " + userSettings.getNrObsFromAnotherUser());
                getTrainDataFromUsersFirebase();
            }

            @Override
            public void onCancelled(FirebaseError firebaseError)
            {

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
                }
                else
                {
                    for (DataSnapshot usrSnapshot : snapshot.getChildren())
                    {
                        if (!usrSnapshot.getKey().equals(userID))
                        {
                            int i = 0;
                            // Scroll/Fling:
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
                            int trainObsOwnerCount = 0;
                            for (DataSnapshot obsSnapshot : scrollSnapshot.getChildren())
                            {
                                //System.out.println("data: " + obsSnapshot.toString());
                                Observation obs = obsSnapshot.getValue(Observation.class);
                                trainScrollFlingObservations.add(obs);
                                trainObsOwnerCount ++;
                            }
                        }
                    }

                    // Built the SVM model for Scroll/Fling Observations if training data exists.
                    if (trainScrollFlingObservations.size() > 0)
                    {
                        scrollFlingSVM = createAndTrainScrollFlingSVMClassifier(trainScrollFlingObservations);
                    }
                    else
                    {
                        System.out.println("No Scroll Fling data available. ");
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
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });
    }

    private void getTestDataFromFirebaseAndTestSystem(String userID)
    {
        //get Scroll Fling Observations from Firebase
        Firebase scrollFlingRef = new Firebase("https://fyp-max.firebaseio.com/testData/" + userID);
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
                }
                else
                {
                    // Scroll Fling Test Data
                    DataSnapshot dpScroll = snapshot.child("scrollFling");
                    for (DataSnapshot obsSnapshot : dpScroll.getChildren())
                    {
                        //System.out.println("data: " + obsSnapshot.toString());
                        Observation obs = obsSnapshot.getValue(Observation.class);
                        scrollFlingObservations.add(obs);
                    }

                    if (scrollFlingObservations.size() > 0)
                    {
                        //create train and test Martices
                        Mat testDataMat = buildTrainOrTestMatForScrollFling(scrollFlingObservations);
                        Mat resultMat = new Mat(scrollFlingObservations.size(), 1, CvType.CV_32S);

                        // SVM
                        scrollFlingSVM.predict(testDataMat, resultMat, 0);
                        int counter = countOwnerResults(resultMat);
                        scrollSVMTextView.setText("Test Data Validation for current Settings\n" + counter + " / " + scrollFlingObservations.size()
                                + " -> " + Math.round((counter * 100) / scrollFlingObservations.size()) + "%");
                        progressBarScrollSVM.setMax(scrollFlingObservations.size());
                        progressBarScrollSVM.setProgress(counter);


                    } else
                    {
                        System.out.println("No Scroll Fling data available. ");
                        Toast toast = Toast.makeText(getApplicationContext(), "No Test data Provided for Scroll/Fling", Toast.LENGTH_SHORT);
                        toast.show();
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

    private void getTestDataForAllUsersFromFirebaseAndTestSystem()
    {
        Firebase scrollFlingRef = new Firebase("https://fyp-max.firebaseio.com/testData");
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
                        retValuesValidation = computeValidationForOneUserAgainstAllOthers(userID, userName, i);
                        //System.out.println("Average: " + retValuesValidation.average);

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
                    ReturnValues tempRV = computeValidationForOneUserAgainstAllOthers(userID, userName, nrObsAtMaxOwnerPercent);

                    String minMaxValues = "\n\n";
                    minMaxValues += "For " + bestValueForObsNumbers + " observations, the Minimum Average Error " + min +  " was computed, with a Percent accuracy of " + percentOnMinAvg + "%";
                    minMaxValues += "\n\nFor " + nrObsAtMaxOwnerPercent +  " observations, the Average error is " + tempRV.average + "  with a Percent accuracy of " + tempRV.ownerPercent + "%";

                    // display results for current values
                    ReturnValues rvCurrent = computeValidationForOneUserAgainstAllOthers(userID, userName, userSettings.getNrObsFromAnotherUser());
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

    final class ReturnValues
    {
        float average;
        float ownerPercent;
    }

    private void getTrainDataForAllUsersFromFirebase()
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

                    getTestDataForAllUsersFromFirebaseAndTestSystem();

                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError)
            {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });
    }

    private ReturnValues computeValidationForOneUserAgainstAllOthers(String forUserID, String forUserName, int numberObsNeededFromOtherUsers)
    {
        ArrayList<Observation> trainScrollFlingDataForUserID = new ArrayList<>();

        // Build the train data for the user with userId = forUserID
        for( HashMap.Entry<String, ArrayList<Observation>> trainDataEntry : trainDataMapScrollFling.entrySet())
        {
            if(trainDataEntry.getKey().equals(forUserID))
            {
                ArrayList<Observation> tempList = changeJudgements(trainDataEntry.getValue(), 1);
                trainScrollFlingDataForUserID.addAll(tempList);
            }
            else
            {
                ArrayList<Observation> tempList = changeJudgements(trainDataEntry.getValue(), 0);
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
        SVM tempScrollFlingSVM = createAndTrainScrollFlingSVMClassifier(trainScrollFlingDataForUserID);

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
                Mat testDataMat = buildTrainOrTestMatForScrollFling(testData);
                Mat resultMat = new Mat(testData.size(), 1, CvType.CV_32S);

                tempScrollFlingSVM.predict(testDataMat, resultMat, 0);
                int counter = countOwnerResults(resultMat);

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

    private int countOwnerResults(Mat mat)
    {
        int counter = 0;
        for (int i = 0; i < mat.rows(); i++)
        {
            if (mat.get(i, 0)[0] == 1) counter++;
        }

        return counter;
    }

    private ArrayList<Observation> changeJudgements(ArrayList<Observation> obsList, int judgementValue)
    {
        for(Observation obs : obsList)
        {
            obs.setJudgement(judgementValue);
        }

        return obsList;
    }

    private SVM createAndTrainScrollFlingSVMClassifier(ArrayList<Observation> arrayListObservations)
    {
        SVM tempSVM = SVM.create();
        //initialise scrollFlingSVM

        tempSVM.setKernel(SVM.CHI2);

        tempSVM.setType(SVM.C_SVC);
        tempSVM.setC(10.55);
        tempSVM.setGamma(0.2);

        Mat trainScrollFlingMat = buildTrainOrTestMatForScrollFling(arrayListObservations);
        Mat labelsScrollFlingMat = buildLabelsMat(arrayListObservations);

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
