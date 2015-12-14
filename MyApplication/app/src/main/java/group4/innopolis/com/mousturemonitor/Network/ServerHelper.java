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
import java.util.TreeMap;

import group4.innopolis.com.mousturemonitor.ISO8601DateParser;

public abstract class ServerHelper extends AsyncTask<Void, Void, TreeMap<Date, Float>> {

    private String[] jsonprojectfields = {"moisture", "createdAt"};
    String X_Parse_REST_API_Key = "aAvsjGidVlAU9XrzQNx1YqELcNeoqLVEyrX005BX";
    String X_Parse_Application_Id = "avhoXwWVRDTz3lf9AeBZFOLATiA0xZbllRLxgUIa";
    public ServerHelper() {
    }


    @Override
    protected TreeMap<Date, Float> doInBackground(Void... params) {
        try {
            return performRequestToParseCom();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected abstract void onPostExecute(final TreeMap<Date, Float> result);


    public TreeMap<Date, Float> performRequestToParseCom() throws Exception {
        String request = "https://api.parse.com/1/classes/MoistureData?order=updatedAt";

        HttpURLConnection connection = request(request, "GET", null);

        switch (connection.getResponseCode()) {
            case HttpURLConnection.HTTP_OK:
                return parseMoistureData(connection);
            default:
                return null;
        }
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

    private TreeMap<Date, Float> parseMoistureData(HttpURLConnection connection) throws IOException {
        JSONObject json;
        TreeMap<Date, Float> data = new TreeMap<Date, Float>() {};
        String response = readBuffer(connection);

        try {
            json = new JSONObject(response);
            JSONArray array = json.getJSONArray("results");
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                ArrayList<String> jsonparsed = new ArrayList<>();
                for (String field : jsonprojectfields) {
                    if (!obj.isNull(field)) {
                        jsonparsed.add(obj.getString(field));
                    } else {
                        jsonparsed.add("");
                    }
                }
                if (!jsonparsed.get(1).equals("") && !jsonparsed.get(0).equals("")) {
                    //SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.US);
                    Date date = ISO8601DateParser.parse(jsonparsed.get(1));
                    data.put(date, Float.parseFloat(jsonparsed.get(0)));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return data;
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
