package ie.dit.max.foregroundApp;

import java.text.DateFormat;

/**
 * Created by Maximilian on 30/01/2016.
 */
public class Comment
{
    private boolean edited;
    private int score;
    private long creation_date;
    private String body;
    private Owner owner;

    public Comment(boolean edited, int score, long creation_date, String body, Owner owner)
    {
        this.edited = edited;
        this.score = score;
        this.creation_date = creation_date;
        this.body = body;
        this.owner = owner;
    }

    public Comment()
    {
    }

    public boolean isEdited()
    {
        return edited;
    }

    public void setEdited(boolean edited)
    {
        this.edited = edited;
    }

    public int getScore()
    {
        return score;
    }

    public void setScore(int score)
    {
        this.score = score;
    }

    public String getCreation_date()
    {
        return DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM).format(creation_date * 1000);
    }

    public void setCreation_date(long creation_date)
    {
        this.creation_date = creation_date;
    }

    public String getBody()
    {
        return body;
    }

    public void setBody(String body)
    {
        this.body = body;
    }

    public Owner getOwner()
    {
        return owner;
    }

    public void setOwner(Owner owner)
    {
        this.owner = owner;
    }
}
