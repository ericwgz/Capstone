package com.example.uwcapstone;

import android.media.AudioRecord;
import android.provider.ContactsContract;
import android.util.Log;

import com.sonicmeter.android.multisonicmeter.TrackRecord;
import com.sonicmeter.android.multisonicmeter.Utils;

class Receiver extends Thread{

    public static final String HELPER = "Helper";
    public static final String SEEKER = "Seeker";
    public static final String ACK = "ACK";
    public static final String SOS = "SOS";
    public static final String TAG = Receiver.class.getSimpleName();

    private static TrackRecord mAudioTrack;
    private static RecordThread mRecordThread;

    private final String mRole;
    private final String mMsg;
    private boolean mExit;
    private short[] idealModel;
    private short[] contrastModel;

    Receiver(String role) {
        mRole = role;
        mMsg = mRole.equals(HELPER) ? SOS : ACK;
        mExit = false;

        mAudioTrack = new TrackRecord();
        mRecordThread = new RecordThread();

        if(mRole.equals(HELPER)) {
            idealModel = DataFile.CDMAsos;
            contrastModel = DataFile.CDMAack;
        } else {
            idealModel = DataFile.CDMAack;
            contrastModel = DataFile.CDMAsos;
        }
    }

    @Override
    public void run() {
        if (!mRole.equals(HELPER) && !mRole.equals(SEEKER)) {
            MainActivity.log("INVALID ROLE");
            return;
        }

        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

        //Start recording check if received SOS
        MainActivity.log(String.format("Searching %s.", mMsg));

        double threshold = 50;

        Utils.initConvolution(DataFile.CDMAack.length);

        mRecordThread.start();

        while (!mExit) {
            double similarity = receivedAudioSimilarity();
            MainActivity.log(String.format("%s similarity : %f.", mMsg, similarity));
            if(receivedAudioSimilarity() > threshold) {
                break;
            }
        }

        mRecordThread.stopRecord();

        if(mRole.equals(SEEKER)) {
            MainActivity.mClientThread.stopThread();
        }

        if(!mExit) {
            MainActivity.log(String.format("%s received %s.", mRole, mMsg));
        }

        if (mRole.equals(HELPER)) {
            // Start sender thread to send ACK
            MainActivity.log("Helper sending ACK.");
            Sender senderThread = new Sender(HELPER);
            senderThread.start();
        }  
    }

    private static class RecordThread extends Thread {
        boolean bContinue = true;

        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

            if (Utils.getRecorderHandle() == null)
                Utils.initRecorder(DataFile.sampleRate);
            int minBufferSize = Utils.getMinBufferSize(DataFile.sampleRate);
            if (Utils.getRecorderHandle().getState() == AudioRecord.STATE_UNINITIALIZED) {
                MainActivity.log("Record Fail, AudioRecord has not been initialized.");
            }
            try {
                Utils.getRecorderHandle().startRecording();
            } catch (Throwable x) {
                MainActivity.log("Error recording: " + x.getMessage());
            }
            while (bContinue && Utils.getRecorderHandle().getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                try {
                    short[] buffer = Utils.recordBuffer(minBufferSize);
                    mAudioTrack.addSamples(buffer);
                } catch (Exception e) {
                    e.printStackTrace();
                    MainActivity.log("Recording Failed " + e.getMessage());
                    stopRecord();
                    break;
                }
            }
            stopRecord();
        }

        void stopRecord() {
            bContinue = false;
            //Stop recording, release AudioRecord instance
            if (Utils.getRecorderHandle() != null) {
                if (Utils.getRecorderHandle().getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    Utils.getRecorderHandle().stop();
                }
                if (Utils.getRecorderHandle().getState() == AudioRecord.STATE_INITIALIZED) {
                    Utils.getRecorderHandle().release();
                }
            }
        }
    }

    private synchronized static short[] getRecordedSequence(int length) {
        return mAudioTrack.getSamples(length);
    }

    void stopThread() {
        mExit = true;
        MainActivity.mClientThread.stopThread();
    }

    private double receivedAudioSimilarity() {
        try {
            sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (!mRole.equals(HELPER) && !mRole.equals(SEEKER)) {
            return 0;
        }
        short[] recordedSequence = getRecordedSequence(DataFile.recordSampleLength * 6);
        if(recordedSequence == null || recordedSequence.length < DataFile.recordSampleLength * 6) {
            return 0;
        }

        Utils.setFilter_convolution(idealModel);
        double similarity = Utils.estimate_max_similarity(recordedSequence, idealModel ,0, recordedSequence.length);


        Utils.setFilter_convolution(contrastModel);
        double contrast = Utils.estimate_max_similarity(recordedSequence, contrastModel ,0, recordedSequence.length);

        Log.d(TAG, String.format("searching for %s, similarity: %f, contrast: %f", mMsg, similarity, contrast));
        if(contrast > similarity) {
            return 0;
        }

        return similarity;
    }
}
