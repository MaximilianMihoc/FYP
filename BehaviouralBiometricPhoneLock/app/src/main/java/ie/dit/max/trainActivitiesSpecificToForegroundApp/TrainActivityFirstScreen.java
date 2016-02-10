package ie.dit.max.trainActivitiesSpecificToForegroundApp;

import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import ie.dit.max.behaviouralbiometricphonelock.OptionsScreen;
import ie.dit.max.behaviouralbiometricphonelock.R;
import ie.dit.max.behaviouralbiometricphonelock.TrainActivity;
import ie.dit.max.foregroundAppStackOverflow.StackOverflowHomeScreen;

public class TrainActivityFirstScreen extends TrainActivity
{

    private int count;
    Button startButton;
    RelativeLayout trainLayout;
    TextView trainActivityTitle;
    TextView centerText;
    String[] europeCountries;
    String[] newArray;
    String[] nonEuropeCountries;

    ListView listViewTrainScreen;
    ArrayAdapter<String> adapter;
    String correctValue;
    int trainIterations;
    boolean endTraining;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_train_activity_first_screen);

        europeCountries = getResources().getStringArray(R.array.europeanCountries);
        nonEuropeCountries = getResources().getStringArray(R.array.nonEuropeCountries);
        trainIterations = 10;
        endTraining = false;

        listViewTrainScreen = (ListView) findViewById(android.R.id.list);
        //trainLayout.removeView(listViewTrainScreen);

        trainLayout = (RelativeLayout)findViewById(R.id.relativeLayoutTraiFirstScreenID);
        trainActivityTitle = (TextView)findViewById(R.id.trainActivityTitle);
        centerText = (TextView)findViewById(R.id.textInCenter);

        startButton = (Button) findViewById(R.id.startTrainingFirstActivity);
        startButton.setOnTouchListener(gestureListener);
        count = 5;
        startButton.setText("Tap me " + count + " times");
        centerText.setText("" + count);

        startButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                count -= 1;
                if(!endTraining)
                {
                    if (count > 1)
                    {
                        startButton.setText("Tap me " + count + " times!");
                        centerText.setText("" + count);
                    } else if (count == 1)
                    {
                        startButton.setText("Tap me one more time Please!");
                        centerText.setText("" + count);
                    } else
                    {
                        trainActivityTitle.setText("Try to find the NON-European country from the list and click on it.");
                        trainLayout.removeView(startButton);
                        trainLayout.removeView(centerText);

                        europeCountries = Randomize(europeCountries);
                        int pos = getRandomNumber(nonEuropeCountries.length);
                        correctValue = nonEuropeCountries[pos];
                        newArray = addNonEuropeanCountry(europeCountries, correctValue);
                        System.out.println("correctValue: " + correctValue);

                        adapter = new ArrayAdapter<>(getApplicationContext(), R.layout.row_layout_train_activity_firstscreen, newArray);
                        listViewTrainScreen.setAdapter(adapter);
                    }
                }
                else
                {
                    //exit
                    Intent trainIntent = new Intent(TrainActivityFirstScreen.this, OptionsScreen.class);
                    startActivity(trainIntent);

                }

            }
        });

        listViewTrainScreen.setOnTouchListener(gestureListener);
        listViewTrainScreen.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {

                if (trainIterations > 0)
                {
                    if(correctValue == newArray[position])
                    {
                        Toast toast = Toast.makeText(getApplicationContext(), "Well Done. Try another one.", Toast.LENGTH_SHORT);
                        toast.show();

                        europeCountries = Randomize(europeCountries);
                        int pos = getRandomNumber(nonEuropeCountries.length);
                        correctValue = nonEuropeCountries[pos];
                        newArray = addNonEuropeanCountry(europeCountries, correctValue);
                        System.out.println("correctValue: " + correctValue);

                        adapter = new ArrayAdapter<>(getApplicationContext(), R.layout.row_layout_train_activity_firstscreen, newArray);
                        listViewTrainScreen.setAdapter(adapter);

                        trainIterations --;
                    }
                    else
                    {
                        Toast toast = Toast.makeText(getApplicationContext(), "That is an European country. \nPlease have another try.", Toast.LENGTH_SHORT);
                        toast.show();

                        trainIterations --;
                    }

                }

                if (trainIterations <= 0 && !endTraining)
                {
                    Toast toast = Toast.makeText(getApplicationContext(), "Train model is complete. Please exit.", Toast.LENGTH_SHORT);
                    toast.show();
                    endTraining = true;
                    trainActivityTitle.setText("Well done, training has been completed");
                    startButton.setText("Exit");
                    trainLayout.addView(startButton);

                }

            }
        });


    }

    // include a non european country in array at a random position
    private String[] addNonEuropeanCountry(String[] array, String element)
    {
        String[] newArray = new String[array.length + 1];
        int position = getRandomNumber(array.length);
        for (int i = 0; i < array.length; i++)
        {
            newArray[i] = array[i];
        }
        String temp = newArray[position];
        newArray[position] = element;
        newArray[newArray.length-1] = temp;

        return newArray;
    }


    // Reference: http://superuser.com/questions/687119/random-shuffling-of-string-array
    private String[] Randomize(String[] arr) {
        String[] randomizedArray = new String[arr.length];
        System.arraycopy(arr, 0, randomizedArray, 0, arr.length);
        Random rgen = new Random();

        for (int i = 0; i < randomizedArray.length; i++) {
            int randPos = rgen.nextInt(randomizedArray.length);
            String tmp = randomizedArray[i];
            randomizedArray[i] = randomizedArray[randPos];
            randomizedArray[randPos] = tmp;
        }

        return randomizedArray;
    }
    // End Reference

    private int getRandomNumber(int value)
    {
        Random rand = new Random();
        int r = rand.nextInt(value+1);

        return r;
    }


}