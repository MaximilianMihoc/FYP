package ie.dit.max.foregroundApp;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.IOException;
import java.util.ArrayList;

import ie.dit.max.behaviouralbiometricphonelock.R;

public class AnswersScreen extends AppCompatActivity
{
    private static final String DEBUG_TAG = "AnswersScreen";

    ListView answersListView;
    AnswerListAdapter answerListAdapter;
    Question questionSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_answers_screen);

        answersListView = (ListView) findViewById(android.R.id.list);

        Bundle bundle = getIntent().getExtras();
        questionSelected = (Question) bundle.getSerializable("selectedQuestion");

        TextView questionTitle = (TextView)findViewById(R.id.questionBody);
        questionTitle.setText(Html.fromHtml(questionSelected.getTitle()));

        startConnection("https://api.stackexchange.com/2.2/questions/" + questionSelected.getQuestion_id() + "/answers?order=desc&sort=activity&site=stackoverflow&filter=!3yXvhCikopVa8vWh*");

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

                ArrayList<Answer> answersList = new ArrayList<>();

                for(int i=0; i < items.length(); i++)
                {
                    JSONObject item = items.getJSONObject(i);

                    JSONObject ownerJson = item.getJSONObject("owner");
                    Owner answerOwner;
                    if(ownerJson.getString("user_type").equals("registered"))
                    {
                        answerOwner = new Owner(ownerJson.getInt("reputation"), ownerJson.getLong("user_id"), ownerJson.getString("display_name"));
                    }
                    else
                    {
                        answerOwner = new Owner();
                        answerOwner.setDisplay_name(ownerJson.getString("display_name"));
                    }

                    //Answer(Owner owner, int comment_count, int down_vote_count, int up_vote_count, boolean is_accepted, int score, long creation_date, String body)
                    Answer answer = new Answer(answerOwner,
                            item.getInt("comment_count"),
                            item.getInt("down_vote_count"),
                            item.getInt("up_vote_count"),
                            item.getBoolean("is_accepted"),
                            item.getInt("score"),
                            item.getLong("creation_date"),
                            item.getString("body"));

                    if(item.getInt("comment_count") != 0)
                    {
                        ArrayList<Comment> commentsList = new ArrayList<>();
                        JSONArray commentsJson = item.getJSONArray("comments");
                        for(int j=0; j < commentsJson.length(); j++)
                        {
                            JSONObject comment = commentsJson.getJSONObject(j);
                            JSONObject ownerCommentJson = comment.getJSONObject("owner");
                            Owner commentOwner;
                            if(ownerJson.getString("user_type").equals("registered"))
                            {
                                commentOwner = new Owner(ownerCommentJson.getInt("reputation"), ownerCommentJson.getLong("user_id"), ownerCommentJson.getString("display_name"));
                            }
                            else
                            {
                                commentOwner = new Owner();
                                commentOwner.setDisplay_name(ownerCommentJson.getString("display_name"));
                            }
                            //Comment(boolean edited, int score, long creation_date, String body, Owner owner)
                            Comment commentObject = new Comment(comment.getBoolean("edited"), comment.getInt("score"), comment.getLong("creation_date"), comment.getString("body"), commentOwner);

                            commentsList.add(commentObject);
                        }
                        answer.setComments(commentsList);
                    }

                    answersList.add(answer);
                }

                answerListAdapter = new AnswerListAdapter(getApplicationContext(), answersList);
                //queionsList.setEmptyView(findViewById(android.R.id.empty));
                answersListView.setAdapter(answerListAdapter);


            } catch (JSONException e)
            {
                e.printStackTrace();
            }

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_answers_screen, menu);
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
