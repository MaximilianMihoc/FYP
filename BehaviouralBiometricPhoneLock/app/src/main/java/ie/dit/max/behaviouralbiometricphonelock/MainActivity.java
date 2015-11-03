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
import org.opencv.ml.Ml;
import org.opencv.ml.SVM;
import org.opencv.ml.TrainData;

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

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        goToTrainButton = (Button) findViewById(R.id.goToTrainButton);
        goToTrainButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent trainIntent = new Intent(MainActivity.this, TrainActivity.class);
                startActivity(trainIntent);
            }
        });

        //SVM svm = TrainData.loadFromCSV();
        //svm.
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
