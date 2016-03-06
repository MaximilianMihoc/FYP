package ie.dit.max.behaviouralbiometricphonelock;

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
import org.opencv.ml.KNearest;
import org.opencv.ml.Ml;
import org.opencv.ml.RTrees;
import org.opencv.ml.SVM;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AllUsersValidation extends AppCompatActivity
{

    Firebase ref;
    private static final String DEBUG_TAG = "Classifiers";

    HashMap<String, ArrayList<Observation>> trainDataMapScrollFling;
    Map<String, ArrayList<Observation>> testDataMapScrollFling;

    private String userID;

    String[] userKeys;
    String[] userNames;

    TextView validationText;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_users_validation);
        Firebase.setAndroidContext(this);
        ref = new Firebase("https://fyp-max.firebaseio.com");

        SharedPreferences sharedpreferences = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        if(sharedpreferences.contains("UserID")) userID = sharedpreferences.getString("UserID", "");

        validationText = (TextView) findViewById(R.id.validationText);

        trainDataMapScrollFling = new HashMap<>();
        testDataMapScrollFling = new HashMap<>();

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
                userKeys = new String[(int) snapshot.getChildrenCount()];
                userNames = new String[(int) snapshot.getChildrenCount()];
                int i = 0;
                for (DataSnapshot usrSnapshot : snapshot.getChildren())
                {
                    User u = usrSnapshot.getValue(User.class);
                    userKeys[i] = u.getUserID();
                    userNames[i++] = u.getUserName();
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

                        // Taps - Later
                        /*DataSnapshot dpTap = usrSnapshot.child("tap");
                        for (DataSnapshot obsSnapshot : dpTap.getChildren())
                        {
                            Observation obs = obsSnapshot.getValue(Observation.class);
                        }*/

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

                        // Taps - Later
                        /*DataSnapshot dpTap = usrSnapshot.child("tap");
                        for (DataSnapshot obsSnapshot : dpTap.getChildren())
                        {
                            Observation obs = obsSnapshot.getValue(Observation.class);
                        }*/

                    }

                    //TODO: Call the validation method here
                    computeValidationForOneUserAgainstAllOthers(userID, "Maximilian");
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError)
            {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });
    }

    private void computeValidationForOneUserAgainstAllOthers(String forUserID, String forUserName)
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
                    // add 10 observations from each other user in the train list
                    for (int i = 0; i <= tempList.size(); i++)
                    {
                        if (i < 10)
                        {
                            trainScrollFlingDataForUserID.add(tempList.get(i));
                        }
                    }
                }
            }
        }

        //create and train the SVM model
        SVM tempScrollFlingSVM = createAndTrainScrollFlingSVMClassifier(trainScrollFlingDataForUserID);

        String output = "For " + forUserName + "\n";

        // test the new SVM model with the test data
        //for( HashMap.Entry<String, ArrayList<Observation>> testDataEntry : testDataMapScrollFling.entrySet())
        for(int i = 0; i < userKeys.length; i++)
        {
            ArrayList<Observation> testData = testDataMapScrollFling.get(userKeys[i]);

            System.out.println(testDataMapScrollFling.get(userKeys[i]));

            if(testData != null)
            {
                Mat testDataMat = buildTrainOrTestMatForScrollFling(testData);
                Mat resultMat = new Mat(testData.size(), 1, CvType.CV_32S);

                tempScrollFlingSVM.predict(testDataMat, resultMat, 0);
                int counter = countOwnerResults(resultMat);

                output += userNames[i] + ": SVM Scroll/Fling -> " + counter + " / " + testData.size() + " -> " + Math.round((counter * 100) / testData.size()) + "%\n";
            }

        }

        validationText.setText(output);

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

    private RTrees createAndTraintapRTreeClassifier(ArrayList<Observation> arrayListObservations)
    {
        RTrees rTree = RTrees.create();
        Mat rTreeTrainMat = buildTrainOrTestMatForTaps(arrayListObservations);
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

    private KNearest createAndTrainTapKNNClassifier(ArrayList<Observation> arrayListObservations)
    {
        KNearest kNN = KNearest.create();
        System.out.println("K is: " + kNN.getDefaultK());

        Mat kNNTrainMat = buildTrainOrTestMatForTaps(arrayListObservations);
        Mat kNNLabelsMat = buildLabelsMat(arrayListObservations);

        kNN.train(kNNTrainMat, Ml.ROW_SAMPLE, kNNLabelsMat);

        return kNN;
    }

    private SVM createAndTrainScrollFlingSVMClassifier(ArrayList<Observation> arrayListObservations)
    {
        SVM tempSVM = SVM.create();
        //initialise scrollFlingSVM
        tempSVM.setKernel(SVM.RBF);

        //tempSVM.setType(SVM.C_SVC);
        tempSVM.setType(SVM.NU_SVC);

        //tempSVM.setP(1);
        //tempSVM.setC(1/Math.pow(2,1));
        //tempSVM.setC(1/Math.pow(2,2));
        //tempSVM.setC(1/Math.pow(2,3));
        //tempSVM.setC(1/Math.pow(2,4));
        //tempSVM.setC(1/Math.pow(2,5));
        //tempSVM.setC(1/Math.pow(2,6));
        //tempSVM.setC(1/Math.pow(2,7));
        //tempSVM.setC(1/Math.pow(2,8));
        //tempSVM.setC(1/Math.pow(2,9));
        //tempSVM.setC(1/Math.pow(2,10));
        //tempSVM.setC(1/Math.pow(2,11));
        //tempSVM.setC(1/Math.pow(2,12));
        //tempSVM.setC(1/Math.pow(2,12.5));
        //tempSVM.setC(1/Math.pow(2,13));

        //tempSVM.setNu(0.99);
        //tempSVM.setNu(1/Math.pow(2,1.1));
        //tempSVM.setNu(1/Math.pow(2,1.5));
        //tempSVM.setNu(1/Math.pow(2,1.7));
        //tempSVM.setNu(1/Math.pow(2,1.9));
        //tempSVM.setNu(1/Math.pow(2,2));
        //tempSVM.setNu(1/Math.pow(2,5));
        //tempSVM.setNu(1/Math.pow(2,6));
        //tempSVM.setNu(1/Math.pow(2,7));
        //tempSVM.setNu(1/Math.pow(2,8));
        //tempSVM.setNu(1/Math.pow(2,9));
        //tempSVM.setNu(1/Math.pow(2,10));
        //tempSVM.setNu(1/Math.pow(2,11));
        //tempSVM.setNu(1/Math.pow(2,13));
        tempSVM.setNu(1/Math.pow(2,18.1));
        //tempSVM.setNu(1/Math.pow(2,18.5));
        //tempSVM.setNu(1/Math.pow(2,28));

        Mat trainScrollFlingMat = buildTrainOrTestMatForScrollFling(arrayListObservations);
        Mat labelsScrollFlingMat = buildLabelsMat(arrayListObservations);

        //System.out.println("Train Matrix is:\n");
        //displayMatrix(trainScrollFlingMat);

        tempSVM.train(trainScrollFlingMat, Ml.ROW_SAMPLE, labelsScrollFlingMat);

        return tempSVM;
    }

    private SVM createAndTrainTapSVMClassifier(ArrayList<Observation> arrayListObservations)
    {
        SVM tempSVM = SVM.create();
        //initialise scrollFlingSVM
        tempSVM.setKernel(SVM.RBF);
        //tapSVM.setType(SVM.C_SVC);
        tempSVM.setType(SVM.NU_SVC);
        tempSVM.setC(1/Math.pow(2,13));
        tempSVM.setNu(1/Math.pow(2,11));

        Mat trainTapMat = buildTrainOrTestMatForTaps(arrayListObservations);
        Mat labelsTapMat = buildLabelsMat(arrayListObservations);

        //System.out.println("Train Matrix for Tap is:\n");
        //displayMatrix(trainTapMat);

        tempSVM.train(trainTapMat, Ml.ROW_SAMPLE, labelsTapMat);

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
            tempMat.put(i, j++, scrollFlingObs.getMeanDirectionOfStroke());

            // Stop x
            tempMat.put(i, j++, scrollFlingObs.getScaledEndPoint().x);

            // Start x
            tempMat.put(i, j++, scrollFlingObs.getScaledStartPoint().x);

            // Stroke Duration
            tempMat.put(i, j++, scrollFlingObs.getScaledDuration());

            // Start y
            tempMat.put(i, j++, scrollFlingObs.getScaledStartPoint().y);

            // Stop y
            tempMat.put(i, j++, scrollFlingObs.getScaledEndPoint().y);
        }

        return tempMat;
    }

    private Mat buildTrainOrTestMatForTaps(ArrayList<Observation> listObservations)
    {
        Mat tempMat = new Mat(listObservations.size(), Tap.numberOfFeatures, CvType.CV_32FC1);

        for(int i = 0; i < listObservations.size(); i++)
        {
            Tap tapInteraction = new Tap(listObservations.get(i).getTouch());
            int j = 0;

            // linear accelerations are part of the observation - get average
            tempMat.put(i, j++, listObservations.get(i).getAverageLinearAcceleration());

            // angular Velocity are part of the observation - get average
            tempMat.put(i, j++, listObservations.get(i).getAverageAngularVelocity());


            tempMat.put(i, j++, tapInteraction.getMidStrokeAreaCovered());

            //Stop x
            tempMat.put(i, j++, tapInteraction.getScaledEndPoint().x);

            // Start x
            tempMat.put(i, j++, tapInteraction.getScaledStartPoint().x);

            //Duration
            tempMat.put(i, j++, tapInteraction.getScaledDuration());

            // Start y
            tempMat.put(i, j++, tapInteraction.getScaledStartPoint().y);

            //Stop y
            tempMat.put(i, j++, tapInteraction.getScaledEndPoint().y);

        }

        return tempMat;
    }
}
