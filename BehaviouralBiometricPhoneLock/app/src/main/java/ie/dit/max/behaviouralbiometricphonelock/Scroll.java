package ie.dit.max.behaviouralbiometricphonelock;

/**
 * Created by Maximilian on 02/11/2015.
 */
public class Scroll extends Touch
{
    public Scroll()
    {
        super();
    }

    @Override
    public String toString()
    {
        return "Scroll{" +
                "startPoint= " + startPoint +
                ", points= " + points +
                ", endPoint= " + endPoint +
                ", duration= " + duration +
                '}';
    }
}
