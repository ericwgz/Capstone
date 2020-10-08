package com.sonicmeter.android.multisonicmeter;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
//import android.support.v4.app.ActivityCompat;
//import android.support.v4.content.ContextCompat;
//import android.support.v7.app.AlertDialog;
//import android.support.v7.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class MainActivityAcoustic extends AppCompatActivity {
    private static MainActivityAcoustic instance;
    private static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;

    private static String thisIP = "0.0.0.0";
    private static int COM_TYPE = -1;
    private double distance;

    private static ConnectClient connectClient = null;
    ConnectServer connectServer = null;

    FindServer findServer = null;

    Server serverThread = null;
    Client clientThread = null;

    Params params = new Params();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= 23) {
            checkAndRequestPermissions();
        }

        instance = this;
        initUI();

        getConnection_Status();

        init_player_recorder();

        //Create the convolution class
        Utils.initConvolution((int)(params.signalSequenceLength * params.bitCount));
    }


    public void initUI()
    {
        TextView logBox = (TextView)findViewById(R.id.textview_log);
        logBox.setMovementMethod(new ScrollingMovementMethod());

        final Button clientBtn = (Button) findViewById(R.id.button_client);
        clientBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                create_Client();
            }
        });

        final Button serverBtn = (Button) findViewById(R.id.button_server);
        serverBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                create_Server();
            }
        });

        final Button returnBtn = (Button) findViewById(R.id.button_return);
        returnBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                return_to_main();
            }
        });
    }

    public static void log(final String text)
    {
        instance.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView logBox = (TextView)instance.findViewById(R.id.textview_log);
                Calendar cal = Calendar.getInstance();
                logBox.append("  "+ text + "\n");//cal.get(Calendar.MINUTE)+ ":" +cal.get(Calendar.SECOND)+ ":" + cal.get(Calendar.MILLISECOND) +
            }
        });
    }
    public static void setDistance(double d)
    {
        instance.distance = d;

        instance.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView logBox = (TextView)instance.findViewById(R.id.textview_sum);
                logBox.setText(String.format("Distance = %f (m)", instance.distance));
            }
        });
    }
    public static void clearlog()
    {
        instance.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView logBox = (TextView)instance.findViewById(R.id.textview_log);
                logBox.setText("");
                logBox = (TextView)instance.findViewById(R.id.textview_sum);
                logBox.setText("");
            }
        });
    }

    public static void alertmsg(final String msg){
        Toast.makeText(instance, msg, Toast.LENGTH_SHORT).show();
    }
    public static void viewMyInfo(final String text)
    {
        instance.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView logBox = (TextView)instance.findViewById(R.id.my_info);
                logBox.setText("  "+ text );
            }
        });
    }

    public static String getIpAddress(){
        WifiManager wifiManager = (WifiManager) instance.getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();
        return String.format("%d.%d.%d.%d", (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));
    }

    public void  getConnection_Status(){
        thisIP = getIpAddress();
        if (thisIP.equals("0.0.0.0")){
            viewMyInfo("WIFI Connection Failed! ");
            setClientButtonState(false);
            setServerButtonState(false);
        }else{
            viewMyInfo("IP = [" + thisIP + "]");
        }
    }

    private void create_Server(){
        connectServer = new ConnectServer(thisIP);
        if(serverThread != null){
            serverThread.stopThread();

        }
        serverThread = new Server();
        serverThread.start();
        COM_TYPE = 0;
    }

    private void create_Client(){
        if (findServer != null)
            findServer.interrupt();
        findServer = new FindServer();
        findServer.start();
        COM_TYPE = 1;
    }

    private void return_to_main(){
        if (findServer != null) {
            findServer.interrupt();
            findServer = null;
        }
        if (clientThread != null) {
            clientThread.interrupt();
            clientThread = null;
        }
        if (serverThread != null) {
            serverThread.stopRecording();
            serverThread.stopThread();
            serverThread = null;
        }

        thisIP = getIpAddress();
        if (thisIP.equals("0.0.0.0")){
            setClientButtonState(false);
            setServerButtonState(false);
        }else{
            setClientButtonState(true);
            setServerButtonState(true);
        }

        removeServerList();
        clearlog();
        viewMyInfo("IP = [" + thisIP + "]");

    }

    public static void setServerButtonState(final boolean state)
    {
        instance.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Button button_server = (Button) instance.findViewById(R.id.button_server);
                button_server.setEnabled(state);
            }
        });
    }
    public static void setClientButtonState(final boolean state)
    {
        instance.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Button button_client = (Button) instance.findViewById(R.id.button_client);
                button_client.setEnabled(state);
            }
        });
    }

    public static void setReturnButtonState(final boolean state)
    {
        instance.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Button button = (Button) instance.findViewById(R.id.button_return);
                button.setEnabled(state);
            }
        });
    }

    public static void setRadioButtonState(final boolean state)
    {
        instance.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                RadioGroup button = (RadioGroup) instance.findViewById(R.id.server_list);
                button.setEnabled(state);
            }
        });
    }

    public static void addServerList(final String text, final int id){
        instance.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                RadioGroup radioGroup = (RadioGroup) instance.findViewById(R.id.server_list);
                RadioButton radioButton = new RadioButton(instance);
                radioButton.setText("Server_" + id + " [" + text + "]");
                radioButton.setId(id + 30000);
                radioButton.setClickable(true);
                radioButton.setOnClickListener(new View.OnClickListener(){
                    public void onClick(View v) {
                        boolean  checked = ((RadioButton) v).isChecked();
                        if (checked){
                            if (instance.clientThread != null)
                                instance.clientThread.interrupt();
                            instance.clientThread = new Client();
                            InetAddress address = null;
                            try {
                                address = InetAddress.getByName(text);
                            } catch (UnknownHostException e) {
                                e.printStackTrace();
                            }
                            instance.clientThread.setTarget(address);
                            instance.clientThread.start();
//                            connectToServer(text, id);
                        }
                    }
                });
                radioGroup.addView(radioButton);
            }
        });
    }

    public static void removeServerList(){
        instance.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                RadioGroup radioGroup = (RadioGroup) instance.findViewById(R.id.server_list);
                radioGroup.removeAllViews();
            }
        });
    }

    public void onRadioButtonClicked(View v)
    {
        boolean  checked = ((RadioButton) v).isChecked();
        if (checked){
            int id = v.getId();
            RadioButton rb = (RadioButton) findViewById(id);
            String txt = rb.getText().toString();
            int port = id;
            String ip = txt;
            connectToServer(ip, port);
        }
    }

    public static void connectToServer(String ip, int port){
        try {
            InetAddress serverAddr = InetAddress.getByName(ip);
            connectClient = new ConnectClient(serverAddr, port);
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    public void init_player_recorder(){
        Utils.initPlayer(params.sampleRate, 0);
        Utils.initRecorder(params.sampleRate);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ID_MULTIPLE_PERMISSIONS: {

                Map<String, Integer> perms = new HashMap<>();
                // Initialize the map with both permissions
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.ACCESS_WIFI_STATE, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.RECORD_AUDIO, PackageManager.PERMISSION_GRANTED);
                // Fill with actual results from user
                if (grantResults.length > 0) {
                    for (int i = 0; i < permissions.length; i++)
                        perms.put(permissions[i], grantResults[i]);
                    // Check for both permissions
                    if (perms.get(Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED
                            && perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                            && perms.get(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
//                        Log.d(TAG, "sms & location services permission granted");
                        // process the normal flow
//                        Intent i = new Intent(MainActivity.this,);
//                        startActivity(i);
                        finish();
                        init_player_recorder();
                        //else any one or both the permissions are not granted
                    } else {
//                        Log.d(TAG, "Some permissions are not granted ask again ");
                        //permission is denied (this is the first time, when "never ask again" is not checked) so ask again explaining the usage of permission
//                        // shouldShowRequestPermissionRationale will return true
                        //show the dialog or snackbar saying its necessary and try again otherwise proceed with setup.
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_WIFI_STATE)
                                || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                            showDialogOK("Service Permissions are required for this app",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            switch (which) {
                                                case DialogInterface.BUTTON_POSITIVE:
                                                    checkAndRequestPermissions();
                                                    break;
                                                case DialogInterface.BUTTON_NEGATIVE:
                                                    // proceed with logic by disabling the related features or quit the app.
                                                    finish();
                                                    break;
                                            }
                                        }
                                    });
                        }
                        //permission is denied (and never ask again is  checked)
                        //shouldShowRequestPermissionRationale will return false
                        else {
                            explain("You need to give some mandatory permissions to continue. Do you want to go to app settings?");
                            //                            //proceed with logic by disabling the related features or quit the app.
                        }
                    }
                }
            }
        }

    }
    private void showDialogOK(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", okListener)
                .create()
                .show();
    }
    private void explain(String msg){
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setMessage(msg)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        //  permissionsclass.requestPermission(type,code);
                        startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:com.exampledemo.parsaniahardik.marshmallowpermission")));
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        finish();
                    }
                });
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        connectServer.tearDown();
        connectClient.tearDown();
        if (findServer != null) {
            findServer.interrupt();
            findServer = null;
        }
        if (clientThread != null) {
            clientThread.interrupt();
            clientThread = null;
        }
        if (serverThread != null) {
            serverThread.stopThread();
            serverThread = null;
        }
    }
}
