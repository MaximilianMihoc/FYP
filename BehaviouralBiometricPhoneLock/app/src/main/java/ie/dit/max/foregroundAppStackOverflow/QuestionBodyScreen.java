package ie.dit.max.foregroundAppStackOverflow;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import ie.dit.max.behaviouralbiometricphonelock.R;
import ie.dit.max.behaviouralbiometricphonelock.TestBehaviouralBiometrics;

/**
 * This activity is used to display Question Body of a question from Stack Exchange
 *
 * @author Maximilian Mihoc.
 * @version 1.0
 * @since 30/01/2016
 */
public class QuestionBodyScreen extends TestBehaviouralBiometrics
{
    private Question questionSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question_body_screen);

        // get Question Object for the question selected in previous screen
        Bundle bundle = getIntent().getExtras();
        questionSelected = (Question) bundle.getSerializable("questionSelected");

        ScrollView Sv = (ScrollView)findViewById(R.id.questionBodyScrollViewID);
        // add gesture listener to the ScrollView. This is used to get Touch and Reaction features for each interaction
        Sv.setOnTouchListener(gestureListener);

        TextView questionTitle = (TextView)findViewById(R.id.bodyScreenTitle);
        questionTitle.setText(Html.fromHtml(questionSelected.getTitle()));

        TextView questionBody = (TextView)findViewById(R.id.questionBody);
        questionBody.setText(Html.fromHtml("<br/>" + questionSelected.getBody()));

        TextView questionOwner = (TextView)findViewById(R.id.questionBodyOwner);
        questionOwner.setText("asked: " + questionSelected.getCreation_date() + "\nBy: " + questionSelected.getOwner().getDisplay_name());

        Button goToAnswers = (Button) findViewById(R.id.goToAnswers);
        Button backHomeScreen = (Button) findViewById(R.id.backHomeScreen);

        // set the name of the button to view Answers. This shows how many answers are available for the question selected.
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

        backHomeScreen.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent goToAnswersIntent = new Intent(QuestionBodyScreen.this, StackOverflowHomeScreen.class);
                startActivity(goToAnswersIntent);
            }
        });

    }
}
