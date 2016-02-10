package ie.dit.max.behaviouralbiometricphonelock;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import ie.dit.max.foregroundAppStackOverflow.StackOverflowHomeScreen;
import ie.dit.max.trainActivitiesSpecificToForegroundApp.TrainActivityFirstScreen;

public class OptionsScreen extends AppCompatActivity
{
    Button goToStackOverflow;
    Button goToTrainFirstActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_options_screen);

        goToStackOverflow = (Button) findViewById(R.id.goToStackOverflowApp);
        goToTrainFirstActivity = (Button) findViewById(R.id.goToTrainFirstScreen);

        goToTrainFirstActivity.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent trainIntent = new Intent(OptionsScreen.this, TrainActivityFirstScreen.class);
                startActivity(trainIntent);
            }
        });

        goToStackOverflow.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent trainIntent = new Intent(OptionsScreen.this, StackOverflowHomeScreen.class);
                startActivity(trainIntent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_options_screen, menu);
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
