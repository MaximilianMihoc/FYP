package ie.dit.max.behaviouralbiometricphonelock;

import org.opencv.core.Point;

/**
 * This class is used to save Scroll/Fling data and calculate the features used for those actions
 *
 * @author Maximilian Mihoc.
 * @version 1.0
 * @since 2nd November 2015
 */
public class ScrollFling extends Touch
{
    // static variable used to define the number of features of a Scroll/Fling Object
    public static final int numberOfFeatures = 10;

    /**
     * ScrollFling Constructor with no parameter
     */
    public ScrollFling()
    {
        super();
    }

    /**
     * Constructor with one parameter
     * When data is coming from the database, it is coming as a touch object.
     * This constructor is used to create a ScrollFling object from the touch object
     *
     * @param t Touch
     */
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

    /**
     * Method calculateMeanDirectionOfStroke
     * This method calculates the mean direction of a stroke by
     * calculating the tangent of all the points of a stroke
     * Return mean direction of stroke using the Mean Resultant Length
     *
     * The feature was removed from the feature space as it was not useful for the identification of users
     *
     * @return double MeanDirectionOfStroke
     */
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

    /**
     * Method calculateDirectEndToEndDistance
     * In this method, Euclidean distance was used to find direct end to end distance
     * The normalized values of the vectors were used so that the feature value returned would also be normalized
     *
     * @return double DirectEndToEndDistance
     */
    public double calculateDirectEndToEndDistance()
    {
        return Math.sqrt(Math.pow(scaledEndPoint.x - scaledStartPoint.x, 2) + Math.pow(scaledEndPoint.y - scaledStartPoint.y, 2));
    }

    /**
     * Method calculateAngleBetweenStartAndEndVectorsInRad
     * In this method, Cosine Formula has been used to find the feature value
     * The angle value has been returned to radians.
     * The conversion to radians was used in order to normalize the feature value
     *
     * @return double AngleBetweenStartAndEndVectorsInRad
     */
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
