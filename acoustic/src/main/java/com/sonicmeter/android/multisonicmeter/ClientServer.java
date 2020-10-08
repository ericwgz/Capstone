package com.sonicmeter.android.multisonicmeter;

import java.net.InetAddress;
import java.util.Calendar;

class ClientServer extends Thread {

    private short[] signalSequence;
    private byte[] playSequence;
    private short[] recordedSequence;

    private Integer randomSeed;

//    private long ts1 = -1; // Nanoseconds
//    private long ts2 = -1; // Nanoseconds
    private boolean bRecording = false;
	
    private static ClientServer instance;
	private static Params params = new Params();
//    NetworkThread networkThread;
    InetAddress targetAddress;

    @Override
    public void run()
    {
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        instance = this;
//        networkThread = new NetworkThread();
//        networkThread.start();

        try {
            MainActivityAcoustic.setReturnButtonState(false);
            if (!Thread.currentThread().isInterrupted()) {

                NetworkThread.send("seed", ""+randomSeed, targetAddress);
                signalSequence = Utils.generateSignalSequence_63(randomSeed);//Utils.generateChirpSequence_seed(params.signalSequenceLength, params.sampleRate, randomSeed, 1);
                playSequence = Utils.convertShortsToBytes( Utils.generateActuateSequence_seed(params.warmSequenceLength, params.signalSequenceLength, params.sampleRate, randomSeed, params.noneSignalLength));


                MainActivityAcoustic.setReturnButtonState(false);

                //Record audio signal played from client

                NetworkThread.waitKeyValue("play");

                //Play signal
                MainActivityAcoustic.log("Server Playing for "+targetAddress.getHostAddress());
                Utils.play(playSequence);
//                MainActivity.log("Server Playing End...");

                Utils.sleep(450);
                recordedSequence = Server.getRecordedSequence(randomSeed, params.recordSampleLength * 6);

                Calendar cal = Calendar.getInstance();
                String str;
                //Save to file
                str = cal.get(Calendar.HOUR_OF_DAY)+ "_" +cal.get(Calendar.MINUTE)+ "_" +cal.get(Calendar.SECOND)+ "_" + cal.get(Calendar.MILLISECOND);
                Utils.saveFile(Utils.convertShortsToBytes(recordedSequence), "s_rec_"+str+".pcm");

                //Calculate ts1 and ts2
                Utils.setFilter_convolution(signalSequence);
                int secondIndex = Utils.estimate_convolution_index(recordedSequence, signalSequence, 0, recordedSequence.length);
                int firstIndex = Utils.estimate_convolution_index(recordedSequence,signalSequence, 0, secondIndex - signalSequence.length);
                MainActivityAcoustic.log("Server: firstIndex = " + firstIndex);
                MainActivityAcoustic.log("Server: secondIndex = " + secondIndex);
                long ts1 = ((long) firstIndex * 1000000000) / params.sampleRate;
                long ts2 = ((long) secondIndex * 1000000000) / params.sampleRate;

                NetworkThread.send("ts1", ""+ts1, targetAddress);
                NetworkThread.send("ts2", ""+ts2, targetAddress);

                long tc1 = NetworkThread.waitKeyValue("tc1");
                long tc2 = NetworkThread.waitKeyValue("tc2");

                MainActivityAcoustic.log("tc1: " + tc1);
                MainActivityAcoustic.log("tc2: " + tc2);
                MainActivityAcoustic.log("ts1: " + ts1);
                MainActivityAcoustic.log("ts2: " + ts2);

                double delta = ((tc2 - tc1) - (ts2 - ts1)) / 2;
                MainActivityAcoustic.log("t: (ms)" + delta / 1000000);
                MainActivityAcoustic.log("Distance: " + (delta / 1000000000) * 340);
                MainActivityAcoustic.setDistance((delta / 1000000000) * 340);


                NetworkThread.resetDataStruct();
                NetworkThread.resetDataMap(targetAddress.getHostAddress());
                MainActivityAcoustic.log("Server calculation ended.");
                MainActivityAcoustic.setReturnButtonState(true);

                Server.removeSeed(randomSeed);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            MainActivityAcoustic.log("Server thread exception: " + e.getMessage());
            MainActivityAcoustic.setReturnButtonState(true);
            Server.removeSeed(randomSeed);
        }

        instance = null;

    }

    public void setRandomSeed(int seed){
        randomSeed = seed;
    }
    public void setTargetAddress(InetAddress target){
        targetAddress = target;
    }

    static void shutdown() {
        instance.interrupt();
        instance = null;
    }

}
