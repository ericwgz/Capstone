package com.example.uwcapstone;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonicmeter.android.multisonicmeter.TrackRecord;
import com.sonicmeter.android.multisonicmeter.Utils;
import com.sonicmeter.android.multisonicmeter.Params;

class Receiver extends Thread{

    public static final String TAG = Receiver.class.getSimpleName();
    private final String mRole;
    private boolean mExit;

    private static TrackRecord mAudioTrack;
    private static RecordThread mRecordThread;

    public Receiver(String role) {
        this.mRole = role;
        mExit = false;
    }

    @Override
    public void run() {
        if (!mRole.equals("Helper") && !mRole.equals("Seeker")) {
            MainActivity.log("INVALID ROLE");
            return;
        }

        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

        double threshold = 20;


        //Start recording check if received SOS
        MainActivity.log(String.format("Searching %s.", mRole.equals("Helper") ? "SOS" : "ACK"));

        while(!mExit) {
            if(isReceived(mRole, threshold)) {
                break;
            }
        }

        if(mExit) {
            return;
        }

        MainActivity.log(String.format("%s received %s.", mRole, mRole.equals("Helper") ? "SOS" : "ACK"));

        if(mRole.equals("Helper")) {

            // Start sender thread to send ACK
            MainActivity.log("Helper sending ACK.");
            Sender senderThread = new Sender("Helper");
            senderThread.start();
        }
    }

    private class RecordThread extends Thread {
        boolean bContinue = true;

        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

            if (Utils.getRecorderHandle() == null)
                Utils.initRecorder(Params.sampleRate);
            int minBufferSize = Utils.getMinBufferSize(Params.sampleRate);
            try {
                Utils.getRecorderHandle().startRecording();
            } catch (Throwable x) {
                MainActivity.log("Error recording: " + x.getMessage());
            }
            int i = 0;
            while (bContinue) {
                try {
                    short[] buffer = Utils.recordBuffer(minBufferSize);
                    mAudioTrack.addSamples(buffer);
                } catch (Exception e) {
                    e.printStackTrace();
                    MainActivity.log("Server Recording Failed " + e.getMessage());
                    Utils.getRecorderHandle().stop();
                    bContinue = false;
                    break;
                } finally {
//                    MainActivity.log("Server Recording Ended");
                }
            }
            try {
                Utils.getRecorderHandle().stop();
            } catch (Throwable x) {
                MainActivity.log("Error recording: " + x.getMessage());
            }
        }

        public void stopRecord() {
            bContinue = false;
        }

    }

    private synchronized static short[] getRecordedSequence(int length){
        return mAudioTrack.getSamples(length);
    }

    public void stopThread() {
        mExit = true;
    }

    private boolean isReceived(final String role, double threshold) {
        mAudioTrack = new TrackRecord();
        mRecordThread = new RecordThread();
        mRecordThread.start();

        try {
            sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        mRecordThread.stopRecord();
        short[] recordedSequence = getRecordedSequence(Params.recordSampleLength * 6);
        double similarity = Utils.estimate_max_similarity(recordedSequence, DataFile.CDMAsos, 0 ,recordedSequence.length);

        return similarity >= threshold;
    }
}
