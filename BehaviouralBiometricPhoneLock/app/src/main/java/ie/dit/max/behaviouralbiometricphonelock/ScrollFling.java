package ie.dit.max.behaviouralbiometricphonelock;

import java.io.Serializable;

/**
 * Created by Maximilian on 02/11/2015.
 */
public class ScrollFling extends Touch implements Serializable
{
    public ScrollFling()
    {
        super();
    }

    @Override
    public String toString()
    {
        return "ScrollFling{" +
                "startPoint= " + startPoint +
                ", points= " + points +
                ", endPoint= " + endPoint +
                ", duration= " + duration +
                ", pressure= " + pressure +
                '}';
    }
}
