package com.example.uwcapstone;

import com.sonicmeter.android.multisonicmeter.Params;
import com.sonicmeter.android.multisonicmeter.Utils;

class Sender extends Thread{

    public static final String HELPER = "Helper";
    public static final String SEEKER = "Seeker";
    public static final String ACK = "ACK";
    public static final String SOS = "SOS";
    public static final int SOS_SEED = 1500;
    public static final int ACK_SEED = 2500;

    private String mRole;
    private volatile boolean mExit;

    Sender(String role) {
        this.mRole = role;
        mExit = false;
    }

    @Override
    public void run() {

        if(!mRole.equals("Seeker") && !mRole.equals("Helper")) {
            MainActivity.log("INVALID ROLE");
            return;
        }


        // keep sending SOS
        MainActivity.log(String.format("%s sending: %s", mRole, mRole.equals("Helper") ? "ACK" : "SOS"));

        int seed = mRole.equals(HELPER) ? ACK_SEED : SOS_SEED;

        byte[] playSequence = Utils.convertShortsToBytes( Utils.generateActuateSequence_seed(Params.warmSequenceLength, Params.signalSequenceLength, Params.sampleRate, seed, Params.noneSignalLength));

        int number = 0;
        while(!mExit) {
            MainActivity.log(String.format("Sending %s %d.", mRole.equals(HELPER) ? ACK : SOS, number++));
            Utils.play(playSequence);
            try {
                sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    void stopThread() {
        mExit = true;
    }

}
