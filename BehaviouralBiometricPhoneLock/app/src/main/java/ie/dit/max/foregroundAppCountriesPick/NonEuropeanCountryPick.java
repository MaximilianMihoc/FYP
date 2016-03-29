package ie.dit.max.foregroundAppCountriesPick;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import ie.dit.max.behaviouralbiometricphonelock.OptionsScreen;
import ie.dit.max.behaviouralbiometricphonelock.R;
import ie.dit.max.behaviouralbiometricphonelock.TestBehaviouralBiometrics;
import ie.dit.max.behaviouralbiometricphonelock.TrainActivity;

public class NonEuropeanCountryPick extends TestBehaviouralBiometrics
{
    Button endButton;
    Button OKbutton;
    Button exitButton;
    Button helpButton;

    RelativeLayout trainLayout;
    TextView activityTitle;
    TextView centerText;
    String[] europeCountries;
    String[] newArray;
    String[] nonEuropeCountries;

    ListView listViewCountryList;
    ArrayAdapter<String> adapter;
    String correctValue;
    int iterations, attempts, goodAttempts, badAttempts;
    boolean endTraining;

    ImageView correctImage;
    ImageView wrongImage;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_non_european_country_pick);

        europeCountries = getResources().getStringArray(R.array.europeanCountries);
        nonEuropeCountries = getResources().getStringArray(R.array.nonEuropeCountries);
        iterations = 10; attempts = 10; goodAttempts = 0; badAttempts = 0;
        endTraining = false;

        // initialise the widgets
        trainLayout = (RelativeLayout)findViewById(R.id.relativeLayoutTraiFirstScreenID);
        listViewCountryList = (ListView) findViewById(android.R.id.list);
        activityTitle = (TextView)findViewById(R.id.trainActivityTitle);
        centerText = (TextView)findViewById(R.id.textInCenter);

        OKbutton = (Button)findViewById(R.id.OKbutton);
        OKbutton.setOnTouchListener(gestureListener);

        helpButton = (Button)findViewById(R.id.helpButton);

        exitButton = (Button)findViewById(R.id.exitButton);
        exitButton.setOnTouchListener(gestureListener);

        endButton = (Button) findViewById(R.id.quitButton);
        endButton.setOnTouchListener(gestureListener);

        correctImage = (ImageView) findViewById(R.id.correctImage);
        wrongImage = (ImageView) findViewById(R.id.wrongImage);

        centerText.setText("In this game you are required to find the NON European country from a list." +
                "\n\nWhen the correct country is found, the list will be regenerated and a new country needs to be found." +
                "\n\nYou have 10 attempts. \n\nClick the Start button to enter the game.");

        trainLayout.removeView(activityTitle);
        trainLayout.removeView(endButton);
        trainLayout.removeView(listViewCountryList);
        trainLayout.removeView(OKbutton);
        trainLayout.removeView(exitButton);

        correctImage.setVisibility(View.INVISIBLE);
        wrongImage.setVisibility(View.INVISIBLE);

        ///
        trainLayout.addView(OKbutton);
        trainLayout.addView(exitButton);
        //

        OKbutton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                iterations = 10;
                goodAttempts = 0;
                badAttempts = 0;

                if (!TestBehaviouralBiometrics.trainDataLoaded)
                {
                    System.out.println("Waiting " + TestBehaviouralBiometrics.trainDataLoaded);
                    try
                    {
                        Thread.sleep(5000);
                    } catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }

                trainLayout.addView(activityTitle);
                trainLayout.addView(endButton);
                trainLayout.addView(listViewCountryList);

                trainLayout.removeView(centerText);
                trainLayout.removeView(OKbutton);
                trainLayout.removeView(exitButton);

                activityTitle.setText("Find the NON European Country" +
                        "\nAttempts: " + attempts + " Good: " + goodAttempts + " Wrong: " + badAttempts);

                // set initial list of values
                europeCountries = Randomize(europeCountries);
                int pos = getRandomNumber(nonEuropeCountries.length);
                correctValue = nonEuropeCountries[pos];
                newArray = addNonEuropeanCountry(europeCountries, correctValue);
                System.out.println("correctValue: " + correctValue);

                adapter = new ArrayAdapter<>(getApplicationContext(), R.layout.row_layout_train_activity_firstscreen, newArray);
                listViewCountryList.setAdapter(adapter);
            }
        });

        endButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                centerText.setText("Results: " +
                        "\nAttempts: " + attempts + " Good: " + goodAttempts + " Wrong: " + badAttempts);

                OKbutton.setText("Retry");

                trainLayout.removeView(activityTitle);
                trainLayout.removeView(endButton);
                trainLayout.removeView(listViewCountryList);

                trainLayout.addView(centerText);
                trainLayout.addView(OKbutton);
                trainLayout.addView(exitButton);
            }
        });

        exitButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(NonEuropeanCountryPick.this, OptionsScreen.class);
                startActivity(intent);
            }
        });

        helpButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Toast toast = Toast.makeText(getApplicationContext(), "Correct Value is: " + correctValue, Toast.LENGTH_SHORT);
                toast.show();
            }
        });

        listViewCountryList.setOnTouchListener(gestureListener);
        listViewCountryList.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                if(correctValue.equals(newArray[position]))
                {
                    //Toast toast = Toast.makeText(getApplicationContext(), "Well Done. Try another one.", Toast.LENGTH_SHORT);
                    //toast.show();
                    correctImage.setVisibility(View.VISIBLE);
                    Timer timer = new Timer();
                    timer.schedule(new MyTimerTask(correctImage), 500);

                    europeCountries = Randomize(europeCountries);
                    int pos = getRandomNumber(nonEuropeCountries.length);
                    correctValue = nonEuropeCountries[pos];
                    newArray = addNonEuropeanCountry(europeCountries, correctValue);
                    System.out.println("correctValue: " + correctValue);

                    adapter = new ArrayAdapter<>(getApplicationContext(), R.layout.row_layout_train_activity_firstscreen, newArray);
                    listViewCountryList.setAdapter(adapter);

                    iterations --;
                    goodAttempts++;

                    activityTitle.setText("Find the NON European Country" +
                            "\nAttempts: " + iterations + " Good: " + goodAttempts + " Wrong: " + badAttempts);
                }
                else
                {
                    //Toast toast = Toast.makeText(getApplicationContext(), "That is an European country. \nPlease have another try.", Toast.LENGTH_SHORT);
                    //toast.show();
                    wrongImage.setVisibility(View.VISIBLE);
                    Timer timer = new Timer();
                    timer.schedule(new MyTimerTask(wrongImage), 500);

                    badAttempts++;
                    iterations --;
                    activityTitle.setText("Find the NON European Country" +
                            "\nAttempts: " + iterations + " Good: " + goodAttempts + " Wrong: " + badAttempts);
                }

                if (iterations <= 0)
                {
                    centerText.setText("Results: "+
                            "\nAttempts: " + attempts + " Good: " + goodAttempts + " Wrong: " + badAttempts);

                    OKbutton.setText("Retry");

                    trainLayout.removeView(activityTitle);
                    trainLayout.removeView(endButton);
                    trainLayout.removeView(listViewCountryList);

                    trainLayout.addView(centerText);
                    trainLayout.addView(OKbutton);
                    trainLayout.addView(exitButton);
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

    private class MyTimerTask extends TimerTask
    {
        private ImageView correctImage;
        public MyTimerTask(ImageView correctImage)
        {
            this.correctImage = correctImage;
        }

        @Override
        public void run()
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    correctImage.setVisibility(View.INVISIBLE);
                }
            });
        }
    }
}