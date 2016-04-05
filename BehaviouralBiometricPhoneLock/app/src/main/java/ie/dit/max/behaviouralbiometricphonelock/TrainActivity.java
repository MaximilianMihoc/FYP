package ie.dit.max.behaviouralbiometricphonelock;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v4.view.GestureDetectorCompat;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import com.firebase.client.Firebase;

import org.opencv.core.Point;

import java.util.ArrayList;

/**
 * This activity represents the Train Phase of the application and needs to be used as extended activity
 *      for all the places where the interactions of the users should be stored as train data.
 *
 * Gesture detectors and Sensors events are used here to gather all the data necessary for an observation.
 *
 * @author Maximilian Mihoc.
 * @version 1.0
 *
 */
public class TrainActivity extends Activity implements
        GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener,
        SensorEventListener
{
    private static final String DEBUG_TAG = "Train Activity";
    private Firebase ref;
    private GestureDetectorCompat mDetector;

    // used for assigning listener to different views.
    public OnTouchListener gestureListener;

    private Point startPoint, endPoint;
    private ArrayList<Point> points = new ArrayList<>();
    private ArrayList<Float> linearAccelerations;
    private ArrayList<Float> angularVelocities;
    private boolean isScroll = false;
    private boolean isFling = false;
    private String userID;
    private Float linearAcceleration;
    private Float angularVelocity;

    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private Sensor senGyroscope;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Firebase.setAndroidContext(this);
        ref = new Firebase(DBVar.mainURL);

        // get User details
        SharedPreferences sharedpreferences = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        if(sharedpreferences.contains("UserID"))  userID = sharedpreferences.getString("UserID", "");

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

        points = new ArrayList<>();
        linearAccelerations = new ArrayList<>();
        angularVelocities = new ArrayList<>();
        linearAcceleration = 0.0f;
        angularVelocity = 0.0f;

        gestureListener = new OnTouchListener()
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

                        //all Observations from Training are owner observations and the Judgement is set to 1
                        tempObs.setJudgement(1);

                        // save each training observation in the database
                        if(!isFling && !isScroll)
                        {
                            //save tap data
                            Firebase newUserRef = ref.child("trainData").child(userID).child("tap");
                            newUserRef.push().setValue(tempObs);
                        }
                        else
                        {
                            // save ScrollFling data
                            Firebase newUserRef = ref.child("trainData").child(userID).child("scrollFling");
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

    @Override
    public boolean onDown(MotionEvent e)
    {
        //add linear Acceleration and angular Velocity to list
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
