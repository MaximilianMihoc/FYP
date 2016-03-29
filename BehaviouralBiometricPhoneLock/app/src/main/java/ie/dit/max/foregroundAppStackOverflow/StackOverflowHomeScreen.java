/*
* Reference: http://developer.android.com/intl/ja/training/basics/network-ops/connecting.html
*
*
*
* */
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

public class StackOverflowHomeScreen extends TestBehaviouralBiometrics
{
    private static final String DEBUG_TAG = "ForegroundApp - StackOverflowHomeScreen";

    ListView questionsListView;
    QuestionListAdapter questionsListAdapter;
    SearchView searchView;
    ArrayList<Question> questionsList;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stackoverflow_home);

        startConnection("https://api.stackexchange.com/2.2/questions?pagesize=100&order=desc&sort=activity&site=stackoverflow&filter=!9YdnSIN18");

        searchView = (SearchView) findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener()
        {
            @Override
            public boolean onQueryTextSubmit(String query)
            {

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


    private void startConnection(String stringUrl)
    {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            new DownloadWebpageTask().execute(stringUrl);
        } else {
            System.out.println(DEBUG_TAG + " No internet connection available.");
        }
    }

    // Uses AsyncTask to create a task away from the main UI thread. This task takes a
    // URL string and uses it to create an HttpUrlConnection. Once the connection
    // has been established, the AsyncTask downloads the contents of the webpage as
    // an InputStream. Finally, the InputStream is converted into a string, which is
    // displayed in the UI by the AsyncTask's onPostExecute method.
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
                JSONObject json = new JSONObject(result);

                JSONArray items = json.getJSONArray("items");

                questionsList = new ArrayList<>();

                for(int i=0; i < items.length(); i++)
                {
                    JSONObject item = items.getJSONObject(i);
                    //System.out.println("Question ID: " + item.get("question_id") + "\nTitle: " + item.get("title"));

                    JSONObject ownerJson = item.getJSONObject("owner");

                    Owner questionOwner;
                    if(ownerJson.getString("user_type").equals("registered"))
                    {
                        questionOwner = new Owner(ownerJson.getInt("reputation"), ownerJson.getLong("user_id"), ownerJson.getString("display_name"));
                    }
                    else
                    {
                        questionOwner = new Owner();
                        questionOwner.setDisplay_name(ownerJson.getString("display_name"));
                    }

                    Question question = new Question( item.getLong("question_id"), item.getInt("answer_count"), item.getLong("creation_date"), item.getString("title"), questionOwner, item.getString("body"));

                    questionsList.add(question);
                }

                questionsListAdapter = new QuestionListAdapter(getApplicationContext(), questionsList);
                questionsListView.setEmptyView(findViewById(android.R.id.empty));
                questionsListView.setAdapter(questionsListAdapter);

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
