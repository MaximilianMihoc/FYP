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
                Intent trainIntent = new Intent(MainActivity.this, TrainActivity.class);
                startActivity(trainIntent);
            }
        });


        /*
        Firebase.setAndroidContext(this);
        Firebase rootRef = new Firebase("https://fyp-max.firebaseio.com/");

        Firebase messageRef = rootRef.child("messageTest").child("MaxIsAwesome");
        messageRef.setValue("Hello Maxim, you are awesome?");*/

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
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
