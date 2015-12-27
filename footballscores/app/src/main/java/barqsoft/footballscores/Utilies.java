package barqsoft.footballscores;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;

import java.util.concurrent.ExecutionException;

import barqsoft.footballscores.sync.FootballSyncAdapter;

/**
 * Created by yehya khaled on 3/3/2015.
 */
public class Utilies {

    public static String getLeague(Context context, int league_num) {
        int[] league_codes = context.getResources().getIntArray(R.array.league_codes);
        String[] league_labels = context.getResources().getStringArray(R.array.league_labels);

        for(int i=0; i<league_codes.length; i++){
            if(league_num == league_codes[i]){
                return context.getResources().getString(R.string.league_text) + " " + league_labels[i];
            }
        }

        return context.getResources().getString(R.string.league_unknown_text);
    }

    public static String getMatchDay(Context context, int match_day, int league_num) {
        if(league_num == R.integer.league_champions_league_code) {
            if (match_day <= 6) {
                return context.getResources().getString(R.string.group_stage_text);
            }
            else if(match_day == 7 || match_day == 8) {
                return context.getResources().getString(R.string.first_knockout_round_text);
            }
            else if(match_day == 9 || match_day == 10) {
                return context.getResources().getString(R.string.quarter_final_text);
            }
            else if(match_day == 11 || match_day == 12) {
                return context.getResources().getString(R.string.semi_final_text);
            }
            else {
                return context.getResources().getString(R.string.final_text);
            }
        }
        else {
            return context.getResources().getString(R.string.matchday_text) + " " + String.valueOf(match_day);
        }
    }

    public static String getScores(int home_goals,int awaygoals) {
        if(home_goals < 0 || awaygoals < 0) {
            return " - ";
        }
        else {
            return String.valueOf(home_goals) + " - " + String.valueOf(awaygoals);
        }
    }

    public static boolean contains(final int[] array, final int key) {
        for (final int i : array) {
            if (i == key) {
                return true;
            }
        }
        return false;
    }

    public static boolean isNetworkAvailable(Context c) {
        ConnectivityManager cm =
                (ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }

    public static void setImageViewBitmapFromUrl(Context context, RemoteViews views, int viewId, String imageUrl) {
        Bitmap bitmap = null;
        try {
            bitmap = Glide.with(context)
                        .load(imageUrl)
                        .asBitmap()
                        .error(R.drawable.no_icon)
                        .into(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                        .get();
        }
        catch (InterruptedException | ExecutionException e) {
            Log.w("setImageViewBitmapUrl", "unable to retrieve image from : " + imageUrl, e);
        }

        if (null != bitmap) {
            views.setImageViewBitmap(viewId, bitmap);
        }
    }

    /**
     *
     * @param c Context used to get the SharedPreferences
     * @return the location status integer type
     */
    @SuppressWarnings("ResourceType")
    static public @FootballSyncAdapter.ScoresStatus
    int getLocationStatus(Context c){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        return sp.getInt(c.getString(R.string.pref_scores_status_key), FootballSyncAdapter.SCORES_STATUS_UNKNOWN);
    }
}
