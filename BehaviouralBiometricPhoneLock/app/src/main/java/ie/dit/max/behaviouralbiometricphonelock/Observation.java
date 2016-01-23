package ie.dit.max.behaviouralbiometricphonelock;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Maximilian on 29/10/2015.
 */
public class Observation implements Serializable
{
    public static final int numberOfFeatures = 9;
    private Touch gesture;
    private Tap tap;
    private ScrollFling scrollFling;
    private float linearAcceleration;
    private float lastLinearAcceleration;
    private float angularVelocity;
    private float lastAngularVelocity;

    public Observation()
    {
        gesture = new Touch();
        linearAcceleration = 0.0f;
        lastLinearAcceleration = 0.0f;
    }

    public Touch getGesture()
    {
        return gesture;
    }

    public void setGesture(Touch gesture)
    {
        this.gesture = gesture;
    }

    public String toString()
    {
        return "Observation: ";
    }

    public float getLinearAcceleration()
    {
        return linearAcceleration;
    }

    public void setLinearAcceleration(float linearAcceleration)
    {
        this.linearAcceleration = linearAcceleration;
    }

    public float getLastLinearAcceleration()
    {
        return lastLinearAcceleration;
    }

    public void setLastLinearAcceleration(float lastLinearAcceleration)
    {
        this.lastLinearAcceleration = lastLinearAcceleration;
    }

    public float getAngularVelocity()
    {
        return angularVelocity;
    }

    public void setAngularVelocity(float angularVelocity)
    {
        this.angularVelocity = angularVelocity;
    }

    public float getLastAngularVelocity()
    {
        return lastAngularVelocity;
    }

    public void setLastAngularVelocity(float lastAngularVelocity)
    {
        this.lastAngularVelocity = lastAngularVelocity;
    }

    public Tap getTap()
    {
        return tap;
    }

    public void setTap(Tap tap)
    {
        this.tap = tap;
    }

    public ScrollFling getScrollFling()
    {
        return scrollFling;
    }

    public void setScrollFling(ScrollFling scrollFling)
    {
        this.scrollFling = scrollFling;
    }
}
