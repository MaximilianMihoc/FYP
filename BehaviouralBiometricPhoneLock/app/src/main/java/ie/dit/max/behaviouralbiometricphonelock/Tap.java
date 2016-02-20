package ie.dit.max.behaviouralbiometricphonelock;

import android.util.Log;

import org.opencv.core.Point;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Maximilian on 31/10/2015.
 */
public class Tap extends Touch
{
    public static final int numberOfFeatures = 8;

    public Tap()
    {
        super();
    }

    public Tap(Touch t)
    {
        super();
        startPoint = t.startPoint;
        endPoint = t.endPoint;
        scaledStartPoint = t.scaledStartPoint;
        scaledEndPoint = t.scaledEndPoint;
        duration = t.duration;
        scaledDuration = t.scaledDuration;
        midStrokeAreaCovered = t.midStrokeAreaCovered;
    }

    @Override
    public String toString()
    {
        return "Tap{" +
                "startPoint= " + startPoint +
                ", points= " + points +
                ", endPoint= " + endPoint +
                ", duration= " + duration +
                ", fingerArea= " + midStrokeAreaCovered +
                '}';
    }
}
