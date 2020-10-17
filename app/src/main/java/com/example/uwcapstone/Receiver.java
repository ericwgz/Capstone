package com.example.uwcapstone;

import android.media.AudioRecord;

import com.sonicmeter.android.multisonicmeter.TrackRecord;
import com.sonicmeter.android.multisonicmeter.Utils;
import com.sonicmeter.android.multisonicmeter.Params;

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
    private Convolution mConvolution;

    Receiver(String role) {
        mRole = role;
        mMsg = mRole.equals(HELPER) ? SOS : ACK;
        mExit = false;

        mAudioTrack = new TrackRecord();
        mRecordThread = new RecordThread();

        short[] model;
        if (mRole.equals(HELPER)) {
            model = DataFile.CDMAsos;
        } else {
            model = DataFile.CDMAack;
        }
        mConvolution = new Convolution(model);
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

        double threshold = 20;

        mRecordThread.start();

        while (!mExit) {
            if(receivedAudioSimilarity() > threshold) {
                break;
            }
        }

        if (mExit) {
            mRecordThread.stopRecord();
            return;
        }

        MainActivity.log(String.format("%s received %s.", mRole, mMsg));

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
                Utils.initRecorder(Params.sampleRate);
            int minBufferSize = Utils.getMinBufferSize(Params.sampleRate);
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
    }

    private double receivedAudioSimilarity() {

        if (!mRole.equals(HELPER) && !mRole.equals(SEEKER)) {
            return 0;
        }

        short[] recordedSequence = getRecordedSequence(Params.recordSampleLength * 6);

        double similarity = mConvolution.estimate_max_similarity(recordedSequence, 0 ,recordedSequence.length);

        return similarity;
    }
}
