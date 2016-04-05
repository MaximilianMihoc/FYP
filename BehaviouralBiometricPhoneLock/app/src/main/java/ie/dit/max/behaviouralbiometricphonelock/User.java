package ie.dit.max.behaviouralbiometricphonelock;

/**
 * This class is used to save User data
 *
 * @author Maximilian Mihoc.
 * @version 1.0
 * @since 22nd January 2016
 */
public class User
{
    private String userName;
    private String userID;
    private String email;

    /**
     * User Constructor with no parameters
     * Mostly used to instantiate user objects with information from the database.
     */
    public User()
    {

    }

    /**
     * User Constructor with 3 parameters
     *
     * @param userName String
     * @param email String
     * @param userID String
     */
    public User(String userName, String email, String userID)
    {
        this.userName = userName;
        this.email = email;
        this.userID = userID;
    }

    //getters and setters
    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getUserID()
    {
        return userID;
    }

    public void setUserID(String userID)
    {
        this.userID = userID;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }
}
