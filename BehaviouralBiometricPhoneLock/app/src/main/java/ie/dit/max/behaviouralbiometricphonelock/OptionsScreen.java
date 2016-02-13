package ie.dit.max.behaviouralbiometricphonelock;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import ie.dit.max.foregroundAppCountriesPick.NonEuropeanCountryPick;
import ie.dit.max.foregroundAppStackOverflow.StackOverflowHomeScreen;
import ie.dit.max.trainActivitiesSpecificToForegroundApp.TrainActivityFirstScreen;

public class OptionsScreen extends AppCompatActivity
{
    Button goToStackOverflow;
    Button goToTrainFirstActivity;
    Button goToCrossValidation;
    Button goToCountryListGame;

    Firebase ref;
    SharedPreferences sharedpreferences;
    private String userID;
    boolean trained;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_options_screen);
        Firebase.setAndroidContext(this);
        ref = new Firebase("https://fyp-max.firebaseio.com");
        sharedpreferences = getSharedPreferences("MyPref", Context.MODE_PRIVATE);

        trained = checkIfTrainingDataExists();

        // get User details
        userID = sharedpreferences.getString("UserID", "");

        goToStackOverflow = (Button) findViewById(R.id.goToStackOverflowApp);
        goToTrainFirstActivity = (Button) findViewById(R.id.goToTrainFirstScreen);
        goToCrossValidation = (Button) findViewById(R.id.goToCrossValidation);
        goToCountryListGame = (Button) findViewById(R.id.goToCountryListGame);

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
                if(checkIfTrainingDataExists() || trained)
                {
                    Intent trainIntent = new Intent(OptionsScreen.this, StackOverflowHomeScreen.class);
                    startActivity(trainIntent);
                }
                else
                {
                    Toast toast = Toast.makeText(getApplicationContext(), "No Training data Provided. Please train the system first.", Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });

        goToCrossValidation.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent trainIntent = new Intent(OptionsScreen.this, CrossValidationActivity.class);
                startActivity(trainIntent);
            }
        });

        goToCountryListGame.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent trainIntent = new Intent(OptionsScreen.this, NonEuropeanCountryPick.class);
                startActivity(trainIntent);
            }
        });
    }

    private boolean checkIfTrainingDataExists()
    {
        Firebase scrollFlingRef = new Firebase("https://fyp-max.firebaseio.com/trainData/" + userID);
        scrollFlingRef.addValueEventListener(new ValueEventListener()
        {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if(dataSnapshot.getValue() == null)
                {
                    trained = false;
                }
                else
                {
                    trained = true;
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError)
            {

            }
        });

        return trained;
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
