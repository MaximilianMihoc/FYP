package ie.dit.max.behaviouralbiometricphonelock;

import android.app.Activity;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import org.opencv.core.Point;

import java.util.ArrayList;

import ie.dit.max.foregroundApp.Home;

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
    String userID;

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
                        Observation tempObs = new Observation(new Touch(), new ArrayList<Float>(), new ArrayList<Float>());

                        if(isFling || isScroll)
                        {
                            //touch = scrollFling
                            ScrollFling scrollFling = new ScrollFling();
                            scrollFling.setStartPoint(startPoint);
                            scrollFling.setEndPoint(endPoint);
                            scrollFling.setPoints(points);
                            scrollFling.setDuration(duration);
                            scrollFling.setPressure(event.getPressure());

                            Log.d(DEBUG_TAG, "ScrollFling: " + scrollFling.toString());
                            //tempObs.setScrollFling(scrollFling);
                            tempObs.setTouch(scrollFling);
                        }
                        else
                        {
                            //touch = tap
                            Tap tap = new Tap();
                            tap.setStartPoint(startPoint);
                            tap.setEndPoint(endPoint);
                            tap.setPoints(points);
                            tap.setDuration(duration);
                            tap.setPressure(event.getPressure());

                            Log.d(DEBUG_TAG, "Tap: " + tap.toString());
                            //tempObs.setTap(tap);
                            tempObs.setTouch(tap);

                        }

                        //adding the lists of linearAccelerations and AngularVelocity to the Observation
                        tempObs.setAngularVelocities(angularVelocities);
                        tempObs.setLinearAccelerations(linearAccelerations);

                        // add Observation to the List of training observations. Separate list of obs for tap gesture.

                        if(!isFling && !isScroll)
                        {
                            tapOnlyObservations.add(tempObs);

                        }
                        else
                        {
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
        Firebase userRef = new Firebase("https://fyp-max.firebaseio.com/trainData/" + userID + "/scrollFling");

        userRef.addValueEventListener(new ValueEventListener()
        {
            @JsonIgnoreProperties(ignoreUnknown = true)
            @Override
            public void onDataChange(DataSnapshot snapshot)
            {
                System.out.println("data: " + snapshot.toString());
                System.out.println("There are " + snapshot.getChildrenCount() + " blog posts");

                for (DataSnapshot obsSnapshot: snapshot.getChildren()) {
                    System.out.println("data: " + obsSnapshot.toString());
                    Observation obs = obsSnapshot.getValue(Observation.class);

                    trainScrollFlingObservations.add(obs);

                    /*Touch t = post.getTouch();
                    if(t instanceof ScrollFling){
                        ScrollFling sf = (ScrollFling)t;
                    }
                    System.out.println(t.toString());*/
                }

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