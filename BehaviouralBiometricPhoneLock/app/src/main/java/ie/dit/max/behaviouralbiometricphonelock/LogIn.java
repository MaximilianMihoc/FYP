package ie.dit.max.behaviouralbiometricphonelock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import ie.dit.max.foregroundApp.StackOverflowHomeScreen;

public class LogIn extends Activity
{

    Firebase ref;

    EditText email;
    EditText password;
    Button logInButton;

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

                        // Attach an listener to read the data at our posts reference
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

                                Intent trainIntent = new Intent(LogIn.this, StackOverflowHomeScreen.class);
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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_log_in, menu);
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
