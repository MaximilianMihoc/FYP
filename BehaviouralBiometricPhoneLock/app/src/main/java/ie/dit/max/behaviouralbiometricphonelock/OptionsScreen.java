package ie.dit.max.behaviouralbiometricphonelock;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import ie.dit.max.evaluationClasses.UserValidationDifferentClassifiers;
import ie.dit.max.foregroundAppCountriesPick.CountryListGameTest;
import ie.dit.max.foregroundAppStackOverflow.StackOverflowHomeScreen;
import ie.dit.max.foregroundAppCountriesPick.CountryListGameTrain;

public class OptionsScreen extends AppCompatActivity
{
    ImageButton goToStackOverflow;
    ImageButton goToCountryListGame;
    Button logOutButton;

    ProgressBar loadingPanel;
    Firebase ref;
    SharedPreferences sharedpreferences;
    private String userID;
    private String userEmail;
    int numberInteractionsNeededToChangePassword = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_options_screen);
        Firebase.setAndroidContext(this);
        ref = new Firebase("https://fyp-max.firebaseio.com");
        sharedpreferences = getSharedPreferences("MyPref", Context.MODE_PRIVATE);

        TestBehaviouralBiometrics.userTrust = 100;

        // get User details
        if(sharedpreferences.contains("UserID")) userID = sharedpreferences.getString("UserID", "");
        if(sharedpreferences.contains("UserEmail")) userEmail = sharedpreferences.getString("UserEmail", "");

        loadingPanel = (ProgressBar) findViewById(R.id.loadingPanel);
        loadingPanel.setVisibility(View.GONE);

        goToStackOverflow = (ImageButton) findViewById(R.id.goToStackOverflowApp);
        goToCountryListGame = (ImageButton) findViewById(R.id.goToCountryListGame);
        logOutButton = (Button) findViewById(R.id.logOutButton);

        goToStackOverflow.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                loadingPanel.setVisibility(View.VISIBLE);
                Firebase scrollFlingRef = new Firebase("https://fyp-max.firebaseio.com/trainData/" + userID + "/scrollFling");
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

        goToCountryListGame.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                loadingPanel.setVisibility(View.VISIBLE);
                Firebase scrollFlingRef = new Firebase("https://fyp-max.firebaseio.com/trainData/" + userID + "/scrollFling");
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
                            Intent trainIntent = new Intent(OptionsScreen.this, CountryListGameTest.class);
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
        AlertDialog alertDialog;
        final EditText userPasswordConfirmed;

        // get prompts.xml view
        LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View promptsView = li.inflate(R.layout.prompt, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(OptionsScreen.this);

        // set prompts.xml to alert dialog builder
        alertDialogBuilder.setView(promptsView);
        userPasswordConfirmed = (EditText) promptsView.findViewById(R.id.confirmPasswordPrompt);

        switch(item.getItemId())
        {
            case R.id.action_settings:
            {
                alertDialogBuilder.setMessage("Open Settings Screen")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        ref.authWithPassword(userEmail, userPasswordConfirmed.getText().toString(), new Firebase.AuthResultHandler()
                        {
                            @Override
                            public void onAuthenticated(AuthData authData)
                            {
                                startActivity(new Intent(OptionsScreen.this, SettingsActivity.class));
                            }

                            @Override
                            public void onAuthenticationError(FirebaseError firebaseError)
                            {
                                Toast toast = Toast.makeText(getApplicationContext(), "Wrong Password", Toast.LENGTH_SHORT);
                                toast.show();
                            }

                        });
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener()
                {

                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                        dialog.cancel();
                    }
                });

                alertDialog = alertDialogBuilder.create();
                alertDialog.show();
                return true;
            }
            case R.id.action_changePassword:
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
                        } else
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
                                    } else
                                    {
                                        DataSnapshot dpScroll = dataSnapshot.child("scrollFling");

                                        if (dpScroll.getChildrenCount() <= numberInteractionsNeededToChangePassword)
                                        {
                                            Toast toast = Toast.makeText(getApplicationContext(), "Please use the app a little longer." +
                                                    "\n\nMore interactions needed to change password.", Toast.LENGTH_LONG);
                                            toast.show();
                                        } else
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

                return true;
            }
            case R.id.action_lockOption:
            {
                alertDialogBuilder.setMessage("View Lock Options")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        ref.authWithPassword(userEmail, userPasswordConfirmed.getText().toString(), new Firebase.AuthResultHandler()
                        {
                            @Override
                            public void onAuthenticated(AuthData authData)
                            {
                                startActivity(new Intent(OptionsScreen.this, MyLockScreenActivity.class));
                            }

                            @Override
                            public void onAuthenticationError(FirebaseError firebaseError)
                            {
                                Toast toast = Toast.makeText(getApplicationContext(), "Wrong Password", Toast.LENGTH_SHORT);
                                toast.show();
                            }

                        });
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener()
                {

                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                        dialog.cancel();
                    }
                });
                alertDialog = alertDialogBuilder.create();
                alertDialog.show();

                return true;
            }
            case R.id.action_trainSystem:
            {
                alertDialogBuilder
                .setCancelable(false).setMessage("Re-Train System")
                .setPositiveButton("OK", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        ref.authWithPassword(userEmail, userPasswordConfirmed.getText().toString(), new Firebase.AuthResultHandler()
                        {
                            @Override
                            public void onAuthenticated(AuthData authData)
                            {
                                startActivity(new Intent(OptionsScreen.this, CountryListGameTrain.class));
                            }

                            @Override
                            public void onAuthenticationError(FirebaseError firebaseError)
                            {
                                Toast toast = Toast.makeText(getApplicationContext(), "Wrong Password", Toast.LENGTH_SHORT);
                                toast.show();
                            }

                        });
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener()
                {

                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                        dialog.cancel();
                    }
                });

                alertDialog = alertDialogBuilder.create();
                alertDialog.show();

                return true;
            }
            case R.id.action_viewRecommendedValues:
            {
                startActivity(new Intent(this, ViewRecomendedValues.class));
                return true;
            }
            case R.id.action_deleteTestData:
            {
                alertDialogBuilder
                .setCancelable(false).setMessage("Delete Test Data")
                .setPositiveButton("OK", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        ref.authWithPassword(userEmail, userPasswordConfirmed.getText().toString(), new Firebase.AuthResultHandler()
                        {
                            @Override
                            public void onAuthenticated(AuthData authData)
                            {
                                Firebase removeDataRef = new Firebase("https://fyp-max.firebaseio.com/testData/" + userID);
                                removeDataRef.removeValue();
                                Toast toast = Toast.makeText(getApplicationContext(), "Test Data Successfully deleted.", Toast.LENGTH_SHORT);
                                toast.show();
                            }

                            @Override
                            public void onAuthenticationError(FirebaseError firebaseError)
                            {
                                Toast toast = Toast.makeText(getApplicationContext(), "Wrong Password", Toast.LENGTH_SHORT);
                                toast.show();
                            }

                        });
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener()
                {

                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                        dialog.cancel();
                    }
                });

                alertDialog = alertDialogBuilder.create();
                alertDialog.show();
                return true;
            }
            case R.id.action_deleteTrainData:
            {
                alertDialogBuilder
                .setCancelable(false).setMessage("Delete Train Data")
                .setPositiveButton("OK", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        ref.authWithPassword(userEmail, userPasswordConfirmed.getText().toString(), new Firebase.AuthResultHandler()
                        {
                            @Override
                            public void onAuthenticated(AuthData authData)
                            {
                                Firebase removeDataRef = new Firebase("https://fyp-max.firebaseio.com/trainData/" + userID);
                                removeDataRef.removeValue();
                                Toast toast = Toast.makeText(getApplicationContext(), "Train Data Successfully deleted.", Toast.LENGTH_SHORT);
                                toast.show();
                            }

                            @Override
                            public void onAuthenticationError(FirebaseError firebaseError)
                            {
                                Toast toast = Toast.makeText(getApplicationContext(), "Wrong Password", Toast.LENGTH_SHORT);
                                toast.show();
                            }

                        });
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener()
                {

                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                        dialog.cancel();
                    }
                });

                alertDialog = alertDialogBuilder.create();
                alertDialog.show();
                return true;
            }
            case R.id.action_evaluation:
            {
                startActivity(new Intent(this, UserValidationDifferentClassifiers.class));
                return true;
            }
            default:
                super.onOptionsItemSelected(item);
        }

        return super.onOptionsItemSelected(item);
    }

}
