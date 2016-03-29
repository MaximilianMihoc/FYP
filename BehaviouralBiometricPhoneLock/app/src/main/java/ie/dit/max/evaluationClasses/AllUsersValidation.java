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

import ie.dit.max.behaviouralbiometricphonelock.Observation;
import ie.dit.max.behaviouralbiometricphonelock.R;
import ie.dit.max.behaviouralbiometricphonelock.ScrollFling;
import ie.dit.max.behaviouralbiometricphonelock.User;
import ie.dit.max.behaviouralbiometricphonelock.UserSettings;

public class AllUsersValidation extends AppCompatActivity
{

    Firebase ref;
    private static final String DEBUG_TAG = "Classifiers";

    HashMap<String, ArrayList<Observation>> trainDataMapScrollFling;
    Map<String, ArrayList<Observation>> testDataMapScrollFling;

    private String userID;
    private String userName;

    String[] userKeys;
    String[] userNames;

    TextView validationText;
    TextView displayMinMaxValues;
    Button buttonChange;

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

                        // used to create confidence matrix
                        // buildAndDisplayConfidenceMatrix();

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

    final class ReturnValues
    {
        float average;
        float ownerPercent;
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
                ArrayList<Observation> tempList = changeJudgements(trainDataEntry.getValue(), 1);
                trainScrollFlingDataForUserID.addAll(tempList);
            }
            else
            {
                ArrayList<Observation> tempList = changeJudgements(trainDataEntry.getValue(), 0);
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
        SVM tempScrollFlingSVM = createAndTrainScrollFlingSVMClassifier(trainScrollFlingDataForUserID);

        //create and train kNN model
        //KNearest tempScrollFlingSVM = createAndTrainScrollFlingKNNClassifier(trainScrollFlingDataForUserID);

        //create and train RTrees model
        //RTrees tempScrollFlingSVM = createAndTrainScrollFlingrTreeClassifier(trainScrollFlingDataForUserID);

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

                //System.out.println("Result Mat " + i + " Of " + forUserName  + ": ");
                //displayMatrix(resultMat);

                if (i < userKeys.length - 1) confusionMatrixRow += counter + ",";
                else confusionMatrixRow += counter + "";

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

    private ArrayList<Observation> changeJudgements(ArrayList<Observation> obsList, int judgementValue)
    {
        for(Observation obs : obsList)
        {
            obs.setJudgement(judgementValue);
        }

        return obsList;
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

    private RTrees createAndTrainScrollFlingrTreeClassifier(ArrayList<Observation> arrayListObservations)
    {
        RTrees rTree = RTrees.create();
        Mat rTreeTrainMat = buildTrainOrTestMatForScrollFling(arrayListObservations);
        Mat rTreeLabelsMat = buildLabelsMat(arrayListObservations);

        rTree.train(rTreeTrainMat, Ml.ROW_SAMPLE, rTreeLabelsMat);

        return rTree;
    }

    private KNearest createAndTrainScrollFlingKNNClassifier(ArrayList<Observation> arrayListObservations)
    {
        KNearest kNN = KNearest.create();
        System.out.println("K is: " + kNN.getDefaultK());

        Mat kNNTrainMat = buildTrainOrTestMatForScrollFling(arrayListObservations);
        Mat kNNLabelsMat = buildLabelsMat(arrayListObservations);

        kNN.train(kNNTrainMat, Ml.ROW_SAMPLE, kNNLabelsMat);

        return kNN;
    }

    private SVM createAndTrainScrollFlingSVMClassifier(ArrayList<Observation> arrayListObservations)
    {
        SVM tempSVM = SVM.create();
        //initialise scrollFlingSVM
        tempSVM.setKernel(SVM.CHI2);

        tempSVM.setType(SVM.C_SVC);
        //tempSVM.setType(SVM.NU_SVC);

        tempSVM.setC(10.55);
        //tempSVM.setC(Math.pow(2,1.12));

        tempSVM.setGamma(0.2);
        //tempSVM.setGamma(Math.pow(2,8));

        /*Mat weightsMat = new Mat(2, 1, CvType.CV_32FC1);
        weightsMat.put(0, 0, 0.1 );
        weightsMat.put(1, 0, 0.9 );
        tempSVM.setClassWeights(weightsMat);*/

        Mat trainScrollFlingMat = buildTrainOrTestMatForScrollFling(arrayListObservations);
        Mat labelsScrollFlingMat = buildLabelsMat(arrayListObservations);

        //System.out.println("Train Matrix is:\n");
        //displayMatrix(trainScrollFlingMat);

        /*System.out.println("Coef0: " + tempSVM.getCoef0() );
        System.out.println("C: " + tempSVM.getC());
        System.out.println("TermCriteria: " + tempSVM.getTermCriteria().toString());
        System.out.println("Degree: " + tempSVM.getDegree());
        System.out.println("Gamma: " + tempSVM.getGamma());
        System.out.println("Nu: " + tempSVM.getNu());
        System.out.println("P: " + tempSVM.getP());
        System.out.println("Type: " + tempSVM.getType());
        System.out.println("ClassWeights: " );
        displayMatrix(tempSVM.getClassWeights());*/

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

    //function to display Mat on console
    public void displayMatrix(Mat matrix)
    {
        for(int i=0; i<matrix.rows(); i++)
        {
            for (int j = 0; j < matrix.cols(); j++)
            {
                System.out.print("\t" + (float)matrix.get(i, j)[0]);
            }
            System.out.println("\n");
        }
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

    private Mat normalizeMat(Mat toNormalize)
    {
        Mat tempMat = toNormalize.clone();

        for(int col = 0; col < toNormalize.cols(); col++)
        {
            // only normalize data from 2 features that are not properly normalised
            if(col == 5 || col == 8)
            {
                double min = getMinValueOFColumn(toNormalize, col);
                double max = getMaxValueOFColumn(toNormalize, col);

                for (int row = 0; row < toNormalize.rows(); row++)
                {
                    double[] element = toNormalize.get(row, col);
                    tempMat.put(row, col, (element[0] - min) / (max - min));
                }
            }
        }

        return tempMat;
    }

    private double getMinValueOFColumn(Mat mat, int col)
    {
        double min = Double.MAX_VALUE;
        for(int i = 0; i < mat.rows(); i++)
        {
            double [] temp = mat.get(i,col);
            if(temp[0] < min ) min = temp[0];
        }

        return min;
    }

    private double getMaxValueOFColumn(Mat mat, int col)
    {
        double max = Double.MIN_VALUE;
        for(int i = 0; i < mat.rows(); i++)
        {
            double [] temp = mat.get(i,col);
            if(temp[0] > max ) max = temp[0];
        }

        return max;
    }
}