package ie.dit.max.behaviouralbiometricphonelock;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Maximilian on 29/10/2015.
 */
public class Observation
{
    private String gesture;
    private List<Float> features;

     public Observation()
     {
         gesture = "";
         features = new ArrayList<Float>();
     }

    public String toString()
    {
        return "Observation: ";
    }

    public String getGesture()
    {
        return gesture;
    }

    public void setGesture(String gest)
    {
        this.gesture = gest;
    }

    public void addFeature(Float feature)
    {
        this.features.add(feature);
    }


}
