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

/**
 *
 *  This activity extends the Train activity and it has been used to collect the train data from the users.
 *  In this activity, a game called Country-List game has been created.
 *
 *  The game consists in finding a country that is Not European, from a list of countries.
 *      Only one country from outside Europe is displayed in the list at a time.
 *      When the correct country has been found, the list of countries gets regenerated and a new NON european
 *      country, randomly chosen from a list of non european countries is added.
 *
 *  This game makes the user interacts with the system a lot and that is what is needed for this project.
 *
 * @author Maximilian Mihoc.
 * @version 1.0
 *
 */
public class CountryListGameTrain extends TrainActivity
{
    private RelativeLayout trainLayout;
    private Button startButton;
    private Button OKbutton;
    private Button exitButton;
    private TextView trainActivityTitle;
    private TextView centerText;
    private TextView centerText2;
    private ImageView correctImage;
    private ImageView wrongImage;
    private ListView listViewTrainScreen;

    private String[] europeCountries;
    private String[] newArray;
    private String[] nonEuropeCountries;
    private int count;

    private ArrayAdapter<String> adapter;
    private String correctValue;
    private int trainIterations, attempts, goodAttempts, badAttempts;
    private boolean endTraining;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_train_country_game);

        // get a list of European countries from Resources
        europeCountries = getResources().getStringArray(R.array.europeanCountries);
        // get a list of NON European countries from Resources
        nonEuropeCountries = getResources().getStringArray(R.array.nonEuropeCountries);

        //initialise variables
        trainIterations = 10;
        attempts = 10;
        goodAttempts = 0;
        badAttempts = 0;
        endTraining = false;

        centerText2 = (TextView)findViewById(R.id.textInCenter2);
        Button helpButton = (Button)findViewById(R.id.helpButton);
        OKbutton = (Button)findViewById(R.id.OKbutton);
        OKbutton.setOnTouchListener(gestureListener);
        exitButton = (Button)findViewById(R.id.exitButton);
        exitButton.setOnTouchListener(gestureListener);
        correctImage = (ImageView) findViewById(R.id.correctImage);
        wrongImage = (ImageView) findViewById(R.id.wrongImage);

        centerText2.setText("Train system\n\n" +
                "You are required to play a small game where you have to find the NON European country from a list." +
                "\n\nWhen the correct country is found, the list will be regenerated and a new country needs to be found." +
                "\n\nYou have 10 attempts. \n\nClick the Start button to enter the game.");
        //

        listViewTrainScreen = (ListView) findViewById(android.R.id.list);

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
                //count number of taps
                count -= 1;
                if (!endTraining)
                {
                    if (count > 1)
                    {
                        startButton.setText("Tap me " + count + " times!");
                        centerText.setText("" + count);
                    }
                    else if (count == 1)
                    {
                        startButton.setText("Tap me one more time Please!");
                        centerText.setText("" + count);
                    }
                    else
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
                }
                else
                {
                    //exit
                    Intent trainIntent = new Intent(CountryListGameTrain.this, OptionsScreen.class);
                    startActivity(trainIntent);

                }

            }
        });

        // When this button is clicked, the game options are displayed.
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

        // hidden help button to find the correct straight away
        helpButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Toast toast = Toast.makeText(getApplicationContext(), "Correct Value is: " + correctValue, Toast.LENGTH_SHORT);
                toast.show();
            }
        });

        // gesture listener used to get touch details from every interaction
        listViewTrainScreen.setOnTouchListener(gestureListener);
        listViewTrainScreen.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                if (trainIterations > 0)
                {
                    if(correctValue.equals(newArray[position]))
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

    /**
     * Method addNonEuropeanCountry
     * This method is used to include a non european country in array at a random position
     *
     * Gets the European country list in and adds in a non european country
     *
     * @param array String[]
     * @param element String
     * @return String[]
     */
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

    /**
     * Method Randomize
     * This method gets an array and it randomises the elements. This is used every time after a user finds
     *      the correct country in order to display the countries again in different order.
     *
     * This process makes sure that the countries will be displayed in different order every time.
     *
     * Reference: http://superuser.com/questions/687119/random-shuffling-of-string-array
     * @param arr
     * @return
     */
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

    /**
     * Method getRandomValue
     *
     * This method teruns a random number between 0 and value +1
     *
     * @param value int
     * @return int
     */
    private int getRandomNumber(int value)
    {
        Random rand = new Random();
        int r = rand.nextInt(value+1);

        return r;
    }

    /**
     * This method is used to create a timer task.
     * Used to set the time that the images displayed for Correct or Wrong are shown for
     *
     */
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
