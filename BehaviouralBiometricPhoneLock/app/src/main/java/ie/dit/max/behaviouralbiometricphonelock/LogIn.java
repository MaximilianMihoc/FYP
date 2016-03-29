package ie.dit.max.behaviouralbiometricphonelock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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

import ie.dit.max.foregroundAppStackOverflow.StackOverflowHomeScreen;

public class LogIn extends Activity
{

    Firebase ref;

    EditText email;
    EditText password;
    Button logInButton;
    TextView goToRegistrationScreen;

    SharedPreferences sharedpreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);
        Firebase.setAndroidContext(this);

        ref = new Firebase("https://fyp-max.firebaseio.com");
        sharedpreferences = getSharedPreferences("MyPref", Context.MODE_PRIVATE);

        email = (EditText) findViewById(R.id.emailLogin);
        password = (EditText) findViewById(R.id.passwordLogin);
        logInButton = (Button) findViewById(R.id.logInButton);
        goToRegistrationScreen = (TextView) findViewById(R.id.goToRegistrationScreen);

        logInButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String emailStr = email.getText().toString();
                String passStr = password.getText().toString();

                ref.authWithPassword(emailStr, passStr, new Firebase.AuthResultHandler() {
                    @Override
                    public void onAuthenticated(AuthData authData)
                    {
                        System.out.println("User ID: " + authData.getUid() + ", Provider: " + authData.getProvider());

                        Firebase userRef = new Firebase("https://fyp-max.firebaseio.com/users/" + authData.getUid());

                        userRef.addListenerForSingleValueEvent(new ValueEventListener()
                        {
                            @Override
                            public void onDataChange(DataSnapshot snapshot)
                            {
                                User usrObj = snapshot.getValue(User.class);
                                System.out.println(usrObj.getUserID() + " - " + usrObj.getEmail());

                                SharedPreferences.Editor editor = sharedpreferences.edit();
                                editor.putString("UserID", usrObj.getUserID());
                                editor.putString("UserEmail", usrObj.getEmail());
                                editor.apply();

                                Intent trainIntent = new Intent(LogIn.this, OptionsScreen.class);
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
                        Toast toast = Toast.makeText(getApplicationContext(), "User Credentials does not exist or incorrect", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                });

            }
        });

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

    @Override
    public void onBackPressed ()
    {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
