package com.sonicmeter.android.multisonicmeter;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import static android.content.ContentValues.TAG;

class NetworkThread extends Thread {

    private int port = 4955;
    private DatagramSocket receiverSocket;
    private DatagramSocket senderSocket;

    private InetAddress target = null;
    private String key = "";
    private String value = "";


    private static NetworkThread instance;
    private static DataStruct dataStruct;
    private static HashMap<String, DataStruct> clientMap;//client list and datastruct


    private static CountDownLatch lock = new CountDownLatch(1);

    NetworkThread() {
        super();
    }

    @Override
    public void run() {
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

        instance = this;
        dataStruct = new DataStruct();
        clientMap = new HashMap<>();

        try {
            if(receiverSocket == null){
                receiverSocket = new DatagramSocket(null);
                receiverSocket.setReuseAddress(true);
                receiverSocket.setBroadcast(true);
                receiverSocket.bind(new InetSocketAddress(port));
            }
            if(senderSocket == null){
                senderSocket = new DatagramSocket();
            }

            String localAddress = MainActivityAcoustic.getIpAddress();

            while(!Thread.currentThread().isInterrupted()) {
                byte[] buf = new byte[256];

//                if (target != null) continue;//wait the other message
                // receive request
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                receiverSocket.receive(packet);     //this code blocks the program flow

                if (packet.getAddress().getHostAddress().equals(localAddress)) continue;

                String msg = new String(packet.getData()).trim();


                final JSONObject jsondata;
                jsondata = new JSONObject(msg);

                try {
                    target = packet.getAddress();
                    String ip = target.getHostAddress();
                    key = jsondata.getString("key");
                    value = jsondata.getString("value");
                    MainActivityAcoustic.log("UDP<=[" + target.getHostAddress() + "] \"" + key +"="+ value + "\"");

                    if (clientMap.containsKey(ip)){
                        dataStruct.updateValue(key, value);
                        clientMap.get(ip).updateValue(key, value);
                    }
                    else {
                        dataStruct.updateValue(key, value);
                        DataStruct dataStruct1 = new DataStruct();
                        dataStruct1.updateValue(key, value);
                        clientMap.put(ip, dataStruct1);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Unable to get request");
                }

            }

            MainActivityAcoustic.log("Network thread shutdown");

        } catch (Exception e) {
            e.printStackTrace();
            MainActivityAcoustic.log("Netwrok thread exception: " + e.getMessage());
        } finally {
            if(receiverSocket != null){
                receiverSocket.close();
                MainActivityAcoustic.log("Receiver socket closed");
            }
            if(senderSocket != null){
                senderSocket.close();
                MainActivityAcoustic.log("Sender socket closed");
            }
        }
    }

    static synchronized void send(String key, String value, InetAddress target)
    {
        final JSONObject jsondata;
        jsondata = new JSONObject();

        try {
            if (target == null){
                MainActivityAcoustic.log("Target Address is null");
                return;
            }
            if(instance.senderSocket == null){
                instance.senderSocket = new DatagramSocket();
            }


            jsondata.put("key", key);
            jsondata.put("value", value);

            byte[] buf = jsondata.toString().getBytes();
            DatagramPacket packet = new DatagramPacket(buf, buf.length, target, instance.port);
            instance.senderSocket.send(packet);

            assert target != null;
            MainActivityAcoustic.log("UDP=>[" + target.getHostAddress() + "] \"" + key + "=" + value + "\"");
        } catch (IOException e) {
            MainActivityAcoustic.log("UDP send exception: " + e.getMessage());
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "JSON Error");
            MainActivityAcoustic.log("JSON exception: " + e.getMessage());
        }
    }

    static long waitKeyValue(String key)
    {
        if (dataStruct == null)
            dataStruct = new DataStruct();
        switch (key)
        {
            case "seed":
                MainActivityAcoustic.log("Waiting for network value: seed");
                while (dataStruct.seed == -1) Utils.sleep(1);
                return dataStruct.seed;

            case "tc0":
                MainActivityAcoustic.log("Waiting for network value: tc0");
                while (dataStruct.tc0 == -1) Utils.sleep(1);
                return dataStruct.tc0;

            case "tc1":
                MainActivityAcoustic.log("Waiting for network value: tc1");
                while (dataStruct.tc1 == -1) Utils.sleep(1);
                return dataStruct.tc1;

            case "tc2":
                MainActivityAcoustic.log("Waiting for network value: tc2");
                while (dataStruct.tc2 == -1) Utils.sleep(1);
                return dataStruct.tc2;

            case "ts0":
                MainActivityAcoustic.log("Waiting for network value: ts0");
                while (dataStruct.ts0 == -1) Utils.sleep(1);
                return dataStruct.ts0;

            case "ts1":
                MainActivityAcoustic.log("Waiting for network value: ts1");
                while (dataStruct.ts1 == -1) Utils.sleep(1);
                return dataStruct.ts1;

            case "ts2":
                MainActivityAcoustic.log("Waiting for network value: ts2");
                while (dataStruct.ts2 == -1) Utils.sleep(1);
                return dataStruct.ts2;

            case "start":
                MainActivityAcoustic.log("Waiting for network value: start");
                while (dataStruct.start == -1) Utils.sleep(1);
                return dataStruct.start;

            case "ready":
                MainActivityAcoustic.log("Waiting for network value: ready");
                while (dataStruct.ready == -1) Utils.sleep(1);
                return dataStruct.ready;

            case "play":
                MainActivityAcoustic.log("Waiting for network value: play");
                while (dataStruct.play == -1) Utils.sleep(1);
                return dataStruct.play;

            case "calc":
                MainActivityAcoustic.log("Waiting for network value: calc");
                while (dataStruct.calc == -1) Utils.sleep(1);
                return dataStruct.calc;
        }

        return -1;

    }

    static long waitKeyValue(String key, String ip)
    {
        while (!clientMap.containsKey(ip)) Utils.sleep(1);
        if(clientMap.containsKey(ip)) {
            DataStruct dataStruct1 = clientMap.get(ip);
            switch (key) {
                case "seed":
                    MainActivityAcoustic.log("Waiting for network value: seed");
                    while (dataStruct1.seed == -1) Utils.sleep(1);
                    return dataStruct1.seed;

                case "tc0":
                    MainActivityAcoustic.log("Waiting for network value: tc0");
                    while (dataStruct1.tc0 == -1) Utils.sleep(1);
                    return dataStruct1.tc0;

                case "tc1":
                    MainActivityAcoustic.log("Waiting for network value: tc1");
                    while (dataStruct1.tc1 == -1) Utils.sleep(1);
                    return dataStruct1.tc1;

                case "tc2":
                    MainActivityAcoustic.log("Waiting for network value: tc2");
                    while (dataStruct1.tc2 == -1) Utils.sleep(1);
                    return dataStruct1.tc2;

                case "ts0":
                    MainActivityAcoustic.log("Waiting for network value: ts0");
                    while (dataStruct1.ts0 == -1) Utils.sleep(1);
                    return dataStruct1.ts0;

                case "ts1":
                    MainActivityAcoustic.log("Waiting for network value: ts1");
                    while (dataStruct1.ts1 == -1) Utils.sleep(1);
                    return dataStruct1.ts1;

                case "ts2":
                    MainActivityAcoustic.log("Waiting for network value: ts2");
                    while (dataStruct1.ts2 == -1) Utils.sleep(1);
                    return dataStruct1.ts2;

                case "start":
                    MainActivityAcoustic.log("Waiting for network value: start");
                    while (dataStruct1.start == -1) Utils.sleep(1);
                    return dataStruct1.start;

                case "ready":
                    MainActivityAcoustic.log("Waiting for network value: ready");
                    while (dataStruct1.ready == -1) Utils.sleep(1);
                    return dataStruct1.ready;

                case "play":
                    MainActivityAcoustic.log("Waiting for network value: play");
                    while (dataStruct1.play == -1) Utils.sleep(1);
                    return dataStruct1.play;
                case "calc":
                    MainActivityAcoustic.log("Waiting for network value: calc");
                    while (dataStruct1.calc == -1) Utils.sleep(1);
                    return dataStruct1.calc;
            }
        }

        return -1;

    }

    static void resetDataStruct() {
        dataStruct = new DataStruct();

    }

    static void resetDataMap(String ip){
        DataStruct data = new DataStruct();
        clientMap.put(ip, data);
    }

    static void clearClientMap(){
        clientMap.clear();
    }

    static void resetDataCache() {
        instance.key = "";
        instance.value = "";
        instance.target = null;
    }

    public InetAddress getTarget(){
        return instance.target;
    }
    public void setTarget(InetAddress mtarget){
        target = mtarget;
    }
    static void shutdown() {
        instance.interrupt();
        instance = null;
    }

    public void releaseSocket(){
        if(receiverSocket != null){
            receiverSocket.close();
            receiverSocket = null;
        }
        if(senderSocket != null){
            senderSocket.close();
            senderSocket = null;
        }
    }
}

