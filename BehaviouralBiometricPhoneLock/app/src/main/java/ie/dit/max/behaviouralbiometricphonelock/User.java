package ie.dit.max.behaviouralbiometricphonelock;

/**
 * Created by Maximilian on 22/01/2016.
 */
public class User
{
    protected String userName;
    protected String userID;
    protected String email;

    public User(String userName, String email, String userID)
    {
        this.userName = userName;
        this.email = email;
        this.userID = userID;
    }

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
