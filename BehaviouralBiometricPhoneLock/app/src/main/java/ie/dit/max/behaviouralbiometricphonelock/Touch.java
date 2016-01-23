package ie.dit.max.behaviouralbiometricphonelock;

import org.opencv.core.Point;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Maximilian on 31/10/2015.
 */
public class Touch implements Serializable
{
    protected Point startPoint;
    protected Point endPoint;
    protected ArrayList<Point> points;
    protected double duration;
    protected double pressure;
    protected Point scaledStartPoint;
    protected Point scaledEndPoint;
    protected double scaledDuration;

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

    public void scaleData()
    {
        // unit length for my vector is 1
        //scale data for Start Vector
        double magnitudeStartVector = Math.sqrt(this.startPoint.x * this.startPoint.x + this.startPoint.y * this.startPoint.y);
        this.scaledStartPoint.x = this.startPoint.x / magnitudeStartVector;
        this.scaledStartPoint.y = this.startPoint.y / magnitudeStartVector;

        //scale data for end Vector
        double magnitudeEndVector = Math.sqrt(this.endPoint.x * this.endPoint.x + this.endPoint.y * this.endPoint.y);
        this.scaledEndPoint.x = this.endPoint.x / magnitudeEndVector;
        this.scaledEndPoint.y = this.endPoint.y / magnitudeEndVector;

        /*
        * Scale duration and save it into scaledDuration variable
        * To scale duration, transform it from milliseconds to seconds.
        * Touch actions usually do not take more than a second and scaling this to seconds would
        * keep data close to my other features data
        * */
        this.scaledDuration = this.duration / 1000;

    }

    public Point getScaledStartPoint()
    {
        return scaledStartPoint;
    }

    public Point getScaledEndPoint()
    {
        return scaledEndPoint;
    }

    public double getScaledDuration()
    {
        return scaledDuration;
    }

    public void addPoint(Point p)
    {
        this.points.add(p);
    }

    public void clearPoints()
    {
        points.clear();
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

    public List<Point> getPoints()
    {
        return points;
    }

    public void setPoints(ArrayList<Point> points)
    {
        this.points = points;
    }

    public double getPressure()
    {
        return pressure;
    }

    public void setPressure(double pressure)
    {
        this.pressure = pressure;
    }
}
