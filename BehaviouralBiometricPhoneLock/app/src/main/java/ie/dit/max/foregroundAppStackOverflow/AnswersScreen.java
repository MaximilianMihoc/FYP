package ie.dit.max.foregroundAppStackOverflow;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import ie.dit.max.behaviouralbiometricphonelock.R;
import ie.dit.max.behaviouralbiometricphonelock.TestBehaviouralBiometrics;

/**
 * This activity is used to display answers of a question from StackExchange
 *
 * @author Maximilian Mihoc.
 * @version 1.0
 * @since 30/01/2016
 */
public class AnswersScreen extends TestBehaviouralBiometrics
{
    private static final String DEBUG_TAG = "Answers Screen";
    private ListView answersListView;
    private AnswerListAdapter answerListAdapter;
    private Question questionSelected;

    private Button backToQuestionBody;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_answers_screen);

        answersListView = (ListView) findViewById(android.R.id.list);
        // add gesture listener to the ListView. This is used to get Touch and Reaction features for each interaction
        answersListView.setOnTouchListener(gestureListener);

        // get the selected question from previous activity. In this case the previous activity is QuestionBodyScreen
        Bundle bundle = getIntent().getExtras();
        questionSelected = (Question) bundle.getSerializable("selectedQuestion");

        // go back to questionBody Screen
        backToQuestionBody = (Button) findViewById(R.id.backToQuestionBody);
        backToQuestionBody.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent trainIntent = new Intent(AnswersScreen.this, QuestionBodyScreen.class);

                Bundle bundle = new Bundle();
                bundle.putSerializable("questionSelected", questionSelected);
                trainIntent.putExtras(bundle);

                startActivity(trainIntent);
            }
        });

        TextView questionTitle = (TextView)findViewById(R.id.questionBody);
        questionTitle.setText(Html.fromHtml("<b>" + questionSelected.getTitle() + "</b>"));

        // get the answers from StackExchange API using Asynchronous task
        startConnection("https://api.stackexchange.com/2.2/questions/" + questionSelected.getQuestion_id() + "/answers?order=desc&sort=activity&site=stackoverflow&filter=!3yXvhCikopVa8vWh*");

    }

    /**
     * Start Asynchronous connection where the answers of a question are retrieved from the API
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
            Log.d(DEBUG_TAG, "No network connection available.");
        }
    }

    /**
     *Uses AsyncTask to create a task away from the main UI thread. This task takes a
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
                // get the answer retrieved by the API and save it in a JSON object
                JSONObject json = new JSONObject(result);
                // get Json array items
                JSONArray items = json.getJSONArray("items");
                ArrayList<Answer> answersList = new ArrayList<>();

                // for each answer in the itemsArray, get the details and create an Answer and Owner objects
                for(int i=0; i < items.length(); i++)
                {
                    JSONObject item = items.getJSONObject(i);
                    JSONObject ownerJson = item.getJSONObject("owner");
                    Owner answerOwner;

                    // there are 2 types of users in the API, registered and unregistered.
                    // for registered users, display mode details
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

                    // check if the answer has comments associated.
                    // Create Comments list with and place it in the Answer object
                    if(item.getInt("comment_count") > 0)
                    {
                        ArrayList<Comment> commentsList = new ArrayList<>();
                        JSONArray commentsJson = item.getJSONArray("comments");

                        //for each comment det details and place them in Comment object
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
                            // Create comment object and add it to the list of comments.
                            Comment commentObject = new Comment(comment.getBoolean("edited"), comment.getInt("score"), comment.getLong("creation_date"), comment.getString("body"), commentOwner);
                            commentsList.add(commentObject);
                        }
                        answer.setComments(commentsList);
                    }

                    answersList.add(answer);
                }

                answerListAdapter = new AnswerListAdapter(getApplicationContext(), answersList);
                answersListView.setAdapter(answerListAdapter);


            } catch (JSONException e)
            {
                e.printStackTrace();
            }

        }
    }

}
