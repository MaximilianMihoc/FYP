package ie.dit.max.foregroundAppStackOverflow;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import ie.dit.max.behaviouralbiometricphonelock.R;
import ie.dit.max.behaviouralbiometricphonelock.TrainActivity;

public class QuestionBodyScreen extends TrainActivity
{

    Question questionSelected;
    Button goToAnswers;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question_body_screen);

        Bundle bundle = getIntent().getExtras();
        questionSelected = (Question) bundle.getSerializable("questionSelected");

        ScrollView Sv = (ScrollView)findViewById(R.id.questionBodyScrollViewID);
        Sv.setOnTouchListener(gestureListener);

        TextView questionTitle = (TextView)findViewById(R.id.bodyScreenTitle);
        questionTitle.setText(Html.fromHtml(questionSelected.getTitle()));

        TextView questionBody = (TextView)findViewById(R.id.questionBody);
        questionBody.setText(Html.fromHtml("<br/>" + questionSelected.getBody()));

        TextView questionOwner = (TextView)findViewById(R.id.questionBodyOwner);
        questionOwner.setText("asked: " + questionSelected.getCreation_date() + "\nBy: " + questionSelected.getOwner().getDisplay_name());

        goToAnswers = (Button) findViewById(R.id.goToAnswers);

        if (questionSelected.getAnswer_count() == 1) goToAnswers.setText("View " + questionSelected.getAnswer_count() + " Answer");
        else goToAnswers.setText("View " + questionSelected.getAnswer_count() + " Answers");

        goToAnswers.setOnTouchListener(gestureListener);
        goToAnswers.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent goToAnswersIntent = new Intent(QuestionBodyScreen.this, AnswersScreen.class);

                Bundle bundle = new Bundle();
                bundle.putSerializable("selectedQuestion", questionSelected);
                goToAnswersIntent.putExtras(bundle);

                startActivity(goToAnswersIntent);
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_question_body_screen, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the StackOverflowHomeScreen/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
