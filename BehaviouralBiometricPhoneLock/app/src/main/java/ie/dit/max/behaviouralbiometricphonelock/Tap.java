package ie.dit.max.behaviouralbiometricphonelock;

import org.opencv.core.Point;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Maximilian on 31/10/2015.
 */
public class Tap extends Touch implements Serializable
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

    public double getFingerArea()
    {
        double sum = 0;
        double fingerArea = 0;

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

        if(sum < 0) sum = -1 * sum;
        fingerArea = sum / 2;

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
                ", fingerArea= " + getFingerArea() +
                '}';
    }
}
