package com.example.logincounter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONException;
import org.json.JSONObject;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Login counter app. Users can be added to a database with the add button. Users can login and see how many times they've logged in
 * with the login button.
 * 
 * Code is heavily adapted from the following sources:
 * http://developer.android.com/training/basics/network-ops/connecting.html
 * http://stackoverflow.com/questions/20383948/how-to-post-the-following-json-array-in-android
 * @author Christopher
 *
 */

public class LoginAddActivity extends Activity {
	public final static String WELCOME_MESSAGE = "LoginAddAcivity.WELCOME_MESSAGE";
	private static String TYPE_APPLICATION_JSON = "application/json";
	private static String UTF_8 = "UTF-8";
	private static String ADD_USER_URL = "http://protected-everglades-8107.herokuapp.com/users/add";
	private static String LOGIN_USER_URL = "http://protected-everglades-8107.herokuapp.com/users/login";
	private static String JSON_ERROR = "Unknown JSON parsing error";
	private static int SUCCESS = 1;
	private static int ERR_BAD_CREDENTIALS = -1;
	private static int ERR_USER_EXISTS = -2;
	private static int ERR_BAD_USERNAME = -3;
	private static int ERR_BAD_PASSWORD = -4;
	private EditText userInputBox;
	private EditText passwordInputBox;
	private TextView errorMessageToUser;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		userInputBox = (EditText) findViewById(R.id.user);
		passwordInputBox = (EditText) findViewById(R.id.password);
		errorMessageToUser = (TextView) findViewById(R.id.result_message);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void sendAddRequest(View view) {
		sendRequestToLoginServer(ADD_USER_URL);
	}
	public void sendLoginRequest(View view) {
		sendRequestToLoginServer(LOGIN_USER_URL);
	}

	public void sendRequestToLoginServer(String url) {
		ConnectivityManager connMgr = (ConnectivityManager) 
				getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		if (networkInfo != null && networkInfo.isConnected()) {
			LoginServerConnection lsr = new LoginServerConnection();
			lsr.setUserName((userInputBox.getText().toString()));
			lsr.execute(url);
		} else {
			errorMessageToUser.setText("No network connection available.");
		}
	}

	private class LoginServerConnection extends AsyncTask<String, Void, String> {
		private String username;
		protected void setUserName(String user) {
			username = user;
		}
		@Override
		protected String doInBackground(String... urls) {
			try {
				return getLoginServerResponse(urls[0]);
			} catch (IOException e) {
				return "Unable to get url.";
			}
		}
		@Override
		protected void onPostExecute(String result) {
			JSONObject serverResponse;
			try {
				serverResponse = new JSONObject(result);
				Integer errorCode = (Integer)serverResponse.get("errCode");
				if (errorCode.equals(SUCCESS)) {
					Integer count = (Integer)serverResponse.get("count");
					String message = "Welcome "+username+", you logged in "+count+" times.";
					Intent intent = new Intent(LoginAddActivity.this, LoginMessageActivity.class);
					intent.putExtra(WELCOME_MESSAGE, message);
					startActivity(intent);
				}
				else if (errorCode.equals(ERR_BAD_CREDENTIALS)) {
					errorMessageToUser.setText("Invalid username and password combination. Please try again.");
				}
				else if (errorCode.equals(ERR_USER_EXISTS)) {
					errorMessageToUser.setText("This user name already exists. Please try again.");
				}
				else if (errorCode.equals(ERR_BAD_USERNAME)) {
					errorMessageToUser.setText("The user name should be non-empty and at most 128 characters long. Please try again.");
				}
				else if (errorCode.equals(ERR_BAD_PASSWORD)) {
					errorMessageToUser.setText("The password should be at most 128 characters long. Please try again.");
				}
				else {
					errorMessageToUser.setText("Unknown error.");
				}
			} catch (JSONException e) {
				errorMessageToUser.setText("JSON parsing error.");
			}
		}
	}

	private String getLoginServerResponse(String serverUrl) throws IOException {
		InputStream is = null;
		int len = 500;
		try {
			URL url = new URL(serverUrl);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestProperty("Content-Type", TYPE_APPLICATION_JSON);
			conn.setRequestProperty("Accept", TYPE_APPLICATION_JSON);
			conn.setRequestMethod("POST");
			conn.connect();
			JSONObject usernameAndPassword = new JSONObject();
			usernameAndPassword.put("user", userInputBox.getText());
			usernameAndPassword.put("password", passwordInputBox.getText());
			byte[] outputBytes = usernameAndPassword.toString().getBytes(UTF_8);
			OutputStream os = conn.getOutputStream();
			os.write(outputBytes);
			os.close();
			is = conn.getInputStream();
			String contentAsString = readIt(is, len);
			return contentAsString;
		} catch(JSONException e) {
			return JSON_ERROR;
		}
		finally {
			if (is != null) {
				is.close();
			} 
		}
	}

	public String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
		Reader reader = null;
		reader = new InputStreamReader(stream, UTF_8);        
		char[] buffer = new char[len];
		reader.read(buffer);
		return new String(buffer);
	}

}
