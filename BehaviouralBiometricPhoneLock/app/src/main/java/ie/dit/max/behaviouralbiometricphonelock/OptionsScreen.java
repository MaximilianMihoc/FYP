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
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
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
    public static boolean saveData;

    Button goToStackOverflow;
    Button goToTrainFirstActivity;
    Button goToCrossValidation;
    Button goToCountryListGame;
    Button goToChangePassword;
    Button logOutButton;
    CheckBox saveDataCheckBox;
    ProgressBar loadingPanel;
    Firebase ref;
    SharedPreferences sharedpreferences;
    private String userID;
    int numberInteractionsNeededToChangePassword = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_options_screen);
        Firebase.setAndroidContext(this);
        ref = new Firebase("https://fyp-max.firebaseio.com");
        sharedpreferences = getSharedPreferences("MyPref", Context.MODE_PRIVATE);

        // get User details
        userID = sharedpreferences.getString("UserID", "");

        loadingPanel = (ProgressBar) findViewById(R.id.loadingPanel);
        loadingPanel.setVisibility(View.GONE);

        goToStackOverflow = (Button) findViewById(R.id.goToStackOverflowApp);
        goToTrainFirstActivity = (Button) findViewById(R.id.goToTrainFirstScreen);
        goToCrossValidation = (Button) findViewById(R.id.goToCrossValidation);
        goToCountryListGame = (Button) findViewById(R.id.goToCountryListGame);
        goToChangePassword = (Button) findViewById(R.id.changePassword);
        logOutButton = (Button) findViewById(R.id.logOutButton);
        saveDataCheckBox = (CheckBox) findViewById(R.id.saveDataCheckBox);
        saveDataCheckBox.setChecked(true);

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
                loadingPanel.setVisibility(View.VISIBLE);
                Firebase scrollFlingRef = new Firebase("https://fyp-max.firebaseio.com/trainData/" + userID);
                scrollFlingRef.addListenerForSingleValueEvent(new ValueEventListener()
                {

                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        loadingPanel.setVisibility(View.GONE);
                        if (dataSnapshot.getValue() == null)
                        {
                            Toast toast = Toast.makeText(getApplicationContext(), "No Training data Provided. Please train the system first.", Toast.LENGTH_SHORT);
                            toast.show();
                        } else
                        {
                            Intent trainIntent = new Intent(OptionsScreen.this, StackOverflowHomeScreen.class);
                            startActivity(trainIntent);
                        }
                    }

                    @Override
                    public void onCancelled(FirebaseError firebaseError)
                    {

                    }
                });
            }
        });

        goToCrossValidation.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent trainIntent = new Intent(OptionsScreen.this, UserValidationDifferentClassifiers.class);
                startActivity(trainIntent);
            }
        });

        goToCountryListGame.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                loadingPanel.setVisibility(View.VISIBLE);
                Firebase scrollFlingRef = new Firebase("https://fyp-max.firebaseio.com/trainData/" + userID);
                scrollFlingRef.addListenerForSingleValueEvent(new ValueEventListener()
                {

                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        loadingPanel.setVisibility(View.GONE);
                        if (dataSnapshot.getValue() == null)
                        {
                            Toast toast = Toast.makeText(getApplicationContext(), "No Training data Provided. Please train the system first.", Toast.LENGTH_SHORT);
                            toast.show();
                        } else
                        {
                            Intent trainIntent = new Intent(OptionsScreen.this, NonEuropeanCountryPick.class);
                            startActivity(trainIntent);
                        }
                    }

                    @Override
                    public void onCancelled(FirebaseError firebaseError)
                    {

                    }
                });

            }
        });
        
        goToChangePassword.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // Check if train data exists for the current user
                Firebase trainDataRef = new Firebase("https://fyp-max.firebaseio.com/trainData/" + userID);
                trainDataRef.addListenerForSingleValueEvent(new ValueEventListener()
                {

                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        loadingPanel.setVisibility(View.GONE);
                        if (dataSnapshot.getValue() == null)
                        {
                            Toast toast = Toast.makeText(getApplicationContext(), "No Training data Provided. Please train the system first.", Toast.LENGTH_SHORT);
                            toast.show();
                        }
                        else
                        {
                            // check if test data for password Change exist
                            Firebase testDataRef = new Firebase("https://fyp-max.firebaseio.com/testDataForPasswordChange/" + userID);
                            testDataRef.addListenerForSingleValueEvent(new ValueEventListener()
                            {

                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot)
                                {
                                    loadingPanel.setVisibility(View.GONE);
                                    System.out.println("dataSnapshot: ");

                                    if (dataSnapshot.getValue() == null)
                                    {
                                        Toast toast = Toast.makeText(getApplicationContext(), "Please use the app for a while before changing the password." +
                                                " Password can not be changed if the application is not used. \n\n" +
                                                "Please play Country List game or Use Stack Overflow for couple of minutes.", Toast.LENGTH_LONG);
                                        toast.show();
                                    }
                                    else
                                    {
                                        DataSnapshot dpScroll = dataSnapshot.child("scrollFling");

                                        if(dpScroll.getChildrenCount() <= numberInteractionsNeededToChangePassword)
                                        {
                                            Toast toast = Toast.makeText(getApplicationContext(), "Please use the app a little longer." +
                                                    "\n\nMore interactions needed to change password.", Toast.LENGTH_LONG);
                                            toast.show();
                                        }
                                        else
                                        {
                                            Intent trainIntent = new Intent(OptionsScreen.this, ChangePassword.class);
                                            startActivity(trainIntent);
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(FirebaseError firebaseError)
                                {

                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(FirebaseError firebaseError)
                    {

                    }
                });

            }
        });

        logOutButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // Delete data that would be used for changing password.
                Firebase deleteTestDataForPasswordChange = new Firebase("https://fyp-max.firebaseio.com/testDataForPasswordChange/" + userID);
                deleteTestDataForPasswordChange.removeValue();

                ref.unauth();
                Intent trainIntent = new Intent(OptionsScreen.this, LogIn.class);
                startActivity(trainIntent);
            }
        });

        saveData = true;
        saveDataCheckBox.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                saveData = saveDataCheckBox.isChecked();
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
        switch(item.getItemId())
        {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.action_lockOption:
                startActivity(new Intent(this, MyLockScreenActivity.class));
                return true;
            default:
                super.onOptionsItemSelected(item);
        }

        return super.onOptionsItemSelected(item);
    }
}
