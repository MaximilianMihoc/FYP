package ie.dit.max.behaviouralbiometricphonelock;

import java.io.Serializable;

/**
 * Created by Maximilian on 02/11/2015.
 */
public class Fling extends Touch implements Serializable
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
