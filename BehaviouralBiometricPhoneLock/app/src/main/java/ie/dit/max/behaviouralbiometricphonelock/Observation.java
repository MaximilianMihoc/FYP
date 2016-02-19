package ie.dit.max.behaviouralbiometricphonelock;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Maximilian on 29/10/2015.
 */
public class Observation
{
    private Touch touch;

    private int judgement;
    private Float averageLinearAcceleration;
    private Float averageAngularVelocity;

    public Observation(Touch touch, ArrayList<Float> linearAccelerations, ArrayList<Float> angularVelocities)
    {
        this.touch = touch;
    }

    public Observation()
    {
        touch = new Touch();
        judgement = 0;
    }

    public static float calculateAVGLinearAcc(ArrayList<Float> linearAccelerations)
    {
        float sum = 0;
        float avg = 0;
        for(float la : linearAccelerations)
        {
            sum += la;
        }

        if(sum > 0 && linearAccelerations.size() > 0)
        {
            avg = sum/linearAccelerations.size();
        }

        return avg;
    }

    public static float calculateAVGAngularVelocity(ArrayList<Float> angularVelocities)
    {
        float sum = 0;
        float avg = 0;
        for(float la : angularVelocities)
        {
            sum += la;
        }

        if(sum > 0 && angularVelocities.size() > 0)
        {
            avg = sum/angularVelocities.size();
        }

        return avg;
    }


    public String toString()
    {
        return "Observation: " +
                //"tap: " + tap.toString() +
                //"scrollFling: " + scrollFling.toString() +
                "linearAccelerations: " + getAverageLinearAcceleration() +
                "angularVelocities: " + getAverageAngularVelocity();
    }

    public Float getAverageLinearAcceleration()
    {
        return averageLinearAcceleration;
    }

    public void setAverageLinearAcceleration(Float averageLinearAcceleration)
    {
        this.averageLinearAcceleration = averageLinearAcceleration;
    }

    public Float getAverageAngularVelocity()
    {
        return averageAngularVelocity;
    }

    public void setAverageAngularVelocity(Float averageAngularVelocity)
    {
        this.averageAngularVelocity = averageAngularVelocity;
    }

    public Touch getTouch()
    {
        return touch;
    }

    public void setTouch(Touch touch)
    {
        this.touch = touch;
    }

    public int getJudgement()
    {
        return judgement;
    }

    public void setJudgement(int judgement)
    {
        this.judgement = judgement;
    }
}
