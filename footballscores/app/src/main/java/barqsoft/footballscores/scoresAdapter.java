package barqsoft.footballscores;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.bumptech.glide.Glide;


/**
 * Created by yehya khaled on 2/26/2015.
 */
public class ScoresAdapter extends CursorAdapter {
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

    private String FOOTBALL_SCORES_HASHTAG = "#Football_Scores";

    public ScoresAdapter(Context context, Cursor cursor, int flags) {
        super(context,cursor,flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View mItem = LayoutInflater.from(context).inflate(R.layout.scores_list_item, parent, false);
        ViewHolder mHolder = new ViewHolder(mItem);
        mItem.setTag(mHolder);
        //Log.v(FetchScoreTask.LOG_TAG,"new View inflated");
        return mItem;
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        final ViewHolder mHolder = (ViewHolder) view.getTag();
        mHolder.home_name.setText(cursor.getString(COL_HOME));
        mHolder.home_name.setContentDescription(cursor.getString(COL_HOME));

        mHolder.away_name.setText(cursor.getString(COL_AWAY));
        mHolder.away_name.setContentDescription(cursor.getString(COL_AWAY));

        mHolder.date.setText(cursor.getString(COL_MATCHTIME));
        mHolder.date.setContentDescription(cursor.getString(COL_MATCHTIME));

        mHolder.score.setText(Utilies.getScores(cursor.getInt(COL_HOME_GOALS), cursor.getInt(COL_AWAY_GOALS)));
        mHolder.score.setContentDescription(Utilies.getScores(cursor.getInt(COL_HOME_GOALS), cursor.getInt(COL_AWAY_GOALS)));

        mHolder.match_id = cursor.getDouble(COL_ID);

        //Log.v("Home Logo", cursor.getString(COL_HOME_LOGO));
        if(null != cursor.getString(COL_HOME_LOGO)) {
            Glide.with(context)
                    .load(cursor.getString(COL_HOME_LOGO))
                    .error(R.drawable.no_icon)
                    .crossFade()
                    .into(mHolder.home_crest);
        }
        else{
            Glide.with(context)
                    .load(R.drawable.no_icon)
                    .into(mHolder.home_crest);
        }
        mHolder.home_crest.setContentDescription(cursor.getString(COL_HOME));

        //Log.v("Away Logo", cursor.getString(COL_AWAY_LOGO));
        if(null != cursor.getString(COL_AWAY_LOGO)) {
            Glide.with(context)
                    .load(cursor.getString(COL_AWAY_LOGO))
                    .error(R.drawable.no_icon)
                    .crossFade()
                    .into(mHolder.away_crest);
        }
        else{
            Glide.with(context)
                    .load(R.drawable.no_icon)
                    .into(mHolder.away_crest);
        }
        mHolder.away_crest.setContentDescription(cursor.getString(COL_AWAY));

        //Log.v(FetchScoreTask.LOG_TAG,mHolder.home_name.getText() + " Vs. " + mHolder.away_name.getText() +" id " + String.valueOf(mHolder.match_id));
        //Log.v(FetchScoreTask.LOG_TAG,String.valueOf(detail_match_id));

        LayoutInflater vi = (LayoutInflater) context.getApplicationContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = vi.inflate(R.layout.fragment_detai, null);
        ViewGroup container = (ViewGroup) view.findViewById(R.id.details_fragment_container);

        if(mHolder.match_id == detail_match_id) {
            //Log.v(FetchScoreTask.LOG_TAG,"will insert extraView");

            container.addView(v, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT
                    , ViewGroup.LayoutParams.MATCH_PARENT));

            TextView match_day = (TextView) v.findViewById(R.id.matchday_textview);
            match_day.setText(Utilies.getMatchDay(context, cursor.getInt(COL_MATCHDAY),
                    cursor.getInt(COL_LEAGUE)));
            match_day.setContentDescription(Utilies.getMatchDay(context, cursor.getInt(COL_MATCHDAY),
                    cursor.getInt(COL_LEAGUE)));

            TextView league = (TextView) v.findViewById(R.id.league_textview);
            league.setText(Utilies.getLeague(context, cursor.getInt(COL_LEAGUE)));
            league.setContentDescription(Utilies.getLeague(context, cursor.getInt(COL_LEAGUE)));

            Button share_button = (Button) v.findViewById(R.id.share_button);
            share_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //add Share Action
                    context.startActivity(createShareForecastIntent(mHolder.home_name.getText() + " "
                            + mHolder.score.getText() + " " + mHolder.away_name.getText() + " "));
                }
            });
        }
        else {
            container.removeAllViews();
        }
    }

    public Intent createShareForecastIntent(String ShareText) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, ShareText + FOOTBALL_SCORES_HASHTAG);
        return shareIntent;
    }

}
