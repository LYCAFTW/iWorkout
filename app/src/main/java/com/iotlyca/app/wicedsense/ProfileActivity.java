package com.iotlyca.app.wicedsense;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.iotlyca.util.Exercise;
import com.iotlyca.util.ExerciseRecyclerAdapter;
import com.iotlyca.util.Friend;
import com.iotlyca.util.FriendAdaptor;
import com.iotlyca.util.HistoryDataRecyclerAdaptor;
import com.iotlyca.util.HttpParse;
import com.shinelw.library.ColorArcProgressBar;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {
    private Button button;
    private ColorArcProgressBar bar;

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private RecyclerView eRecyclerView;
    private RecyclerView.Adapter eAdapter;
    private RecyclerView.LayoutManager eLayoutManager;

    private RecyclerView rRecyclerView;
    private RecyclerView.Adapter rAdapter;
    private RecyclerView.LayoutManager rLayoutManager;

    private String email;
    private String password;
    private String recommendation = "";

    final String[] values = new String[]{"chest press", "seated row", "leg press", "abdominal", "bicep curl", "counter balance smith", "tricep press", "leg extension", "hyperextension"};
    final int[] picSource = new int[]{R.drawable.chest_press, R.drawable.seated_row, R.drawable.leg_press, R.drawable.abdominal,
            R.drawable.bicep_curl, R.drawable.counter_balance_smith, R.drawable.tricep_press, R.drawable.leg_extension, R.drawable.hyperextension};


    private Map<String, Map<String,String>> summaryDic = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        email = sharedPreferences.getString("email", "");
        password = sharedPreferences.getString("password", "");

        HashMap<String,String> data = new HashMap<>();
        data.put("email",email);
        data.put("password",password);
        new getInfo().execute(data);
        summaryDic = HttpParse.getSummaryDic();

        Log.d("test", Integer.toString(summaryDic.size()));

        setContentView(R.layout.profile_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ProfileActivity.this, ExerciseSelectActivity.class);
                startActivity(intent);
            }
        });

        CardView exerciseView = (CardView) findViewById(R.id.exercise_cardView);
        exerciseView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ProfileActivity.this, ExerciseSelectActivity.class);
                // this is the test of recommendation ExerciseSelectActivity list
                recommendation = summaryDic.containsKey("11") ? summaryDic.get("11").get("lalala") : "";
                intent.putExtra("start", recommendation);
                startActivity(intent);
            }
        });

        initProfile();
        // friend recyle view
        initData();
        initView();
    }

    private void initProfile() {
        TextView minutes = (TextView) findViewById(R.id.minutes);
        TextView times = (TextView) findViewById(R.id.times);
        TextView days = (TextView) findViewById(R.id.days);
        TextView calories = (TextView) findViewById(R.id.caloris);
        if (summaryDic.containsKey("0")) {
            String data = summaryDic.get("0").get("number");
            if (data == null) return;
            String[] dataArray = data.split(",");
            minutes.setText(dataArray[0]);
            times.setText(dataArray[1]);
            days.setText(dataArray[2]);
            calories.setText(dataArray[3]);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.profile_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {
            startActivity(new Intent(ProfileActivity.this, ProfileActivity.class));
            this.finish();
            return true;
        }

        if (id == R.id.action_login) {
//            Intent intent = new Intent(ProfileActivity.this, RecentDataActivity.class);
//            intent.putExtra("history", summaryDic.get("0").get("posts"));
            startActivity(new Intent(ProfileActivity.this,RegisterActivity.class));
            this.finish();
            return true;
        }

        if (id == R.id.action_logout) {
            startActivity(new Intent(ProfileActivity.this,RegisterActivity.class));
            this.finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initData() {
        mLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        mAdapter = new FriendAdaptor(getData());
        eLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        eAdapter = new ExerciseRecyclerAdapter(getExerciseData());
        rLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        rAdapter = new HistoryDataRecyclerAdaptor(getHistoryData());
    }

    private void initView() {
        mRecyclerView = (RecyclerView) findViewById(R.id.friend_recyler_view);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        eRecyclerView = (RecyclerView) findViewById(R.id.exercise_recycler_view);
        eRecyclerView.setLayoutManager(eLayoutManager);
        eRecyclerView.setAdapter(eAdapter);

        rRecyclerView = (RecyclerView) findViewById(R.id.history_recyler_view);
        rRecyclerView.setLayoutManager(rLayoutManager);
        rRecyclerView.setAdapter(rAdapter);
    }

    private ArrayList<Friend> getData() {
        ArrayList<Friend> data = new ArrayList<>();

        for(int i = 1; i <= 10; i++) {
            String key = Integer.toString(i);
            if (!summaryDic.containsKey(key)) continue;
            Map<String, String> map = summaryDic.get(key);
            Log.d("key", key);
            data.add(new Friend(map.get("pic"), map.get("username"), map.get("summary")));
        }
        return data;
    }

    private ArrayList<Exercise> getExerciseData() {


        String[] values = new String[]{"chest press", "seated row", "leg press", "abdominal", "bicep curl", "counter balance smith", "tricep press", "leg extension","hyperextension"};
        int[] picSource = new int[]{R.drawable.chest_press, R.drawable.seated_row, R.drawable.leg_press, R.drawable.abdominal,
                R.drawable.bicep_curl, R.drawable.counter_balance_smith, R.drawable.tricep_press, R.drawable.leg_extension, R.drawable.hyperextension};

        ArrayList<Exercise> data = new ArrayList<>();
        if (!summaryDic.containsKey("11")) return data;
        String recommendation = summaryDic.get("11").get("lalala");
        char [] charArray = recommendation.toCharArray();
        for(int i = 0; i < 3; i++) {
            int index = charArray[i] - '0';
            data.add(new Exercise(values[index], picSource[index]));
        }
        return data;
    }

    private ArrayList<String> getHistoryData() {
        ArrayList<String> data = new ArrayList<>();

        String key = Integer.toString(0);
        if (!summaryDic.containsKey(key)) return data;
        String historyData = summaryDic.get(key).get("posts");

        for (String item : historyData.split(",")) data.add(item);
        return data;
    }

    private class getInfo extends AsyncTask<HashMap<String,String>, Void, String> {

        //  The API_Endpoint is hold on an old AWS EC2 and no longer valid, please re-deploy the end point
        String API_Endpoint = Settings.API_Endpoint;
        public getInfo() {
            super();
        }

        @Override
        protected String doInBackground(HashMap<String,String>... params) {
            String status = "";

            HttpURLConnection urlConnection = null;
            try {
                JSONObject payload = new JSONObject();
                for(Map.Entry<String,String> entry: params[0].entrySet())
                    payload.put(entry.getKey(),entry.getValue());
                System.out.println(payload.toString());
                URL url= new URL(API_Endpoint+"/users/");
                System.out.println(url);
                urlConnection = (HttpURLConnection) url.openConnection();

                urlConnection.setDoInput(true);

                urlConnection.setDoOutput(true);

                urlConnection.setRequestProperty("Content-Type", "application/json");

                urlConnection.setRequestMethod("POST");

                String authStr = params[0].get("email")+":"+params[0].get("password");
                System.out.println(authStr);

                // encode data using BASE64
                String authEncoded = Base64.encodeToString(authStr.getBytes(), Base64.DEFAULT);

                urlConnection.setRequestProperty("Authorization", "Basic "+authEncoded);

                OutputStreamWriter wr = new OutputStreamWriter(urlConnection.getOutputStream());

                wr.write(payload.toString());

                wr.flush();

                //display what returns the POST request

                StringBuilder sb = new StringBuilder();
                int HttpResult = urlConnection.getResponseCode();
                if (HttpResult == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(urlConnection.getInputStream(), "utf-8"));
                    String line = null;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    br.close();
                    System.out.println(sb.toString());

                } else {
                    System.out.println(urlConnection.getResponseMessage());
                }

                status = sb.toString();

            } catch (Exception e) {
                e.printStackTrace();
                Log.e("HTTPPost", e.toString());
            } finally {
                //Disconnect. Once the response body has been read, the HttpURLConnection should be closed by calling disconnect(). Disconnecting releases the resources held by a connection so they may be closed or reused.
                if (urlConnection != null)
                    urlConnection.disconnect();
            }
            return status;
        }

        private Map<String, Map<String,String>> summaryDic = new HashMap<>();
        @Override
        protected void onPostExecute(String result) {
            Log.d("HTTPPost", "Payload:" + result);
            HttpParse.updateSummary(result);
        }
    }

}
