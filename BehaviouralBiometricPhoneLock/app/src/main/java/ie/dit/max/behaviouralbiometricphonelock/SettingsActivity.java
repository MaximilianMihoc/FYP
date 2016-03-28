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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import ie.dit.max.foregroundAppStackOverflow.StackOverflowHomeScreen;

public class SettingsActivity extends AppCompatActivity
{
    TextView thresholdLabel;
    TextView nrObsLabel;
    SeekBar thresholdSeekBar;
    SeekBar nrObsSeekBar;
    Button saveSettings;

    Firebase ref;
    SharedPreferences sharedpreferences;
    String userID;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Firebase.setAndroidContext(this);

        ref = new Firebase("https://fyp-max.firebaseio.com");
        sharedpreferences = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        userID = sharedpreferences.getString("UserID", "");

        thresholdLabel = (TextView) findViewById(R.id.thresholdValue);
        nrObsLabel = (TextView) findViewById(R.id.nrObservations);
        thresholdSeekBar = (SeekBar) findViewById(R.id.thresholdSeekBar);
        nrObsSeekBar = (SeekBar) findViewById(R.id.nrObsSeekBar);
        saveSettings = (Button) findViewById(R.id.saveSettingsButton);

        //default Settings:
        showUserSettings();

        thresholdSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                thresholdLabel.setText("Threshold: " + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {

            }
        });

        nrObsSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                nrObsLabel.setText("Number of guest Observation to use:\n" + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {

            }
        });

        saveSettings.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Firebase newUserRef = ref.child("settings").child(userID);

                UserSettings us = new UserSettings(thresholdSeekBar.getProgress(), nrObsSeekBar.getProgress());

                newUserRef.setValue(us);

                Toast toast = Toast.makeText(getApplicationContext(), "SAVED", Toast.LENGTH_SHORT);
                toast.show();
            }
        });

    }

    private void showUserSettings()
    {
        final Firebase settingsRef = new Firebase("https://fyp-max.firebaseio.com/settings/" + userID);
        settingsRef.addListenerForSingleValueEvent(new ValueEventListener()
        {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.getValue() == null)
                {
                    Toast toast = Toast.makeText(getApplicationContext(), "Default Settings presented. Change them as desired.", Toast.LENGTH_SHORT);
                    toast.show();

                    thresholdLabel.setText("Threshold: 70");
                    thresholdSeekBar.setProgress(70);
                    nrObsLabel.setText("Number of guest Observation to use: 5");
                    nrObsSeekBar.setProgress(5);

                }
                else
                {
                    UserSettings userSettings = dataSnapshot.getValue(UserSettings.class);

                    thresholdLabel.setText("Threshold: " + userSettings.getThreshold());
                    thresholdSeekBar.setProgress(userSettings.getThreshold());
                    nrObsLabel.setText("Number of guest Observation to use:\n" + userSettings.getNrObsFromAnotherUser());
                    nrObsSeekBar.setProgress(userSettings.getNrObsFromAnotherUser());
                }

            }

            @Override
            public void onCancelled(FirebaseError firebaseError)
            {

            }
        });
    }


}
