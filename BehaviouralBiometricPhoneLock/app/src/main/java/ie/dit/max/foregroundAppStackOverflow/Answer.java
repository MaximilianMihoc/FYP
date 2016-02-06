package ie.dit.max.foregroundAppStackOverflow;

import java.text.DateFormat;
import java.util.ArrayList;

/**
 * Created by Maximilian on 30/01/2016.
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

    public Answer()
    {
    }

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
}
