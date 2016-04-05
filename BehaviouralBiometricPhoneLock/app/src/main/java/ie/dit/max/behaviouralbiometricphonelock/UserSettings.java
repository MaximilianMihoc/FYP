package ie.dit.max.behaviouralbiometricphonelock;

/**
 * This class is used to save User Settings data
 *
 * @author Maximilian Mihoc.
 * @version 1.0
 * @since 28th March 2016
 */
public class UserSettings
{
    private int threshold;
    private int nrObsFromAnotherUser;
    private boolean saveTestData;

    /**
     * User Constructor with no parameters
     * Mostly used to instantiate user objects with information from the database.
     */
    public UserSettings()
    {
    }

    /**
     * UserSettings Constructor with 3 parameters
     *
     * @param threshold int
     * @param nrObsFromAnotherUser int
     * @param saveTestData boolean
     */
    public UserSettings(int threshold, int nrObsFromAnotherUser, boolean saveTestData)
    {
        this.threshold = threshold;
        this.nrObsFromAnotherUser = nrObsFromAnotherUser;
        this.saveTestData = saveTestData;
    }

    //getters and setters
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
