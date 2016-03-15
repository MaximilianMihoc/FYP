package ie.dit.max.behaviouralbiometricphonelock;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity
{

    // Check if OpenCV loads properly
    static
    {
        if (!OpenCVLoader.initDebug())
        {
            Log.i("OpenCV", "OpenCV initialization failed");
        } else
        {
            Log.i("OpenCV", "OpenCV initialization successful");
        }
    }

    //Declare global variables
    Button goToTrainButton;
    Button foToLogInActivity;
    Button goToRegistrationActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        goToTrainButton = (Button) findViewById(R.id.goToTrainButton);
        foToLogInActivity = (Button) findViewById(R.id.goToLoginActivity);
        goToRegistrationActivity = (Button) findViewById(R.id.goToRegistrationActivity);

        foToLogInActivity.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent trainIntent = new Intent(MainActivity.this, LogIn.class);
                startActivity(trainIntent);
            }
        });

        goToRegistrationActivity.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent trainIntent = new Intent(MainActivity.this, RegisterUser.class);
                startActivity(trainIntent);
            }
        });

        goToTrainButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent trainIntent = new Intent(MainActivity.this, CrossValidationActivity.class);
                startActivity(trainIntent);
            }
        });


    }
}
