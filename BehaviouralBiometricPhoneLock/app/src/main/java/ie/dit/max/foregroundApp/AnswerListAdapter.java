package ie.dit.max.foregroundApp;

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
 * Created by Maximilian on 30/01/2016.
 */
public class AnswerListAdapter extends ArrayAdapter<Answer>
{
    ArrayList<Answer> answers;
    Context context;
    LayoutInflater inflater;
    public AnswerListAdapter(Context context, ArrayList<Answer> items)
    {
        super(context, 0, items);
        this.context = context;
        this.answers = items;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View row = convertView;
        Answer answer = answers.get(position);
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(answer != null)
        {
            row = inflater.inflate(R.layout.answer_row, parent, false);

            TextView answerBody = (TextView) row.findViewById(R.id.answerBody);
            answerBody.setText(Html.fromHtml("<br/>" + answer.getBody()));

            TextView answerOwner = (TextView) row.findViewById(R.id.answerBodyOwner);
            answerOwner.setText(Html.fromHtml("answered: " + answer.getCreation_date() + "\n" + answer.getOwner().getDisplay_name()));

            TextView comments = (TextView) row.findViewById(R.id.answerComments);
            comments.setText("Comments To be added");

        }

        return row;
    }
}
