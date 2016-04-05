package ie.dit.max.foregroundAppStackOverflow;

import java.text.DateFormat;
import java.util.ArrayList;

/**
 * This class stores one answer details returned From Stack Exchange API
 *
 * @author Maximilian Mihoc.
 * @version 1.0
 * @since 30/01/2016
 */
public class Answer
{
    private Owner owner;
    private int comment_count;
    private int down_vote_count;
    private int up_vote_count;
    private boolean is_accepted;
    private int score;
    private long creation_date;
    private String body;
    private ArrayList<Comment> comments;

    /**
     * Answer Constructor
     *
     * @param owner Owner
     * @param comment_count int
     * @param down_vote_count int
     * @param up_vote_count int
     * @param is_accepted boolean
     * @param score int
     * @param creation_date long
     * @param body String
     */
    public Answer(Owner owner, int comment_count, int down_vote_count, int up_vote_count, boolean is_accepted, int score, long creation_date, String body)
    {
        this.owner = owner;
        this.comment_count = comment_count;
        this.down_vote_count = down_vote_count;
        this.up_vote_count = up_vote_count;
        this.is_accepted = is_accepted;
        this.score = score;
        this.creation_date = creation_date;
        this.body = body;
    }

    /**
     * Answer Constructor with no parameters
     */
    public Answer()
    {
    }

    // getters and setters
    public Owner getOwner()
    {
        return owner;
    }

    public void setOwner(Owner owner)
    {
        this.owner = owner;
    }

    public int getComment_count()
    {
        return comment_count;
    }

    public void setComment_count(int comment_count)
    {
        this.comment_count = comment_count;
    }

    public int getDown_vote_count()
    {
        return down_vote_count;
    }

    public void setDown_vote_count(int down_vote_count)
    {
        this.down_vote_count = down_vote_count;
    }

    public int getUp_vote_count()
    {
        return up_vote_count;
    }

    public void setUp_vote_count(int up_vote_count)
    {
        this.up_vote_count = up_vote_count;
    }

    public boolean is_accepted()
    {
        return is_accepted;
    }

    public void setIs_accepted(boolean is_accepted)
    {
        this.is_accepted = is_accepted;
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

    public ArrayList<Comment> getComments()
    {
        return comments;
    }

    public void setComments(ArrayList<Comment> comments)
    {
        this.comments = comments;
    }

    @Override
    public String toString()
    {
        return "Answer{" +
                "owner=" + owner +
                ", comment_count=" + comment_count +
                ", down_vote_count=" + down_vote_count +
                ", up_vote_count=" + up_vote_count +
                ", is_accepted=" + is_accepted +
                ", score=" + score +
                ", creation_date=" + creation_date +
                ", body='" + body + '\'' +
                ", comments=" + comments +
                '}';
    }
}
