package application.myapplication;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.gesture.GestureOverlayView;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

/**
 * Created by Maximilian on 26/01/2016.
 */
public class GlobalTouchService extends Service implements
        GestureDetector.OnGestureListener,
        SensorEventListener
{
    public static final String DEBUG_TAG = "Gestures";
    public GestureDetector mDetector;

    //to be used for list views.
    public View.OnTouchListener gestureListener;

    boolean isScroll = false;
    boolean isFling = false;

    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private Sensor senGyroscope;

    //private long lastUpdate = 0;
    //private float last_x, last_y, last_z;
    //private static final int SHAKE_THRESHOLD = 600;

    private float lastLinearAcceleration;
    private float linearAcceleration;

    private float lastAngularVelocity;
    private float angularVelocity;

    View view;

    private String TAG = this.getClass().getSimpleName();
    // window manager
    private WindowManager mWindowManager;
    // linear layout will use to detect touch event
    private LinearLayout touchLayout;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // create linear layout
        touchLayout = new LinearLayout(this);
        // set layout width 30 px and height is equal to full screen
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(30, LinearLayout.LayoutParams.MATCH_PARENT);
        touchLayout.setLayoutParams(lp);
        // set color if you want layout visible on screen
		//touchLayout.setBackgroundColor(Color.CYAN);
        // set on touch listener
        //touchLayout.setOnTouchListener(this);

        // fetch window manager object
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        // set layout parameter of window manager
        WindowManager.LayoutParams mParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_TOAST,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSPARENT);

        mParams.gravity = Gravity.LEFT | Gravity.TOP;
        Log.d(TAG, "add View");

        mDetector = new GestureDetector(this, this);

        touchLayout.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                mDetector.onTouchEvent(event);

                //Log.d(DEBUG_TAG, "onTouchEvent: " + event.toString());

                double duration;

                int action = MotionEventCompat.getActionMasked(event);
                switch(action)
                {
                    case (MotionEvent.ACTION_DOWN):
                    {

                        Log.d(DEBUG_TAG, "ACTION_DOWN: " + event.toString());

                        return true;
                    }
                    case (MotionEvent.ACTION_MOVE):
                    {
                        Log.d(DEBUG_TAG, "ACTION_MOVE: " + event.toString());

                        return true;
                    }
                    case (MotionEvent.ACTION_UP):
                    {
                        Log.d(DEBUG_TAG, "ACTION_UP: " + event.toString());

                        // add linear accelerations to the Observation
                        Log.d(DEBUG_TAG, "Linear Accelerations on touch - lastLinearAcceleration: " + lastLinearAcceleration + " LinearAcceleration: " + linearAcceleration);

                        // add angular velocity to the Observation on touch gesture
                        Log.d(DEBUG_TAG, "Angular Velocity on touch - lastAngularVelocity: " + lastAngularVelocity + " Angular Velocity: " + angularVelocity);

                        // add Observation to the List of training observations. Separate list of obs for tap gesture.
                        isFling = false;
                        isScroll = false;
                        return true;
                    }
                    default:
                    {
                        Log.d(DEBUG_TAG, "DEFAULT: " + event.toString());
                    }
                }
                return mDetector.onTouchEvent(event);
            }
        });

        linearAcceleration = 0.0f;
        lastLinearAcceleration = 0.0f;

        angularVelocity = 0.0f;
        lastAngularVelocity = 0.0f;

        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Accelerometer declarations
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);

        // Gyroscope declarations
        senGyroscope = senSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        senSensorManager.registerListener(this, senGyroscope, SensorManager.SENSOR_DELAY_FASTEST);

        mWindowManager.addView(touchLayout, mParams);

    }

    @Override
    public void onDestroy() {
        if(mWindowManager != null) {
            if(touchLayout != null) mWindowManager.removeView(touchLayout);
        }
        super.onDestroy();
    }

    @Override
    public boolean onDown(MotionEvent e)
    {
        //Log.d(DEBUG_TAG, "ACTION_Down: " + e.toString());
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e)
    {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e)
    {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
    {
        isScroll = true;
        //Log.d(DEBUG_TAG, "onScroll: " + e1.toString() + " <-> " + e2.toString());
        return true;
    }

    @Override
    public void onLongPress(MotionEvent e)
    {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
    {
        isFling = true;
        //Log.d(DEBUG_TAG, "onFling: " + e1.toString() + " <-> " + e2.toString());
        return true;
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

            lastAngularVelocity = angularVelocity;
            angularVelocity = (float) Math.sqrt((double) (x*x + y*y + z*z));

        }

        if (mySensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION)
        {
            //this sensor gives me the linear acceleration values for x, y, z.
            // linear acceleration is the acceleration - earth gravity.

            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];

            lastLinearAcceleration = linearAcceleration;
            linearAcceleration = (float) Math.sqrt((double) (x*x + y*y + z*z));

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {

    }
}

