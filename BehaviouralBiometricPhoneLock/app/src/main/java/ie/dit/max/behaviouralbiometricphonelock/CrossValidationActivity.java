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
                    userNames[i++] = "User " + i; //u.getUserName();
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

                    //scrollFlingSVM.setC(2.55);
                    //scrollFlingSVM.setC(1/Math.pow(2,1));
                    //scrollFlingSVM.setC(1/Math.pow(2,2));
                    //scrollFlingSVM.setC(1/Math.pow(2,3));
                    //scrollFlingSVM.setC(1/Math.pow(2,4));
                    //scrollFlingSVM.setC(1/Math.pow(2,5));
                    //scrollFlingSVM.setC(1/Math.pow(2,6));
                    //scrollFlingSVM.setC(1/Math.pow(2,7));
                    //scrollFlingSVM.setC(1/Math.pow(2,8));
                    //scrollFlingSVM.setC(1/Math.pow(2,9));
                    //scrollFlingSVM.setC(1/Math.pow(2,10));
                    //scrollFlingSVM.setC(1/Math.pow(2,11));
                    scrollFlingSVM.setC(1/Math.pow(2,12));
                    //scrollFlingSVM.setC(1/Math.pow(2,12.5));
                    //scrollFlingSVM.setC(1/Math.pow(2,13));

                    //scrollFlingSVM.setP(1);
                    //scrollFlingSVM.setGamma(0.001953125);

                    //scrollFlingSVM.setNu(1/Math.pow(2,2));
                    //scrollFlingSVM.setNu(1/Math.pow(2,5));
                    //scrollFlingSVM.setNu(1/Math.pow(2,6));
                    //scrollFlingSVM.setNu(1/Math.pow(2,7));
                    //scrollFlingSVM.setNu(1/Math.pow(2,8));
                    //scrollFlingSVM.setNu(1/Math.pow(2,9));
                    //scrollFlingSVM.setNu(1/Math.pow(2,10));
                    //scrollFlingSVM.setNu(1/Math.pow(2,11));
                    scrollFlingSVM.setNu(1/Math.pow(2,13));

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
                DataSnapshot tapSnapshot = snapshot.child("tap");
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

                    //tapSVM.setC(1/Math.pow(2,1));
                    //tapSVM.setC(1/Math.pow(2,2));
                    //tapSVM.setC(1/Math.pow(2,3));
                    //tapSVM.setC(1/Math.pow(2,4));
                    //tapSVM.setC(1/Math.pow(2,5));
                    //tapSVM.setC(1/Math.pow(2,6));
                    //tapSVM.setC(1/Math.pow(2,7));
                    //tapSVM.setC(1/Math.pow(2,8));
                    //tapSVM.setC(1/Math.pow(2,9));
                    //tapSVM.setC(1/Math.pow(2,10));
                    //tapSVM.setC(1/Math.pow(2,11));
                    //tapSVM.setC(1/Math.pow(2,12));
                    //tapSVM.setC(1/Math.pow(2,12.5));
                    tapSVM.setC(1/Math.pow(2,13));

                    //tapSVM.setP(1);
                    //tapSVM.setGamma(0.001953125);

                    //tapSVM.setNu(1/Math.pow(2,5));
                    //tapSVM.setNu(1/Math.pow(2,6));
                    //tapSVM.setNu(1/Math.pow(2,7));
                    //tapSVM.setNu(1/Math.pow(2,8));
                    //tapSVM.setNu(1/Math.pow(2,9));
                    //tapSVM.setNu(1/Math.pow(2,10));
                    tapSVM.setNu(1/Math.pow(2,11));

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

                            int i = 0;
                            // Scroll/Fling:
                            DataSnapshot dpScroll = usrSnapshot.child("scrollFling");
                            for (DataSnapshot obsSnapshot : dpScroll.getChildren())
                            {
                                Observation obs = obsSnapshot.getValue(Observation.class);
                                obs.setJudgement(0);
                                if(i < 10) trainScrollFlingObservations.add(obs);
                                i++;
                            }

                            i = 0;
                            // Taps:
                            DataSnapshot dpTap = usrSnapshot.child("tap");
                            for (DataSnapshot obsSnapshot : dpTap.getChildren())
                            {
                                Observation obs = obsSnapshot.getValue(Observation.class);
                                obs.setJudgement(0);
                                //if(i < 20)
                                    trainTapOnlyObservations.add(obs);
                                i++;
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
