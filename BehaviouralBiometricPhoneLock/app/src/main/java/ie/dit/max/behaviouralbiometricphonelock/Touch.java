package ie.dit.max.behaviouralbiometricphonelock;

import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Maximilian on 31/10/2015.
 */
public class Touch
{
    protected Point startPoint;
    protected Point endPoint;
    protected List<Point> points;
    protected double duration;

    public Touch()
    {
        startPoint = new Point();
        endPoint = new Point();
        points = new ArrayList<Point>();
        duration = 0;
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

    public void setPoints(List<Point> points)
    {
        this.points = points;
    }
}
