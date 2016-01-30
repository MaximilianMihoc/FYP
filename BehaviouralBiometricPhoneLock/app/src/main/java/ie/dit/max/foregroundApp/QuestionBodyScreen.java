package ie.dit.max.foregroundApp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import ie.dit.max.behaviouralbiometricphonelock.R;

public class QuestionBodyScreen extends AppCompatActivity
{

    Question questionSelected;
    Button goToAnswers;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question_body_screen);

        questionSelected = new Question();
        questionSelected.setQuestion_id(13409651);
        questionSelected.setAnswer_count(4);
        questionSelected.setCreation_date(1453883985);
        questionSelected.setBody("<p>I know that I can set the content of the view in an Android app by saying setContentView(int). Is there a function I can use to know what the current content view is? I don't know if that makes any sense, but what I'm looking for is a function called, say, getContentView that returns an int.</p>\n\n<p>Ideally, it would look like this:</p>\n\n<pre><code>setContentView(R.layout.main); // sets the content view to main.xml\nint contentView = getContentView(); // does this function exist?\n</code></pre>\n\n<p>How would I do that?</p>\n");
        questionSelected.setOwner(new Owner(534, 1081786, "Lincoln Bergeson"));
        questionSelected.setTitle("Best practise to initialize the same elements accross many Activities?");

        TextView questionTitle = (TextView)findViewById(R.id.bodyScreenTitle);
        questionTitle.setText(Html.fromHtml(questionSelected.getTitle()));

        TextView questionBody = (TextView)findViewById(R.id.questionBody);
        questionBody.setText(Html.fromHtml("<br/>" + questionSelected.getBody()));

        TextView questionOwner = (TextView)findViewById(R.id.questionBodyOwner);
        questionOwner.setText("asked: " + questionSelected.getCreation_date() + "\n" + questionSelected.getOwner().getDisplay_name());

        goToAnswers = (Button) findViewById(R.id.goToAnswers);
        goToAnswers.setText("View " + questionSelected.getAnswer_count() + " Answers");
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
        // automatically handle clicks on the Home/Up button, so long
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
