package ie.dit.max.behaviouralbiometricphonelock;

import android.app.Activity;
import android.support.v4.view.GestureDetectorCompat;
import android.os.Bundle;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.EditText;

import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.List;

public class TrainActivity extends Activity implements
        GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener
{

    private static final String DEBUG_TAG = "Gestures";
    private GestureDetectorCompat mDetector;

    EditText textInstruction1;
    Point startPoint, endPoint;
    List<Point> points = new ArrayList<Point>();

    boolean isScroll = false;
    boolean isFling = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_train);

        mDetector = new GestureDetectorCompat(this, this);
        mDetector.setOnDoubleTapListener(this);

        //Point p;

        textInstruction1 = (EditText) findViewById(R.id.textInstruction1);
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

                if(isFling)
                {
                    //touch = fling
                    Fling fling = new Fling();
                    fling.setStartPoint(startPoint);
                    fling.setEndPoint(endPoint);
                    fling.setPoints(points);
                    fling.setDuration(duration);

                    Log.d(DEBUG_TAG, "Fling: " + fling.toString());
                }
                else if(isScroll)
                {
                    //touch = scroll
                    Scroll scroll = new Scroll();
                    scroll.setStartPoint(startPoint);
                    scroll.setEndPoint(endPoint);
                    scroll.setPoints(points);
                    scroll.setDuration(duration);

                    Log.d(DEBUG_TAG, "Scroll: " + scroll.toString());
                }
                else
                {
                    //touch = tap
                    Tap tap = new Tap();
                    tap.setStartPoint(startPoint);
                    tap.setEndPoint(endPoint);
                    tap.setPoints(points);
                    tap.setDuration(duration);

                    Log.d(DEBUG_TAG, "Tap: " + tap.toString());
                }

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
        return false;
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
}
