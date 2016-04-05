package ie.dit.max.behaviouralbiometricphonelock;

/**
 * This class is used to save Tap data and calculate the features used for this action
 *
 * @author Maximilian Mihoc.
 * @version 1.0
 * @since 31st October 2015
 */
public class Tap extends Touch
{
    //static variable used to define the number of features of a Tap Object
    public static final int numberOfFeatures = 8;

    /**
     * Tap Constructor with no parameter
     */
    public Tap()
    {
        super();
    }

    /**
     * Constructor with one parameter
     * When data is coming from the database, it is coming as a touch object.
     * This constructor is used to create a Tap object from the touch object
     *
     * @param t Touch
     */
    public Tap(Touch t)
    {
        super();
        startPoint = t.startPoint;
        endPoint = t.endPoint;
        scaledStartPoint = t.scaledStartPoint;
        scaledEndPoint = t.scaledEndPoint;
        duration = t.duration;
        scaledDuration = t.scaledDuration;
        midStrokeAreaCovered = t.midStrokeAreaCovered;
    }

    @Override
    public String toString()
    {
        return "Tap{" +
                "startPoint= " + startPoint +
                ", endPoint= " + endPoint +
                ", duration= " + duration +
                '}';
    }
}
