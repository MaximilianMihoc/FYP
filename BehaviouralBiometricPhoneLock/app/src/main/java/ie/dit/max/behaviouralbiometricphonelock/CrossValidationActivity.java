/*
*   This Activity is for Scroll/Fling interaction tests
*
* */
package ie.dit.max.behaviouralbiometricphonelock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.ml.Ml;
import org.opencv.ml.SVM;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class CrossValidationActivity extends Activity
{
    Firebase ref;

    private static final String DEBUG_TAG = "Test Activity";

    Spinner spinner;

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

    String[] userKeys;
    String[] userNames;

    private SharedPreferences sharedpreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        Firebase.setAndroidContext(this);
        ref = new Firebase("https://fyp-max.firebaseio.com");

        // get User ID details
        //userID = "7796e45a-8310-48e9-9cca-5f9d4ce3f83a"; // - maximilian.mihoc@yahoo.com
        //userID = "6cae8406-d86b-4d56-bc1d-0e86324962c5";    // Sanita T
        //userID = "2b38c8f2-9dc8-4c85-94b8-2dec5e31681d";    // EDy
        //userID = "617c6be8-240c-47bd-ae80-146e77c87576";    // Ciaran
        //userID = "966562f0-8c33-4d5f-ba1d-73925eee377d";    // Bogdan
        //userID = "eb3197dd-4b01-44e2-acd3-9c4b86ac3729";    // Michael

        sharedpreferences = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        //if(sharedpreferences.contains("UserID")) userID = sharedpreferences.getString("UserID", "");

        scrollFlingObservations = new ArrayList<>();
        trainScrollFlingObservations = new ArrayList<>();
        tapOnlyObservations = new ArrayList<>();
        trainTapOnlyObservations = new ArrayList<>();
        points = new ArrayList<>();

        outputData = (TextView) findViewById(R.id.predictions);

        spinner = (Spinner)findViewById(R.id.spinner);
        populateSpinner();

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                userID = userKeys[position];

                scrollFlingObservations = new ArrayList<>();
                trainScrollFlingObservations = new ArrayList<>();
                tapOnlyObservations = new ArrayList<>();
                trainTapOnlyObservations = new ArrayList<>();
                points = new ArrayList<>();

                out = "";
                outputData.setText("Waiting for Data");

                /* Get user training data from Firebase */
                getTrainDataFromOtherUsersFirebase();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {

            }
        });
    }

    private void populateSpinner()
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

                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, userNames);
                spinner.setAdapter(adapter);

            }

            @Override
            public void onCancelled(FirebaseError firebaseError)
            {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });
    }

    private void getTrainingDataFromFirebase()
    {
        //get Scroll Fling Observations from Firebase
        Firebase scrollFlingRef = new Firebase("https://fyp-max.firebaseio.com/trainData/" + userID);
        scrollFlingRef.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot snapshot)
            {
                //System.out.println("data: " + snapshot.toString());
                //System.out.println("There are " + snapshot.getChildrenCount() + " observations");

                DataSnapshot scrollSnapshot = snapshot.child("scrollFling");
                for (DataSnapshot obsSnapshot : scrollSnapshot.getChildren())
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
                if (trainScrollFlingObservations.size() > 0)
                {
                    //initialise scrollFlingSVM
                    scrollFlingSVM = SVM.create();
                    scrollFlingSVM.setKernel(SVM.RBF);

                    //scrollFlingSVM.setType(SVM.C_SVC);
                    scrollFlingSVM.setType(SVM.NU_SVC);

                    //scrollFlingSVM.setC(0.25);
                    scrollFlingSVM.setC(0.0001220703125);

                    //scrollFlingSVM.setP(1);
                    //scrollFlingSVM.setGamma(0.001953125);

                    //scrollFlingSVM.setNu(0.00390625);
                    scrollFlingSVM.setNu(0.00048828125);

                    Mat trainScrollFlingMat = buildTrainOrTestMatForScrollFling(trainScrollFlingObservations);
                    Mat labelsScrollFlingMat = buildLabelsMat(trainScrollFlingObservations);

                    System.out.println("Train Matrix is:\n");
                    displayMatrix(trainScrollFlingMat);

                    scrollFlingSVM.train(trainScrollFlingMat, Ml.ROW_SAMPLE, labelsScrollFlingMat);
                    // end training scrollFlingSNM
                }else
                {
                    System.out.println("No Scroll Fling data available. ");
                    // display a Toast letting the user know that there is no training data available.
                    Toast toast = Toast.makeText(getApplicationContext(), "No Training data Provided", Toast.LENGTH_SHORT);
                    toast.show();
                }

                // Tap Information
                DataSnapshot tapSnapshot = snapshot.child("scrollFling");
                for (DataSnapshot obsSnapshot : tapSnapshot.getChildren())
                {
                    Observation obs = obsSnapshot.getValue(Observation.class);
                    trainTapOnlyObservations.add(obs);
                }

                if (trainTapOnlyObservations.size() > 0)
                {
                    //initialise scrollFlingSVM
                    tapSVM = SVM.create();
                    tapSVM.setKernel(SVM.RBF);

                    //tapSVM.setType(SVM.C_SVC);
                    tapSVM.setType(SVM.NU_SVC);

                    //tapSVM.setC(0.3);
                    tapSVM.setC(0.0001220703125);

                    //tapSVM.setP(1);
                    //tapSVM.setGamma(0.001953125);

                    //tapSVM.setNu(0.00390625);
                    tapSVM.setNu(0.00048828125);

                    Mat trainTapMat = buildTrainOrTestMatForTaps(trainTapOnlyObservations);
                    Mat labelsTapMat = buildLabelsMat(trainTapOnlyObservations);

                    //System.out.println("Train Matrix for Tap is:\n");
                    //displayMatrix(trainTapMat);

                    tapSVM.train(trainTapMat, Ml.ROW_SAMPLE, labelsTapMat);
                    // end training TapSVM
                }else
                {
                    System.out.println("No Tap data available. ");
                    // display a Toast letting the user know that there is no training data available.
                    Toast toast = Toast.makeText(getApplicationContext(), "No Training data Provided for Taps", Toast.LENGTH_SHORT);
                    toast.show();
                }

                 /* Get test data from firebase and return predictions. */
                getTestDataFromFirebaseAndTestSystem();

            }

            @Override
            public void onCancelled(FirebaseError firebaseError)
            {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });
    }

    private void getTrainDataFromOtherUsersFirebase()
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
                        if (!usrSnapshot.getKey().equals(userID))
                        {
                            //System.out.println("usrSnapshot: " + usrSnapshot.child("scrollFling"));

                            // Scroll/Fling:
                            DataSnapshot dpScroll = usrSnapshot.child("scrollFling");
                            for (DataSnapshot obsSnapshot : dpScroll.getChildren())
                            {
                                Observation obs = obsSnapshot.getValue(Observation.class);
                                obs.setJudgement(0);
                                trainScrollFlingObservations.add(obs);
                            }

                            // Taps:
                            DataSnapshot dpTap = usrSnapshot.child("tap");
                            for (DataSnapshot obsSnapshot : dpScroll.getChildren())
                            {
                                Observation obs = obsSnapshot.getValue(Observation.class);
                                obs.setJudgement(0);
                                trainTapOnlyObservations.add(obs);
                            }
                        }
                    }

                    // train data for the user.
                    getTrainingDataFromFirebase();

                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError)
            {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });
    }


    private void getTestDataFromFirebaseAndTestSystem()
    {
        //get Scroll Fling Observations from Firebase
        Firebase scrollFlingRef = new Firebase("https://fyp-max.firebaseio.com/testData/" + userID + "/scrollFling");
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
                    for (DataSnapshot obsSnapshot : snapshot.getChildren())
                    {
                        //System.out.println("data: " + obsSnapshot.toString());
                        Observation obs = obsSnapshot.getValue(Observation.class);
                        scrollFlingObservations.add(obs);
                    }

                    Mat testDataMat = buildTrainOrTestMatForScrollFling(scrollFlingObservations);

                    // create the result Mat
                    Mat resultMat = new Mat(scrollFlingObservations.size(), 1, CvType.CV_32S);

                    scrollFlingSVM.predict(testDataMat, resultMat, 0);

                    int counter = 0;
                    out += "Scroll/Fling:\n";
                    for (int i = 0; i < resultMat.rows(); i++)
                    {
                        out += "\tpredicted" + i + ": " + resultMat.get(i, 0)[0];
                        if (resultMat.get(i, 0)[0] == 1) counter++;
                    }

                    out += "\nOwners: " + counter + " out of " + scrollFlingObservations.size();
                    outputData.setText(out);

                    //System.out.println("Scroll Fling Result Mat: ");
                    //displayMatrix(resultMat);
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
        tapRef.addListenerForSingleValueEvent(new ValueEventListener()
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

                    int counter = 0;
                    out += "\n\nTaps:\n";
                    for (int i = 0; i < resultMat.rows(); i++)
                    {
                        out += "\tpredicted" + i + ": " + resultMat.get(i, 0)[0];
                        if (resultMat.get(i, 0)[0] == 1) counter++;
                    }

                    out += "\nOwners: " + counter + " out of " + tapOnlyObservations.size();
                    outputData.setText(out);

                    //System.out.println("Tap Result Mat: ");
                    //displayMatrix(resultMat);
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

            // linear accelerations are part of the observation - get average
            tempMat.put(i, j++, listObservations.get(i).getAverageLinearAcceleration());

            // angular Velocity are part of the observation - get average
            tempMat.put(i, j++, listObservations.get(i).getAverageAngularVelocity());

            tempMat.put(i, j++, scrollFlingObs.calculateMidStrokeAreaCovered());

            // Angle between start and end vectors
            tempMat.put(i, j++, scrollFlingObs.calculateAngleBetweenStartAndEndVectorsInRad());

            tempMat.put(i, j++, scrollFlingObs.calculateDirectEndToEndDistance());

            // Mean Direction
            tempMat.put(i, j++, scrollFlingObs.calculateMeanDirectionOfStroke());

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
            tempMat.put(i, j++, listObservations.get(i).getAverageLinearAcceleration());

            tempMat.put(i, j++, tapInteraction.getScaledStartPoint().x);
            tempMat.put(i, j++, tapInteraction.getScaledStartPoint().y);
            tempMat.put(i, j++, tapInteraction.getScaledEndPoint().x);
            tempMat.put(i, j++, tapInteraction.getScaledEndPoint().y);
            tempMat.put(i, j++, tapInteraction.getScaledDuration());
            tempMat.put(i, j++, tapInteraction.calculateFingerArea());
        }

        return tempMat;
    }

}
