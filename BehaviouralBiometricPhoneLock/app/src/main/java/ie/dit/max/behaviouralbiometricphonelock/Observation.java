package ie.dit.max.behaviouralbiometricphonelock;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Maximilian on 29/10/2015.
 */
public class Observation implements Serializable
{
    private ArrayList<Float> features;
    private Touch gesture;

    public Observation()
    {
        features = new ArrayList<Float>();
        gesture = new Touch();
    }

    public Touch getGesture()
    {
        return gesture;
    }

    public void setGesture(Touch gesture)
    {
        this.gesture = gesture;
    }
/*public void addGesture(Touch t)
    {
        this.gestures.add(t);
    }
    public ArrayList<Touch> getGestures()
    {
        return gestures;
    }

    public void setGestures(ArrayList<Touch> gestures)
    {
        this.gestures = gestures;
    }*/

    public List<Float> getFeatures()
    {
        return features;
    }

    public void setFeatures(ArrayList<Float> features)
    {
        this.features = features;
    }

    public String toString()
    {
        return "Observation: ";
    }

    public void addFeature(Float feature)
    {
        this.features.add(feature);
    }


}
