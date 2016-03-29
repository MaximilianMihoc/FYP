package ie.dit.max.foregroundAppCountriesPick;

import android.content.Intent;
import android.os.Bundle;
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

import ie.dit.max.behaviouralbiometricphonelock.OptionsScreen;
import ie.dit.max.behaviouralbiometricphonelock.R;
import ie.dit.max.behaviouralbiometricphonelock.TrainActivity;

public class CountryListGameTrain extends TrainActivity
{

    private int count;
    Button startButton;
    Button OKbutton;
    Button exitButton;
    Button helpButton;

    RelativeLayout trainLayout;
    TextView trainActivityTitle;
    TextView centerText;
    TextView centerText2;

    ImageView correctImage;
    ImageView wrongImage;

    String[] europeCountries;
    String[] newArray;
    String[] nonEuropeCountries;

    ListView listViewTrainScreen;
    ArrayAdapter<String> adapter;
    String correctValue;
    int trainIterations, attempts, goodAttempts, badAttempts;
    boolean endTraining;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_train_country_game);

        europeCountries = getResources().getStringArray(R.array.europeanCountries);
        nonEuropeCountries = getResources().getStringArray(R.array.nonEuropeCountries);
        trainIterations = 10; attempts = 10; goodAttempts = 0; badAttempts = 0;
        endTraining = false;

        centerText2 = (TextView)findViewById(R.id.textInCenter2);

        helpButton = (Button)findViewById(R.id.helpButton);

        OKbutton = (Button)findViewById(R.id.OKbutton);
        OKbutton.setOnTouchListener(gestureListener);

        exitButton = (Button)findViewById(R.id.exitButton);
        exitButton.setOnTouchListener(gestureListener);

        correctImage = (ImageView) findViewById(R.id.correctImage);
        wrongImage = (ImageView) findViewById(R.id.wrongImage);

        centerText2.setText("Now please play a small game where you are required to find the NON European country from a list." +
                "\n\nWhen the correct country is found, the list will be regenerated and a new country needs to be found." +
                "\n\nYou have 10 attempts. \n\nClick the Start button to enter the game.");
        //

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

        trainLayout.removeView(centerText2);
        trainLayout.removeView(OKbutton);
        trainLayout.removeView(exitButton);
        trainLayout.removeView(listViewTrainScreen);

        correctImage.setVisibility(View.INVISIBLE);
        wrongImage.setVisibility(View.INVISIBLE);

        startButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                count -= 1;
                if (!endTraining)
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
                        trainLayout.removeView(centerText);
                        trainLayout.removeView(OKbutton);
                        trainLayout.removeView(exitButton);
                        trainLayout.removeView(trainActivityTitle);
                        trainLayout.removeView(startButton);

                        trainLayout.addView(centerText2);
                        trainLayout.addView(OKbutton);
                        trainLayout.addView(exitButton);
                    }
                } else
                {
                    //exit
                    Intent trainIntent = new Intent(CountryListGameTrain.this, OptionsScreen.class);
                    startActivity(trainIntent);

                }

            }
        });

        OKbutton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                trainIterations = 10;
                goodAttempts = 0;
                badAttempts = 0;

                trainLayout.addView(trainActivityTitle);
                trainLayout.addView(listViewTrainScreen);

                trainLayout.removeView(centerText);
                trainLayout.removeView(centerText2);
                trainLayout.removeView(OKbutton);
                trainLayout.removeView(exitButton);

                trainActivityTitle.setText("Find the NON European Country" +
                        "\nAttempts: " + attempts + " Good: " + goodAttempts + " Wrong: " + badAttempts);

                // set initial list of values
                trainLayout.removeView(startButton);
                trainLayout.removeView(centerText);

                europeCountries = Randomize(europeCountries);
                int pos = getRandomNumber(nonEuropeCountries.length - 1);
                correctValue = nonEuropeCountries[pos];
                newArray = addNonEuropeanCountry(europeCountries, correctValue);
                System.out.println("correctValue: " + correctValue);

                adapter = new ArrayAdapter<>(getApplicationContext(), R.layout.row_layout_train_activity_firstscreen, newArray);
                listViewTrainScreen.setAdapter(adapter);
            }
        });

        exitButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(CountryListGameTrain.this, OptionsScreen.class);
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
                        correctImage.setVisibility(View.VISIBLE);
                        Timer timer = new Timer();
                        timer.schedule(new MyTimerTask(correctImage), 500);

                        europeCountries = Randomize(europeCountries);
                        int pos = getRandomNumber(nonEuropeCountries.length - 1);
                        correctValue = nonEuropeCountries[pos];
                        newArray = addNonEuropeanCountry(europeCountries, correctValue);
                        System.out.println("correctValue: " + correctValue);

                        adapter = new ArrayAdapter<>(getApplicationContext(), R.layout.row_layout_train_activity_firstscreen, newArray);
                        listViewTrainScreen.setAdapter(adapter);

                        trainIterations --;
                        goodAttempts ++;
                        trainActivityTitle.setText("Find the NON European Country" +
                                "\nAttempts: " + trainIterations + " Good: " + goodAttempts + " Wrong: " + badAttempts);
                    }
                    else
                    {
                        wrongImage.setVisibility(View.VISIBLE);
                        Timer timer = new Timer();
                        timer.schedule(new MyTimerTask(wrongImage), 500);

                        trainIterations --;
                        badAttempts ++;
                        trainActivityTitle.setText("Find the NON European Country" +
                                "\nAttempts: " + trainIterations + " Good: " + goodAttempts + " Wrong: " + badAttempts);
                    }

                }

                if (trainIterations <= 0 && !endTraining)
                {
                    endTraining = true;

                    centerText.setText("Train model is complete. Please exit.");
                    startButton.setText("Exit");
                    trainLayout.addView(startButton);
                    trainLayout.addView(centerText);

                    trainLayout.removeView(listViewTrainScreen);
                    trainLayout.removeView(trainActivityTitle);

                }

            }
        });


    }

    // include a non european country in array at a random position
    private String[] addNonEuropeanCountry(String[] array, String element)
    {
        String[] newArray = new String[array.length + 1];
        int position = getRandomNumber(array.length-1);
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
