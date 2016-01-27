package application.myapplication;

import android.gesture.Gesture;
import android.gesture.GestureOverlayView;
import android.gesture.Prediction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;

public class GestureOverlayTest extends AppCompatActivity implements GestureOverlayView.OnGesturePerformedListener
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        GestureOverlayView gstrOvrlyVw = new GestureOverlayView(this);
        View infl = getLayoutInflater().inflate(R.layout.activity_main, null);
        gstrOvrlyVw.addView(infl);
        gstrOvrlyVw.addOnGesturePerformedListener(this);

    }

    @Override
    public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture)
    {
        Log.d("GestureOverlay", "onGesturePerformed: " + gesture.toString());
    }
}
