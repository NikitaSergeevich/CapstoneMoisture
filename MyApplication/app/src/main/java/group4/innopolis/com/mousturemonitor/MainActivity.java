package group4.innopolis.com.mousturemonitor;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import group4.innopolis.com.mousturemonitor.Network.IpServerHelper;
import group4.innopolis.com.mousturemonitor.Network.ServerHelper;

public class MainActivity extends AppCompatActivity {

    private static Socket android_socket;
    private static PrintWriter out = null;
    private static BufferedReader in = null;
    private static final int SERVER_PORT = 4444;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeConnectButton();

        new IpServerHelper() {
            @Override
            protected void onPostExecute(final String ip) {
                EditText control = (EditText) findViewById(R.id.ip_adress);
                control.setText(ip);
            }
        }.execute();

        syncData();
    }

    private void syncData(){
        Button syncButton = (Button) findViewById(R.id.sync);

        syncButton.setText("Loading...");

        new ServerHelper() {
            @Override
            protected void onPostExecute(final TreeMap<Date, Float> list) {
                draw(list);
                Button syncButton = (Button) findViewById(R.id.sync);
                syncButton.setText("Sync");
            }
        }.execute();
    }

    public void sync(View view) {
        syncData();
    }

    private void initializeConnectButton() {
        Button connect = (Button) findViewById(R.id.connect);
        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ip = ((EditText) findViewById(R.id.ip_adress)).getText().toString();
                //logMessage(String.format("Trying to connect to :%s", ip));
                if (isConnected() && !ip.isEmpty()) {
                    new Thread(new createSocket()).start();
                }
            }
        });
    }

    public boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo net = cm.getActiveNetworkInfo();
        if (net != null && net.isAvailable() && net.isConnected())
            return true;
        else
            return false;
    }

    public void watering(View view) {
        sendMessage("water");
    }

    class createSocket implements Runnable{
        public void run() {
            try {
                String ip = ((EditText) findViewById(R.id.ip_adress)).getText().toString();
                android_socket = new Socket(ip, SERVER_PORT);
                android_socket.setKeepAlive(true);
                createSocketWriterReader();
                new Thread(new AndroidClient()).start();
                runOnUiThread(updateUI);
                //logMessage("Device is connected");
            }
            catch (final Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Connection is not established: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        private Runnable updateUI = new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.watering).setVisibility(View.VISIBLE);
                findViewById(R.id.connected).setVisibility(View.VISIBLE);
                findViewById(R.id.connect).setVisibility(View.GONE);
                findViewById(R.id.ip_adress).setVisibility(View.GONE);
            }
        };
    }

    class AndroidClient implements Runnable{
        private String serverResponse = null;
        @Override
        public void run() {
            while (true) {
                try {
                    if ((serverResponse = in.readLine()) != null) {
                    /*Message msg = new Message();
                     msg.obj = serverResponse;
                     handler.sendMessage(msg);
                     textToSpeech.speak(String.valueOf(msg.obj), TextToSpeech.QUEUE_FLUSH, null);*/
                    }
                }
                catch (IOException e) {
                    //logMessage("Cannot write to the server");
                }
            }
        }
    }

    /*Initialization of input/output streams*/
    private void createSocketWriterReader() {
        try {
            out = new PrintWriter(new DataOutputStream(android_socket.getOutputStream()), true);
            in = new BufferedReader(new InputStreamReader(android_socket.getInputStream()));
        }
        catch (IOException e) {
            //logMessage("Cannot write to the server");
        }
    }


    private void draw(TreeMap<Date, Float> sourceData){
        LineChart chart = (LineChart) findViewById(R.id.chart);

        LineData data = new LineData(getLabelsForX(sourceData), getAllData(sourceData, "Moisture chart"));

        chart.setData(data);

        chart.notifyDataSetChanged();

        chart.invalidate();
    }

    private String[] getLabelsForX(Map<Date, Float> sourceData) {
        String[] result = new String[sourceData.size()];

        int index = 0;
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");

        for (Date date : sourceData.keySet()) {
            result[index] = format.format(date);
            index++;
        }

        return result;
    }

    private ArrayList<LineDataSet> getAllData(TreeMap<Date, Float> data, String legendLabel) {
        ArrayList<LineDataSet> result = new ArrayList<LineDataSet>();
        ArrayList<Entry> entrySet = new ArrayList<Entry>();

        int i = 0;

        for (Map.Entry<Date, Float> entry : data.entrySet()) {
            entrySet.add(createPoint(i, entry.getValue()));
            i++;
        }

        result.add(new LineDataSet(entrySet, legendLabel));
        return result;
    }

    private Entry createPoint(Integer x, Float y) {
        return new Entry(y, x);
    }

    private void sendMessage(String messsage)
    {
        out.println(messsage);
        out.flush();
    }



}
