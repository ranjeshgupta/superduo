package barqsoft.footballscores.widget;

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import barqsoft.footballscores.MainActivity;
import barqsoft.footballscores.R;
import barqsoft.footballscores.Utilies;
import barqsoft.footballscores.data.DatabaseContract;

/**
 * Created by Nish on 25-12-2015.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class DetailWidgetRemoteViewsService extends RemoteViewsService {
    public static final String LOG_TAG = "DetailWidget";
    public double detail_match_id = 0;

    public static final int COL_DATE = 1;
    public static final int COL_MATCHTIME = 2;
    public static final int COL_HOME = 3;
    public static final int COL_HOME_ID = 4;
    public static final int COL_HOME_LOGO = 5;
    public static final int COL_AWAY = 6;
    public static final int COL_AWAY_ID = 7;
    public static final int COL_AWAY_LOGO = 8;
    public static final int COL_LEAGUE = 9;
    public static final int COL_HOME_GOALS = 10;
    public static final int COL_AWAY_GOALS = 11;
    public static final int COL_ID = 12;
    public static final int COL_MATCHDAY = 13;

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            private Cursor cursor = null;
            private String today = null;
            private String yesterday = null;
            private String dayBeforeYesterday = null;

            @Override
            public void onCreate() {
                // Nothing to do
            }

            @Override
            public void onDataSetChanged() {
                if (cursor != null) {
                    cursor.close();
                }
                // This method is called by the app hosting the widget (e.g., the launcher)
                // However, our ContentProvider is not exported so it doesn't have access to the
                // data. Therefore we need to clear (and finally restore) the calling identity so
                // that calls use our process and permission
                final long identityToken = Binder.clearCallingIdentity();

                // Get today's data from the ContentProvider
                //Date date = new Date(System.currentTimeMillis());
                Calendar cal = Calendar.getInstance();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                today = simpleDateFormat.format(cal.getTime());
                cal.add(Calendar.DATE, -1);
                yesterday = simpleDateFormat.format(cal.getTime());

                cal.add(Calendar.DATE, -1);
                dayBeforeYesterday = simpleDateFormat.format(cal.getTime());

                Log.v(LOG_TAG, today + " - " + yesterday + " - " + dayBeforeYesterday);

                Uri scoreMostRecentUri = DatabaseContract.scores_table.buildScoreRecent();
                cursor = getContentResolver().query(
                        scoreMostRecentUri,
                        null,
                        null,
                        new String[]{today},
                        DatabaseContract.scores_table.DATE_COL + " DESC, " + DatabaseContract.scores_table.TIME_COL + " DESC");
                        //DatabaseContract.scores_table.DATE_COL + " ASC, " + DatabaseContract.scores_table.TIME_COL + " ASC");

                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if (cursor != null) {
                    cursor.close();
                    cursor = null;
                }
            }

            @Override
            public int getCount() {
                return cursor == null ? 0 : cursor.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION ||
                        cursor == null || !cursor.moveToPosition(position)) {
                    return null;
                }
                RemoteViews views = new RemoteViews(getPackageName(),
                        R.layout.widget_detail_list_item);

                Log.v(LOG_TAG, "cursor created");

                String homeName = cursor.getString(COL_HOME);
                String awayName = cursor.getString(COL_AWAY);
                String matchDate = cursor.getString(COL_DATE);
                String matchDateTime = cursor.getString(COL_MATCHTIME);
                String scores = Utilies.getScores(cursor.getInt(COL_HOME_GOALS), cursor.getInt(COL_AWAY_GOALS));
                String homeCrest = cursor.getString(COL_HOME_LOGO);
                String awayCrest = cursor.getString(COL_AWAY_LOGO);

                /*
                Log.w(LOG_TAG, "homeName : " + R.id.widget_home_name + " : " + homeName);
                Log.w(LOG_TAG, "awayName : " + R.id.widget_away_name + " : " + awayName);
                Log.w(LOG_TAG, "matchDateTime : " + R.id.widget_data_textview + " : " + matchDateTime);
                Log.w(LOG_TAG, "scores : " + R.id.widget_score_textview + " : " + scores);
                Log.w(LOG_TAG, "homeCrest : " + R.id.widget_home_crest + " : " + homeCrest);
                Log.w(LOG_TAG, "awayCrest : " + R.id.widget_away_crest + " : " + awayCrest);
                */

                Utilies.setImageViewBitmapFromUrl(DetailWidgetRemoteViewsService.this, views, R.id.widget_home_crest, homeCrest);
                views.setTextViewText(R.id.widget_home_name, homeName);

                views.setTextViewText(R.id.widget_score_textview, scores);
                views.setTextViewText(R.id.widget_data_textview, matchDateTime);

                Utilies.setImageViewBitmapFromUrl(DetailWidgetRemoteViewsService.this, views, R.id.widget_away_crest, awayCrest);
                views.setTextViewText(R.id.widget_away_name, awayName);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    setRemoteContentDescription(views, R.id.widget_home_crest, homeName);
                    setRemoteContentDescription(views, R.id.widget_away_crest, awayName);
                }

                final Intent fillInIntent = new Intent();
                fillInIntent.putExtra(MainActivity.EXTRA_SELECTED_MATCH_ID, cursor.getString(COL_ID));
                if(matchDate.equals(today)){
                    fillInIntent.putExtra(MainActivity.EXTRA_CURRENT_FRAGMENT, "2");
                }
                else if(matchDate.equals(yesterday)){
                    fillInIntent.putExtra(MainActivity.EXTRA_CURRENT_FRAGMENT, "1");
                }
                else if(matchDate.equals(dayBeforeYesterday)){
                    fillInIntent.putExtra(MainActivity.EXTRA_CURRENT_FRAGMENT, "0");
                }
                else {
                    fillInIntent.putExtra(MainActivity.EXTRA_CURRENT_FRAGMENT, "2");
                }
                views.setOnClickFillInIntent(R.id.widget_list_item, fillInIntent);

                return views;
            }

            @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
            private void setRemoteContentDescription(RemoteViews views, int viewId, String description) {
                views.setContentDescription(viewId, description);
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.widget_detail_list_item);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                if (cursor.moveToPosition(position))
                    return cursor.getLong(COL_ID);
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
