package com.iotlyca.app.wicedsense;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity implements
        View.OnClickListener {

    private static final String TAG = "EmailPassword";
    private EditText mEmailField;
    private EditText mPasswordField;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Views

        mEmailField = (EditText) findViewById(R.id.field_email);
        mPasswordField = (EditText) findViewById(R.id.field_password);
        // Buttons
        findViewById(R.id.email_sign_in_button).setOnClickListener(this);
        findViewById(R.id.email_create_account_button).setOnClickListener(this);

    }


    private void createAccount(final String email, String password) {
        Log.d(TAG, "createAccount:" + email);
        if (!validateForm()) {
            return;
        }
        HashMap<String,String> data = new HashMap<>();
//                post
//                data.put("body","hahahahaahahah");
        data.put("email",email);
        data.put("name",email);//= =.......
        data.put("password",password);
        new Login().execute(data);
    }

    private void signIn(final String email, String password) {
        Log.d(TAG, "signIn:" + email);
        if (!validateForm()) {
            return;
        }
        HashMap<String,String> data = new HashMap<>();

        data.put("email",email);
//        data.put("name",email);//= =.......
        data.put("password",password);
        new Login().execute(data);
    }

    private boolean validateForm() {
        boolean valid = true;

        String email = mEmailField.getText().toString();
        if (TextUtils.isEmpty(email)) {
            mEmailField.setError("Required.");
            valid = false;
        } else {
            mEmailField.setError(null);
        }

        String password = mPasswordField.getText().toString();
        if (TextUtils.isEmpty(password)) {
            mPasswordField.setError("Required.");
            valid = false;
        } else {
            mPasswordField.setError(null);
        }

        return valid;
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.email_create_account_button) {
            createAccount(mEmailField.getText().toString(), mPasswordField.getText().toString());
        } else if (i == R.id.email_sign_in_button) {
            signIn(mEmailField.getText().toString(), mPasswordField.getText().toString());
        }
    }

    protected void saveUserName(String email, String password){

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("email", email);
        editor.putString("password", password);
        editor.commit();
    }

    private class Login extends AsyncTask<HashMap<String,String>, Void, String> {

        //  The API_Endpoint is hold on an old AWS EC2 and no longer valid, please re-deploy the end point
        String API_Endpoint = Settings.API_Endpoint;
        public Login() {
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
                URL url;
                if(params[0].containsKey("name")) {
                    url = new URL(API_Endpoint+"/register/");
                } else{
                    url = new URL(API_Endpoint+"/login/");
                }
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
                if(status.contains("\"status\": \"pass\"")){
                    saveUserName(params[0].get("email"),params[0].get("password"));
                }
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

        @Override
        protected void onPostExecute(String result) {
            Log.d("HTTPPost", "Payload:" + result);
            if(result.contains("\"status\": \"pass\"")){
                startActivity(new Intent(RegisterActivity.this, ProfileActivity.class));
                RegisterActivity.this.finish();
            }
            else{
                Log.d(TAG, "wrong...");
                Toast.makeText(RegisterActivity.this, "wrong",Toast.LENGTH_SHORT).show();
            }
        }
    }


}