package com.example.uwcapstone;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
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

public class MainActivity extends AppCompatActivity implements OnItemSelectedListener {

    private static MainActivity instance;
    private Spinner mySpinner = null;
    private static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;
    String msg = "";
    Sender clientThread = null;
    Receiver receiverThread = null;
    Params params = new Params();

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
        mStartSendBtn = (Button) findViewById(R.id.seeker);
        setSeekerBtnState(true);
        mStopSendBtn = (Button) findViewById(R.id.stopSeek);
        setStopSeekBtnState(false);
        mStartReceiveBtn = (Button) findViewById(R.id.helper);
        setHelperBtnState(true);
        mStopReceiveBtn = (Button) findViewById(R.id.stopHelp);
        setStopHelpBtnState(false);
        mySpinner = (Spinner) findViewById(R.id.msgToSend);

        // create a container to hold the values that would integrate to the spinner
        ArrayAdapter<String> myAdapter = new ArrayAdapter<String>(MainActivity.this,
                android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.messages));

        // specify the adapter would have a drop down list
        myAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // list in adapter shown in spinner
        mySpinner.setAdapter(myAdapter);

        // initial database


        // initial audiotrack player
        Utils.initPlayer(params.sampleRate, 0);
        Utils.initRecorder(params.sampleRate);

        Utils.initConvolution((int)(params.signalSequenceLength * params.bitCount));

        // register OnItemSelected event
        mySpinner.setOnItemSelectedListener(this);

        mStartSendBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // start to play sequence
                log("start sender thread as seeker");
                mStartSendBtn.setEnabled(false);
                mStopSendBtn.setEnabled(true);
                mStartReceiveBtn.setEnabled(false);
                mStopReceiveBtn.setEnabled(false);
                instance.clientThread = new Sender("Seeker");
                instance.clientThread.start();
            }
        });

        mStopSendBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // stop playing sequence
                log("stop seeker thread");
                mStartSendBtn.setEnabled(true);
                mStopSendBtn.setEnabled(false);
                mStartReceiveBtn.setEnabled(true);
                mStopReceiveBtn.setEnabled(false);
                instance.clientThread.stopThread();
                setStopSeekBtnState(false);
                setSeekerBtnState(true);
                clientThread = null;
            }
        });

        mStartReceiveBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // start to listen to sequence
                log("start receiver thread as helper");
                mStartSendBtn.setEnabled(false);
                mStopSendBtn.setEnabled(false);
                mStartReceiveBtn.setEnabled(false);
                mStopReceiveBtn.setEnabled(true);
                instance.receiverThread = new Receiver("Helper");
                instance.receiverThread.start();
            }
        });

        mStopReceiveBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // stop listening to sequence
                Log.d("stop", "helper thread");
                mStartSendBtn.setEnabled(true);
                mStopSendBtn.setEnabled(false);
                mStartReceiveBtn.setEnabled(true);
                mStopReceiveBtn.setEnabled(false);
                instance.receiverThread.stopThread();
                setStopHelpBtnState(false);
                setHelperBtnState(true);
                receiverThread = null;
            }
        });
    }

    // Spinner OnItemSelectedListener
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

    public static void setSeekerBtnState(final boolean state)
    {
        instance.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Button button_server = (Button) instance.findViewById(R.id.seeker);
                button_server.setEnabled(state);
            }
        });
    }
    public static void setStopSeekBtnState(final boolean state)
    {
        instance.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Button button_client = (Button) instance.findViewById(R.id.stopSeek);
                button_client.setEnabled(state);
            }
        });
    }

    public static void setHelperBtnState(final boolean state)
    {
        instance.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Button button = (Button) instance.findViewById(R.id.helper);
                button.setEnabled(state);
            }
        });
    }

    public static void setStopHelpBtnState(final boolean state)
    {
        instance.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Button button = (Button) instance.findViewById(R.id.stopHelp);
                button.setEnabled(state);
            }
        });
    }

    public static void log(final String text)
    {
        instance.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView logBox = (TextView)instance.findViewById(R.id.textview_log);
                logBox.setMovementMethod(ScrollingMovementMethod.getInstance());
                //Calendar cal = Calendar.getInstance();
                logBox.append("  "+ text + "\n");//cal.get(Calendar.MINUTE)+ ":" +cal.get(Calendar.SECOND)+ ":" + cal.get(Calendar.MILLISECOND) +
            }
        });
    }

    public static void decodedMsg(final String text)
    {
        instance.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView decodedMsgBox = (TextView)instance.findViewById(R.id.decoded_msg);
                decodedMsgBox.setMovementMethod(ScrollingMovementMethod.getInstance());
                //Calendar cal = Calendar.getInstance();
                decodedMsgBox.append("  "+ text + "\n");//cal.get(Calendar.MINUTE)+ ":" +cal.get(Calendar.SECOND)+ ":" + cal.get(Calendar.MILLISECOND) +
            }
        });
    }

    private  boolean checkAndRequestPermissions() {
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
            return false;
        }
        return true;
    }

}
