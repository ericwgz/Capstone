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
    private boolean mPauseReceive;
    private short[] idealModel;
    private short[] contrastModel;

    Receiver(String role) {
        mRole = role;
        mMsg = mRole.equals(HELPER) ? SOS : ACK;
        mExit = false;
        mPauseReceive = mRole.equals(SEEKER);

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

        Utils.initConvolution(DataFile.CDMAack.length);
        Utils.initRecorder(DataFile.sampleRate);
        if (Utils.getRecorderHandle().getState() == AudioRecord.STATE_UNINITIALIZED) {
            Log.d(TAG, "Record Fail, AudioRecord has not been initialized.");
        }
        try {
            Utils.getRecorderHandle().startRecording();
        } catch (Exception e) {
            Log.d(TAG, "Error recording: " + e.getMessage());
        }
        Log.d(TAG, "Start Record Thread, now AudioRecord status is"+Utils.getRecorderHandle().getState());

        mRecordThread.start();

        int count = 0;

        while (!mExit) {
            double similarity = receivedAudioSimilarity();
            if (mPauseReceive) {
                continue;
            }
            MainActivity.log(String.format("%s similarity : %f.", mMsg, similarity));
            if(similarity > DataFile.getThreshold()) {
                count++;
                if(count >= 3) {
                    break;
                }
            } else {
                count = 0;
            }
        }

        mRecordThread.stopRecord();

        if(!mExit) {
            MainActivity.log(String.format("%s received %s.", mRole, mMsg));

            if(mRole.equals(SEEKER)) {
                MainActivity.mClientThread.receivedACK();
            }

            if (mRole.equals(HELPER)) {
                // Start sender thread to send ACK
                MainActivity.log("Helper sending ACK.");
                MainActivity.mClientThread = new Sender(HELPER);
                MainActivity.mClientThread.start();
            }
        }
    }

    private static class RecordThread extends Thread {
        boolean bContinue = true;

        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

            int minBufferSize = Utils.getMinBufferSize(DataFile.sampleRate);

            while (bContinue) {
                if(Utils.getRecorderHandle() == null || Utils.getRecorderHandle().getRecordingState() == AudioRecord.STATE_UNINITIALIZED) {
                    if(Utils.getRecorderHandle() != null) {
                        Log.d(TAG, "run: AudioRecord status fail, current status:" + Utils.getRecorderHandle().getState());
                    }
                    Log.d(TAG, "run: AudioRecord fail.");
                    continue;
                }
                try {
                    short[] buffer = Utils.recordBuffer(minBufferSize);
                    mAudioTrack.addSamples(buffer);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d(TAG, "Recording Failed " + e.getMessage());
                    stopRecord();
                    break;
                }
            }
        }

        void stopRecord() {
            bContinue = false;
            Utils.releaseAudioRecord();
            if(Utils.getRecorderHandle() != null) {
                Log.d(TAG, "Stop Record Thread Fail, AudioRecord has not been set to null.");
            }
            Log.d(TAG, "Stop Record Thread.");
        }
    }

    private synchronized static short[] getRecordedSequence(int length) {
        return mAudioTrack.getSamples(length);
    }

    void stopThread() {
        mExit = true;
        if(mRole.equals(HELPER) && MainActivity.mClientThread != null) {
            MainActivity.mClientThread.stopThread();
        }
    }

    private double receivedAudioSimilarity() {
        if (mPauseReceive) {
            return 0;
        }
        try {
            sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (!mRole.equals(HELPER) && !mRole.equals(SEEKER)) {
            return 0;
        }
        short[] recordedSequence = getRecordedSequence(DataFile.recordSampleLength * 6);
//        if(recordedSequence == null || recordedSequence.length < DataFile.recordSampleLength * 6) {
//            return 0;
//        }

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

    public void pauseReceive() {
        mPauseReceive = true;
    }

    public void resumeReceive() {
        mPauseReceive = false;
    }
}
