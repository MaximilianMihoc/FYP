package ie.dit.max.behaviouralbiometricphonelock;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
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

/**
 *  This activity represents the Options Screen of this application. After user Registers or Logs in,
 *      if the train data is provided, it is redirected straight to this screen.
 *
 *  From this screen, the user can choose to open the Stack Overflow app,
 *      play the Country-List game of make settings changes from the Menu.
 *
 *  Checks for tha train data are made for most of the user choices in order to see if train data exists.
 *      If train data does not exist, the user can not open some of the options provided.
 *
 * @author Maximilian Mihoc.
 * @version 1.0
 *
 */
public class OptionsScreen extends AppCompatActivity
{
    private static final String DEBUG_TAG = "Options Screen Activity";
    private ProgressBar loadingPanel;
    private Firebase ref;
    private String userID;
    private String userEmail;
    private int numberInteractionsNeededToChangePassword = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_options_screen);

        Firebase.setAndroidContext(this);
        ref = new Firebase(DBVar.mainURL);

        // get User details from Shared Preferences
        SharedPreferences sharedpreferences = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        if(sharedpreferences.contains("UserID")) userID = sharedpreferences.getString("UserID", "");
        if(sharedpreferences.contains("UserEmail")) userEmail = sharedpreferences.getString("UserEmail", "");

        TestBehaviouralBiometrics.userTrust = 100;

        loadingPanel = (ProgressBar) findViewById(R.id.loadingPanel);
        loadingPanel.setVisibility(View.GONE);

        ImageButton goToStackOverflow = (ImageButton) findViewById(R.id.goToStackOverflowApp);
        ImageButton goToCountryListGame = (ImageButton) findViewById(R.id.goToCountryListGame);
        Button logOutButton = (Button) findViewById(R.id.logOutButton);

        // On CLick listener to open Stack Overflow application
        goToStackOverflow.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // check network connection
                if(isNetworkAvailable())
                {
                    loadingPanel.setVisibility(View.VISIBLE);

                    // check if training data exists for the current user.
                    Firebase scrollFlingRef = new Firebase(DBVar.mainURL + "/trainData/" + userID + "/scrollFling");
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
                            }
                            else
                            {
                                // open Stack Overflow if system had been trained
                                Intent intent = new Intent(OptionsScreen.this, StackOverflowHomeScreen.class);
                                startActivity(intent);
                            }
                        }

                        @Override
                        public void onCancelled(FirebaseError firebaseError)
                        {
                            System.out.println(DEBUG_TAG + "The read failed: " + firebaseError.getMessage());
                        }
                    });
                }
                else
                {
                    Toast toast = Toast.makeText(getApplicationContext(), "No Internet Connection Available.\n\nPlease Connect to Internet and try again.", Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });

        // On Click Listener to play the Country-List game
        goToCountryListGame.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // check network connection
                if(isNetworkAvailable())
                {
                    loadingPanel.setVisibility(View.VISIBLE);
                    // check if train data exists
                    Firebase scrollFlingRef = new Firebase(DBVar.mainURL + "/trainData/" + userID + "/scrollFling");
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
                                Intent intent = new Intent(OptionsScreen.this, CountryListGameTest.class);
                                startActivity(intent);
                            }
                        }

                        @Override
                        public void onCancelled(FirebaseError firebaseError)
                        {
                            System.out.println(DEBUG_TAG + "The read failed: " + firebaseError.getMessage());
                        }
                    });
                }
                else
                {
                    Toast toast = Toast.makeText(getApplicationContext(), "No Internet Connection Available.\n\nPlease Connect to Internet and try again.", Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });

        // Log Out button listener
        logOutButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // Delete data that would be used for changing password.
                Firebase deleteTestDataForPasswordChange = new Firebase(DBVar.mainURL + "/testDataForPasswordChange/" + userID);
                deleteTestDataForPasswordChange.removeValue();

                // log out user
                ref.unauth();

                Intent intent = new Intent(OptionsScreen.this, LogIn.class);
                startActivity(intent);
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

    /**
     * Method onOptionsItemSelected
     * This method is called when the menu button is clicked. Because Some of the activities should not be available to all the users,
     *      an alert dialog has been created and displayed every time when a user wants to take important actions. The Dialog is used with 2 scopes,
     *      one is to check if the user is the Owner by confirming the password and the other is to ask the user if he really wants to perform the action.
     *      In case that buttons are pressed by mistake, the changes will not be made straight away.
     *
     * Change password button is also one of the menu options. This method checks if the user has enough train and test data in order to change the password and
     *      check the behavioural biometrics (interactions) before changing the password.
     *
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // create alert Dialog for password confirmation
        AlertDialog alertDialog;
        final EditText userPasswordConfirmed;

        // get prompts.xml view
        LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View promptsView = li.inflate(R.layout.prompt, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(OptionsScreen.this);

        // set prompts.xml to alert dialog builder
        alertDialogBuilder.setView(promptsView);
        userPasswordConfirmed = (EditText) promptsView.findViewById(R.id.confirmPasswordPrompt);

        //check the network connection, needed to check the confirmed password.
        if(isNetworkAvailable())
        {
            switch (item.getItemId())
            {
                case R.id.action_settings:
                {
                    //set alert dialog parameters
                    alertDialogBuilder.setMessage("Open Settings Screen")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            // check if confirmed password is correct and let user access the settings or not
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
                    Firebase trainDataRef = new Firebase(DBVar.mainURL + "/trainData/" + userID);
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
                                Firebase testDataRef = new Firebase(DBVar.mainURL + "/testDataForPasswordChange/" + userID);
                                testDataRef.addListenerForSingleValueEvent(new ValueEventListener()
                                {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot)
                                    {
                                        loadingPanel.setVisibility(View.GONE);
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
                                            if (dpScroll.getChildrenCount() <= numberInteractionsNeededToChangePassword)
                                            {
                                                Toast toast = Toast.makeText(getApplicationContext(), "Please use the app a little longer." +
                                                        "\n\nMore interactions needed to change password.", Toast.LENGTH_LONG);
                                                toast.show();
                                            }
                                            else
                                            {
                                                Intent intent = new Intent(OptionsScreen.this, ChangePassword.class);
                                                startActivity(intent);
                                            }
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

                        @Override
                        public void onCancelled(FirebaseError firebaseError)
                        {
                            System.out.println(DEBUG_TAG + "The read failed: " + firebaseError.getMessage());
                        }
                    });

                    return true;
                }
                case R.id.action_lockOption:
                {
                    // confirm password to change lock options
                    alertDialogBuilder.setMessage("View Lock Options")
                            .setCancelable(false)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener()
                            {
                                public void onClick(DialogInterface dialog, int id)
                                {
                                    // check if confirmed password is correct
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
                    // confirm password to re-train the system
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
                    // no password confirmation needed to see the recomended values
                    startActivity(new Intent(this, ViewRecomendedValues.class));
                    return true;
                }
                case R.id.action_deleteTestData:
                {
                    // confirm password to delete test data.
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
                    // confirm password to delete train data
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
                    // no password required here. This was only used for Evaluation and demo.
                    // It can be deleted from the system any time.
                    startActivity(new Intent(this, UserValidationDifferentClassifiers.class));
                    return true;
                }
                default:
                    super.onOptionsItemSelected(item);
            }
        }
        else
        {
            Toast toast = Toast.makeText(getApplicationContext(), "No Internet Connection Available.\n\nPlease Connect to Internet and try again.", Toast.LENGTH_SHORT);
            toast.show();
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Method isNetworkAvailable
     * check if there is a connection to the internet
     * Reference: http://stackoverflow.com/questions/4238921/detect-whether-there-is-an-internet-connection-available-on-android
     *
     * @return boolean connected
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

}
