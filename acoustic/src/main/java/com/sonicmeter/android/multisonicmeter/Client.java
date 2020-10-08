package com.sonicmeter.android.multisonicmeter;

import java.net.InetAddress;
import java.util.Calendar;

class Client extends Thread {

    private short[] signalSequence;
    private byte[] playSequence;
    private short[] recordedSequence;

    private Integer randomSeed;

//    private long tc1 = -1; // Nanoseconds
//    private long tc2 = -1; // Nanoseconds

    private boolean bRecording = false;

    private static Params params = new Params();
    private static Client instance;
    NetworkThread networkThread;
    InetAddress target;


    @Override
    public void run() {

        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

        instance = this;


        try {
            MainActivityAcoustic.setReturnButtonState(false);
//            MainActivity.setClientButtonState(false);
            MainActivityAcoustic.setRadioButtonState(false);

            // send "start" signal to server
            networkThread.send("start", "0", target);

            // wait for "ready" signal  from server
            randomSeed = (int)networkThread.waitKeyValue("seed", target.getHostAddress());

            if (randomSeed == -1){
                MainActivityAcoustic.log("Server is busy. Try later!");

                networkThread.resetDataStruct();

                MainActivityAcoustic.setReturnButtonState(true);
                MainActivityAcoustic.setRadioButtonState(true);
                return;
            }

            // Generate signal
            signalSequence = Utils.generateSignalSequence_63(randomSeed);//Utils.generateChirpSequence_seed(params.signalSequenceLength, params.sampleRate, randomSeed, 1);
            playSequence = Utils.convertShortsToBytes( Utils.generateActuateSequence_seed(params.warmSequenceLength, params.signalSequenceLength, params.sampleRate, randomSeed, params.noneSignalLength));

            // Launch Recording Thread assumming 5s
            bRecording = true;
            new Thread(new Runnable() {
                public void run() {
                    MainActivityAcoustic.log("Client Recording Start...");
                    recordedSequence = Utils.record(params.sampleRate, params.recordSampleLength*6);
                    MainActivityAcoustic.log("Client Recording End...");
                    bRecording = false;
                }
            }).start();


            //Play signal
            MainActivityAcoustic.log("Client Playing Start...");
            Utils.play(playSequence);
//            MainActivity.log("Client Playing End...");

            // send "play" signal to server
            NetworkThread.send("play", "0", target);

            while (bRecording) Utils.sleep(1);
            Calendar cal = Calendar.getInstance();
            String str = cal.get(Calendar.HOUR_OF_DAY)+ "_" +cal.get(Calendar.MINUTE)+ "_" +cal.get(Calendar.SECOND)+ "_" + cal.get(Calendar.MILLISECOND);
            Utils.saveFile(Utils.convertShortsToBytes(recordedSequence), "c_rec_"+str+".pcm");
            // Calculate tc2 and tc1
			Utils.setFilter_convolution(signalSequence);
            int firstIndex = Utils.estimate_convolution_index(recordedSequence, signalSequence, 0,recordedSequence.length ); // find self-recording first
            int secondIndex = Utils.estimate_convolution_index(recordedSequence,signalSequence, firstIndex + signalSequence.length, recordedSequence.length);
            MainActivityAcoustic.log("Client: firstIndex = " + firstIndex);
            MainActivityAcoustic.log("Client: secondIndex = " + secondIndex);

            long tc1 = ((long) firstIndex * 1000000000) / params.sampleRate;
            long tc2 = ((long) secondIndex * 1000000000) / params.sampleRate;

            networkThread.send("tc1", ""+tc1, target);
            networkThread.send("tc2", ""+tc2, target);
            long ts1 = networkThread.waitKeyValue("ts1",target.getHostAddress());
            long ts2 = networkThread.waitKeyValue("ts2",target.getHostAddress());

            MainActivityAcoustic.log("tc1: " + tc1);
            MainActivityAcoustic.log("tc2: " + tc2);
            MainActivityAcoustic.log("ts1: " + ts1);
            MainActivityAcoustic.log("ts2: " + ts2);

            double delta = ((tc2 - tc1) - (ts2 - ts1)) / 2;
            MainActivityAcoustic.log("t: (ms)" + delta / 1000000);
            MainActivityAcoustic.log("Distance: " + (delta / 1000000000) * 340);
            MainActivityAcoustic.setDistance((delta / 1000000000) * 340);

            networkThread.resetDataStruct();

            MainActivityAcoustic.setReturnButtonState(true);
            MainActivityAcoustic.setRadioButtonState(true);
            MainActivityAcoustic.log("Client calculation ended.");
        } catch (Exception e) {
            e.printStackTrace();
            MainActivityAcoustic.log("Client thread exception: " + e.getMessage());
            MainActivityAcoustic.setReturnButtonState(true);
//            MainActivity.setClientButtonState(true);
            MainActivityAcoustic.setRadioButtonState(true);
        }

        instance = null;
    }

    public void setTarget(InetAddress mtarget){
        target = mtarget;
        networkThread = new NetworkThread();
        networkThread.setTarget(mtarget);
        networkThread.start();
    }

    public static void shutdown() {
        instance.interrupt();
        instance = null;
    }
}
