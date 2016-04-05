package ie.dit.max.behaviouralbiometricphonelock;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

/**
 *  This activity lets the user change the values for the threshold and for the number of observations
 *      used from other users in order to train the system. Default settings will be used if they are not updated by the user.
 *
 *  The "View Recommended Values" screen can be used here to set values for the settings.
 *
 * @author Maximilian Mihoc.
 * @version 1.0
 *
 */
public class SettingsActivity extends AppCompatActivity
{
    private static final String DEBUG_TAG = "Settings Activity";
    private TextView thresholdLabel;
    private TextView nrObsLabel;
    private SeekBar thresholdSeekBar;
    private SeekBar nrObsSeekBar;
    private CheckBox saveDataCheckBox;

    private Firebase ref;
    private String userID;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Firebase.setAndroidContext(this);
        ref = new Firebase(DBVar.mainURL);

        SharedPreferences sharedpreferences = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        if(sharedpreferences.contains("UserID")) userID = sharedpreferences.getString("UserID", "");

        thresholdLabel = (TextView) findViewById(R.id.thresholdValue);
        nrObsLabel = (TextView) findViewById(R.id.nrObservations);
        thresholdSeekBar = (SeekBar) findViewById(R.id.thresholdSeekBar);
        nrObsSeekBar = (SeekBar) findViewById(R.id.nrObsSeekBar);
        Button saveSettings = (Button) findViewById(R.id.saveSettingsButton);

        saveDataCheckBox = (CheckBox) findViewById(R.id.saveDataCheckBox);

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
                // Save new values for User Settings in the database
                Firebase newUserRef = ref.child("settings").child(userID);
                UserSettings us = new UserSettings(thresholdSeekBar.getProgress(), nrObsSeekBar.getProgress(), saveDataCheckBox.isChecked());
                newUserRef.setValue(us);

                Toast toast = Toast.makeText(getApplicationContext(), "SAVED", Toast.LENGTH_SHORT);
                toast.show();
            }
        });

    }

    /**
     * This methode is used to update the settings shown on the screen.
     * It will display default settings or user settings from the database.
     */
    private void showUserSettings()
    {
        final Firebase settingsRef = new Firebase(DBVar.mainURL + "/settings/" + userID);
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
                    saveDataCheckBox.setChecked(true);

                }
                else
                {
                    UserSettings userSettings = dataSnapshot.getValue(UserSettings.class);

                    thresholdLabel.setText("Threshold: " + userSettings.getThreshold());
                    thresholdSeekBar.setProgress(userSettings.getThreshold());
                    nrObsLabel.setText("Number of guest Observation to use:\n" + userSettings.getNrObsFromAnotherUser());
                    nrObsSeekBar.setProgress(userSettings.getNrObsFromAnotherUser());
                    saveDataCheckBox.setChecked(userSettings.getSaveTestData());
                }

            }

            @Override
            public void onCancelled(FirebaseError firebaseError)
            {
                System.out.println(DEBUG_TAG + "The read failed: " + firebaseError.getMessage());
            }
        });
    }


}
