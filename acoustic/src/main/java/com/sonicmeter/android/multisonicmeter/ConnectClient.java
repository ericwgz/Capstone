package com.sonicmeter.android.multisonicmeter;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import static android.content.ContentValues.TAG;

public class ConnectClient {

    private InetAddress mAddress;
    private int PORT;

    private Socket mSocket;

    private final String CLIENT_TAG = "Client";

    private Thread mSendThread;
    private Thread mRecThread;


    public ConnectClient(InetAddress address, int port){
        Log.d(CLIENT_TAG, "Creating Client Connection");
        this.mAddress = address;
        this.PORT = port;

        mSendThread = new Thread(new SendingThread("Create Client"));
        mSendThread.start();
    }

    public void tearDown() {

        try {
            mSocket.close();
            mSendThread.interrupt();
            mRecThread.interrupt();
        } catch (Exception e) {
            Log.e(TAG, "Error when closing server DatagramSocket.");
        }
    }

    class SendingThread implements Runnable {

        String message;

        public SendingThread(String message) {
            this.message = message;
        }

        @Override
        public void run() {
            try {
                if (mSocket == null) {
                    mSocket = new Socket(mAddress, PORT);
                    Log.d(CLIENT_TAG, "Client-side socket initialized.");

                } else {
                    Log.d(CLIENT_TAG, "Socket already initialized. skipping!");
                }

                mRecThread = new Thread(new ReceivingThread());
                mRecThread.start();

            } catch (UnknownHostException e) {
                Log.d(CLIENT_TAG, "Initializing socket failed, UHE", e);
                return;
            } catch (IOException e) {
                Log.d(CLIENT_TAG, "Initializing socket failed, IOE.", e);
                return;
            }

            while (true) {
                try {
                    String msg = message;
                    PrintWriter out = new PrintWriter(
                            new BufferedWriter(
                                    new OutputStreamWriter(mSocket.getOutputStream())), true);
                    out.println(msg);
                    out.flush();
                    MainActivityAcoustic.log(msg);
//                    updateMessages(msg, true);
                } catch (IOException e){
                    e.printStackTrace();
                    return;
                }
            }
        }
    }

    class ReceivingThread implements Runnable {

        @Override
        public void run() {

            BufferedReader input;
            try {
                input = new BufferedReader(new InputStreamReader(
                        mSocket.getInputStream()));
                while (!Thread.currentThread().isInterrupted()) {

                    String messageStr = null;
                    messageStr = input.readLine();
                    if (messageStr != null) {
                        Log.d(CLIENT_TAG, "Read from the stream: " + messageStr);
                        MainActivityAcoustic.log(messageStr);
                    } else {
                        Log.d(CLIENT_TAG, "The nulls! The nulls!");
                        break;
                    }
                }
                input.close();

            } catch (IOException e) {
                Log.e(CLIENT_TAG, "Server loop error: ", e);
            }
        }
    }



}
