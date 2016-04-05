package ie.dit.max.behaviouralbiometricphonelock;

import java.util.ArrayList;

/**
 * This class is used to save one interaction of a user with a device
 *
 * @author Maximilian Mihoc.
 * @version 1.0
 * @since 29th October 2015
 *
 */
public class Observation
{
    private Touch touch;
    private int judgement;
    private Float averageLinearAcceleration;
    private Float averageAngularVelocity;

    /**
     * Constructor with 3 marameters
     * @param touch Touch object
     * @param linearAcceleration average linear acceleration
     * @param angularVelocity aerage angular velocity
     */
    public Observation(Touch touch, Float linearAcceleration, Float angularVelocity)
    {
        this.touch = touch;
        this.averageLinearAcceleration = linearAcceleration;
        this.averageAngularVelocity = angularVelocity;
    }

    /**
     * Constructor with no parameter.
     * Mostly used by the database when data is retrieved
     *
     */
    public Observation()
    {
        touch = new Touch();
        judgement = 0;
    }

    /**
     * Method calculateAVGLinearAcc
     * This method takes an array list of floats and calculates the average of it
     *
     * @param linearAccelerations ArrayList of linear accelerations on different time of interaction
     * @return float average
     */
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

    /**
     * Method calculateAVGAngularVelocity
     * This method takes an array list of floats and calculates the average of it
     *
     * @param angularVelocities ArrayList of angular velocities on different time of interaction
     * @return float average
     */
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

    @Override
    public String toString()
    {
        return "Observation{" +
                "touch=" + touch +
                ", judgement=" + judgement +
                ", averageLinearAcceleration=" + averageLinearAcceleration +
                ", averageAngularVelocity=" + averageAngularVelocity +
                '}';
    }

    // getters and setters for the private variables
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
