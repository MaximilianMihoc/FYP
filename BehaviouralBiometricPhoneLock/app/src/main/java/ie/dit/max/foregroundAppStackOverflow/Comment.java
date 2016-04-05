package ie.dit.max.foregroundAppStackOverflow;

import java.text.DateFormat;

/**
 * This class stores one Comment details returned From Stack Exchange API
 *
 * @author Maximilian Mihoc.
 * @version 1.0
 * @since 30/01/2016
 */
public class Comment
{
    private boolean edited;
    private int score;
    private long creation_date;
    private String body;
    private Owner owner;

    /**
     * Comment constructor with 5 parameters
     *
     * @param edited boolean
     * @param score int
     * @param creation_date long
     * @param body String
     * @param owner Owner
     */
    public Comment(boolean edited, int score, long creation_date, String body, Owner owner)
    {
        this.edited = edited;
        this.score = score;
        this.creation_date = creation_date;
        this.body = body;
        this.owner = owner;
    }

    /**
     * Comment Constructor, no parameters
     */
    public Comment()
    {
    }

    public boolean isEdited()
    {
        return edited;
    }

    // getters and setters
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
        //change the date format of the date
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
