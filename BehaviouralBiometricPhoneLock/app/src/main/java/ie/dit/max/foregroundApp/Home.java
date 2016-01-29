/*
* Reference: http://developer.android.com/intl/ja/training/basics/network-ops/connecting.html
*
*
*
* */
package ie.dit.max.foregroundApp;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ie.dit.max.behaviouralbiometricphonelock.R;

public class Home extends AppCompatActivity
{
    private static final String DEBUG_TAG = "ForegroundApp";

    ListView queionsList;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        startConnection("https://api.stackexchange.com/2.2/questions?order=desc&sort=activity&site=stackoverflow");

        queionsList = (ListView) findViewById(android.R.id.list);

    }

    private void startConnection(String stringUrl)
    {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            new DownloadWebpageTask().execute(stringUrl);
        } else {
            Log.d(DEBUG_TAG, "No network connection available.");
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

                List<String> your_array_list = new ArrayList<String>();

                for(int i=0; i < items.length(); i++)
                {
                    JSONObject item = items.getJSONObject(i);
                    System.out.println("Question ID: " + item.get("question_id") + "\nTitle: " + item.get("title"));
                    your_array_list.add(item.get("title").toString());
                }

                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, your_array_list );

                queionsList.setAdapter(arrayAdapter);


            } catch (JSONException e)
            {
                e.printStackTrace();
            }

        }
    }
}
