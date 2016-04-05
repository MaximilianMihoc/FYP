package ie.dit.max.foregroundAppStackOverflow;

import java.io.Serializable;

/**
 * Class used to store Owner details from Stack Exchange API.
 * Owner is the person that wrote a Question, and Answer of a Comment on Stack Overflow.
 *
 * @author Maximilian Mihoc.
 * @version 1.0
 * @since 30/01/2016
 */
public class Owner implements Serializable
{
    private int reputation;
    private long user_id;
    private String display_name;

    /**
     * Constructor for Owner Object with 3 parameters
     *
     * @param reputation int
     * @param user_id long
     * @param display_name String
     */
    public Owner(int reputation, long user_id, String display_name)
    {
        this.reputation = reputation;
        this.user_id = user_id;
        this.display_name = display_name;
    }

    /**
     * Constructor with no parameters
     */
    public Owner()
    {
        this.reputation = 0;
        this.user_id = 0;
    }

    // getters and setters.
    public int getReputation()
    {
        return reputation;
    }

    public void setReputation(int reputation)
    {
        this.reputation = reputation;
    }

    public long getUser_id()
    {
        return user_id;
    }

    public void setUser_id(long user_id)
    {
        this.user_id = user_id;
    }

    public String getDisplay_name()
    {
        return display_name;
    }

    public void setDisplay_name(String display_name)
    {
        this.display_name = display_name;
    }
}
