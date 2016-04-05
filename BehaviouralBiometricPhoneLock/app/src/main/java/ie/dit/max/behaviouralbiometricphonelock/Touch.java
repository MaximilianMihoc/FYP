package ie.dit.max.behaviouralbiometricphonelock;

import org.opencv.core.Point;

import java.util.ArrayList;

/**
 * This class is used to save touch data of one interaction and calculate common features of Scroll/Fling and Tap
 *
 * @author Maximilian Mihoc.
 * @version 1.0
 * @since 31st October 2015
 *
 */
public class Touch
{
    protected Point startPoint;
    protected Point endPoint;
    protected ArrayList<Point> points;
    protected double duration;
    protected Point scaledStartPoint;
    protected Point scaledEndPoint;
    protected double scaledDuration;
    protected Double midStrokeAreaCovered;
    protected Double meanDirectionOfStroke;
    protected Double directEndToEndDistance;
    protected Double angleBetweenStartAndEndVectorsInRad;

    /**
     *  Constructor used to instantiate the Object
     */
    public Touch()
    {
        startPoint = new Point();
        endPoint = new Point();
        scaledStartPoint = new Point();
        scaledEndPoint = new Point();
        points = new ArrayList<>();
        duration = 0;
        scaledDuration = 0;
    }

    /**
     * Method getScaledStartPoint
     * This method returns the StartPoint of the touch action, normalised between 0 and 1
     * The normalisation had been made using the magnitude of the vector
     *
     * @return Point
     */
    public Point getScaledStartPoint()
    {
        // unit length for my vector is 1
        double magnitudeStartVector = Math.sqrt(startPoint.x * startPoint.x + startPoint.y * startPoint.y);
        scaledStartPoint.x = startPoint.x / magnitudeStartVector;
        scaledStartPoint.y = startPoint.y / magnitudeStartVector;

        return scaledStartPoint;
    }

    /**
     * Method getScaledEndPoint
     * This method returns the EndPoint of the touch action, normalised between 0 and 1
     * The normalisation had been made using the magnitude of the vector
     *
     * @return Point
     */
    public Point getScaledEndPoint()
    {
        //scale data for end Vector
        double magnitudeEndVector = Math.sqrt(endPoint.x * endPoint.x + endPoint.y * endPoint.y);
        scaledEndPoint.x = endPoint.x / magnitudeEndVector;
        scaledEndPoint.y = endPoint.y / magnitudeEndVector;

        return scaledEndPoint;
    }

    /**
     * Method getScaledDuration
     * Scale duration and save it into scaledDuration variable
     * To scale duration, transform it from milliseconds to seconds.
     * Touch actions usually do not take more than a second and scaling this to seconds would
     * keep data normalised between 0 and 1
     *
     * @return double duration
     */
    public double getScaledDuration()
    {
        scaledDuration = duration / 1000;
        return scaledDuration;
    }

    /**
     * Method normalizeStrokePoints
     * This method will create a list with all the points of the stroke, including start and end points
     * The returned list contains all the stroke points normalized
     * Magnitude vector has been used for each point in order to be normalized
     *
     * @return ArrayList of Points
     */
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

        return scaledPoints;
    }

    /**
     * Method calculateMidStrokeAreaCovered
     * Using the normalised version of a stroke, Mid stroke area covered feature is calculated.
     * The area is calculated using Shoelace formula
     *
     * @return double midStrokeArea
     */
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

        return midStrokeArea;
    }

    //getters and setters to return and set protected variables in other objects
    public Double getMidStrokeAreaCovered()
    {
        return midStrokeAreaCovered;
    }

    public void setMidStrokeAreaCovered(Double midStrokeAreaCovered)
    {
        this.midStrokeAreaCovered = midStrokeAreaCovered;
    }

    public Double getMeanDirectionOfStroke()
    {
        return meanDirectionOfStroke;
    }

    public void setMeanDirectionOfStroke(Double meanDirectionOfStroke)
    {
        this.meanDirectionOfStroke = meanDirectionOfStroke;
    }

    public void setScaledStartPoint(Point scaledStartPoint)
    {
        this.scaledStartPoint = scaledStartPoint;
    }

    public Double getDirectEndToEndDistance()
    {
        return directEndToEndDistance;
    }

    public void setDirectEndToEndDistance(Double directEndToEndDistance)
    {
        this.directEndToEndDistance = directEndToEndDistance;
    }

    public Double getAngleBetweenStartAndEndVectorsInRad()
    {
        return angleBetweenStartAndEndVectorsInRad;
    }

    public void setAngleBetweenStartAndEndVectorsInRad(Double angleBetweenStartAndEndVectorsInRad)
    {
        this.angleBetweenStartAndEndVectorsInRad = angleBetweenStartAndEndVectorsInRad;
    }

    public void setScaledEndPoint(Point scaledEndPoint)
    {
        this.scaledEndPoint = scaledEndPoint;
    }

    public void setScaledDuration(double scaledDuration)
    {
        this.scaledDuration = scaledDuration;
    }

    public double getDuration()
    {
        return this.duration;
    }

    public void setDuration(double tempDuration)
    {
        this.duration = tempDuration;
    }

    public Point getEndPoint()
    {
        return endPoint;
    }

    public void setEndPoint(Point endPoint)
    {
        this.endPoint = endPoint;
    }

    public Point getStartPoint()
    {
        return startPoint;
    }

    public void setStartPoint(Point startPoint)
    {
        this.startPoint = startPoint;
    }

    public ArrayList<Point> retrievePoints()
    {
        return points;
    }

    public void initialisePoints(ArrayList<Point> points)
    {
        this.points = points;
    }
}
