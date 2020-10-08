package com.sonicmeter.android.multisonicmeter;


import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

import static android.content.ContentValues.TAG;

public class FindServer extends Thread {
    private String thisIP;
    private int ServerPORT = 4954;
    private DatagramSocket receiverSocket = null;

    private ArrayList<String> serverIP_list;

    public void FindServer(){
        this.serverIP_list = new ArrayList<>();
        MainActivityAcoustic.removeServerList();
    }

    public void tearDown(){
        receiverSocket.close();
        this.interrupt();
    }

    @Override
    public void run() {
        try {
            this.serverIP_list = new ArrayList<>();
            MainActivityAcoustic.removeServerList();
            MainActivityAcoustic.clearlog();
            MainActivityAcoustic.setClientButtonState(false);
            MainActivityAcoustic.setServerButtonState(false);
            if(receiverSocket == null){
                receiverSocket = new DatagramSocket(null);
                receiverSocket.setReuseAddress(true);
                receiverSocket.setBroadcast(true);
                receiverSocket.bind(new InetSocketAddress(ServerPORT));
                receiverSocket.setSoTimeout(3000);
            }
            int counter = 0;
            while (!Thread.currentThread().isInterrupted()) {
                byte[] buf = new byte[256];
                // receive request
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                try {
                    receiverSocket.receive(packet);
                }catch (SocketTimeoutException e){
                    MainActivityAcoustic.setClientButtonState(true);
                    MainActivityAcoustic.setServerButtonState(true);
                    receiverSocket.close();
                    return;
                }

                if (packet.getAddress().getHostAddress().equals(MainActivityAcoustic.getIpAddress())) continue;
                InetAddress senderAddr = packet.getAddress();
                String senderIP = senderAddr.getHostAddress();

                String msg = new String(packet.getData()).trim();

                if (msg.startsWith("SERVER_HERE=")){
                    MainActivityAcoustic.setServerButtonState(false);
//                    int port = Integer.parseInt(msg.replace("SERVER_HERE=", ""));
                    if (!serverIP_list.contains(senderIP)){
                        serverIP_list.add(senderIP);
                        int id = serverIP_list.indexOf(senderIP);
                        //Add the server to the MainActivity's server_list
                        MainActivityAcoustic.addServerList(senderIP, id);
                    }

                }

            }
        }
        catch (SocketException e){
            e.printStackTrace();
            MainActivityAcoustic.setClientButtonState(true);
        }
        catch (IOException e) {
            Log.e(TAG, "Error creating ServerSocket: ", e);
            e.printStackTrace();
            MainActivityAcoustic.setClientButtonState(true);
        }
        finally {
            MainActivityAcoustic.setClientButtonState(true);
            receiverSocket.close();
        }
    }
}
