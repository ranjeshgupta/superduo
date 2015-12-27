package barqsoft.footballscores.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import java.util.TimeZone;
import java.util.Vector;

import barqsoft.footballscores.Utilies;
import barqsoft.footballscores.data.DatabaseContract;
import barqsoft.footballscores.R;

/**
 * Created by yehya khaled on 3/2/2015.
 */
public class MyFetchService extends IntentService
{
    public static final String LOG_TAG = "MyFetchService";
    public static int[] LEAGUE_SELECTED_CODES;
    private Vector<ContentValues> mCVTeam;

    public MyFetchService()
    {
        super("MyFetchService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        LEAGUE_SELECTED_CODES = getResources().getIntArray(R.array.leagues_selected);
        mCVTeam = new Vector<ContentValues>();

        if (!Utilies.isNetworkAvailable(getApplicationContext())) {
            createTeamCrest();

            getData("p2");
            getData("n3");
        }
    }

    private void getData (String timeFrame)
    {
        //Creating fetch URL
        final String BASE_URL = getString(R.string.api_uri_fixture); //Base URL
        final String QUERY_TIME_FRAME = "timeFrame"; //Time Frame parameter to determine days

        Uri fetch_build = Uri.parse(BASE_URL).buildUpon().
                appendQueryParameter(QUERY_TIME_FRAME, timeFrame).build();
        //Log.v(LOG_TAG, "The url we are looking at is: "+fetch_build.toString()); //log spam
        String JSON_data = getJsonDataFromUri(fetch_build);
        try {
            if (JSON_data != null) {
                //This bit is to check if the data contains any matches. If not, we call processJson on the dummy data
                JSONArray matches = new JSONObject(JSON_data).getJSONArray("fixtures");
                if (matches.length() == 0) {
                    //if there is no data, call the function on dummy data
                    //this is expected behavior during the off season.
                    processJSONdata(getString(R.string.dummy_data), getApplicationContext(), false);
                    return;
                }


                processJSONdata(JSON_data, getApplicationContext(), true);
            } else {
                //Could not Connect
                Log.d(LOG_TAG, "Could not connect to server.");
            }
        }
        catch(Exception e)
        {
            Log.e(LOG_TAG,e.getMessage());
        }
    }

    private String getJsonDataFromUri(Uri apiUri) {
        HttpURLConnection m_connection = null;
        BufferedReader reader = null;
        String JSON_data = null;
        //Opening Connection
        if( null != apiUri ) {
            try {
                URL fetch = new URL(apiUri.toString());
                m_connection = (HttpURLConnection) fetch.openConnection();
                m_connection.setRequestMethod("GET");
                m_connection.addRequestProperty("X-Auth-Token", getString(R.string.api_key));
                m_connection.connect();

                // Read the input stream into a String
                InputStream inputStream = m_connection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }
                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                JSON_data = buffer.toString();
            } catch (Exception e) {
                Log.e(LOG_TAG, "Exception here" + e.getMessage());
            } finally {
                if (m_connection != null) {
                    m_connection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "Error Closing Stream");
                    }
                }
            }
        }
        else{
            return null;
        }
        return JSON_data;
    }


    private void processJSONdata (String JSONdata,Context mContext, boolean isReal)
    {
        //JSON data
        // This set of league codes is for the 2015/2016 season. In fall of 2016, they will need to
        // be updated. Feel free to use the codes

        final String SEASON_LINK = getString(R.string.uri_season_link);
        final String MATCH_LINK = getString(R.string.uri_match_link);
        final String TEAM_LINK = getString(R.string.uri_team_link);
        final String FIXTURES = "fixtures";
        final String LINKS = "_links";
        final String SOCCER_SEASON = "soccerseason";
        final String SELF = "self";
        final String MATCH_DATE = "date";
        final String HOME_TEAM = "homeTeamName";
        final String AWAY_TEAM = "awayTeamName";
        final String RESULT = "result";
        final String HOME_GOALS = "goalsHomeTeam";
        final String AWAY_GOALS = "goalsAwayTeam";
        final String MATCH_DAY = "matchday";
        final String HOME_TEAM_ID = "homeTeam";
        final String AWAY_TEAM_ID = "awayTeam";

        //Match data
        String League = null;
        String mDate = null;
        String mTime = null;
        String Home = null;
        Integer Home_id = null;
        String homeTeamString = null;
        String Home_logo = null;
        String Away = null;
        Integer Away_id = null;
        String awayTeamString = null;
        String Away_logo = null;
        String Home_goals = null;
        String Away_goals = null;
        String match_id = null;
        String match_day = null;


        try {
            JSONArray matches = new JSONObject(JSONdata).getJSONArray(FIXTURES);


            //ContentValues to be inserted
            Vector<ContentValues> values = new Vector <ContentValues> (matches.length());
            for(int i = 0;i < matches.length();i++)
            {

                JSONObject match_data = matches.getJSONObject(i);
                League = match_data.getJSONObject(LINKS).getJSONObject(SOCCER_SEASON).
                        getString("href");
                League = League.replace(SEASON_LINK,"");
                //This if statement controls which leagues we're interested in the data from.
                //add leagues here in order to have them be added to the DB.
                // If you are finding no data in the app, check that this contains all the leagues.
                // If it doesn't, that can cause an empty DB, bypassing the dummy data routine.

                //if(Arrays.asList(LEAGUE_SELECTED_CODES).contains(Integer.parseInt(League))) {
                if(Utilies.contains(LEAGUE_SELECTED_CODES, Integer.parseInt(League))){
                    match_id = match_data.getJSONObject(LINKS).getJSONObject(SELF).
                            getString("href");
                    match_id = match_id.replace(MATCH_LINK, "");
                    if(!isReal){
                        //This if statement changes the match ID of the dummy data so that it all goes into the database
                        match_id=match_id+Integer.toString(i);
                    }

                    homeTeamString = match_data.getJSONObject(LINKS).getJSONObject(HOME_TEAM_ID).
                            getString("href");
                    homeTeamString = homeTeamString.replace(TEAM_LINK, "");
                    Home_id = Integer.parseInt(homeTeamString);

                    awayTeamString = match_data.getJSONObject(LINKS).getJSONObject(AWAY_TEAM_ID).
                            getString("href");
                    awayTeamString = awayTeamString.replace(TEAM_LINK, "");
                    Away_id = Integer.parseInt(awayTeamString);

                    mDate = match_data.getString(MATCH_DATE);
                    mTime = mDate.substring(mDate.indexOf("T") + 1, mDate.indexOf("Z"));
                    mDate = mDate.substring(0,mDate.indexOf("T"));
                    SimpleDateFormat match_date = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss");
                    match_date.setTimeZone(TimeZone.getTimeZone("UTC"));
                    try {
                        Date parseddate = match_date.parse(mDate+mTime);
                        SimpleDateFormat new_date = new SimpleDateFormat("yyyy-MM-dd:HH:mm");
                        new_date.setTimeZone(TimeZone.getDefault());
                        mDate = new_date.format(parseddate);
                        mTime = mDate.substring(mDate.indexOf(":") + 1);
                        mDate = mDate.substring(0,mDate.indexOf(":"));

                        if(!isReal){
                            //This if statement changes the dummy data's date to match our current date range.
                            Date fragmentdate = new Date(System.currentTimeMillis()+((i-2)*86400000));
                            SimpleDateFormat mformat = new SimpleDateFormat("yyyy-MM-dd");
                            mDate=mformat.format(fragmentdate);
                        }
                    }
                    catch (Exception e)
                    {
                        Log.d(LOG_TAG, "error here!");
                        Log.e(LOG_TAG,e.getMessage());
                    }
                    Home = match_data.getString(HOME_TEAM);
                    Away = match_data.getString(AWAY_TEAM);
                    Home_goals = match_data.getJSONObject(RESULT).getString(HOME_GOALS);
                    Away_goals = match_data.getJSONObject(RESULT).getString(AWAY_GOALS);
                    match_day = match_data.getString(MATCH_DAY);
                    Home_logo = getTeamLogoByTeamId(Home_id);
                    Away_logo = getTeamLogoByTeamId(Away_id);
                    ContentValues match_values = new ContentValues();
                    match_values.put(DatabaseContract.scores_table.MATCH_ID,match_id);
                    match_values.put(DatabaseContract.scores_table.DATE_COL,mDate);
                    match_values.put(DatabaseContract.scores_table.TIME_COL,mTime);
                    match_values.put(DatabaseContract.scores_table.HOME_COL,Home);
                    match_values.put(DatabaseContract.scores_table.HOME_ID_COL, Home_id);
                    match_values.put(DatabaseContract.scores_table.HOME_LOGO_COL, Home_logo);
                    match_values.put(DatabaseContract.scores_table.AWAY_COL,Away);
                    match_values.put(DatabaseContract.scores_table.AWAY_ID_COL, Away_id);
                    match_values.put(DatabaseContract.scores_table.AWAY_LOGO_COL, Away_logo);
                    match_values.put(DatabaseContract.scores_table.HOME_GOALS_COL,Home_goals);
                    match_values.put(DatabaseContract.scores_table.AWAY_GOALS_COL,Away_goals);
                    match_values.put(DatabaseContract.scores_table.LEAGUE_COL,League);
                    match_values.put(DatabaseContract.scores_table.MATCH_DAY,match_day);

                    values.add(match_values);
                }
            }
            int inserted_data = 0;
            ContentValues[] insert_data = new ContentValues[values.size()];
            values.toArray(insert_data);
            inserted_data = mContext.getContentResolver().bulkInsert(
                    DatabaseContract.BASE_CONTENT_URI,insert_data);

            //Log.v(LOG_TAG,"Succesfully Inserted : " + String.valueOf(inserted_data));
        }
        catch (JSONException e)
        {
            Log.e(LOG_TAG,e.getMessage());
        }

    }

    void createTeamCrest(){
        for(final int teamCode : LEAGUE_SELECTED_CODES){
            try {

                Uri uriTeam = Uri.parse(getString(R.string.uri_season_link))
                        .buildUpon()
                        .appendPath(Integer.toString(teamCode))
                        .appendPath(getString(R.string.uri_team_path))
                        .build();

                // query the api and get the teams
                String JSON_team = getJsonDataFromUri(uriTeam);

                // process the returned json data
                if (JSON_team != null) {
                    createCVTeam(JSON_team);
                } else {
                    Log.d(LOG_TAG, "Failed to load json for code "+ teamCode);
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "Exception here in loadTeams: " + e.getMessage());
            }
        }
    }

    void createCVTeam(String JSON_team){
        final String TEAMS = getString(R.string.uri_team_path);
        final String TEAM_LINK = getString(R.string.uri_team_link);
        final String LINKS = "_links";
        final String SELF = "self";
        final String CREST_URL = "crestUrl";
        String teamId = null;

        try {
            JSONArray teams = new JSONObject(JSON_team).getJSONArray(TEAMS);

            if (teams.length() > 0) {
                for(int i = 0; i < teams.length(); i++) {

                    JSONObject team = teams.getJSONObject(i);

                    teamId = team.getJSONObject(LINKS).getJSONObject(SELF).getString("href");
                    teamId = teamId.replace(TEAM_LINK, "");

                    String teamLogoUrl = team.getString(CREST_URL);
                    //Log.e(LOG_TAG, teamLogoUrl);
                    if( null != teamLogoUrl && teamLogoUrl.endsWith(".svg")) {
                        String fileName = teamLogoUrl.substring(teamLogoUrl.lastIndexOf("/") + 1);
                        String[] arrParts = teamLogoUrl.split("/wikipedia/");
                        if(arrParts.length == 2) {
                            String leftPart = arrParts[0];
                            String rightPart = arrParts[1];

                            String[] arrPath = rightPart.split("/");
                            if(arrPath.length > 0) {
                                rightPart = arrPath[0] + "/thumb";
                                for (int j = 1; j < arrPath.length; j++) {
                                    rightPart = rightPart + "/" + arrPath[j];
                                }
                                rightPart = rightPart + "/144px-" + fileName + ".png";
                                teamLogoUrl = leftPart + "/wikipedia/" + rightPart;
                            }
                        }
                    }
                    //Log.w(LOG_TAG, teamId + " : " + teamLogoUrl);

                    ContentValues cvTeam = new ContentValues();
                    cvTeam.put(DatabaseContract.scores_table.HOME_ID_COL, Integer.parseInt(teamId));
                    cvTeam.put(DatabaseContract.scores_table.HOME_LOGO_COL, teamLogoUrl);

                    mCVTeam.add(cvTeam);
                }
            } else {
                Log.e(LOG_TAG, "Team JSON is null");
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    private String getTeamLogoByTeamId(int teamId) {

        for (ContentValues team : mCVTeam) {
            if (team.getAsInteger(DatabaseContract.scores_table.HOME_ID_COL).equals(teamId)) {
                return team.getAsString(DatabaseContract.scores_table.HOME_LOGO_COL);
            }
        }

        return null;
    }


}
