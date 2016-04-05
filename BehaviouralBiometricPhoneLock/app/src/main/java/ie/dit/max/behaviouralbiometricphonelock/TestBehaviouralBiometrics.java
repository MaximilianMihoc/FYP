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
import org.opencv.ml.SVM;

import java.util.ArrayList;
import org.opencv.android.OpenCVLoader;

/**
 * This activity represents the Test Phase of the application and needs to be used as extended activity
 *      for all the places where the interactions of the users should be checked.
 * In this activity can be found the Trust model, that decides if the phone should be locked or not.
 * Gesture detectors and Sensors events are used here to gather all the data necessary for an observation
 *
 * @author Maximilian Mihoc.
 * @version 1.0
 *
 */
public class TestBehaviouralBiometrics extends Activity implements
        GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener, SensorEventListener
{
    private static final String DEBUG_TAG = "Test Activity";

    //public variables
    public static boolean trainDataLoaded = false;
    public static double userTrust = 100;

    // used for assigning listener to different views.
    public View.OnTouchListener gestureListener;

    //private variables
    private static SVM scrollFlingSVM;
    private static UserSettings userSettings;
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

    private Point startPoint, endPoint;
    private ArrayList<Point> points = new ArrayList<>();
    private ArrayList<Observation> trainScrollFlingObservations;
    private ArrayList<Float> linearAccelerations;
    private ArrayList<Float> angularVelocities;

    private boolean isScroll = false;
    private boolean isFling = false;

    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private Sensor senGyroscope;
    private Float linearAcceleration;
    private Float angularVelocity;
    private String userID;

    private Firebase ref;

    // Check if OpenCV library loads properly
    static
    {
        if (!OpenCVLoader.initDebug())
        {
            Log.i(DEBUG_TAG, "OpenCV initialization failed");
        }
        else
        {
            Log.i(DEBUG_TAG, "OpenCV initialization successful");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Firebase.setAndroidContext(this);
        ref = new Firebase(DBVar.mainURL);

        // get User details
        SharedPreferences sharedpreferences = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        if(sharedpreferences.contains("UserID")) userID = sharedpreferences.getString("UserID", "");

        // instantiate the Device manager.
        devicePolicyManager = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
        componentName = new ComponentName(this, MyAdminReceiver.class);

        // instantiate gesture detector
        mDetector = new GestureDetectorCompat(this, this);
        mDetector.setOnDoubleTapListener(this);
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

        linearAcceleration = 0.0f;
        angularVelocity = 0.0f;

        /* Get user Settings */
        getUserSettings();

        gestureListener = new View.OnTouchListener()
        {
            public boolean onTouch(View v, MotionEvent event)
            {
                // need to call the gesture detector first so that the strokes can be differentiated from taps
                mDetector.onTouchEvent(event);

                //add linear Acceleration and angular Velocity to list
                linearAccelerations.add(linearAcceleration);
                angularVelocities.add(angularVelocity);

                double duration;
                int action = event.getAction();
                // save details of the interaction in the Observation and Touch objects.
                // Event action checked to find when the interaction starts and finishes.
                switch (action)
                {
                    case (MotionEvent.ACTION_DOWN):
                    {
                        // store start point coordinates
                        startPoint = new Point(event.getX(), event.getY());
                        return false;
                    }
                    case (MotionEvent.ACTION_MOVE):
                    {
                        // save all the intermediate points of the stroke into a list
                        Point newP = new Point(event.getX(), event.getY());
                        points.add(newP);
                        return false;
                    }
                    case (MotionEvent.ACTION_UP):
                    {
                        //save end point coordinates
                        endPoint = new Point(event.getX(), event.getY());
                        // get duration of the end point
                        duration = event.getEventTime() - event.getDownTime();
                        Observation tempObs = new Observation();

                        //check if the action is ScrollFling or Tap action
                        if(isFling || isScroll)
                        {
                            //save all the necessary details of scrollFling actions and calculate some of the features
                            ScrollFling scrollFling = new ScrollFling();
                            scrollFling.setStartPoint(startPoint);
                            scrollFling.setEndPoint(endPoint);
                            scrollFling.initialisePoints(points);
                            scrollFling.setDuration(duration);

                            scrollFling.setMidStrokeAreaCovered(scrollFling.calculateMidStrokeAreaCovered());
                            scrollFling.setMeanDirectionOfStroke(scrollFling.calculateMeanDirectionOfStroke());
                            scrollFling.setDirectEndToEndDistance(scrollFling.calculateDirectEndToEndDistance());
                            scrollFling.setAngleBetweenStartAndEndVectorsInRad(scrollFling.calculateAngleBetweenStartAndEndVectorsInRad());

                            //save ScrollFling as Touch object
                            tempObs.setTouch(scrollFling);
                        }
                        else
                        {
                            // save all the necessary details of tap actions and calculate some of the features
                            Tap tap = new Tap();
                            tap.setStartPoint(startPoint);
                            tap.setEndPoint(endPoint);
                            tap.initialisePoints(points);
                            tap.setDuration(duration);

                            tap.setMidStrokeAreaCovered(tap.calculateMidStrokeAreaCovered());

                            // save Tap as Touch Object
                            tempObs.setTouch(tap);

                        }

                        //adding the lists of linearAccelerations and AngularVelocity to the Observation
                        tempObs.setAverageAngularVelocity(Observation.calculateAVGAngularVelocity(angularVelocities));
                        tempObs.setAverageLinearAcceleration(Observation.calculateAVGLinearAcc(linearAccelerations));

                        //In this section check each observation against the trained model and make predictions
                        if(!isFling && !isScroll)
                        {
                            // no predictions on taps, just store interactions
                            // add test data in Firebase if the check box for adding data is selected.
                            if(userSettings.getSaveTestData())
                            {
                                Firebase newUserRef = ref.child("testData").child(userID).child("tap");
                                newUserRef.push().setValue(tempObs);
                            }
                        }
                        else
                        {
                            /*
                            *       TRUST MODEL implementation
                            * Different rewards or penalty are applied to the UserTrust variable
                            *   based on the value of the Observation Confidence.
                            *
                            *   When the confidence returned by the classifier is between -2 and 2, reward and penalty values are smaller.
                            *   When the confidence returned by the classifier is less than -2 and greater than 2, reward and penalty values are greater.
                            *
                            *   Penalty values are always bigger than reward values.
                            *
                            * */

                            ArrayList<Observation> testOneObservation = new ArrayList<>();
                            testOneObservation.add(tempObs);

                            // build the Matrix that stores the last interaction of the user that needs to be predicted.
                            Mat testDataMat = Classifier.buildTrainOrTestMatForScrollFling(testOneObservation);
                            Mat resultMat = new Mat(testOneObservation.size(), 1, CvType.CV_32S);

                            if (scrollFlingSVM.isTrained())
                            {
                                // predict each interaction and build result matrix for it.
                                scrollFlingSVM.predict(testDataMat, resultMat, 1);

                                // result matrix contains the confidence of the predicted observation
                                double observationConfidenceFromSVM =  resultMat.get(0, 0)[0];

                                // check the confidence of the observations.
                                // Negative value for predictions are Owners and positive values for predictions are Guests/Intruders.
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

                                    if(userTrust - newConf < 0)
                                        userTrust = 0;
                                    else
                                        userTrust -= newConf;
                                }

                                // Analyse userTrust Value and lock phone and log user out if user not genuine
                                if(userTrust < userSettings.getThreshold())
                                {
                                    // Delete data that would be used for changing password.
                                    Firebase deleteTestDataForPasswordChange = new Firebase("https://fyp-max.firebaseio.com/testDataForPasswordChange/" + userID);
                                    deleteTestDataForPasswordChange.removeValue();

                                    //log out user
                                    ref.unauth();
                                    Intent intent = new Intent(TestBehaviouralBiometrics.this, LogIn.class);
                                    startActivity(intent);

                                    //lock user
                                    if (devicePolicyManager.isAdminActive(componentName))
                                    {
                                        devicePolicyManager.lockNow();
                                    }
                                }

                            }

                            // save test data in Firebase.
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
     *      in the trust model.
     * Owner confidence should be between 10 and 0, 10 being the max reward and 0 min reward
     * Guest confidence between 10 and 0, 10 being the max penalty and 0 being the min penalty
     *
     * @param value double
     * @param min double
     * @param max double
     * @param high double
     * @param low double
     * @return double normalized confidence
     */
    private double normalizeOwnerConfidence(double value, double min, double max, double high, double low)
    {
        //normalization formula
        return ((value - min)/(max - min)) * (high - low) + low;
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

                /* Get user training data from Firebase - Owner and Guest data */
                getTrainDataFromFirebase();
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
    private void getTrainDataFromFirebase()
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
                            // ScrollFling data for Guests
                            DataSnapshot dpScroll = usrSnapshot.child("scrollFling");
                            int countGuestObs = 0;
                            for (DataSnapshot obsSnapshot : dpScroll.getChildren())
                            {
                                Observation obs = obsSnapshot.getValue(Observation.class);
                                // judgements is set to 0 because for guest data
                                obs.setJudgement(0);

                                // only use a defined number of observations from guest users.
                                if (countGuestObs < userSettings.getNrObsFromAnotherUser())
                                    trainScrollFlingObservations.add(obs);
                                countGuestObs++;
                            }
                        }
                        else  // get data for the Owner
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

    @Override
    public boolean onDown(MotionEvent e)
    {
        //add linear Acceleration and angular Velocity to the list
        linearAccelerations.add(linearAcceleration);
        angularVelocities.add(angularVelocity);
        return false;
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
        // calculate the angular velocity and linear acceleration every time the sensor state changes.

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
