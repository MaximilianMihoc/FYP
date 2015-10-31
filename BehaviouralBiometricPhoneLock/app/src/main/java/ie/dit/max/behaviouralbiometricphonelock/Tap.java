package ie.dit.max.behaviouralbiometricphonelock;

import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Maximilian on 31/10/2015.
 */
public class Tap extends Touch
{
    public Tap()
    {
        super();
    }

    public double getFingerArea()
    {
        
        return 0;
    }

    @Override
    public String toString()
    {
        return "Tap{" +
                "startPoint= " + startPoint +
                ", points= " + points +
                ", endPoint= " + endPoint +
                ", duration= " + duration +
                '}';
    }
}
