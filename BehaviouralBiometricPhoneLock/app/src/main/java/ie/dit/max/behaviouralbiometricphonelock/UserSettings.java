package ie.dit.max.behaviouralbiometricphonelock;

/**
 * Created by Maximilian on 28/03/2016.
 */
public class UserSettings
{
    private int threshold;
    private int nrObsFromAnotherUser;

    public UserSettings()
    {
    }

    public UserSettings(int threshold, int nrObsFromAnotherUser)
    {
        this.threshold = threshold;
        this.nrObsFromAnotherUser = nrObsFromAnotherUser;
    }

    public int getThreshold()
    {
        return threshold;
    }

    public void setThreshold(int threshold)
    {
        this.threshold = threshold;
    }

    public int getNrObsFromAnotherUser()
    {
        return nrObsFromAnotherUser;
    }

    public void setNrObsFromAnotherUser(int nrObsFromAnotherUser)
    {
        this.nrObsFromAnotherUser = nrObsFromAnotherUser;
    }
}
