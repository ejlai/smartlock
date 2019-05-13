//Android project is based on this github project: https://github.com/HarryGoodwin/Arduino-Android-Sensors
package cat.uab.elai.smartlock;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
public class MainActivity extends AppCompatActivity {
    Button btnOn, btnOff;
    TextView txtString, txtStringLength;
    private Handler bluetoothIn;
    private final int handlerState = 0; //used to identify handler message
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private final StringBuilder recDataString = new StringBuilder();
    private ConnectedThread mConnectedThread;
    // SPP UUID service - this should work for most devices
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    //String for MAC address
    static String address;

    // for HMAC
    private String myCMD, myRndCode, strHash;
    private final String ON="W5sb";
    private final String OFF="xmG9";
    //shared key
    private final String myKey = "yyQNVTj-?V%McY$7Mx=UY_x2gMA=e^FRh7&^TFMHZ6P9kLdrDPLSZzV5eMf#J3-5U&FJ$gf*rkCJ8dTaFZr@!w9XcgNY2H?vjp^gxWuTWTg+!wcgJ63zVU$U+@My#+YbKL@jrRt528x+fCZ7EQ-P8EkAU&xTb3@y*XPpXE5u+#6g!Z&kzyQv!?r@fXTs3LMhSvx_VYkEm5DJzA5zTcH&Lytuxre_d7hPrZ9e6cfLT%6x?JZU2s&bBGbC2zv=ZU?w";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Link the buttons and textViews to respective views
        btnOn = findViewById(R.id.buttonOn);
        btnOff = findViewById(R.id.buttonOff);
        txtString = findViewById(R.id.txtString);
        txtStringLength = findViewById(R.id.testView1);
        bluetoothIn = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (msg.what == handlerState) { //if message is what we want
                    String readMessage = (String) msg.obj;// msg.arg1 = bytes from connect thread
                    recDataString.append(readMessage); //keep appending to string until ~
                    int endOfLineIndex = recDataString.indexOf("~");// determine the end-of-line
                    if (endOfLineIndex > 0) {// make sure there data before ~
                        String inString = recDataString.substring(0, endOfLineIndex);// extract string
                        txtString.setText("Connect token = " + inString);
                        myRndCode = inString;
                        int dataLength = inString.length();//get length of data received
                        recDataString.delete(0, recDataString.length());//clear all string data
                        inString = " ";
                    }
                }
                return true;
            }
        });

        btAdapter = BluetoothAdapter.getDefaultAdapter(); //get Bluetooth adapter
        checkBTState();
        // Set up onClick listeners for buttons to send 1 or 0 to turn on/off LED
        btnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                strHash = HmacSHA256(OFF+myRndCode,myKey);//message, key
                mConnectedThread.write(OFF+myRndCode+strHash); // send message + hashed string via Bluetooth
                Toast.makeText(getBaseContext(), "Request sent to turn LED OFF", Toast.LENGTH_SHORT).show();
            }
        });
        btnOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                strHash = HmacSHA256(ON+myRndCode,myKey);//message, key
                mConnectedThread.write(ON+myRndCode+strHash); // send message + hashed string vie Bluetooth
                //mConnectedThread.write("1");    // Send "1" via Bluetooth
                Toast.makeText(getBaseContext(), "Request sent to turn LED on", Toast.LENGTH_SHORT).show();
            }
        });
    } //end of onCreate()
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {

        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //creates secure outgoing connection with BT device using UUID
    }
    @Override
    public void onResume() {
        super.onResume();
        //Get MAC address from DeviceListActivity via intent
        Intent intent = getIntent();
        //Get the MAC address from the DeviceListActivty via EXTRA
        address = intent.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        //create device and set the MAC address
        BluetoothDevice device = btAdapter.getRemoteDevice(address);
        try {
            btSocket = createBluetoothSocket(device);
            Toast.makeText(getBaseContext(), "Connect successfully", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_LONG).show();
        }
        // Establish the Bluetooth socket connection.
        try {
            btSocket.connect();
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                //insert code to deal with this
            }
        }
        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();
        //send a character when resuming or beginning transmission to check device is connected
        //If it is not an exception will be thrown in the write method and finish() will be called
		//acknowledge of being connected then controller respond by sending a token back
        mConnectedThread.write("x"); 
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            //Don't leave Bluetooth sockets open when leaving activity
            btSocket.close();
        } catch (IOException e2) {
            //insert code to deal with this
        }
    }

    //Checks that the Android device Bluetooth is available and prompts to be turned on if off
    private void checkBTState() {
        if (btAdapter == null) {
            Toast.makeText(getBaseContext(), "Device does not support bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (btAdapter.isEnabled()) {
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }
    //create new class for connect thread
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        //creation of the connect thread
        private ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        public void run() {
            byte[] buffer = new byte[256];
            int bytes;
            // Keep looping to listen for received messages
            while (true) {
                try {
                    bytes = mmInStream.read(buffer); //read bytes from input buffer
                    String readMessage = new String(buffer, 0, bytes);
                    // Send the obtained bytes to the UI Activity via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }
        //write method
        private void write(String input) {
            byte[] msgBuffer = input.getBytes(); //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);//write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                Toast.makeText(getBaseContext(), "Connection Failure", Toast.LENGTH_LONG).show();
                finish();

            }
        }
    }
    //HMAC implementation
    private String HmacSHA256(String message, String key) {
        String messageDigest="";
        try {
            final String hashingAlgorithm = "HmacSHA256"; //or "HmacSHA1", "HmacSHA512"
            byte[] bytes = hmac(hashingAlgorithm, key.getBytes(), message.getBytes());
            messageDigest = bytesToHex(bytes);
            Log.i("TAG", "message digest: " + messageDigest);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return messageDigest;
    }
    private static byte[] hmac(String algorithm, byte[] key, byte[] message) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(algorithm);
        mac.init(new SecretKeySpec(key, algorithm));
        return mac.doFinal(message);
    }
    private static String bytesToHex(byte[] bytes) {
        final char[] hexArray = "0123456789abcdef".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0, v; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}