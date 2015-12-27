package barqsoft.footballscores;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import barqsoft.footballscores.data.DatabaseContract;
import barqsoft.footballscores.sync.FootballSyncAdapter;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        SharedPreferences.OnSharedPreferenceChangeListener {
    public ScoresAdapter mAdapter;
    public static final int SCORES_LOADER = 0;
    private String[] fragmentdate = new String[1];
    private int last_selected_item = -1;
    private int mPosition = ListView.INVALID_POSITION;
    private static final String mPositionStateKey = "position";
    private ListView mScoreList;

    public MainFragment() {
    }

    private void update_scores() {
        FootballSyncAdapter.syncImmediately(getActivity());
    }

    public void setFragmentDate(String date) {
        fragmentdate[0] = date;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             final Bundle savedInstanceState) {
        update_scores();

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        if(savedInstanceState!=null){
            if (savedInstanceState.containsKey(mPositionStateKey)) {
                mPosition = savedInstanceState.getInt(mPositionStateKey);
            }
        }

        mScoreList = (ListView) rootView.findViewById(R.id.scores_list);

        View emptyView = rootView.findViewById(R.id.listview_scores_empty);
        mScoreList.setEmptyView(emptyView);

        mAdapter = new ScoresAdapter(getActivity(),null,0);
        mScoreList.setAdapter(mAdapter);
        getLoaderManager().initLoader(SCORES_LOADER, null, this);
        mAdapter.detail_match_id = MainActivity.selected_match_id;
        mScoreList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mPosition = position;
                ViewHolder selected = (ViewHolder) view.getTag();
                mAdapter.detail_match_id = selected.match_id;
                MainActivity.selected_match_id = (int) selected.match_id;
                mAdapter.notifyDataSetChanged();
            }
        });
        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mPosition != ListView.INVALID_POSITION) {
            outState.putInt(mPositionStateKey, mPosition);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(getActivity(), DatabaseContract.scores_table.buildScoreWithDate(),
                null,null,fragmentdate,null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

        int i = 0;
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            i++;
            cursor.moveToNext();
        }
        //Log.v(FetchScoreTask.LOG_TAG,"Loader query: " + String.valueOf(i));
        mAdapter.swapCursor(cursor);
        //mAdapter.notifyDataSetChanged();

        if (mPosition != ListView.INVALID_POSITION) {
            mScoreList.smoothScrollToPosition(mPosition);
        }

        updateEmptyView();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader)  {
        mAdapter.swapCursor(null);
    }

    @Override
    public void onResume() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sp.registerOnSharedPreferenceChangeListener(this);
        super.onResume();
    }

    @Override
    public void onPause() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sp.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    private void updateEmptyView() {
        if ( mScoreList.getCount() == 0 ) {
            TextView tv = (TextView) getView().findViewById(R.id.listview_scores_empty);
            if ( null != tv ) {
                // if cursor is empty, why? do we have an invalid location
                int message = R.string.empty_scores_list;
                @FootballSyncAdapter.ScoresStatus int location = Utilies.getLocationStatus(getActivity());
                switch (location) {
                    case FootballSyncAdapter.SCORES_STATUS_SERVER_DOWN:
                        message = R.string.empty_scores_list_server_down;
                        break;
                    case FootballSyncAdapter.SCORES_STATUS_SERVER_INVALID:
                        message = R.string.empty_scores_list_server_error;
                        break;
                    default:
                        if (!Utilies.isNetworkAvailable(getActivity()) ) {
                            message = R.string.empty_scores_list_no_network;
                        }
                }
                tv.setText(message);
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if ( key.equals(getString(R.string.pref_scores_status_key)) ) {
            updateEmptyView();
        }
    }

}
