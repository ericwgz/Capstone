package com.example.uwcapstone;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.sonicmeter.android.multisonicmeter.Utils;
import com.sonicmeter.android.multisonicmeter.Params;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static MainActivity instance;
    private Spinner frequencySpinner;
    private Spinner sampleRateSpinner;
    private Spinner bandWidthSpinner;
    private static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;
    String msg = "";
    public static String frequencyInput = "6000";
    public static String sampleRateInput = "48000";
    public static String bandWidthInput = "1000";

    static Sender mClientThread;
    static Receiver mReceiverThread;

    Button mStartSendBtn;
    Button mStopSendBtn;
    Button mStartReceiveBtn;
    Button mStopReceiveBtn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        instance = this;

        checkAndRequestPermissions();

        // initial UI state
        mStartSendBtn = findViewById(R.id.seeker);
        mStopSendBtn = findViewById(R.id.stopSeek);
        mStartReceiveBtn = findViewById(R.id.helper);
        mStopReceiveBtn = findViewById(R.id.stopHelp);

        frequencySpinner = findViewById(R.id.frequency_spinner);
        frequencySpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapter, View view, int position, long id) {
                // get message in spinner
                msg = adapter.getItemAtPosition(position).toString();
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                msg = "MESSAGE IS NOT SELECTED";
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
        sampleRateSpinner = findViewById(R.id.sampleRate_spinner);
        sampleRateSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapter, View view, int position, long id) {
                // get message in spinner
                msg = adapter.getItemAtPosition(position).toString();
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                msg = "MESSAGE IS NOT SELECTED";
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
        bandWidthSpinner = findViewById(R.id.bandWidth_spinner);
        bandWidthSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapter, View view, int position, long id) {
                // get message in spinner
                msg = adapter.getItemAtPosition(position).toString();
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                msg = "MESSAGE IS NOT SELECTED";
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        });

        // create a container to hold the values that would integrate to the spinner
        ArrayAdapter<String> frequencyAdapter = new ArrayAdapter<>(MainActivity.this,
                android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.frequency));
        ArrayAdapter<String> sampleRateAdapter = new ArrayAdapter<>(MainActivity.this,
                android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.sampleRate));
        ArrayAdapter<String> bandWidthAdapter = new ArrayAdapter<>(MainActivity.this,
                android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.bandWidth));

        // specify the adapter would have a drop down list
        frequencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sampleRateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bandWidthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // list in adapter shown in spinner
        frequencySpinner.setAdapter(frequencyAdapter);
        sampleRateSpinner.setAdapter(sampleRateAdapter);
        bandWidthSpinner.setAdapter(bandWidthAdapter);

        // initial audiotrack player
        Utils.initPlayer(Params.sampleRate, 0);
        Utils.initRecorder(Params.sampleRate);

        Utils.initConvolution((Params.signalSequenceLength * Params.bitCount));

        mStartSendBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // start to play sequence
                log("Current Role : Seeker.");

                mStartSendBtn.setEnabled(false);
                mStopSendBtn.setEnabled(true);
                mStartReceiveBtn.setEnabled(false);
                mStopReceiveBtn.setEnabled(false);

                mClientThread = new Sender("Seeker");
                mClientThread.start();
            }
        });

        mStopSendBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // stop playing sequence
                log("Stop seeking help.");

                mStartSendBtn.setEnabled(true);
                mStopSendBtn.setEnabled(false);
                mStartReceiveBtn.setEnabled(true);
                mStopReceiveBtn.setEnabled(false);

                if(mClientThread != null)
                    mClientThread.stopThread();
                if(mReceiverThread != null)
                    mReceiverThread.stopThread();
            }
        });

        mStartReceiveBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // start to listen to sequence
                log("Current Role : Helper.");

                mStartSendBtn.setEnabled(false);
                mStopSendBtn.setEnabled(false);
                mStartReceiveBtn.setEnabled(false);
                mStopReceiveBtn.setEnabled(true);

                mReceiverThread = new Receiver("Helper");
                mReceiverThread.start();
            }
        });

        mStopReceiveBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // stop listening to sequence
                log("Stop help.");

                mStartSendBtn.setEnabled(true);
                mStopSendBtn.setEnabled(false);
                mStartReceiveBtn.setEnabled(true);
                mStopReceiveBtn.setEnabled(false);

                if(mClientThread != null)
                    mClientThread.stopThread();
                if(mReceiverThread != null)
                    mReceiverThread.stopThread();
            }
        });
    }

    public static void log(final String text)
    {
        instance.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView logBox = instance.findViewById(R.id.textview_log);
                logBox.setMovementMethod(ScrollingMovementMethod.getInstance());
                //Calendar cal = Calendar.getInstance();
                logBox.append("  "+ text + "\n");//cal.get(Calendar.MINUTE)+ ":" +cal.get(Calendar.SECOND)+ ":" + cal.get(Calendar.MILLISECOND) +
            }
        });
    }

//    public static void decodedMsg(final String text)
//    {
//        instance.runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                TextView decodedMsgBox = instance.findViewById(R.id.decoded_msg);
//                decodedMsgBox.setMovementMethod(ScrollingMovementMethod.getInstance());
//                //Calendar cal = Calendar.getInstance();
//                decodedMsgBox.append("  "+ text + "\n");//cal.get(Calendar.MINUTE)+ ":" +cal.get(Calendar.SECOND)+ ":" + cal.get(Calendar.MILLISECOND) +
//            }
//        });
//    }

    private void checkAndRequestPermissions() {
        int permissionWifi = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE);
        int writepermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permissionRecordAudio = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);


        ArrayList<String> listPermissionsNeeded = new ArrayList<>();

        if (writepermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (permissionWifi != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_WIFI_STATE);
        }
        if (permissionRecordAudio != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.RECORD_AUDIO);
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), REQUEST_ID_MULTIPLE_PERMISSIONS);
        }
    }

}
