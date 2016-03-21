package ie.dit.max.behaviouralbiometricphonelock;

import org.opencv.core.Point;

import java.util.ArrayList;

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
