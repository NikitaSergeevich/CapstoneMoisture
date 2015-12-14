import java.io.DataOutputStream;
import java.util.Date;

import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.SerialDataEvent;
import com.pi4j.io.serial.SerialDataListener;
import com.pi4j.io.serial.SerialFactory;
import com.pi4j.io.serial.SerialPortException;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.wiringpi.SoftPwm;

import java.util.concurrent.Callable;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.trigger.GpioCallbackTrigger;
import com.pi4j.io.gpio.trigger.GpioPulseStateTrigger;
import com.pi4j.io.gpio.trigger.GpioSetStateTrigger;
import com.pi4j.io.gpio.trigger.GpioSyncStateTrigger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.pi4j.system.NetworkInfo;
import com.pi4j.system.SystemInfo;
import java.net.InetAddress;
import java.io.*;
import java.net.*;

public class SerialReader {

    static GpioPinDigitalOutput pin = null;
    static boolean rotated = false;
    static Boolean isWorking = false;
    static int counter = 0;

    static String X_Parse_REST_API_Key = "aAvsjGidVlAU9XrzQNx1YqELcNeoqLVEyrX005BX";
    static String X_Parse_Application_Id = "avhoXwWVRDTz3lf9AeBZFOLATiA0xZbllRLxgUIa";
    static String request = "https://api.parse.com/1/classes/MoistureData";
    static String requestip = "https://api.parse.com/1/classes/IP/mFVWswTKmN";
    static String IP = "";

    static ServerSocket servers = null;
    static Socket rpi_socket = null;
	static BufferedReader in = null;
	static String input = "", output = "";

    public static void main(String args[]) throws IOException {

        System.out.println("<--Pi4J--> Serial Communication Example ... started.");
        System.out.println(" ... connect using settings: 9600, N, 8, 1.");
        System.out.println(" ... data received on serial port should be displayed below.");
        final GpioController gpio = GpioFactory.getInstance();

        // Serial Port (Mousture)
        final Serial serial = SerialFactory.createInstance();

        // Servo
        com.pi4j.wiringpi.Gpio.wiringPiSetup();
        SoftPwm.softPwmCreate(26, 0, 100);

        // Button
        final GpioPinDigitalInput myButton = gpio.provisionDigitalInputPin(RaspiPin.GPIO_02, PinPullResistance.PULL_DOWN);

		// create and register the serial data listener
        serial.addListener(new SerialDataListener() {
            @Override
            public synchronized void dataReceived(SerialDataEvent event) {
                synchronized (isWorking) {
                    if (isWorking)
                        return;
                    isWorking = true;

                    String[] strings = event.getData().split("\n");
                    String lastString = strings[strings.length-1];
                    int foo = Integer.parseInt(lastString.trim());

                    try {
                        JSONObject jsonParam = new JSONObject();
                        jsonParam.put("moisture", foo);
                        URL requestURL = new URL(request);
                        HttpURLConnection connection = (HttpURLConnection)requestURL.openConnection();
                        connection.setReadTimeout(10000);
                        connection.setConnectTimeout(15000);
                        connection.setRequestMethod("POST");
                        connection.setRequestProperty("X-Parse-REST-API-Key", X_Parse_REST_API_Key);
                        connection.setRequestProperty("X-Parse-Application-Id", X_Parse_Application_Id);
						connection.setDoOutput(true);
                        DataOutputStream printout = new DataOutputStream(connection.getOutputStream());
                        printout.write(jsonParam.toString().getBytes());
                        printout.close();
						System.out.print("\r Posting data — " + connection.getResponseCode() + 
							". Moisture data — " + lastString);

                        if (foo >= 600 && foo < 1000 ) // && SerialReader.rotated == false)
                        {
                            System.out.println("Soil is dry. Need watering");
                            giveWater();
                            System.out.println("Wait for soil to be moistured after watering");
                            Thread.sleep(20000);
						}
						else{
							Thread.sleep(5000);
						}
                    }
                    catch (Exception e)
                    {
                        System.out.println(e.getMessage());
                        return;
                    }
                    isWorking = false;
                }
            }
        });

        try {
            serial.open("/dev/ttyACM0", 9600);
        }
        catch(SerialPortException ex1) {
            try {
                serial.open("/dev/ttyACM1", 9600);
            }
            catch(SerialPortException ex2) {
                System.out.println(" ==>> SERIAL SETUP FAILED : " + ex2.getMessage());
            }
        }
		
		// create server socket
        try
      {
          servers = new ServerSocket(4444);
        }
      catch (IOException e)
      {
          System.out.println("Couldn't listen to port 4444");
          System.exit(-1);
        }

        try
        {
          System.out.println("IOT Server is Started");
          System.out.println("Waiting for a client...");
          rpi_socket = servers.accept();
          System.out.println("Client connected");
          in = new BufferedReader(new InputStreamReader(rpi_socket.getInputStream()));
          //out = new PrintWriter(rpi_socket.getOutputStream(), true);
        }
        catch (Exception e)
        {
          System.out.println("Exeption in initialization");
          System.exit(-1);
        }

		try {
			IP = InetAddress.getLocalHost().getHostAddress();
			JSONObject jsonParam = new JSONObject();
			jsonParam.put("ip", IP);
			URL requestURL = new URL(requestip);
			HttpURLConnection connection = (HttpURLConnection)requestURL.openConnection();
			connection.setReadTimeout(10000);
			connection.setConnectTimeout(15000);
			connection.setRequestMethod("PUT");
			connection.setRequestProperty("X-Parse-REST-API-Key", X_Parse_REST_API_Key);
			connection.setRequestProperty("X-Parse-Application-Id", X_Parse_Application_Id);
			connection.setDoOutput(true);
			DataOutputStream printout = new DataOutputStream(connection.getOutputStream());
			printout.write(jsonParam.toString().getBytes());
			printout.close();
			System.out.println(connection.getResponseCode());
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
			return;
		}

        System.out.println("Wait for messages");
		while(true)
		{
			while ((input = in.readLine()) != null)
			{
				if (input.equalsIgnoreCase("exit"))
					break;
				System.out.println("Command recieved: " + input);
				try
				{
          giveWater();
					//out.println(str);
					//out.flush();
				}
				catch (Exception e)
				{
					System.out.println("Exeption");
				}
			}
			break;
        }
        //out.close();
        in.close();
    }


    public static void giveWater() throws Exception
    {
        System.out.println("Start opening water");
        SoftPwm.softPwmWrite(26, 5);
        Thread.sleep(330);

        System.out.println("Stop opening (stay open)");
        SoftPwm.softPwmWrite(26, 0);
        Thread.sleep(5000);

        System.out.println("Start closing");
        SoftPwm.softPwmWrite(26, 25);
        Thread.sleep(380);

        System.out.println("Stop closing (stay closed)");
        SoftPwm.softPwmWrite(26, 0);
    }
}
