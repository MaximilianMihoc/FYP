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

import ie.dit.max.foregroundAppStackOverflow.StackOverflowHomeScreen;

public class RegisterUser extends AppCompatActivity
{

    Firebase ref;

    EditText userName;
    EditText email;
    EditText confirmEmail;
    EditText password;
    EditText confirmPassword;
    Button registerButton;
    SharedPreferences sharedpreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_user);
        Firebase.setAndroidContext(this);

        ref = new Firebase("https://fyp-max.firebaseio.com");
        sharedpreferences = getSharedPreferences("MyPref", Context.MODE_PRIVATE);

        userName = (EditText) findViewById(R.id.userNameRegister);
        email = (EditText) findViewById(R.id.emailRegister);
        confirmEmail = (EditText) findViewById(R.id.confirmEmail);
        password = (EditText) findViewById(R.id.passwordRegister);
        confirmPassword = (EditText) findViewById(R.id.confirmPassword);
        registerButton = (Button) findViewById(R.id.registerButton);

        /* checks for confirm fields and error checking to be made later*/

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
                else if(password.getText().toString().trim().length() <= 4)
                {
                    Toast.makeText(getApplicationContext(), "Please Enter a password of at least 4 characters.", Toast.LENGTH_SHORT).show();
                }
                else if(!password.getText().toString().equals(confirmPassword.getText().toString()))
                {
                    Toast.makeText(getApplicationContext(), "Password and Confirmation password do not match.", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    ref.createUser(email.getText().toString(), password.getText().toString(), new Firebase.ValueResultHandler<Map<String, Object>>()
                    {
                        @Override
                        public void onSuccess(Map<String, Object> result)
                        {
                            System.out.println("Successfully created user account with uid: " + result.get("uid"));

                            Firebase newUserRef = ref.child("users").child(result.get("uid").toString());

                            User newUser = new User(userName.getText().toString(), email.getText().toString(), result.get("uid").toString());

                            newUserRef.setValue(newUser);

                            // log in the new user in the new created account.
                            ref.authWithPassword(email.getText().toString(), password.getText().toString(), new Firebase.AuthResultHandler()
                            {
                                @Override
                                public void onAuthenticated(AuthData authData)
                                {
                                    System.out.println("User ID: " + authData.getUid() + ", Provider: " + authData.getProvider());

                                    Firebase userRef = new Firebase("https://fyp-max.firebaseio.com/users/" + authData.getUid());

                                    userRef.addValueEventListener(new ValueEventListener()
                                    {
                                        @Override
                                        public void onDataChange(DataSnapshot snapshot)
                                        {
                                            User usrObj = snapshot.getValue(User.class);
                                            System.out.println(usrObj.getUserID() + " - " + usrObj.getEmail());

                                            SharedPreferences.Editor editor = sharedpreferences.edit();
                                            editor.putString("UserID", usrObj.getUserID());
                                            editor.commit();

                                            Intent trainIntent = new Intent(RegisterUser.this, OptionsScreen.class);
                                            startActivity(trainIntent);
                                        }

                                        @Override
                                        public void onCancelled(FirebaseError firebaseError)
                                        {
                                            System.out.println("The read failed: " + firebaseError.getMessage());
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
                            System.out.println("Error on Register Activity -> Create user");
                        }
                    });


                }

            }
        });

    }

    //Reference: http://howtodoinjava.com/regex/java-regex-validate-email-address/
    private boolean isEmailValid(String enteredEmail){
        String regex = "^[\\w!#$%&'*+/=?`{|}~^-]+(?:\\.[\\w!#$%&'*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(enteredEmail);
        return ((!enteredEmail.isEmpty()) && (matcher.matches()));
    }
    // end Reference

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_register_user, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the StackOverflowHomeScreen/Up button, so long
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
