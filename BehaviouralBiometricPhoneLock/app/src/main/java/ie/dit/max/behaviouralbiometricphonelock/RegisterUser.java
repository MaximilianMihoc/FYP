package ie.dit.max.behaviouralbiometricphonelock;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import java.util.Map;

public class RegisterUser extends AppCompatActivity
{

    Firebase ref;

    EditText userName;
    EditText email;
    EditText confirmEmail;
    EditText password;
    EditText confirmPassword;
    Button registerButton;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_user);
        Firebase.setAndroidContext(this);

        ref = new Firebase("https://fyp-max.firebaseio.com");

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

                ref.createUser(email.getText().toString(), password.getText().toString(), new Firebase.ValueResultHandler<Map<String, Object>>() {
                    @Override
                    public void onSuccess(Map<String, Object> result) {
                        System.out.println("Successfully created user account with uid: " + result.get("uid"));

                        Firebase newUserRef = ref.child("users").child(result.get("uid").toString());

                        User newUser = new User(userName.getText().toString(), email.getText().toString(), result.get("uid").toString());

                        newUserRef.setValue(newUser);
                    }

                    @Override
                    public void onError(FirebaseError firebaseError) {
                        System.out.println("Error on Register Activity -> Create user");
                    }
                });

                Intent trainIntent = new Intent(RegisterUser.this, LogIn.class);
                startActivity(trainIntent);
            }
        });

    }

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
