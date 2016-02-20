package ie.dit.max.behaviouralbiometricphonelock;

import org.opencv.core.Point;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Maximilian on 31/10/2015.
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

    public Point getScaledStartPoint()
    {
        // unit length for my vector is 1
        //scale data for Start Vector
        double magnitudeStartVector = Math.sqrt(startPoint.x * startPoint.x + startPoint.y * startPoint.y);
        scaledStartPoint.x = startPoint.x / magnitudeStartVector;
        scaledStartPoint.y = startPoint.y / magnitudeStartVector;

        return scaledStartPoint;
    }

    public Point getScaledEndPoint()
    {
        //scale data for end Vector
        double magnitudeEndVector = Math.sqrt(endPoint.x * endPoint.x + endPoint.y * endPoint.y);
        scaledEndPoint.x = endPoint.x / magnitudeEndVector;
        scaledEndPoint.y = endPoint.y / magnitudeEndVector;

        return scaledEndPoint;
    }

    public double getScaledDuration()
    {
        /*
        * Scale duration and save it into scaledDuration variable
        * To scale duration, transform it from milliseconds to seconds.
        * Touch actions usually do not take more than a second and scaling this to seconds would
        * keep data close to my other features data
        * */
        scaledDuration = duration / 1000;

        return scaledDuration;
    }

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
