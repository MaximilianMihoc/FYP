package ie.dit.max.foregroundAppStackOverflow;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import ie.dit.max.behaviouralbiometricphonelock.R;

/**
 * This is a custom array adapter used to populate the Question list view from the Stack Overflow Home Screen activity
 *
 * @author Maximilian Mihoc.
 * @version 1.0
 * @since 30/01/2016
 */
public class QuestionListAdapter extends ArrayAdapter<Question>
{
    private ArrayList<Question> questions;
    private Context context;
    private LayoutInflater inflater;

    /**
     * Constructor QuestionListAdapter
     * @param context Context
     * @param items ArrayList Questions
     */
    public QuestionListAdapter(Context context, ArrayList<Question> items)
    {
        super(context, 0, items);
        this.context = context;
        this.questions = items;
    }

    //This method is executed for each item in the list in order to populate it with related data from each Question Object
    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View row = convertView;
        Question q = questions.get(position);
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(q != null)
        {
            row = inflater.inflate(R.layout.question_row, parent, false);

            TextView qTitle = (TextView) row.findViewById(R.id.questionTitle);
            qTitle.setText(Html.fromHtml(q.getTitle()));

            TextView dateAnswers = (TextView) row.findViewById(R.id.dateAndAnswersCount);
            if(q.getAnswer_count() == 1 )
                dateAnswers.setText("asked " + q.getCreation_date() + "\n" + q.getAnswer_count() + " Answer");
            else
                dateAnswers.setText("asked " + q.getCreation_date() + "\n" + q.getAnswer_count() + " Answers");

            TextView qOwner = (TextView) row.findViewById(R.id.questionOwner);
            qOwner.setText("Owner: " + q.getOwner().getDisplay_name() + " -> Reputation: " + q.getOwner().getReputation());

        }

        return row;
    }
}
