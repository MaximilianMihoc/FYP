package ie.dit.max.behaviouralbiometricphonelock;

import org.opencv.core.Point;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Maximilian on 02/11/2015.
 */
public class ScrollFling extends Touch
{
    public ScrollFling()
    {
        super();
    }

    // This method will create a list with all the points on the stroke, including start and end points
    public ArrayList<Point> normalizeStrokePoints()
    {
        ArrayList<Point> scaledPoints = new ArrayList<>();

        //Add StartPoint of Stroke to the list of points
        scaledPoints.add(getScaledStartPoint());

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
        scaledPoints.add(getScaledEndPoint());

        // Add start point to the end of the list in order to apply Shoelace formula easier to find the area.
        scaledPoints.add(getScaledStartPoint());

        //System.out.println("Points in scroll/field: " + scaledPoints.toString());

        return scaledPoints;
    }

    public double calculateMidStrokeAreaCovered()
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

    public double calculateDirectionOfEndToEndLine()
    {
        double direction;
        direction = (endPoint.y - startPoint.y)/(endPoint.x - startPoint.x);

        System.out.println("Direction angle: " + 1/Math.atan2(endPoint.y - startPoint.y, endPoint.x - startPoint.x)) ;

        return Math.atan2(endPoint.y - startPoint.y, endPoint.x - startPoint.x);
    }

    @Override
    public String toString()
    {
        return "ScrollFling{" +
                "startPoint= " + startPoint +
                ", points= " + points +
                ", endPoint= " + endPoint +
                ", duration= " + duration +
                ", midStrokeArea= " + calculateMidStrokeAreaCovered() +
                ",direction= " + calculateDirectionOfEndToEndLine() +
                '}';
    }
}
