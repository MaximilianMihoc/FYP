package ie.dit.max.foregroundAppStackOverflow;

import android.content.Context;
import android.graphics.Color;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import ie.dit.max.behaviouralbiometricphonelock.R;

/**
 * This is a custom array adapter used to populate the answers list from the Answers Screen
 *
 * @author Maximilian Mihoc.
 * @version 1.0
 * @since 30/01/2016
 */
public class AnswerListAdapter extends ArrayAdapter<Answer>
{
    private String[] colors = new String[] { "#C7C3D0", "#9A92AB" };
    private ArrayList<Answer> answers;
    private Context context;
    private LayoutInflater inflater;

    /**
     * Constructor AnswerListAdapter
     *
     * @param context Context
     * @param items ArrayList of answers
     */
    public AnswerListAdapter(Context context, ArrayList<Answer> items)
    {
        super(context, 0, items);
        this.context = context;
        this.answers = items;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        /*
        * This method is executed for each item in the list in order to populate it with related data.
        * */
        View row = convertView;
        Answer answer = answers.get(position);
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(answer != null)
        {
            row = inflater.inflate(R.layout.answer_row, parent, false);
            TextView answerBody = (TextView) row.findViewById(R.id.answerBody);

            int colorPos = position % colors.length;
            // The answer accepted as the Correct answer has a green background
            if(answer.is_accepted())
            {
                row.setBackgroundColor(Color.parseColor("#59F059"));
                answerBody.setText(Html.fromHtml("<b>Accepted Answer</b><br/>" + answer.getBody()));
            }
            else
            {
                row.setBackgroundColor(Color.parseColor(colors[colorPos]));
                answerBody.setText(Html.fromHtml(answer.getBody()));
            }

            TextView answerOwner = (TextView) row.findViewById(R.id.answerBodyOwner);
            answerOwner.setText(Html.fromHtml("answered: " + answer.getCreation_date() + "<br/>By: " + answer.getOwner().getDisplay_name()));

            // display comments related to each answer if they exist.
            // In case they do not exist, hide the widgets used to display comments.
            if(answer.getComment_count() > 0)
            {
                TextView comments = (TextView) row.findViewById(R.id.answerComments);
                String comStr = "<b>Comments:</b><br/>";
                ArrayList<Comment> coms = answer.getComments();
                for (int i = 0; i < coms.size(); i++)
                {
                    Comment c = coms.get(i);
                    comStr += (i+1) + ". " + c.getBody() + " <br/> By: " + c.getOwner().getDisplay_name() + " - " + c.getCreation_date() + "<br/><br/>";
                }
                comments.setText(Html.fromHtml(comStr));
            }
            else
            {
                TextView comments = (TextView) row.findViewById(R.id.answerComments);
                comments.setVisibility(View.INVISIBLE);
                View viewComments1 = row.findViewById(R.id.viewComments1);
                viewComments1.setVisibility(View.INVISIBLE);
                View viewComments2 = row.findViewById(R.id.viewComments2);
                viewComments2.setVisibility(View.INVISIBLE);
            }
        }

        return row;
    }
}
