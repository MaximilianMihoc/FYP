package ie.dit.max.behaviouralbiometricphonelock;

import org.opencv.core.Point;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Maximilian on 02/11/2015.
 */
public class ScrollFling extends Touch
{
    public static final int numberOfFeatures = 10;

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
