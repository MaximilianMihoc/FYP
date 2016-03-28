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
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

public class ChangePassword extends AppCompatActivity
{
    Firebase ref;

    EditText oldPassword;
    EditText newPassword;
    EditText newPasswordConfirmed;
    Button changePassword;

    SharedPreferences sharedpreferences;
    private String userID;
    private String userEmail;

    String oldPasswordStr;
    String newPasswordStr;
    String newPasswordConfirmedStr;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);
        Firebase.setAndroidContext(this);

        ref = new Firebase("https://fyp-max.firebaseio.com");
        sharedpreferences = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        if(sharedpreferences.contains("UserEmail")) userEmail = sharedpreferences.getString("UserEmail", "");

        oldPassword = (EditText) findViewById(R.id.oldPassword);
        newPassword = (EditText) findViewById(R.id.newPassword1);
        newPasswordConfirmed = (EditText) findViewById(R.id.newPassword2);
        changePassword = (Button) findViewById(R.id.changePassword);

        changePassword.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                oldPasswordStr = oldPassword.getText().toString();
                newPasswordStr = newPassword.getText().toString();
                newPasswordConfirmedStr = newPasswordConfirmed.getText().toString();

                ref.authWithPassword(userEmail, oldPasswordStr, new Firebase.AuthResultHandler()
                {
                    @Override
                    public void onAuthenticated(AuthData authData)
                    {
                        if(newPasswordStr.trim().length() < 5)
                        {
                            Toast.makeText(getApplicationContext(), "Please Enter a New Password of at least 5 characters.", Toast.LENGTH_SHORT).show();
                        }
                        else if(!newPasswordStr.equals(newPasswordConfirmedStr))
                        {
                            Toast.makeText(getApplicationContext(), "Password and Confirmation Password do not match.", Toast.LENGTH_SHORT).show();
                        }
                        else
                        {
                            ref.changePassword(userEmail, oldPasswordStr, newPasswordStr, new Firebase.ResultHandler()
                            {
                                @Override
                                public void onSuccess()
                                {
                                    Toast.makeText(getApplicationContext(), "Password successfully changed", Toast.LENGTH_SHORT).show();

                                    Intent trainIntent = new Intent(ChangePassword.this, OptionsScreen.class);
                                    startActivity(trainIntent);
                                }

                                @Override
                                public void onError(FirebaseError firebaseError)
                                {
                                    Toast.makeText(getApplicationContext(), "Could not change password, please try again.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }

                    @Override
                    public void onAuthenticationError(FirebaseError firebaseError)
                    {
                        Toast toast = Toast.makeText(getApplicationContext(), "Old Password Incorrect", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                });


            }
        });
    }
}
