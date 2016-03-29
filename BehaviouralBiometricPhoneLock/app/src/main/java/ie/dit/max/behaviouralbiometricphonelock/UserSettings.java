package ie.dit.max.behaviouralbiometricphonelock;

/**
 * Created by Maximilian on 28/03/2016.
 */
public class UserSettings
{
    private int threshold;
    private int nrObsFromAnotherUser;
    private boolean saveTestData;



    public UserSettings()
    {
    }

    public UserSettings(int threshold, int nrObsFromAnotherUser, boolean saveTestData)
    {
        this.threshold = threshold;
        this.nrObsFromAnotherUser = nrObsFromAnotherUser;
        this.saveTestData = saveTestData;
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

    public boolean getSaveTestData()
    {
        return saveTestData;
    }

    public void setSaveTestData(boolean saveTestData)
    {
        this.saveTestData = saveTestData;
    }
}
