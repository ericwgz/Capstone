package com.sonicmeter.android.multisonicmeter;


import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import static android.content.ContentValues.TAG;

public class ConnectServer {
    private int ServerPORT = 4954;
    private String ServerIP = null;
    private InetAddress senderAddr;
    Thread mThread = null;
    Thread mAdvertiseThread = null;

    ServerSocket mServerSocket = null;
    private Socket mSocket;
    private int mPort = -1;

    private ConnectClient mConnectionClient;


    public ConnectServer(String serverIP) {
        ServerIP = serverIP;


        mAdvertiseThread = new Thread(new AdvertiseThread());
        mAdvertiseThread.start();
        MainActivityAcoustic.setServerButtonState(false);
        MainActivityAcoustic.setClientButtonState(false);
        MainActivityAcoustic.viewMyInfo("IP = ["+ServerIP+"]  Server started!");
        MainActivityAcoustic.removeServerList();

    }

    public int getLocalPort() {
        return mPort;
    }

    public void setLocalPort(int port) {
        mPort = port;
    }

    private synchronized void setSocket(Socket socket) {
        Log.d(TAG, "setSocket being called.");
        if (socket == null) {
            Log.d(TAG, "Setting a null socket.");
        }
        if (mSocket != null) {
            if (mSocket.isConnected()) {
                try {
                    mSocket.close();
                } catch (IOException e) {
                    // TODO(alexlucas): Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        mSocket = socket;
    }

    private Socket getSocket() {
        return mSocket;
    }


    public void tearDown() {
        mThread.interrupt();
        mAdvertiseThread.interrupt();
        try {
            if (mServerSocket != null)
                mServerSocket.close();
            if (mSocket != null)
                mSocket.close();
        } catch (Exception e) {
            Log.e(TAG, "Error when closing server DatagramSocket.");
        }
    }

    class AdvertiseThread implements Runnable{
        @Override
        public void run() {
            DatagramSocket socket = null;
            try {
                if (socket == null) {
                    socket = new DatagramSocket();
                }
            }catch (Exception e){
                e.printStackTrace();
            }

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String msg = "SERVER_HERE=";//+getLocalPort();
                    byte[] buf1 = msg.getBytes();
                    DatagramPacket packet1 = new DatagramPacket(buf1, buf1.length, InetAddress.getByName("255.255.255.255"), ServerPORT);
                    socket.send(packet1);

                    Thread.sleep(100);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e){
                    e.printStackTrace();
                }

            }
        }
    }

    class ServerThread implements Runnable {

        @Override
        public void run() {

            try {
                // Since discovery will happen via Nsd, we don't need to care which port is
                // used.  Just grab an available one  and advertise it via Nsd.
                mServerSocket = new ServerSocket(0);
                setLocalPort(mServerSocket.getLocalPort());

                //Send the Server's exist to the Clients with broadcast
                mAdvertiseThread = new Thread(new AdvertiseThread());
                mAdvertiseThread.start();

                while (true) {//!Thread.currentThread().isInterrupted()
                    Log.d(TAG, "ServerSocket Created, awaiting connection");
                    setSocket(mServerSocket.accept());
                    Log.d(TAG, "Connected.");
                    if (mConnectionClient == null) {
                        int port = mSocket.getPort();
                        InetAddress address = mSocket.getInetAddress();
                        mConnectionClient = new ConnectClient(address, port);
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Error creating ServerSocket: ", e);
                e.printStackTrace();
            }
        }
    }

}
