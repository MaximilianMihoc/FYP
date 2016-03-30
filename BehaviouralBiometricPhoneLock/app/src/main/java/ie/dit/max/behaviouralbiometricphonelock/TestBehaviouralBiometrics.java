package ie.dit.max.behaviouralbiometricphonelock;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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
import org.opencv.android.OpenCVLoader;

/**
 * Created by Maximilian on 04/02/2016.
 */
public class TestBehaviouralBiometrics extends Activity implements
        GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener, SensorEventListener
{
    // Check if OpenCV loads properly
    static
    {
        if (!OpenCVLoader.initDebug())
        {
            Log.i("OpenCV", "OpenCV initialization failed");
        } else
        {
            Log.i("OpenCV", "OpenCV initialization successful");
        }
    }

    Firebase ref;

    public static boolean trainDataLoaded = false;
    public static double userTrust = 100;
    private static final String DEBUG_TAG = "Test Activity";

    private DevicePolicyManager devicePolicyManager;
    private ComponentName componentName;

    private double highOwner1 = 5;  // MaxReword1 for Owner
    private double highOwner2 = 10; // MaxReword2 for Owner
    private double lowOwner1 = 1;   // MinReword1 for Owner
    private double lowOwner2 = 5;   // MinReword2 for Owner

    private double highGuest1 = 5;  // MaxReword1 for Guest
    private double highGuest2 = 10; // MaxReword2 for Guest
    private double lowGuest1 = 1;   // MinReword1 for Guest
    private double lowGuest2 = 5;   // MinReword2 for Guest

    private GestureDetectorCompat mDetector;

    //to be used for assigning listener to different views.
    public View.OnTouchListener gestureListener;

    Point startPoint, endPoint;
    ArrayList<Point> points = new ArrayList<>();
    ArrayList<Observation> trainScrollFlingObservations;

    private ArrayList<Float> linearAccelerations;
    private ArrayList<Float> angularVelocities;

    boolean isScroll = false;
    boolean isFling = false;

    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private Sensor senGyroscope;

    private Float linearAcceleration;
    private Float angularVelocity;
    private UserSettings userSettings;

    SharedPreferences sharedpreferences;
    private String userID;

    private static SVM scrollFlingSVM;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Firebase.setAndroidContext(this);
        ref = new Firebase("https://fyp-max.firebaseio.com");
        sharedpreferences = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        devicePolicyManager = (DevicePolicyManager)getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        componentName = new ComponentName(this, MyAdminReceiver.class);

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

        trainScrollFlingObservations = new ArrayList<>();
        points = new ArrayList<>();
        linearAccelerations = new ArrayList<>();
        angularVelocities = new ArrayList<>();

        // get User details
        userID = sharedpreferences.getString("UserID", "");

        /* Get user Settings */
        getUserSettings();

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

                            //Log.d(DEBUG_TAG, "ScrollFling: " + scrollFling.toString());
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

                            //Log.d(DEBUG_TAG, "Tap: " + tap.toString());
                            //tempObs.setTap(tap);
                            tempObs.setTouch(tap);

                        }

                        //adding the lists of linearAccelerations and AngularVelocity to the Observation
                        tempObs.setAverageAngularVelocity(Observation.calculateAVGAngularVelocity(angularVelocities));
                        tempObs.setAverageLinearAcceleration(Observation.calculateAVGLinearAcc(linearAccelerations));

                        //In this section check each observation
                        if(!isFling && !isScroll)
                        {
                            //no predictions on taps, just store interactions

                            // add test data in Firebase if the check box for adding data is selected.
                            if(userSettings.getSaveTestData())
                            {
                                Firebase newUserRef = ref.child("testData").child(userID).child("tap");
                                newUserRef.push().setValue(tempObs);
                            }

                        }
                        else
                        {
                            // Trust Model implementation

                            ArrayList<Observation> testOneObservation = new ArrayList<>();
                            testOneObservation.add(tempObs);

                            Mat testDataMat = buildTrainOrTestMatForScrollFling(testOneObservation);
                            Mat resultMat = new Mat(testOneObservation.size(), 1, CvType.CV_32S);

                            if (scrollFlingSVM.isTrained())
                            {
                                scrollFlingSVM.predict(testDataMat, resultMat, 1);

                                double observationConfidenceFromSVM =  resultMat.get(0, 0)[0];

                                System.out.println("Confidence: " + observationConfidenceFromSVM);


                                if(observationConfidenceFromSVM < 0)
                                {
                                    //Owner Observation
                                    double newConf;
                                    observationConfidenceFromSVM = Math.abs(observationConfidenceFromSVM);

                                    if(observationConfidenceFromSVM <= 2)
                                    {
                                        // first reward apply
                                        newConf = normalizeOwnerConfidence(observationConfidenceFromSVM, 0, 2, highOwner1, lowOwner1);
                                    }
                                    else
                                    {
                                        // second rewards apply
                                        newConf = normalizeOwnerConfidence(observationConfidenceFromSVM, 2, 10, highOwner2, lowOwner2);
                                    }

                                    System.out.println("Normalised Confidence Owner: " + newConf);

                                    if(userTrust + newConf > 100)
                                        userTrust = 100;
                                    else
                                        userTrust += newConf;
                                }
                                else
                                {
                                    // guest Observation
                                    double newConf;

                                    if(observationConfidenceFromSVM <= 2)
                                    {
                                        // first reward apply
                                        newConf = normalizeOwnerConfidence(observationConfidenceFromSVM, 0, 2, highGuest1, lowGuest1);
                                    }
                                    else
                                    {
                                        // second rewards apply
                                        newConf = normalizeOwnerConfidence(observationConfidenceFromSVM, 2, 10, highGuest2, lowGuest2);
                                    }

                                    System.out.println("Normalised Confidence Guest: " + newConf);

                                    if(userTrust - newConf < 0)
                                        userTrust = 0;
                                    else
                                        userTrust -= newConf;
                                }

                                System.out.println("User Trust: " + userTrust);

                                // Analyse Trust Values and lock phone if user not genuine
                                if(userTrust < userSettings.getThreshold())
                                {
                                    //log out user
                                    ref.unauth();
                                    Intent intent = new Intent(TestBehaviouralBiometrics.this, LogIn.class);
                                    startActivity(intent);

                                    //lock user
                                    if (devicePolicyManager.isAdminActive(componentName))
                                    {
                                        devicePolicyManager.lockNow();
                                    }
                                    else
                                    {
                                        Toast.makeText(getApplicationContext(), "Not Registered as admin", Toast.LENGTH_SHORT).show();
                                    }

                                }

                            }

                            // uncomment the next 2 lines to add test data in Firebase.
                            if(userSettings.getSaveTestData())
                            {
                                Firebase newUserRef = ref.child("testData").child(userID).child("scrollFling");
                                newUserRef.push().setValue(tempObs);
                            }

                            //save test data to be used if password changes. Gets deleted automatically
                            Firebase newUserRef = ref.child("testDataForPasswordChange").child(userID).child("scrollFling");
                            newUserRef.push().setValue(tempObs);
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

    /**
     * Normalize the confidentiality returned by the clasifier in order to be used
     * in the trust model.
     * Owner confidence should be between 10 and 0, 10 being the max reward and 0 min reward
     * Guest confidence between 10 and 0, 10 being the max penalty and 0 being the min penalty
     *
     * */
    private double normalizeOwnerConfidence(double value, double min, double max, double high, double low)
    {
        //normalization formula
        return ((value - min)/(max - min)) * (high - low) + low;
    }

    private int countOwnerResults(Mat mat)
    {
        int counter = 0;
        for (int i = 0; i < mat.rows(); i++)
        {
            if (mat.get(i, 0)[0] < 0) counter++;
        }

        return counter;
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
                    Toast toast = Toast.makeText(getApplicationContext(), "No Train data Provided", Toast.LENGTH_SHORT);
                    toast.show();
                }
                else
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
                                if (countGuestObs < userSettings.getNrObsFromAnotherUser())
                                    trainScrollFlingObservations.add(obs);
                                countGuestObs++;
                            }

                        }
                        else  // get data from the actual user
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
                // set the train flag to true
                trainDataLoaded = true;
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

                /* Get user training data from Firebase - Owner and Guest data */
                getTrainDataFromFirebase();
            }

            @Override
            public void onCancelled(FirebaseError firebaseError)
            {

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
