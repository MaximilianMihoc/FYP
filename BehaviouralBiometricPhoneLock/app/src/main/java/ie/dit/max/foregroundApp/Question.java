package ie.dit.max.foregroundApp;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Maximilian on 30/01/2016.
 */
public class Question implements Serializable
{
    private long question_id;
    private int answer_count;
    private long creation_date;
    private String title;
    private Owner owner;
    private String body;


    public Question(long question_id, int answer_count, long creation_date, String title, Owner owner, String body)
    {
        this.question_id = question_id;
        this.answer_count = answer_count;
        this.creation_date = creation_date;
        this.title = title;
        this.owner = owner;
        this.body = body;
    }

    public Question()
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

    public long getQuestion_id()
    {
        return question_id;
    }

    public void setQuestion_id(long question_id)
    {
        this.question_id = question_id;
    }

    public int getAnswer_count()
    {
        return answer_count;
    }

    public void setAnswer_count(int answer_count)
    {
        this.answer_count = answer_count;
    }

    public String getCreation_date() // return Date in Date Format
    {
        return DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM).format(creation_date * 1000);
    }

    public void setCreation_date(long creation_date)
    {
        this.creation_date = creation_date;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getBody()
    {
        return body;
    }

    public void setBody(String body)
    {
        this.body = body;
    }
}
