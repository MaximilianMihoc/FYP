package ie.dit.max.behaviouralbiometricphonelock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v4.view.GestureDetectorCompat;
import android.os.Bundle;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.List;

public class TrainActivity extends Activity implements
        GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener,
        SensorEventListener
{

    private static final String DEBUG_TAG = "Gestures";
    private GestureDetectorCompat mDetector;

    EditText textInstruction1;
    Point startPoint, endPoint;
    ArrayList<Point> points = new ArrayList<Point>();
    ArrayList<Observation> observations;

    boolean isScroll = false;
    boolean isFling = false;

    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private long lastUpdate = 0;
    private float last_x, last_y, last_z;
    private static final int SHAKE_THRESHOLD = 600;

    Button goToTest;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_train);

        mDetector = new GestureDetectorCompat(this, this);
        mDetector.setOnDoubleTapListener(this);

        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        //Point p;

        textInstruction1 = (EditText) findViewById(R.id.textInstruction1);

        observations = new ArrayList<>();

        goToTest = (Button) findViewById(R.id.testActivityBtn);
        goToTest.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent testActivityIntent = new Intent(TrainActivity.this, TestActivity.class);

                Bundle bundle = new Bundle();
                bundle.putSerializable("trainObservations", observations);
                testActivityIntent.putExtras(bundle);

                startActivity(testActivityIntent);
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        this.mDetector.onTouchEvent(event);

        Log.d(DEBUG_TAG, "onTouchEvent: " + event.toString());

        double duration;

        int action = MotionEventCompat.getActionMasked(event);
        switch(action)
        {
            case (MotionEvent.ACTION_DOWN):
            {
                startPoint = new Point(event.getX(), event.getY());

                return true;
            }
            case (MotionEvent.ACTION_MOVE):
            {
                Point newP = new Point(event.getX(), event.getY());
                points.add(newP);

                return true;
            }
            case (MotionEvent.ACTION_UP):
            {
                endPoint = new Point(event.getX(), event.getY());
                duration = event.getEventTime() - event.getDownTime();
                Observation tempObs = new Observation();

                if(isFling)
                {
                    //touch = fling
                    Fling fling = new Fling();
                    fling.setStartPoint(startPoint);
                    fling.setEndPoint(endPoint);
                    fling.setPoints(points);
                    fling.setDuration(duration);
                    fling.setPressure(event.getPressure());

                    Log.d(DEBUG_TAG, "Fling: " + fling.toString());
                    tempObs.setGesture(fling);
                }
                else if(isScroll)
                {
                    //touch = scroll
                    Scroll scroll = new Scroll();
                    scroll.setStartPoint(startPoint);
                    scroll.setEndPoint(endPoint);
                    scroll.setPoints(points);
                    scroll.setDuration(duration);
                    scroll.setPressure(event.getPressure());

                    Log.d(DEBUG_TAG, "Scroll: " + scroll.toString());
                    tempObs.setGesture(scroll);

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
                    tempObs.setGesture(tap);
                }

                // add Observation to the List of training observations
                observations.add(tempObs);

                points.clear();
                isFling = false;
                isScroll = false;
                return true;
            }
        }

        //Log.d(DEBUG_TAG, "Touch Count: " + event.getPointerCount());
        //Log.d(DEBUG_TAG, "Pressure: " + event.getPressure());

        return super.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent e)
    {
        //This event does not give me much information
        //Log.d(DEBUG_TAG, "onDown: " + e.toString());
        return true;
    }


    @Override
    public boolean onSingleTapConfirmed(MotionEvent e)
    {
        //Log.d(DEBUG_TAG, "onSingleTapConfirmed: " + e.toString());
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
        //Log.d(DEBUG_TAG, "onSingleTapUp: " + e.toString());
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
    {
        isScroll = true;
        Log.d(DEBUG_TAG, "onScroll: " + e1.toString() + " <-> " + e2.toString());
        return true;
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
        Log.d(DEBUG_TAG, "onFling: " + e1.toString() + " <-> " + e2.toString());
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_train, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent)
    {
        Sensor mySensor = sensorEvent.sensor;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER)
        {
            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];

            long curTime = System.currentTimeMillis();

            if ((curTime - lastUpdate) > 1) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;

                float speed = Math.abs(x + y + z - last_x - last_y - last_z)/ diffTime * 10000;

                if (speed > SHAKE_THRESHOLD)
                {

                }

                last_x = x;
                last_y = y;
                last_z = z;
            }

            Log.i("Accelerometer", " x= " + x + " y= " + y + " z= " + z);

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
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        senSensorManager.unregisterListener(this);
    }
}
