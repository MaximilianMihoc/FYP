package ie.dit.max.behaviouralbiometricphonelock;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ie.dit.max.foregroundAppCountriesPick.CountryListGameTrain;

/**
 * This activity is used for user Registration. An account gets created for an user using this activity
 * Error checking are made for each field and warnings are displayed to the user if something is wrong
 * After an account has been created, user gets logged in and is redirected to train the system in the Train Activity
 * User details are saved in Shared preferences.
 *
 * @author Maximilian Mihoc.
 * @version 1.0
 */

public class RegisterUser extends AppCompatActivity
{
    private static final String DEBUG_TAG = "Register Activity";
    private Firebase ref;
    private EditText userName;
    private EditText email;
    private EditText confirmEmail;
    private EditText password;
    private EditText confirmPassword;
    private SharedPreferences sharedpreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_user);
        Firebase.setAndroidContext(this);
        ref = new Firebase(DBVar.mainURL);
        sharedpreferences = getSharedPreferences("MyPref", Context.MODE_PRIVATE);

        // widgets references
        userName = (EditText) findViewById(R.id.userNameRegister);
        email = (EditText) findViewById(R.id.emailRegister);
        confirmEmail = (EditText) findViewById(R.id.confirmEmail);
        password = (EditText) findViewById(R.id.passwordRegister);
        confirmPassword = (EditText) findViewById(R.id.confirmPassword);
        Button registerButton = (Button) findViewById(R.id.registerButton);

        // register new user
        registerButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(userName.getText().toString().trim().length() < 2 )
                {
                    Toast.makeText(getApplicationContext(), "Please Enter a Username of at least 2 characters.", Toast.LENGTH_SHORT).show();
                }
                else if( !isEmailValid(email.getText().toString()))
                {
                    Toast.makeText(getApplicationContext(), "Invalid e-mail address.", Toast.LENGTH_SHORT).show();
                }
                else if(!email.getText().toString().equals(confirmEmail.getText().toString()))
                {
                    Toast.makeText(getApplicationContext(), "E-mail and Confirmation e-mail do not match.", Toast.LENGTH_SHORT).show();
                }
                else if(password.getText().toString().trim().length() < 5)
                {
                    Toast.makeText(getApplicationContext(), "Please Enter a password of at least 5 characters.", Toast.LENGTH_SHORT).show();
                }
                else if(!password.getText().toString().equals(confirmPassword.getText().toString()))
                {
                    Toast.makeText(getApplicationContext(), "Password and Confirmation password do not match.", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    // create user with the details entered in the registration form.
                    ref.createUser(email.getText().toString(), password.getText().toString(), new Firebase.ValueResultHandler<Map<String, Object>>()
                    {
                        @Override
                        public void onSuccess(Map<String, Object> result)
                        {
                            // save user details in the database under "users" object
                            Firebase newUserRef = ref.child("users").child(result.get("uid").toString());
                            User newUser = new User(userName.getText().toString(), email.getText().toString(), result.get("uid").toString());
                            newUserRef.setValue(newUser);
                            Log.i(DEBUG_TAG, "Successfully created user account with uid: " + result.get("uid"));

                            // log in the new user in the new created account.
                            ref.authWithPassword(email.getText().toString(), password.getText().toString(), new Firebase.AuthResultHandler()
                            {
                                @Override
                                public void onAuthenticated(AuthData authData)
                                {
                                    Log.i(DEBUG_TAG, "User ID: " + authData.getUid() + ", Provider: " + authData.getProvider());
                                    Firebase userRef = new Firebase(DBVar.mainURL + "/users/" + authData.getUid());
                                    userRef.addListenerForSingleValueEvent(new ValueEventListener()
                                    {
                                        @Override
                                        public void onDataChange(DataSnapshot snapshot)
                                        {
                                            User usrObj = snapshot.getValue(User.class);
                                            System.out.println(usrObj.getUserID() + " - " + usrObj.getEmail());

                                            //save user data to shared preferences
                                            SharedPreferences.Editor editor = sharedpreferences.edit();
                                            editor.putString("UserID", usrObj.getUserID());
                                            editor.putString("UserEmail", usrObj.getEmail());
                                            editor.apply();

                                            // check if train data exist and redirect user to different activities if data exists of not
                                            Firebase scrollFlingRef = new Firebase(DBVar.mainURL + "/trainData/" + usrObj.getUserID() + "/scrollFling");
                                            scrollFlingRef.addListenerForSingleValueEvent(new ValueEventListener()
                                            {

                                                @Override
                                                public void onDataChange(DataSnapshot dataSnapshot)
                                                {
                                                    if (dataSnapshot.getValue() == null)
                                                    {
                                                        Intent trainIntent = new Intent(RegisterUser.this, CountryListGameTrain.class);
                                                        startActivity(trainIntent);
                                                    }
                                                    else
                                                    {
                                                        Intent intent = new Intent(RegisterUser.this, OptionsScreen.class);
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
                                    Toast toast = Toast.makeText(getApplicationContext(), "Something went Wrong, Please try again", Toast.LENGTH_SHORT);
                                    toast.show();
                                }
                            });
                        }

                        @Override
                        public void onError(FirebaseError firebaseError)
                        {
                            Log.i(DEBUG_TAG, "Error on Register Activity -> Create user");
                        }
                    });


                }

            }
        });

    }

    /**
     * Method to check for valid Email address format
     * Reference: http://howtodoinjava.com/regex/java-regex-validate-email-address/
     *
     * @param enteredEmail String
     * @return boolean validEmail
     */
    private boolean isEmailValid(String enteredEmail){
        String regex = "^[\\w!#$%&'*+/=?`{|}~^-]+(?:\\.[\\w!#$%&'*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(enteredEmail);
        return ((!enteredEmail.isEmpty()) && (matcher.matches()));
    }
    // end Reference
}
