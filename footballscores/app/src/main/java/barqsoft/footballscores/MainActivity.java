package barqsoft.footballscores;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import barqsoft.footballscores.sync.FootballSyncAdapter;

public class MainActivity extends AppCompatActivity {
    public static int selected_match_id;
    public static int current_fragment = 2;
    public static String LOG_TAG = "MainActivity";
    private final String save_tag = "Save Test";
    private PagerFragment my_main;

    public static final String EXTRA_SELECTED_MATCH_ID = "match";
    public static final String EXTRA_CURRENT_FRAGMENT = "current_fagment";

    public static final String mPagerCurrentStateKey = "Pager_Current";
    public static final String mSelectedMatchStateKey = "Selected_match";
    public static final String mMyMainStateKey = "my_main";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(LOG_TAG, "Reached MainActivity onCreate");

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(null != getSupportActionBar()) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        if (getIntent().hasExtra(EXTRA_SELECTED_MATCH_ID)) {
            selected_match_id = Integer.parseInt(getIntent().getStringExtra(EXTRA_SELECTED_MATCH_ID), 10);
        }

        if (getIntent().hasExtra(EXTRA_CURRENT_FRAGMENT)) {
            current_fragment = Integer.parseInt(getIntent().getStringExtra(EXTRA_CURRENT_FRAGMENT), 10);
        }


        if (savedInstanceState == null) {
            my_main = new PagerFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, my_main)
                    .commit();
        }
        else{
            if (savedInstanceState.containsKey(mSelectedMatchStateKey)) {
                selected_match_id = savedInstanceState.getInt(mSelectedMatchStateKey);
            }
        }
        FootballSyncAdapter.initializeSyncAdapter(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_about) {
            Intent start_about = new Intent(this,AboutActivity.class);
            startActivity(start_about);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.v(save_tag,"will save");
        Log.v(save_tag,"fragment: "+String.valueOf(my_main.mPagerHandler.getCurrentItem()));
        Log.v(save_tag,"selected id: "+selected_match_id);

        outState.putInt(mPagerCurrentStateKey, my_main.mPagerHandler.getCurrentItem());
        outState.putInt(mSelectedMatchStateKey, selected_match_id);
        getSupportFragmentManager().putFragment(outState, mMyMainStateKey, my_main);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.v(save_tag,"will retrive");
        Log.v(save_tag,"fragment: "+String.valueOf(savedInstanceState.getInt(mPagerCurrentStateKey)));
        Log.v(save_tag,"selected id: "+savedInstanceState.getInt(mSelectedMatchStateKey));

        current_fragment = savedInstanceState.getInt(mPagerCurrentStateKey);
        selected_match_id = savedInstanceState.getInt(mSelectedMatchStateKey);
        my_main = (PagerFragment) getSupportFragmentManager().getFragment(savedInstanceState, mMyMainStateKey);
        super.onRestoreInstanceState(savedInstanceState);
    }
}
