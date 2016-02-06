package ie.dit.max.behaviouralbiometricphonelock;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Maximilian on 29/10/2015.
 */
public class Observation
{
    public static final int numberOfFeatures = 9;

    private Touch touch;
    private ArrayList<Float> linearAccelerations;
    private ArrayList<Float> angularVelocities;
    private int judgement;

    public Observation(Touch touch, ArrayList<Float> linearAccelerations, ArrayList<Float> angularVelocities)
    {
        this.touch = touch;
        this.linearAccelerations = linearAccelerations;
        this.angularVelocities = angularVelocities;
    }

    public Observation()
    {
        touch = new Touch();
        linearAccelerations = new ArrayList<>();
        angularVelocities = new ArrayList<>();
        judgement = 0;
    }

    public String toString()
    {
        return "Observation: " +
                //"tap: " + tap.toString() +
                //"scrollFling: " + scrollFling.toString() +
                "linearAccelerations: " + linearAccelerations.toString() +
                "angularVelocities: " + angularVelocities.toString();
    }

    public static int getNumberOfFeatures()
    {
        return numberOfFeatures;
    }

    public ArrayList<Float> getLinearAccelerations()
    {
        return linearAccelerations;
    }

    public void setLinearAccelerations(ArrayList<Float> linearAccelerations)
    {
        this.linearAccelerations = linearAccelerations;
    }

    public ArrayList<Float> getAngularVelocities()
    {
        return angularVelocities;
    }

    public void setAngularVelocities(ArrayList<Float> angularVelocities)
    {
        this.angularVelocities = angularVelocities;
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
