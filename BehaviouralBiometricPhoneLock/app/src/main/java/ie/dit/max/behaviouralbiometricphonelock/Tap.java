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
    public static final int numberOfFeatures = 10;
    protected ArrayList<Point> scaledPoints;

    public Tap()
    {
        super();
        scaledPoints = new ArrayList<>();
    }

    private void scalePoints(ArrayList<Point> points)
    {
        for(Point p : points)
        {
            Point scaledP = new Point();
            double magnitudePointVector = Math.sqrt(p.x * p.x + p.y * p.y);
            scaledP.x = p.x / magnitudePointVector;
            scaledP.y = p.y / magnitudePointVector;

            scaledPoints.add(scaledP);
        }
    }

    public double calculateFingerArea()
    {
        double sum = 0;
        double fingerArea = 0;

        // calculate finger area using Shoelace formula

        if (!points.isEmpty())
        {
            for(int i = 0; i < points.size() - 1; i++)
            {
                Point p = points.get(i);

                if(i == 0)
                {
                    sum += ( (startPoint.x * p.y) - (startPoint.y * p.x) );
                }
                else
                {
                    Point nextP = points.get(i+1);
                    sum += ( (p.x * nextP.y) - (p.y * nextP.x) );
                }
            }
        }
        sum += ( (endPoint.x * startPoint.y) - (endPoint.y * startPoint.x) );

        //System.out.println("Points in Tap: " + points.toString());

        if(sum < 0) sum = -1 * sum;
        fingerArea = sum / 2;

        //scale value in order to be between 0 and 1
        if(fingerArea != 0) fingerArea = 100/fingerArea;

        return fingerArea;
    }

    @Override
    public String toString()
    {
        return "Tap{" +
                "startPoint= " + startPoint +
                ", points= " + points +
                ", endPoint= " + endPoint +
                ", duration= " + duration +
                ", pressure= " + pressure +
                ", fingerArea= " + calculateFingerArea() +
                '}';
    }
}
