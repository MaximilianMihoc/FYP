package ie.dit.max.behaviouralbiometricphonelock;

import android.app.Activity;
import android.support.v4.view.GestureDetectorCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.EditText;

import org.opencv.core.Point;

public class TrainActivity extends Activity implements
        GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener
{

    private static final String DEBUG_TAG = "Gestures";
    private GestureDetectorCompat mDetector;

    EditText textInstruction1;

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

    Tap tap = new Tap();

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        this.mDetector.onTouchEvent(event);

        int action = event.getAction();
        switch(action)
        {
            case (MotionEvent.ACTION_DOWN):
            {
                Point newP = new Point(event.getX(), event.getY());
                tap.setStartPoint(newP);
            }
            case (MotionEvent.ACTION_MOVE):
            {
                Point newP = new Point(event.getX(), event.getY());
                tap.addPoint(newP);
            }
            case (MotionEvent.ACTION_UP):
            {
                Point newP = new Point(event.getX(), event.getY());
                tap.setEndPoint(newP);
            }
        }

        Log.d(DEBUG_TAG, "onTouchEvent: " + event.toString());
        //Log.d(DEBUG_TAG, "Touch Count: " + event.getPointerCount());
        //Log.d(DEBUG_TAG, "Pressure: " + event.getPressure());

        Log.d(DEBUG_TAG, "Tap: " + tap.toString());

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
