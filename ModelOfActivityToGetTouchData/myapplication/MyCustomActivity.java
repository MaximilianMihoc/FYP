package application.myapplication;

import android.app.ListActivity;
import android.content.Context;
import android.gesture.GestureOverlayView;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class MyCustomActivity extends ListActivity implements
        GestureDetector.OnGestureListener,
        GestureOverlayView.OnGestureListener,
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

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        /*WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY, 0,
                PixelFormat.TRANSLUCENT );

        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        params.gravity = Gravity.RIGHT | Gravity.LEFT;
        WindowManager vm = (WindowManager) getSystemService(WINDOW_SERVICE);

        LayoutInflater inflater = LayoutInflater.from(MyCustomActivity.this);
        ViewGroup mTopView = (ViewGroup) inflater.inflate(R.layout.activity_main, null);
        getWindow().setAttributes(params);
        vm.addView(mTopView, params);*/

        mDetector = new GestureDetector(this, this);

        gestureListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return mDetector.onTouchEvent(event);
            }};

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

    }

    /*This method will be called when there is no other listener that triggers the same
    * event. When the user is scrolling on an empty layout or over some text.
    * */
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        // this will call the gesture motion events when the touch happened on a layout without any
        // widgets.
        this.mDetector.onTouchEvent(event);

        // add linear accelerations to the Observation
        Log.d(DEBUG_TAG, "Linear Accelerations on touch - lastLinearAcceleration: " + lastLinearAcceleration + " LinearAcceleration: " + linearAcceleration);
        // add angular velocity to the Observation on touch gesture
        Log.d(DEBUG_TAG, "Angular Velocity on touch - lastAngularVelocity: " + lastAngularVelocity + " Angular Velocity: " + angularVelocity);
        return super.onTouchEvent(event);
    }

    /*
    *   The methods implemented in the following methods are used to collect data from touch actions
    *   no matter if there is another event that needs to be executed with the event.
    *
    * */

    @Override
    public boolean onDown(MotionEvent e)
    {
        Log.d(DEBUG_TAG, "OnDown: " + e.toString());
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e)
    {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e)
    {
        Log.d(DEBUG_TAG, "onSingleTapUp: " + e.toString());
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
    {
        isScroll = true;
        Log.d(DEBUG_TAG, "onScroll: " + e1.toString() + " <-> " + e2.toString());
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
        Log.d(DEBUG_TAG, "onFling: " + e1.toString() + " <-> " + e2.toString());
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

    @Override
    public void onGestureStarted(GestureOverlayView overlay, MotionEvent event)
    {
        Log.d("onGestureStarted", "overlay: " + overlay.toString() + " <--------------------> event: " + event.toString());
    }

    @Override
    public void onGesture(GestureOverlayView overlay, MotionEvent event)
    {
        Log.d("onGesture", "overlay: " + overlay.toString() + " <--------------------> event: " + event.toString());
    }

    @Override
    public void onGestureEnded(GestureOverlayView overlay, MotionEvent event)
    {
        Log.d("onGestureEnded", "overlay: " + overlay.toString() + " <--------------------> event: " + event.toString());
    }

    @Override
    public void onGestureCancelled(GestureOverlayView overlay, MotionEvent event)
    {
        Log.d("onGestureCancelled", "overlay: " + overlay.toString() + " <--------------------> event: " + event.toString());
    }
}
