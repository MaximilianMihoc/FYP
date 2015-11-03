package ie.dit.max.behaviouralbiometricphonelock;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ViewGroup;

public class MyViewGroupActivity extends ViewGroup
{


    private float mLastX;
    private float mLastY;
    private float mStartY;
    private boolean mIsBeingDragged;
    private float mTouchSlop;

    public MyViewGroupActivity(Context context)
    {
        super(context);
    }

    public MyViewGroupActivity(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public MyViewGroupActivity(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastX = event.getX();
                mLastY = event.getY();
                mStartY = mLastY;
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mIsBeingDragged = false;
                break;
            case MotionEvent.ACTION_MOVE:
                float x = event.getX();
                float y = event.getY();
                float xDelta = Math.abs(x - mLastX);
                float yDelta = Math.abs(y - mLastY);

                float yDeltaTotal = y - mStartY;
                if (yDelta > xDelta && Math.abs(yDeltaTotal) > mTouchSlop) {
                    mIsBeingDragged = true;
                    mStartY = y;
                    return true;
                }
                break;
        }
        Log.d("MyViewGroupActivity", "onInterceptTouchEvent: " + event.toString());
        return false;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b)
    {

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mIsBeingDragged = false;
                break;
            case MotionEvent.ACTION_MOVE:
                float x = event.getX();
                float y = event.getY();

                float xDelta = Math.abs(x - mLastX);
                float yDelta = Math.abs(y - mLastY);

                float yDeltaTotal = y - mStartY;
                if (!mIsBeingDragged && yDelta > xDelta && Math.abs(yDeltaTotal) > mTouchSlop) {
                    mIsBeingDragged = true;
                    mStartY = y;
                    yDeltaTotal = 0;
                }
                if (yDeltaTotal < 0)
                    yDeltaTotal = 0;

//                if (mIsBeingDragged) {
//                    scrollTo(0, yDeltaTotal);
//                }

                mLastX = x;
                mLastY = y;
                break;
        }
        Log.d("MyViewGroupActivity", "onTouchEvent: " + event.toString());
        return true;
    }
}
