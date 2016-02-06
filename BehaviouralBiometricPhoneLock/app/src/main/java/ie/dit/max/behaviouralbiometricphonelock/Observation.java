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

    public float calculateAVGLinearAcc()
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

    public float calculateAVGAngularVelocity()
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
                "linearAccelerations: " + linearAccelerations.toString() +
                "angularVelocities: " + angularVelocities.toString();
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
