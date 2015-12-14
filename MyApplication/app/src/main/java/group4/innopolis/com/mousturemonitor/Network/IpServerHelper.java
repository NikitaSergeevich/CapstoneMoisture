package group4.innopolis.com.mousturemonitor.Network;

import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import group4.innopolis.com.mousturemonitor.ISO8601DateParser;

public abstract class IpServerHelper extends AsyncTask<Void, Void, String> {

    String X_Parse_REST_API_Key = "aAvsjGidVlAU9XrzQNx1YqELcNeoqLVEyrX005BX";
    String X_Parse_Application_Id = "avhoXwWVRDTz3lf9AeBZFOLATiA0xZbllRLxgUIa";
    private boolean isIpRequest;


    @Override
    protected String doInBackground(Void... params) {
        try {
            return performRequestToParseCom();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected abstract void onPostExecute(final String result);


    public String performRequestToParseCom() throws Exception {
        String request = "https://api.parse.com/1/classes/IP";

        HttpURLConnection connection = request(request, "GET", null);

        switch (connection.getResponseCode()) {
            case HttpURLConnection.HTTP_OK:
                return parseIpData(connection);
            default:
                return null;
        }
    }

    private String parseIpData(HttpURLConnection connection) throws IOException {
        JSONObject json;
        String response = readBuffer(connection);
        String result = null;

        try {
            json = new JSONObject(response);
            JSONArray array = json.getJSONArray("results");
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                ArrayList<String> jsonparsed = new ArrayList<>();
                jsonparsed.add(obj.getString("ip"));
                result = jsonparsed.get(0);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

    public HttpURLConnection request(String request, String method, JSONObject params) throws Exception {
        URL requestURL = new URL(request);
        HttpURLConnection connection = (HttpURLConnection) requestURL.openConnection();
        connection.setReadTimeout(10000);
        connection.setConnectTimeout(15000);
        connection.setRequestMethod(method);
        connection.setRequestProperty("X-Parse-REST-API-Key", X_Parse_REST_API_Key);
        connection.setRequestProperty("X-Parse-Application-Id", X_Parse_Application_Id);
        connection.setDoInput(true);
        connection.connect();
        return connection;
    }

    private String readBuffer(HttpURLConnection connection) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line = reader.readLine();
        while (line != null) {
            sb.append(line + "\n");
            line = reader.readLine();
        }
        return sb.toString();
    }
}
