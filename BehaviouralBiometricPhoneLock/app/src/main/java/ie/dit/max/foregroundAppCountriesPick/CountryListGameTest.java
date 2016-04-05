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
import ie.dit.max.behaviouralbiometricphonelock.TestBehaviouralBiometrics;

/**
 *
 *  This activity extends the Test activity and it has been used to collect the test data from the users.
 *  For this activity, a game called Country-List game has been created.
 *
 *  The game consists in finding a country that is Not European, from a list of countries.
 *      Only one country from outside Europe is displayed in the list at a time.
 *      When the correct country has been found, the list of countries gets regenerated and a new NON european
 *      country, randomly chosen from a list of non european countries is added.
 *
 *  This game makes the user interacts with the system a lot and that is what is needed for this project.
 *
 *  This game has been included in the test phase so that users that test the application without looking at StackOverflow,
 *      they can test it playing this game.
 *
 * @author Maximilian Mihoc.
 * @version 1.0
 *
 */
public class CountryListGameTest extends TestBehaviouralBiometrics
{
    private Button endButton;
    private Button OKbutton;
    private Button exitButton;

    private RelativeLayout trainLayout;
    private TextView activityTitle;
    private TextView centerText;
    private String[] europeCountries;
    private String[] newArray;
    private String[] nonEuropeCountries;

    private ListView listViewCountryList;
    private ArrayAdapter<String> adapter;
    private String correctValue;
    private int iterations, attempts, goodAttempts, badAttempts;

    private ImageView correctImage;
    private ImageView wrongImage;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_country_game);

        europeCountries = getResources().getStringArray(R.array.europeanCountries);
        nonEuropeCountries = getResources().getStringArray(R.array.nonEuropeCountries);
        iterations = 10; attempts = 10; goodAttempts = 0; badAttempts = 0;

        // initialise the widgets
        trainLayout = (RelativeLayout)findViewById(R.id.relativeLayoutTraiFirstScreenID);
        listViewCountryList = (ListView) findViewById(android.R.id.list);
        activityTitle = (TextView)findViewById(R.id.trainActivityTitle);
        centerText = (TextView)findViewById(R.id.textInCenter);

        OKbutton = (Button)findViewById(R.id.OKbutton);
        OKbutton.setOnTouchListener(gestureListener);

        Button helpButton = (Button)findViewById(R.id.helpButton);

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

        trainLayout.addView(OKbutton);
        trainLayout.addView(exitButton);

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
                centerText.setText("Results " +
                        "\nAttempts: " + attempts + " Good: " + goodAttempts + " Wrong: " + badAttempts);
                centerText.setTextSize(30);

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
                Intent intent = new Intent(CountryListGameTest.this, OptionsScreen.class);
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


    /**
     * Method Randomize
     * This method gets an array and it randomises the elements. This is used every time after a user finds
     *      the correct country in order to display the countries again in different order.
     *
     * This process makes sure that the countries will be displayed in different order every time.
     *
     * Reference: http://superuser.com/questions/687119/random-shuffling-of-string-array
     *
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