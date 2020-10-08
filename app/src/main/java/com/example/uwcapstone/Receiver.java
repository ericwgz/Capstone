package com.example.uwcapstone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonicmeter.android.multisonicmeter.TrackRecord;
import com.sonicmeter.android.multisonicmeter.Utils;
import com.sonicmeter.android.multisonicmeter.Params;

class Receiver extends Thread{
    String role;
    boolean bContinue;
    boolean isReceived;
    private static Receiver instance;
    //Map<byte[], String> serverMap;
    //private short[] signalSequence;
    Map<String, Integer> tmp;
    //private short[] recordedSequence;
    int length;
    boolean exit;

    private static Params params = new Params();
//    private static DataFile cdmaCode = new DataFile();

    static TrackRecord audioTrack = new TrackRecord();
    private RecordThread recordThread;

    public Receiver(String role) {
        this.role = role;
        bContinue = true;
        exit = false;
        isReceived = false;

        length = Params.recordSampleLength * 6;
        tmp = new HashMap<>();

        tmp.put("SOS", 1500);
//        tmp.put("UnderGround", 200);
        tmp.put("ACK", 2500);
    }

    @Override
    public void run() {
        if (role.equals("Helper")) {
            // check if received SOS
            Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
            MainActivity.setHelperBtnState(false);
            MainActivity.setStopHelpBtnState(true);
            List<short[]> signalList = new ArrayList<>();
            List<String> msgList = new ArrayList<>();

//            signalList.add(Utils.generateSignalSequence_63(tmp.get("SOS")));
//            signalList.add(Utils.generateSignalSequence_63(tmp.get("ACK")));
            signalList.add(DataFile.CDMAsos);
            signalList.add(DataFile.CDMAack);
            msgList.addAll(tmp.keySet());

            try {
                //Start recording check if received SOS
                MainActivity.log("Server starts to record");
                recordThread = new RecordThread();
                recordThread.start();

                while (!exit) {
                    Utils.sleep(500);
                    short[] recordedSequence = getRecordedSequence(params.recordSampleLength * 6);
                    List<Double> similarityList = new ArrayList();

                    for (int i = 0; i < signalList.size(); i++) {
                        // set matched filter
                        Utils.setFilter_convolution(signalList.get(i));
                        double similarity = Utils.estimate_max_similarity(recordedSequence, signalList.get(i), 0 ,recordedSequence.length);
                        similarityList.add(similarity);
                    }

                    double max = - Math.exp(100);
                    double threshold = 20;
                    int index = -1;
                    for (int i = 0; i < similarityList.size(); i++) {
                        if (similarityList.get(i) > max) {
                            max = similarityList.get(i);
                            index = i;
                        }
                    }

                    if (max < threshold || (index != -1 && msgList.get(index).equals("Received"))) {
                        if (max < threshold) {
                            MainActivity.log("max similarity: " + max + " < threshold: " + threshold + "; value invalid.");
                        } else {
                            MainActivity.log("Decoded message is " + msgList.get(index));
                        }

                        recordThread.stopRecord();
                        recordThread.interrupt();
                        Utils.sleep(500);
                        audioTrack = new TrackRecord();
                        recordThread = new RecordThread();
                        recordThread.start();
                        continue;
//                    Server newServerThread =new Server();
//                    newServerThread.start();
//                    exit = true;
                    }

                    if(index != -1) {
                        String msg = msgList.get(index);
                        MainActivity.decodedMsg(msg);

//                    if (msg.equals("Received")) {
//                        isReceived = true;
//                    }

                        MainActivity.log("SOS deviation: "+ similarityList.get(0));
//                    MainActivity.log("Underground deviation: "+ similarityList.get(1));
                        MainActivity.log("ACK deviation: "+ similarityList.get(1));

                        exit = true;
                        recordThread.stopRecord();

//                    if (role.equals("Receiver")) {
                        Sender senderThread = new Sender("Helper");
                        senderThread.start();
                        Utils.sleep(10000);
                        senderThread.stopThread();
//                    }
                    }
                    // Start sender thread to send ACK
                }

            } catch (Exception e) {
                MainActivity.log("Error!"+e.getMessage());
            }
        } else if (role.equals("Seeker")) {
            // check if received ACK

        } else {
            MainActivity.log("INVALID ROLE");
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
                    audioTrack.addSamples(buffer);
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

        public void stopRecord(){
            bContinue = false;
        }
    }

    synchronized static short[] getRecordedSequence(int length){
        return audioTrack.getSamples(length);
    }

    public void stopRecording(){
        instance.recordThread.stopRecord();
    }

    public void stopThread(){
        exit = true;
    }
}
