package ie.dit.max.foregroundApp;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.Calendar;

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


    public Question(long question_id, int answer_count, long creation_date, String title, Owner owner)
    {
        this.question_id = question_id;
        this.answer_count = answer_count;
        this.creation_date = creation_date;
        this.title = title;
        this.owner = owner;
    }

    public Question()
    {
    }

    public String ConvertMillisecondsToDateString(long milliseconds)
    {
        //Calendar calendar = Calendar.getInstance();
        //calendar.setTimeInMillis(milliseconds);

        return DateFormat.getDateInstance(DateFormat.SHORT).format(milliseconds);
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
        return ConvertMillisecondsToDateString(creation_date*1000);
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
}
