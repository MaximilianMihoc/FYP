package ie.dit.max.behaviouralbiometricphonelock;

/**
 * Created by Maximilian on 02/11/2015.
 */
public class Fling extends Touch
{
    public Fling()
    {
        super();
    }

    @Override
    public String toString()
    {
        return "Fling{" +
                "startPoint= " + startPoint +
                ", points= " + points +
                ", endPoint= " + endPoint +
                ", duration= " + duration +
                ", pressure= " + pressure +
                '}';
    }
}
