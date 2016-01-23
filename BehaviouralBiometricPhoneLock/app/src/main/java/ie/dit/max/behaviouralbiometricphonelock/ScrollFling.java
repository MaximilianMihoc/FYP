package ie.dit.max.behaviouralbiometricphonelock;

import org.opencv.core.Point;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Maximilian on 02/11/2015.
 */
public class ScrollFling extends Touch implements Serializable
{
    public ScrollFling()
    {
        super();
    }

    // This method will create a list with all the points on the stroke, including start and end points
    private ArrayList<Point> normalizeStrokePoints()
    {
        ArrayList<Point> scaledPoints = new ArrayList<>();

        //Add StartPoint of Stroke to the list of points
        double magnitudeStartPoint = Math.sqrt(startPoint.x * startPoint.x + startPoint.y * startPoint.y);
        Point scaledStartPoint = new Point();
        scaledStartPoint.x = startPoint.x / magnitudeStartPoint;
        scaledStartPoint.y = startPoint.y / magnitudeStartPoint;
        scaledPoints.add(scaledStartPoint);

        // normalize all points of the stroke and store them into a new array
        for(Point p : points)
        {
            Point scaledP = new Point();
            double magnitudePointVector = Math.sqrt(p.x * p.x + p.y * p.y);
            scaledP.x = p.x / magnitudePointVector;
            scaledP.y = p.y / magnitudePointVector;

            scaledPoints.add(scaledP);
        }

        //Add EndPoint of Stroke to the list of points
        double magnitudeEndPoint = Math.sqrt(endPoint.x * endPoint.x + endPoint.y * endPoint.y);
        Point scaledEndPoint = new Point();
        scaledEndPoint.x = endPoint.x / magnitudeEndPoint;
        scaledEndPoint.y = endPoint.y / magnitudeEndPoint;
        scaledPoints.add(scaledEndPoint);

        // Add start point to the end of the list in order to apply Shoelace formula easier to find the area.
        scaledPoints.add(scaledStartPoint);

        return scaledPoints;
    }

    public double getMidStrokeAreaCovered()
    {
        ArrayList<Point> strokePoints = normalizeStrokePoints();

        double sum = 0;
        double midStrokeArea = 0;

        if (!strokePoints.isEmpty())
        {
            Point p = strokePoints.get(0);
            for(int i = 1; i < strokePoints.size() - 1; i++)
            {
                Point nextP = strokePoints.get(i+1);
                sum += ( (p.x * nextP.y) - (p.y * nextP.x) );

                p = nextP;
            }
        }

        if(sum < 0) sum = -1 * sum;
        midStrokeArea = sum / 2;

        //scale value in order to be between 0 and 1
        //if(midStrokeArea != 0) midStrokeArea = 1/midStrokeArea;

        return midStrokeArea;
    }

    @Override
    public String toString()
    {
        return "ScrollFling{" +
                "startPoint= " + startPoint +
                ", points= " + points +
                ", endPoint= " + endPoint +
                ", duration= " + duration +
                ", midStrokeArea= " + getMidStrokeAreaCovered() +
                '}';
    }
}
