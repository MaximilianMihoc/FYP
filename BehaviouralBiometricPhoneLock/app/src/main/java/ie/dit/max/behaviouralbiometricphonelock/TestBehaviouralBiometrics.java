package ie.dit.max.behaviouralbiometricphonelock;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
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

/**
 * Created by Maximilian on 04/02/2016.
 */
public class TestBehaviouralBiometrics extends Activity implements
        GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener, SensorEventListener
{
    Firebase ref;

    private static final String DEBUG_TAG = "Test Activity";
    private GestureDetectorCompat mDetector;

    //to be used for assigning listener to different views.
    public View.OnTouchListener gestureListener;

    Point startPoint, endPoint;
    ArrayList<Point> points = new ArrayList<>();
    ArrayList<Observation> trainScrollFlingObservations;
    ArrayList<Observation> scrollFlingObservations;
    ArrayList<Observation> tapOnlyObservations;
    ArrayList<Observation> trainTapOnlyObservations;

    private ArrayList<Float> linearAccelerations;
    private ArrayList<Float> angularVelocities;

    boolean isScroll = false;
    boolean isFling = false;

    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private Sensor senGyroscope;

    private Float linearAcceleration;
    private Float angularVelocity;

    SharedPreferences sharedpreferences;
    private String userID;

    private SVM scrollFlingSVM;
    private SVM tapSVM;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Firebase.setAndroidContext(this);
        ref = new Firebase("https://fyp-max.firebaseio.com");
        sharedpreferences = getSharedPreferences("MyPref", Context.MODE_PRIVATE);

        mDetector = new GestureDetectorCompat(this, this);
        mDetector.setOnDoubleTapListener(this);

        linearAcceleration = 0.0f;
        angularVelocity = 0.0f;

        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Accelerometer declarations
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);

        // Gyroscope declarations
        senGyroscope = senSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        senSensorManager.registerListener(this, senGyroscope, SensorManager.SENSOR_DELAY_FASTEST);

        scrollFlingObservations = new ArrayList<>();
        trainScrollFlingObservations = new ArrayList<>();
        tapOnlyObservations = new ArrayList<>();
        trainTapOnlyObservations = new ArrayList<>();
        points = new ArrayList<>();
        linearAccelerations = new ArrayList<>();
        angularVelocities = new ArrayList<>();

        // get User details
        userID = sharedpreferences.getString("UserID", "");

        /* Get user training data from Firebase */
        getTrainingDataFromFirebase();

        gestureListener = new View.OnTouchListener()
        {
            public boolean onTouch(View v, MotionEvent event)
            {
                // need to call the gesture detector first so that the strokes can be differentiated from taps
                mDetector.onTouchEvent(event);

                //assignValuesToObservations(event);
                //add linear Acceleration and angular Velocity to list
                linearAccelerations.add(linearAcceleration);
                angularVelocities.add(angularVelocity);

                double duration;

                int action = event.getAction();
                switch (action)
                {
                    case (MotionEvent.ACTION_DOWN):
                    {
                        startPoint = new Point(event.getX(), event.getY());
                        return false;
                    }
                    case (MotionEvent.ACTION_MOVE):
                    {
                        Point newP = new Point(event.getX(), event.getY());
                        points.add(newP);
                        return false;
                    }
                    case (MotionEvent.ACTION_UP):
                    {
                        endPoint = new Point(event.getX(), event.getY());
                        duration = event.getEventTime() - event.getDownTime();
                        Observation tempObs = new Observation();

                        if(isFling || isScroll)
                        {
                            //touch = scrollFling
                            ScrollFling scrollFling = new ScrollFling();
                            scrollFling.setStartPoint(startPoint);
                            scrollFling.setEndPoint(endPoint);
                            scrollFling.initialisePoints(points);
                            scrollFling.setDuration(duration);

                            scrollFling.setMidStrokeAreaCovered(scrollFling.calculateMidStrokeAreaCovered());
                            scrollFling.setMeanDirectionOfStroke(scrollFling.calculateMeanDirectionOfStroke());
                            scrollFling.setDirectEndToEndDistance(scrollFling.calculateDirectEndToEndDistance());
                            scrollFling.setAngleBetweenStartAndEndVectorsInRad(scrollFling.calculateAngleBetweenStartAndEndVectorsInRad());


                            Log.d(DEBUG_TAG, "ScrollFling: " + scrollFling.toString());
                            //tempObs.setScrollFling(scrollFling);
                            tempObs.setTouch(scrollFling);
                        } else
                        {
                            //touch = tap
                            Tap tap = new Tap();
                            tap.setStartPoint(startPoint);
                            tap.setEndPoint(endPoint);
                            tap.initialisePoints(points);
                            tap.setDuration(duration);

                            tap.setMidStrokeAreaCovered(tap.calculateMidStrokeAreaCovered());

                            Log.d(DEBUG_TAG, "Tap: " + tap.toString());
                            //tempObs.setTap(tap);
                            tempObs.setTouch(tap);

                        }

                        //adding the lists of linearAccelerations and AngularVelocity to the Observation
                        tempObs.setAverageAngularVelocity(Observation.calculateAVGAngularVelocity(angularVelocities));
                        tempObs.setAverageLinearAcceleration(Observation.calculateAVGLinearAcc(linearAccelerations));

                        //In this section check each observation, one at a time and assign a judgement to it.
                        if(!isFling && !isScroll)
                        {
                            //create a list containing only one Obs which s used to create the test Mat for Tap
                            ArrayList<Observation> tempObsList = new ArrayList<>();
                            tempObsList.add(tempObs);

                            Mat testDataMat = buildTrainOrTestMatForTaps(tempObsList);

                            // create the result Mat
                            Mat resultMat = new Mat(tempObsList.size(), 1, CvType.CV_32S);
                            if (tapSVM.isTrained() && testDataMat.rows() > 0)
                            {
                                tapSVM.predict(testDataMat, resultMat, 0);
                                // set test Observation Judgement given by the SVM
                                if(resultMat.rows() > 0)
                                    tempObs.setJudgement((int) resultMat.get(0, 0)[0]);

                                /*if(tempObs.getJudgement() == 1)
                                {
                                    Toast toast = Toast.makeText(getApplicationContext(), "Tap Owner", Toast.LENGTH_SHORT);
                                    toast.show();
                                }
                                else
                                {
                                    Toast toast = Toast.makeText(getApplicationContext(), "Tap Guest", Toast.LENGTH_SHORT);
                                    toast.show();
                                }*/

                            }

                            //System.out.println("Tap resultMat: " );
                            //displayMatrix(resultMat);

                            // uncomment the next 2 lines to add test data in Firebase.
                            Firebase newUserRef = ref.child("testData").child(userID).child("tap");
                            newUserRef.push().setValue(tempObs);

                            tapOnlyObservations.add(tempObs);
                        }
                        else
                        {
                            //create a list containing only one Obs which s used to create the test Mat
                            ArrayList<Observation> tempObsList = new ArrayList<>();
                            tempObsList.add(tempObs);

                            Mat testDataMat = buildTrainOrTestMatForScrollFling(tempObsList);

                            // create the result Mat
                            Mat resultMat = new Mat(tempObsList.size(), 1, CvType.CV_32S);


                            if (scrollFlingSVM.isTrained() && testDataMat.rows() > 0)
                            {
                                scrollFlingSVM.predict(testDataMat, resultMat, 0);
                                //svm.predict(testDataMat, resultMat, StatModel.RAW_OUTPUT);

                                // set test Observation Judgement given by the SVM
                                if (resultMat.rows() > 0)
                                    tempObs.setJudgement((int) resultMat.get(0, 0)[0]);

                                /*if(tempObs.getJudgement() == 1)
                                {
                                    Toast toast = Toast.makeText(getApplicationContext(), "Owner", Toast.LENGTH_SHORT);
                                    toast.show();
                                }
                                else
                                {
                                    Toast toast = Toast.makeText(getApplicationContext(), "Guest", Toast.LENGTH_SHORT);
                                    toast.show();
                                }*/
                            }

                            //System.out.println("resultMat: " );
                            //displayMatrix(resultMat);

                            // uncomment the next 2 lines to add test data in Firebase.
                            Firebase newUserRef = ref.child("testData").child(userID).child("scrollFling");
                            newUserRef.push().setValue(tempObs);

                            scrollFlingObservations.add(tempObs);
                        }

                        points.clear();
                        linearAccelerations.clear();
                        angularVelocities.clear();
                        isFling = false;
                        isScroll = false;
                        return false;
                    }
                }

                return mDetector.onTouchEvent(event);
            }
        };
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
                }else
                {
                    System.out.println("No Scroll Fling data available. ");
                    // display a Toast letting the user know that there is no training data available.
                    Toast toast = Toast.makeText(getApplicationContext(), "No Training data Provided", Toast.LENGTH_SHORT);
                    toast.show();
                }

                // Tap information
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
                }else
                {
                    System.out.println("No Tap data available. ");
                    // display a Toast letting the user know that there is no training data available.
                    Toast toast = Toast.makeText(getApplicationContext(), "No Training data Provided for Taps", Toast.LENGTH_SHORT);
                    toast.show();
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


    private Mat buildTrainOrTestMatForScrollFling(ArrayList<Observation> listObservations)
    {
        Mat tempMat = new Mat(listObservations.size(), 8, CvType.CV_32FC1); //ScrollFling.numberOfFeatures, CvType.CV_32FC1);

        for(int i = 0; i < listObservations.size(); i++)
        {
            ScrollFling scrollFlingObs = new ScrollFling(listObservations.get(i).getTouch());
            int j = 0;

            tempMat.put(i, j++, scrollFlingObs.getMidStrokeAreaCovered());
            //tempMat.put(i, j++, scrollFlingObs.calculateDirectionOfEndToEndLine());
            tempMat.put(i, j++, scrollFlingObs.getScaledStartPoint().x);
            tempMat.put(i, j++, scrollFlingObs.getScaledStartPoint().y);
            tempMat.put(i, j++, scrollFlingObs.getScaledEndPoint().x);
            tempMat.put(i, j++, scrollFlingObs.getScaledEndPoint().y);
            tempMat.put(i, j++, scrollFlingObs.getScaledDuration());

            // linear accelerations are part of the observation - get average
            tempMat.put(i, j++, listObservations.get(i).getAverageLinearAcceleration());

            // angular Velocity are part of the observation - get average
            tempMat.put(i, j, listObservations.get(i).getAverageAngularVelocity());
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
            tempMat.put(i, j++, tapInteraction.getMidStrokeAreaCovered());

            // linear accelerations are part of the observation - get average
            tempMat.put(i, j++, listObservations.get(i).getAverageLinearAcceleration());

            // angular Velocity are part of the observation - get average
            tempMat.put(i, j, listObservations.get(i).getAverageAngularVelocity());
        }

        return tempMat;
    }

    @Override
    public boolean onDown(MotionEvent e)
    {
        //add linear Acceleration and angular Velocity to list
        linearAccelerations.add(linearAcceleration);
        angularVelocities.add(angularVelocity);

        return false;
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

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e)
    {
        return false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e)
    {
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e)
    {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e)
    {
        //Log.d(DEBUG_TAG, "onShowPress: " + e.toString());
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e)
    {
        //add linear Acceleration and angular Velocity to list
        linearAccelerations.add(linearAcceleration);
        angularVelocities.add(angularVelocity);

        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
    {
        isScroll = true;
        //add linear Acceleration and angular Velocity to list
        linearAccelerations.add(linearAcceleration);
        angularVelocities.add(angularVelocity);

        return false;
    }

    @Override
    public void onLongPress(MotionEvent e)
    {
        //Log.d(DEBUG_TAG, "onLongPress: " + e.toString());
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
    {
        isFling = true;
        //add linear Acceleration and angular Velocity to list
        linearAccelerations.add(linearAcceleration);
        angularVelocities.add(angularVelocity);

        return false;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent)
    {
        Sensor mySensor = sensorEvent.sensor;

        if (mySensor.getType() == Sensor.TYPE_GYROSCOPE)
        {
            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];

            angularVelocity = (float) Math.sqrt((double) (x*x + y*y + z*z));
        }

        if (mySensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION)
        {
            //this sensor gives me the linear acceleration values for x, y, z.
            // linear acceleration is the acceleration - earth gravity.
            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];

            linearAcceleration = (float) Math.sqrt((double) (x*x + y*y + z*z));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {

    }

    @Override
    protected void onResume()
    {
        super.onResume();
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        senSensorManager.registerListener(this, senGyroscope, SensorManager.SENSOR_DELAY_FASTEST);

    }

    @Override
    protected void onPause()
    {
        super.onPause();
        senSensorManager.unregisterListener(this);
    }
}
