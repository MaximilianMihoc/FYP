package ie.dit.max.foregroundApp;

/**
 * Created by Maximilian on 30/01/2016.
 */
public class Owner
{
    private int reputation;
    private long user_id;
    private String display_name;

    public Owner(int reputation, long user_id, String display_name)
    {
        this.reputation = reputation;
        this.user_id = user_id;
        this.display_name = display_name;
    }

    public Owner()
    {
        this.reputation = 0;
        this.user_id = 0;
    }

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
