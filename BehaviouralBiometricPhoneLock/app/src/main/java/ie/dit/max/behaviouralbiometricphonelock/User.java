package ie.dit.max.behaviouralbiometricphonelock;

import java.io.Serializable;

/**
 * Created by Maximilian on 22/01/2016.
 */
public class User
{
    private String userName;
    private String userID;
    private String email;

    public User()
    {
    }

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
