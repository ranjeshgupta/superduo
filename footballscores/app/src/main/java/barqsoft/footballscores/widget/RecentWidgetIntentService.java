package barqsoft.footballscores.widget;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import barqsoft.footballscores.MainActivity;
import barqsoft.footballscores.R;
import barqsoft.footballscores.Utilies;
import barqsoft.footballscores.data.DatabaseContract;

/**
 * Created by Nish on 25-12-2015.
 */
public class RecentWidgetIntentService extends IntentService {
    public static final String LOG_TAG = "RecentWidget";
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

    public RecentWidgetIntentService() {
        super("RecentWidgetIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.v(LOG_TAG, "widget handler start");
        // Retrieve all of the Today widget ids: these are the widgets we need to update
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this,
                RecentWidgetProvider.class));

        // Get today's data from the ContentProvider
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        Uri scoreMostRecentUri = DatabaseContract.scores_table.buildScoreRecent();
        Cursor cursor = getContentResolver().query(
                scoreMostRecentUri,
                null,
                null,
                new String[] { simpleDateFormat.format(date) },
                DatabaseContract.scores_table.DATE_COL + " DESC, " + DatabaseContract.scores_table.TIME_COL + " DESC LIMIT 1");
                //DatabaseContract.scores_table.DATE_COL + " ASC, " + DatabaseContract.scores_table.TIME_COL + " ASC LIMIT 1");

        if (cursor == null) {
            Log.w(LOG_TAG, "cursor is null");
            return;
        } else if (!cursor.moveToFirst()) {
            Log.w(LOG_TAG, "unable to cursor to move first");
            cursor.close();
            return;
        }

        Log.v(LOG_TAG, "cursor created");

        String homeName = cursor.getString(COL_HOME);
        String awayName = cursor.getString(COL_AWAY);
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

        for (int appWidgetId : appWidgetIds) {
            Log.v(LOG_TAG, "widget app " + appWidgetId + " started");

            RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget_recent);

            views.setViewVisibility(R.id.widget_empty, View.GONE);

            Utilies.setImageViewBitmapFromUrl(this, views, R.id.widget_home_crest, homeCrest);
            views.setTextViewText(R.id.widget_home_name, homeName);

            views.setTextViewText(R.id.widget_score_textview, scores);
            views.setTextViewText(R.id.widget_data_textview, matchDateTime);

            Utilies.setImageViewBitmapFromUrl(this, views, R.id.widget_away_crest, awayCrest);
            views.setTextViewText(R.id.widget_away_name, awayName);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                setRemoteContentDescription(views, R.id.widget_home_crest, homeName);
                setRemoteContentDescription(views, R.id.widget_away_crest, awayName);
            }

            // Create an Intent to launch MainActivity
            Intent launchIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, launchIntent, 0);
            views.setOnClickPendingIntent(R.id.widget, pendingIntent);

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
            Log.v(LOG_TAG, "widget app " + appWidgetId + " created");
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    private void setRemoteContentDescription(RemoteViews views, int viewId, String description) {
        views.setContentDescription(viewId, description);
    }
}
