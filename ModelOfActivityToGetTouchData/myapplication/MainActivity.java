package application.myapplication;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends MyCustomActivity
{

    private ListView lv;

    //Intent globalService;
    Button b;
    SearchView sv;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        b = (Button) findViewById(R.id.button);
        b.setOnTouchListener(gestureListener);

        b.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                b.setText("Stop Service");
                Toast.makeText(getApplicationContext(), "Start Service", Toast.LENGTH_SHORT).show();
            }
        });


        lv = (ListView) findViewById(android.R.id.list);
        List<String> your_array_list = new ArrayList<String>();
        for (int i = 0; i < 15; i++)
        {
            your_array_list.add("item" + i);
        }

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                your_array_list );

        lv.setAdapter(arrayAdapter);

        lv.setOnTouchListener(gestureListener);

        sv = (SearchView) findViewById(R.id.searchView);
        sv.setOnTouchListener(gestureListener);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

    @Override
    public void onDestroy()
    {
        //stopService(globalService);

        super.onDestroy();
    }
    @Override

    public void onPause()
    {
        //stopService(globalService);
        super.onPause();
    }
}
