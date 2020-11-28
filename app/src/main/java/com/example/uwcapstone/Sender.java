package com.example.uwcapstone;

import com.sonicmeter.android.multisonicmeter.Utils;

class Sender extends Thread {

    public static final String HELPER = "Helper";
    public static final String SEEKER = "Seeker";
    public static final String ACK = "ACK";
    public static final String SOS = "SOS";
    public static final int SOS_SEED = 1500;
    public static final int ACK_SEED = 2500;

    private String mRole;
    private String mMsg;
    private volatile boolean mExit;
    private long mStartTime;
    private long mEndTime;

    Sender(String role) {
        mRole = role;
        mMsg = mRole.equals(HELPER) ? ACK : SOS;
        mExit = false;
        mStartTime = System.currentTimeMillis();
    }

    @Override
    public void run() {

        if(!mRole.equals("Seeker") && !mRole.equals("Helper")) {
            MainActivity.log("INVALID ROLE");
            return;
        }

        if (mRole.equals(SEEKER)) {
            // Start receiver thread to search ACK
            MainActivity.mReceiverThread = new Receiver(SEEKER);
            MainActivity.mReceiverThread.start();
        }


        // keep sending SOS
        MainActivity.log(String.format("%s sending: %s", mRole, mMsg));

        int seed = mRole.equals(HELPER) ? ACK_SEED : SOS_SEED;

        byte[] playSequence = Utils.convertShortsToBytes(Utils.generateActuateSequence_seed(DataFile.warmSequenceLength, DataFile.signalSequenceLength, DataFile.sampleRate, seed, DataFile.noneSignalLength));

        int number = 0;
        while(!mExit) {
            if(number == 0) {
                MainActivity.log(String.format("Sending %s", mMsg));
            }
            number++;
            if(mRole.equals(SEEKER) && number >= 20) {
                number = 0;

                if(MainActivity.mReceiverThread != null) {
                    MainActivity.mReceiverThread.resumeReceive();
                }
                try {
                    sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(MainActivity.mReceiverThread != null) {
                    MainActivity.mReceiverThread.pauseReceive();
                }
            }
            Utils.play(playSequence);
        }
    }

    void stopThread() {
        mExit = true;
        if(mRole.equals(SEEKER) && MainActivity.mReceiverThread != null) {
            MainActivity.mReceiverThread.stopThread();
        }
    }

    public void receivedACK() {
        mExit = true;
        mEndTime = System.currentTimeMillis();
        MainActivity.log(String.format("Total time usage: %f second.", (mEndTime - mStartTime) * 1.0 / 1000 ));
    }
}
