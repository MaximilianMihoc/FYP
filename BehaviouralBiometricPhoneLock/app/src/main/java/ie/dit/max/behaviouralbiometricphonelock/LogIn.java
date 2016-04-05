package ie.dit.max.behaviouralbiometricphonelock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import ie.dit.max.foregroundAppCountriesPick.CountryListGameTrain;

/**
 * This Activity is user for User Log In
 * In this activity, after the user enters its email and password and clicks the Log in button
 *  the application checks if internet connections is enabled, is the user credentials exists in the database
 *  and logs user in if the conditions are met. User details are saved in Shared preferences for later usage
 *
 * Error messages are displayed to the user every time when something goes wrong.
 *
 * @author Maximilian Mihoc.
 * @version 1.0
 *
 */
public class LogIn extends Activity
{
    private static final String DEBUG_TAG = "Log In Activity";
    private Firebase ref;
    private EditText email;
    private EditText password;
    private SharedPreferences sharedpreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);
        // set Android Context to be used by Firebase database
        Firebase.setAndroidContext(this);
        // create reference to the database
        ref = new Firebase(DBVar.mainURL);

        //initialise the shared preferences in order to save user details
        sharedpreferences = getSharedPreferences("MyPref", Context.MODE_PRIVATE);

        email = (EditText) findViewById(R.id.emailLogin);
        password = (EditText) findViewById(R.id.passwordLogin);
        Button logInButton = (Button) findViewById(R.id.logInButton);
        TextView goToRegistrationScreen = (TextView) findViewById(R.id.goToRegistrationScreen);

        //on click listener "LogIn" button
        logInButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String emailStr = email.getText().toString();
                String passStr = password.getText().toString();

                // check network connection
                if (isNetworkAvailable())
                {
                    // authenticate user with email and password
                    ref.authWithPassword(emailStr, passStr, new Firebase.AuthResultHandler()
                    {
                        @Override
                        public void onAuthenticated(AuthData authData)
                        {
                            Log.i(DEBUG_TAG ,"User ID: " + authData.getUid() + ", Provider: " + authData.getProvider());

                            //Once the user gets authenticated, get his/hers details and save the userId and email to Shared Preferences.
                            Firebase userRef = new Firebase(DBVar.mainURL + "/users/" + authData.getUid());
                            userRef.addListenerForSingleValueEvent(new ValueEventListener()
                            {
                                @Override
                                public void onDataChange(DataSnapshot snapshot)
                                {
                                    User usrObj = snapshot.getValue(User.class);
                                    System.out.println(usrObj.getUserID() + " - " + usrObj.getEmail());

                                    //save user data to Shared preferences
                                    SharedPreferences.Editor editor = sharedpreferences.edit();
                                    editor.putString("UserID", usrObj.getUserID());
                                    editor.putString("UserEmail", usrObj.getEmail());
                                    editor.apply();

                                    // check to see if the user has training data in the database
                                    // if training data exists, redirect user to Options Activity
                                    // if training data does not exist, redirect user to train Activity
                                    Firebase scrollFlingRef = new Firebase(DBVar.mainURL + "/trainData/" + usrObj.getUserID() + "/scrollFling");
                                    scrollFlingRef.addListenerForSingleValueEvent(new ValueEventListener()
                                    {

                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot)
                                        {
                                            if (dataSnapshot.getValue() == null)
                                            {
                                                Intent trainIntent = new Intent(LogIn.this, CountryListGameTrain.class);
                                                startActivity(trainIntent);
                                            } else
                                            {
                                                Intent intent = new Intent(LogIn.this, OptionsScreen.class);
                                                startActivity(intent);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(FirebaseError firebaseError)
                                        {
                                            Log.i(DEBUG_TAG, "The read failed: " + firebaseError.getMessage());
                                        }
                                    });
                                }

                                @Override
                                public void onCancelled(FirebaseError firebaseError)
                                {
                                    Log.i(DEBUG_TAG, "The read failed: " + firebaseError.getMessage());
                                }
                            });
                        }

                        @Override
                        public void onAuthenticationError(FirebaseError firebaseError)
                        {
                            Toast toast = Toast.makeText(getApplicationContext(), "User Credentials does not exist or incorrect", Toast.LENGTH_SHORT);
                            toast.show();
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

        // On click listener for "Registration" button. Redirect user to Registration Screen
        goToRegistrationScreen.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent trainIntent = new Intent(LogIn.this, RegisterUser.class);
                startActivity(trainIntent);
            }
        });
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

    @Override
    public void onBackPressed ()
    {
        // force app to close when the back button is clicked
        // Without this, after the user gets logged out, pressing the back button would redirect him back in the application
        //   and that should not happen.
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
