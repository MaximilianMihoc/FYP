package ie.dit.max.behaviouralbiometricphonelock;

import org.opencv.core.Point;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Maximilian on 02/11/2015.
 */
public class ScrollFling extends Touch
{
    public static final int numberOfFeatures = 11;

    public ScrollFling()
    {
        super();
    }

    public ScrollFling(Touch t)
    {
        super();
        startPoint = t.startPoint;
        endPoint = t.endPoint;
        scaledStartPoint = t.scaledStartPoint;
        scaledEndPoint = t.scaledEndPoint;
        duration = t.duration;
        scaledDuration = t.scaledDuration;
        midStrokeAreaCovered = t.midStrokeAreaCovered;
        meanDirectionOfStroke = t.meanDirectionOfStroke;
        directEndToEndDistance = t.directEndToEndDistance;
        angleBetweenStartAndEndVectorsInRad = t.angleBetweenStartAndEndVectorsInRad;
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

    // Return mean direction of stroke using the Mean Resultant Length
    public double calculateMeanDirectionOfStroke()
    {
        double directionsSum = 0;
        for(Point p : points)
        {
            double angle = Math.atan(p.y / p.x);
            directionsSum += Math.exp(angle);
        }

        return Math.atan((1 / points.size() - 1) * directionsSum);
    }

    public double calculateDirectEndToEndDistance()
    {
        // Apply Pitagora's theorem to find direct end to end distance.
        // Do this for the scaled values of the coordinates so that the distance would be normalised straight away.
        return Math.sqrt(Math.pow(scaledEndPoint.x - scaledStartPoint.x, 2) + Math.pow(scaledEndPoint.y - scaledStartPoint.y, 2));
    }

    public double calculateAngleBetweenStartAndEndVectorsInRad()
    {
        double dotProduct = startPoint.x * endPoint.x + startPoint.y * endPoint.y;
        double lengthStart = Math.sqrt( startPoint.x*startPoint.x + startPoint.y*startPoint.y);
        double lengthEnd = Math.sqrt( endPoint.x*endPoint.x + endPoint.y*endPoint.y);
        double angle = Math.acos(dotProduct / (lengthStart * lengthEnd));

        // Convert Degrees to Radians (180 degrees = PI rad)
        return (angle * Math.PI) / 180;
    }

    @Override
    public String toString()
    {
        return "ScrollFling{" +
                "startPoint= " + startPoint +
                ", endPoint= " + endPoint +
                ", duration= " + duration +
                '}';
    }
}
