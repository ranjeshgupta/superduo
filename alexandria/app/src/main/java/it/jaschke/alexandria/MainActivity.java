package it.jaschke.alexandria;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import it.jaschke.alexandria.api.Callback;


public class MainActivity extends AppCompatActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks, Callback, BookDetail.Callbacks {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment navigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private String mTitle;
    private static final String mTitleStateKey = "mTitle";

    private String mEan;
    private static final String mEanStateKey = "mEan";

    private String mBookTitle;
    private static final String mBookTitleStateKey = "mBookTitle";

    public static boolean IS_TABLET = false;
    private BroadcastReceiver messageReciever;

    public static final String MESSAGE_EVENT = "MESSAGE_EVENT";
    public static final String MESSAGE_KEY = "MESSAGE_EXTRA";

    public static final int LISTOFBOOKS_FRAGMENT_POS = 0;
    public static final int ADDBOOK_FRAGMENT_POS = 1;
    public static final int ABOUT_FRAGMENT_POS = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IS_TABLET = isTablet();
        if(IS_TABLET){
            setContentView(R.layout.activity_main_tablet);
        }else {
            setContentView(R.layout.activity_main);
        }
        Utility.setupUI(findViewById(R.id.drawer_layout), this);

        messageReciever = new MessageReciever();
        IntentFilter filter = new IntentFilter(MESSAGE_EVENT);
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReciever,filter);

        navigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);

        mTitle = (String) getTitle();

        // Set up the drawer.
        navigationDrawerFragment.setUp(R.id.navigation_drawer,
                    (DrawerLayout) findViewById(R.id.drawer_layout));

        //hide keyboard
        Utility.hideSoftKeyboard(this);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(mTitleStateKey)) {
                mTitle = savedInstanceState.getString(mTitleStateKey, null);
            }

            if (savedInstanceState.containsKey(mEanStateKey)) {
                mEan = savedInstanceState.getString(mEanStateKey, null);
            }

            if (savedInstanceState.containsKey(mBookTitleStateKey)) {
                mBookTitle = savedInstanceState.getString(mBookTitleStateKey, null);
            }

            // when rotating to landscape booklist on a tablet select the previously selected book
            if (IS_TABLET && findViewById(R.id.right_container) != null && mTitle.equals(getString(R.string.books)) && mEan != null && mBookTitle != null) {
                onItemSelected(mEan, mBookTitle);
                mEan = null;
                mBookTitle = null;
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        outState.putString(mTitleStateKey, mTitle);
        outState.putString(mEanStateKey, mEan);
        outState.putString(mBookTitleStateKey, mBookTitle);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {

        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment nextFragment;

        switch (position){
            default:
            case LISTOFBOOKS_FRAGMENT_POS:
                nextFragment = new ListOfBooks();
                break;
            case ADDBOOK_FRAGMENT_POS:
                nextFragment = new AddBook();
                break;
            case ABOUT_FRAGMENT_POS:
                nextFragment = new About();
                break;

        }

        // if we are on a tablet in landscape mode pop the book detail fragment (in case we have one)
        if(findViewById(R.id.right_container) != null) {
            fragmentManager.popBackStack(getString(R.string.detail), FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }

        fragmentManager.beginTransaction()
                .replace(R.id.container, nextFragment)
                .addToBackStack((String) mTitle)
                .commit();

        Utility.hideSoftKeyboard(this);
    }

    public void setTitle(int titleId) {
        mTitle = getString(titleId);
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if(null != actionBar) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(mTitle);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!navigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        else if (item.getItemId() == android.R.id.home) {
            // hide keyboard when the drawer is opened
            Utility.hideSoftKeyboard(this);

            // if we are not on a tablet in landscape mode
            if(findViewById(R.id.right_container) == null) {
                // if we are coming back from the bookdetail fragment, reset the hamburger
                if (mTitle.equals(getString(R.string.detail))) {
                    getSupportFragmentManager().popBackStack();
                    toggleToolbarDrawerIndicator(false);
                    return true;
                }
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReciever);
        super.onDestroy();
    }


    @Override
    public void onItemSelected(String ean, String title) {
        mEan = ean;
        mBookTitle = title;

        Bundle args = new Bundle();
        args.putString(BookDetail.EAN_KEY, ean);
        args.putString(BookDetail.TITLE_KEY, title);

        BookDetail fragment = new BookDetail();
        fragment.setArguments(args);

        int id = R.id.container;
        if(findViewById(R.id.right_container) != null){
            id = R.id.right_container;
        }
        getSupportFragmentManager().beginTransaction()
                .replace(id, fragment)
                .addToBackStack(getString(R.string.detail))
                .commit();

        // toggle the hamburger icon for the back icon
        toggleToolbarDrawerIndicator(true);

        // hide the keyboard when we navigate to the bookdetail fragment
        Utility.hideSoftKeyboard(this);
    }


    public void toggleToolbarDrawerIndicator(boolean backToHome) {
        // if we are not on a tablet in landscape mode
        if(findViewById(R.id.right_container) == null) {
            navigationDrawerFragment.toggleToolbarDrawerIndicator(backToHome);
        }
    }

    private class MessageReciever extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getStringExtra(MESSAGE_KEY)!=null){
                Toast.makeText(MainActivity.this, intent.getStringExtra(MESSAGE_KEY), Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean isTablet() {
        return (getApplicationContext().getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    @Override
    public void onBackPressed() {

        // only close the drawer if it's opened
        if (navigationDrawerFragment.isDrawerOpen()) {
            navigationDrawerFragment.closeDrawer();
            return;
        }

        // if we are coming back from the book detail fragment, reset the hamburger icon
        if (mTitle.equals(getString(R.string.detail))) {
            toggleToolbarDrawerIndicator(false);
        }

        if(getSupportFragmentManager().getBackStackEntryCount()<2){
            finish();
        }
        super.onBackPressed();
    }

}