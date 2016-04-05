package ie.dit.max.foregroundAppStackOverflow;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import ie.dit.max.behaviouralbiometricphonelock.OptionsScreen;
import ie.dit.max.behaviouralbiometricphonelock.R;
import ie.dit.max.behaviouralbiometricphonelock.TestBehaviouralBiometrics;

/**
 * This activity is used to display Questions in a list, all returned from Stack Exchange API
 *
 * @author Maximilian Mihoc.
 * @version 1.0
 * @since 30/01/2016
 */

public class StackOverflowHomeScreen extends TestBehaviouralBiometrics
{
    private static final String DEBUG_TAG = "ForegroundApp - StackOverflowHomeScreen";
    private ListView questionsListView;
    private ArrayList<Question> questionsList;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stackoverflow_home);

        // get the last modified questions from StackExchange API using Asynchronous task
        startConnection("https://api.stackexchange.com/2.2/questions?pagesize=100&order=desc&sort=activity&site=stackoverflow&filter=!9YdnSIN18");

        SearchView searchView = (SearchView) findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener()
        {
            @Override
            public boolean onQueryTextSubmit(String query)
            {
                // check if query to use is unsafe and make it safe
                String safeQuery = URLParamEncoder.encode(query);

                String url = "https://api.stackexchange.com/2.2/search?order=desc&sort=activity&intitle=" + safeQuery + "&site=stackoverflow&filter=!9YdnSIN18";
                System.out.println("URL: " + url);

                startConnection(url);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText)
            {
                return false;
            }
        });

        questionsListView = (ListView) findViewById(android.R.id.list);
        // add gesture listener to the ListView. This is used to get Touch and Reaction features for each interaction
        questionsListView.setOnTouchListener(gestureListener);
        questionsListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                Intent goToQuestionBodyIntent = new Intent(StackOverflowHomeScreen.this, QuestionBodyScreen.class);

                Bundle bundle = new Bundle();
                bundle.putSerializable("questionSelected", questionsList.get(position));
                goToQuestionBodyIntent.putExtras(bundle);

                startActivity(goToQuestionBodyIntent);

                //Toast toast = Toast.makeText(getApplicationContext(), questionsList.get(position).getTitle(), Toast.LENGTH_SHORT);
                //toast.show();
            }
        });

    }

    @Override
    public void onBackPressed ()
    {
        Intent goToAnswersIntent = new Intent(StackOverflowHomeScreen.this, OptionsScreen.class);
        startActivity(goToAnswersIntent);
    }

    /**
     * Start Asynchronous connection to get questions from the API based on the URL
     *
     * @param stringUrl String
     */
    private void startConnection(String stringUrl)
    {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            new DownloadWebpageTask().execute(stringUrl);
        } else {
            System.out.println(DEBUG_TAG + "No internet connection available.");
        }
    }

    /**
     * Uses AsyncTask to create a task away from the main UI thread. This task takes a
     * URL string and uses it to create an HttpUrlConnection. Once the connection
     * has been established, the AsyncTask downloads the contents of the webpage as
     * an InputStream. Finally, the InputStream is converted into a string, which is
     * displayed in the UI by the AsyncTask's onPostExecute method.
     *
     * Reference: http://developer.android.com/training/basics/network-ops/connecting.html
     *
     * The code has been changed to meet the needs of this app
     */
    private class DownloadWebpageTask extends AsyncTask<String, Void, String>
    {
        @Override
        protected String doInBackground(String... urls)
        {
            // params comes from the execute() call: params[0] is the url.
            try {

                return GetHTTPResponse.downloadUrl(urls[0]);
            } catch (IOException e) {
                return "Unable to retrieve web page. URL may be invalid.";
            }
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result)
        {
            try
            {
                // get the result and place it into a JSON object.
                JSONObject json = new JSONObject(result);
                // create JSONArray to store all items returned
                JSONArray items = json.getJSONArray("items");
                questionsList = new ArrayList<>();

                // for every item in the JSONArray, create a Question Object and add it to an ArrayList of Questions
                for(int i=0; i < items.length(); i++)
                {
                    JSONObject item = items.getJSONObject(i);
                    JSONObject ownerJson = item.getJSONObject("owner");
                    Owner questionOwner;

                    // there are 2 types of users in the API, registered and unregistered.
                    // for registered users, display mode details
                    if(ownerJson.getString("user_type").equals("registered"))
                    {
                        questionOwner = new Owner(ownerJson.getInt("reputation"), ownerJson.getLong("user_id"), ownerJson.getString("display_name"));
                    }
                    else
                    {
                        questionOwner = new Owner();
                        questionOwner.setDisplay_name(ownerJson.getString("display_name"));
                    }

                    // create Question object and add it to arrayList
                    Question question = new Question( item.getLong("question_id"), item.getInt("answer_count"), item.getLong("creation_date"), item.getString("title"), questionOwner, item.getString("body"));
                    questionsList.add(question);
                }

                // initialise the Array adapter that is used to Populate lte List View with the Questions stored in the questions Array List
                QuestionListAdapter questionsListAdapter = new QuestionListAdapter(getApplicationContext(), questionsList);
                questionsListView.setEmptyView(findViewById(android.R.id.empty));
                questionsListView.setAdapter(questionsListAdapter);

                // check if the Training data has been loaded for the current user before the list is displayed.
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


            } catch (JSONException e)
            {
                e.printStackTrace();
            }

        }
    }
}
