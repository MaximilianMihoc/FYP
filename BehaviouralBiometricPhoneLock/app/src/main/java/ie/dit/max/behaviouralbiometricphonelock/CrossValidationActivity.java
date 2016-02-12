/*
*   This Activity is for Scroll/Fling interaction tests
*
* */
package ie.dit.max.behaviouralbiometricphonelock;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
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
import org.opencv.core.Point;
import org.opencv.ml.Ml;
import org.opencv.ml.SVM;

import java.util.ArrayList;

public class CrossValidationActivity extends Activity
{
    Firebase ref;

    private static final String DEBUG_TAG = "Test Activity";

    Point startPoint, endPoint;
    ArrayList<Point> points = new ArrayList<>();
    ArrayList<Observation> trainScrollFlingObservations;
    ArrayList<Observation> scrollFlingObservations;
    ArrayList<Observation> tapOnlyObservations;
    ArrayList<Observation> trainTapOnlyObservations;

    private String userID;

    private SVM scrollFlingSVM;
    private SVM tapSVM;

    TextView outputData;
    String out = "";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        Firebase.setAndroidContext(this);
        ref = new Firebase("https://fyp-max.firebaseio.com");

        // get User ID details
        userID = "7796e45a-8310-48e9-9cca-5f9d4ce3f83a";

        scrollFlingObservations = new ArrayList<>();
        trainScrollFlingObservations = new ArrayList<>();
        tapOnlyObservations = new ArrayList<>();
        trainTapOnlyObservations = new ArrayList<>();
        points = new ArrayList<>();

        outputData = (TextView) findViewById(R.id.predictions);

        /* Get user training data from Firebase */
        getTrainingDataFromFirebase();



    }

    private void getTrainingDataFromFirebase()
    {
        //get Scroll Fling Observations from Firebase
        Firebase scrollFlingRef = new Firebase("https://fyp-max.firebaseio.com/trainData/" + userID + "/scrollFling");
        scrollFlingRef.addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot snapshot)
            {
                //System.out.println("data: " + snapshot.toString());
                //System.out.println("There are " + snapshot.getChildrenCount() + " observations");
                if(snapshot.getValue() == null)
                {
                    System.out.println("No Scroll Fling data available. ");
                    // display a Toast letting the user know that there is no training data available.
                    Toast toast = Toast.makeText(getApplicationContext(), "No Training data Provided", Toast.LENGTH_SHORT);
                    toast.show();
                }
                else
                {
                    for (DataSnapshot obsSnapshot: snapshot.getChildren())
                    {
                        //System.out.println("data: " + obsSnapshot.toString());
                        Observation obs = obsSnapshot.getValue(Observation.class);
                        trainScrollFlingObservations.add(obs);

                        //just some tests
                        /*Touch t = obs.getTouch();
                        ScrollFling sf = new ScrollFling(t);
                        System.out.println(sf.toString());*/
                    }

                    // Built the SVM model for Scroll/Fling Observations if training data exists.
                    if(trainScrollFlingObservations.size() > 0)
                    {
                        //initialise scrollFlingSVM
                        scrollFlingSVM =  SVM.create();
                        scrollFlingSVM.setKernel(SVM.RBF);
                        scrollFlingSVM.setType(SVM.ONE_CLASS);
                        //scrollFlingSVM.setC(0.3);
                        //scrollFlingSVM.setP(1);
                        scrollFlingSVM.setGamma(0.001953125);
                        scrollFlingSVM.setNu(0.00390625);

                        Mat trainScrollFlingMat = buildTrainOrTestMatForScrollFling(trainScrollFlingObservations);
                        Mat labelsScrollFlingMat = buildLabelsMat(trainScrollFlingObservations);

                        //System.out.println("Train Matrix is:\n");
                        //displayMatrix(trainScrollFlingMat);

                        scrollFlingSVM.train(trainScrollFlingMat, Ml.ROW_SAMPLE, labelsScrollFlingMat);
                        // end training scrollFlingSNM
                    }
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError)
            {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });

        //get Tap Observations from Firebase
        Firebase tapRef = new Firebase("https://fyp-max.firebaseio.com/trainData/" + userID + "/tap");
        tapRef.addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot snapshot)
            {
                if (snapshot.getValue() == null)
                {
                    System.out.println("No Tap data available. ");
                    // display a Toast letting the user know that there is no training data available.
                    Toast toast = Toast.makeText(getApplicationContext(), "No Training data Provided for Taps", Toast.LENGTH_SHORT);
                    toast.show();
                } else
                {
                    for (DataSnapshot obsSnapshot : snapshot.getChildren())
                    {
                        Observation obs = obsSnapshot.getValue(Observation.class);
                        trainTapOnlyObservations.add(obs);
                    }

                    if (trainTapOnlyObservations.size() > 0)
                    {
                        //initialise scrollFlingSVM
                        tapSVM = SVM.create();
                        tapSVM.setKernel(SVM.RBF);
                        tapSVM.setType(SVM.ONE_CLASS);
                        //tapSVM.setC(0.3);
                        //tapSVM.setP(1);
                        tapSVM.setGamma(0.001953125);
                        tapSVM.setNu(0.00390625);

                        Mat trainTapMat = buildTrainOrTestMatForTaps(trainTapOnlyObservations);
                        Mat labelsTapMat = buildLabelsMat(trainTapOnlyObservations);

                        //System.out.println("Train Matrix for Tap is:\n");
                        //displayMatrix(trainTapMat);

                        tapSVM.train(trainTapMat, Ml.ROW_SAMPLE, labelsTapMat);
                        // end training TapSVM
                    }

                     /* Get test data from firebase and return predictions. */
                    getTrainDataFromFirebaseAndTestSystem();
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError)
            {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });
    }

    private void getTrainDataFromFirebaseAndTestSystem()
    {
        //get Scroll Fling Observations from Firebase
        Firebase scrollFlingRef = new Firebase("https://fyp-max.firebaseio.com/testData/" + userID + "/scrollFling");
        scrollFlingRef.addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot snapshot)
            {
                if(snapshot.getValue() == null)
                {
                    System.out.println("No Scroll Fling data available. ");
                    Toast toast = Toast.makeText(getApplicationContext(), "No Test data Provided", Toast.LENGTH_SHORT);
                    toast.show();
                }
                else
                {
                    for (DataSnapshot obsSnapshot: snapshot.getChildren())
                    {
                        //System.out.println("data: " + obsSnapshot.toString());
                        Observation obs = obsSnapshot.getValue(Observation.class);
                        scrollFlingObservations.add(obs);
                    }

                    Mat testDataMat = buildTrainOrTestMatForScrollFling(scrollFlingObservations);

                    // create the result Mat
                    Mat resultMat = new Mat(scrollFlingObservations.size(), 1, CvType.CV_32S);

                    scrollFlingSVM.predict(testDataMat, resultMat, 0);

                    int counter=0;
                    out += "Scroll/Fling:\n";
                    for(int i = 0; i < resultMat.rows(); i++)
                    {
                        out += "\tpredicted" + i + ": " + resultMat.get(i, 0)[0];
                        if(resultMat.get(i, 0)[0] == 1) counter++;
                    }

                    out += "\nOwners: " + counter + " out of " + scrollFlingObservations.size();
                    outputData.setText(out);

                    System.out.println("Scroll Fling Result Mat: ");
                    displayMatrix(resultMat);
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError)
            {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });

        //get Tap Observations from Firebase
        Firebase tapRef = new Firebase("https://fyp-max.firebaseio.com/testData/" + userID + "/tap");
        tapRef.addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot snapshot)
            {
                if (snapshot.getValue() == null)
                {
                    System.out.println("No Tap data available. ");
                    Toast toast = Toast.makeText(getApplicationContext(), "No Test data Provided for Taps", Toast.LENGTH_SHORT);
                    toast.show();
                } else
                {
                    for (DataSnapshot obsSnapshot : snapshot.getChildren())
                    {
                        Observation obs = obsSnapshot.getValue(Observation.class);
                        tapOnlyObservations.add(obs);
                    }

                    Mat testDataMat = buildTrainOrTestMatForTaps(tapOnlyObservations);

                    // create the result Mat
                    Mat resultMat = new Mat(tapOnlyObservations.size(), 1, CvType.CV_32S);
                    tapSVM.predict(testDataMat, resultMat, 0);

                    int counter=0;
                    out += "\n\nTaps:\n";
                    for(int i = 0; i < resultMat.rows(); i++)
                    {
                        out += "\tpredicted" + i + ": " + resultMat.get(i, 0)[0];
                        if(resultMat.get(i, 0)[0] == 1) counter++;
                    }

                    out += "\nOwners: " + counter + " out of " + tapOnlyObservations.size();
                    outputData.setText(out);

                    System.out.println("Tap Result Mat: ");
                    displayMatrix(resultMat);
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError)
            {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });
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

            tempMat.put(i, j++, scrollFlingObs.calculateMidStrokeAreaCovered());
            tempMat.put(i, j++, scrollFlingObs.calculateDirectionOfEndToEndLine());
            tempMat.put(i, j++, scrollFlingObs.getScaledStartPoint().x);
            tempMat.put(i, j++, scrollFlingObs.getScaledStartPoint().y);
            tempMat.put(i, j++, scrollFlingObs.getScaledEndPoint().x);
            tempMat.put(i, j++, scrollFlingObs.getScaledEndPoint().y);
            tempMat.put(i, j++, scrollFlingObs.getScaledDuration());

            // linear accelerations are part of the observation - get average
            tempMat.put(i, j++, listObservations.get(i).calculateAVGLinearAcc());

            // angular Velocity are part of the observation - get average
            tempMat.put(i, j, listObservations.get(i).calculateAVGAngularVelocity());
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

            tempMat.put(i, j++, tapInteraction.getScaledStartPoint().x);
            tempMat.put(i, j++, tapInteraction.getScaledStartPoint().y);
            tempMat.put(i, j++, tapInteraction.getScaledEndPoint().x);
            tempMat.put(i, j++, tapInteraction.getScaledEndPoint().y);
            tempMat.put(i, j++, tapInteraction.getScaledDuration());
            tempMat.put(i, j++, tapInteraction.calculateFingerArea());

            // linear accelerations are part of the observation - get average
            tempMat.put(i, j++, listObservations.get(i).calculateAVGLinearAcc());

            // angular Velocity are part of the observation - get average
            tempMat.put(i, j, listObservations.get(i).calculateAVGAngularVelocity());
        }

        return tempMat;
    }

}
