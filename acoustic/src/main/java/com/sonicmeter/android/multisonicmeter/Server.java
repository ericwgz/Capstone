package com.sonicmeter.android.multisonicmeter;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Random;

class Server extends Thread {

    boolean bContinue = true;
    private static Server instance;
    NetworkThread networkThread;
    InetAddress targetAddress;

    static int LIMIT_CLIENTS = 10;
    static HashMap<Integer, String> clientSeeds;

//    static HashMap<Integer, TrackRecord> recordMap;

    private RecordThread recordThread;
    static TrackRecord audioTrack = new TrackRecord();

    @Override
    public void run()
    {

        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        instance = this;

        networkThread = new NetworkThread();
        networkThread.start();

        clientSeeds = new HashMap<>();
//        recordMap = new HashMap<>();

        MainActivityAcoustic.setServerButtonState(false);
        MainActivityAcoustic.setClientButtonState(false);
        try {
            //Start recording Server
            recordThread = new RecordThread();
            recordThread.start();


//            int id = 0;
//            Params params = new Params();
//            int minb = Utils.getMinBufferSize(params.sampleRate);
//            int delay = 450;
//            int delay = (int)((float)Utils.getMinBufferSize(params.sampleRate) / (float)params.sampleRate * 1000 * 8);
//            MainActivity.log("delay="+delay);
            while (bContinue) {
/*                int randomSeed = 4955;

                short[] signalSequence = Utils.generateSignalSequence_63(randomSeed);//Utils.generateChirpSequence_seed(params.signalSequenceLength, params.sampleRate, randomSeed, 1);
                byte[] playSequence = Utils.convertShortsToBytes( Utils.generateActuateSequence_seed(params.warmSequenceLength, params.signalSequenceLength, params.sampleRate, randomSeed, params.noneSignalLength));
                Utils.play(playSequence);
                Utils.sleep(50);
                Utils.play(playSequence);
                Utils.sleep(delay);
                short[] recordedSequence = Server.getRecordedSequence(randomSeed, params.recordSampleLength *4 );
                if (recordedSequence != null)
                    Utils.saveFile(Utils.convertShortsToBytes(recordedSequence),"recorded_seq_" + id + ".pcm");

                id++;
                Utils.sleep(1000);
                continue;
*/

                networkThread.waitKeyValue("start");
                targetAddress = networkThread.getTarget();
                networkThread.resetDataStruct();
                if ( clientSeeds.containsValue(targetAddress.getHostAddress()) ) continue;
                //If receive the new client request
                int seed = generateSeed(targetAddress.getHostAddress());

                if (seed == -1){//If server is busy
                    networkThread.send("seed", ""+seed, targetAddress);
                    continue;
                }

                ClientServer clientServer = new ClientServer();
                clientServer.setRandomSeed(seed);
                clientServer.setTargetAddress(targetAddress);
                clientServer.start();

                Utils.sleep(1);

            }

            //If server was crashed, end the recording thread
            recordThread.stopRecord();

        }
        catch (Exception e)
        {
            e.printStackTrace();
            MainActivityAcoustic.log("Server thread exception: " + e.getMessage());
            MainActivityAcoustic.setReturnButtonState(true);
        }
        finally {
            networkThread.interrupt();
        }

        instance = null;

    }

    public void stopThread(){
//        if (recordThread != null){
//            recordThread.stopRecord();
//            recordThread = null;
//        }
        bContinue = false;
    }


    static int generateSeed(String ip){
        if (clientSeeds.size() < LIMIT_CLIENTS) {
            int seed = new Random().nextInt(4095);
            while (clientSeeds.containsKey(seed)) seed = new Random().nextInt(4095);

            //if new seed, append
            clientSeeds.put(seed, ip);

//            //Create the trackRecord object and put to Map
//            TrackRecord trackRecord = new TrackRecord();
//            recordMap.put(seed, trackRecord);

            return seed;
        }

        return -1;
    }

    static void removeSeed(int seed){
        if (clientSeeds.containsKey(seed)) {
            String ip = clientSeeds.get(seed);
            instance.networkThread.resetDataMap(ip);
            clientSeeds.remove(seed);
        }
//        if (recordMap.containsKey(seed)){
//            TrackRecord track = recordMap.get(seed);
//            track.releaseLock();
//            track = null;
//            recordMap.remove(seed);
//        }
    }


    private class RecordThread extends Thread{
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
                MainActivityAcoustic.log("Error recording: " + x.getMessage());
            }
            int i = 0;
            while (bContinue){
                try {
                    short[] buffer = Utils.recordBuffer(minBufferSize);
                    audioTrack.addSamples(buffer);


                    /*
                    for (TrackRecord track : recordMap.values()){
                        if (track != null)
                            track.addSamples(buffer);
                        MainActivity.log("Recorded=" + buffer.length);
                    }
                    * */



                } catch (Exception e){
                    e.printStackTrace();
                    MainActivityAcoustic.log("Server Recording Failed " + e.getMessage());
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
                MainActivityAcoustic.log("Error recording: " + x.getMessage());
            }

        }

        public void stopRecord(){
            bContinue = false;
        }
    }

    synchronized static short[] getRecordedSequence(int seed, int length){
        /**
         TrackRecord trackRecord = recordMap.get(seed);
         if (trackRecord != null)
         return trackRecord.getSamples(length);
         */
        return audioTrack.getSamples(length);
    }

    static void stopRecording(){
        instance.recordThread.stopRecord();
    }

}
