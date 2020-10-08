package com.example.uwcapstone;

import android.util.Log;

import com.sonicmeter.android.multisonicmeter.Params;
import com.sonicmeter.android.multisonicmeter.Utils;

import java.util.HashMap;
import java.util.Map;

class Sender extends Thread{
    String role;
    Map<String, Integer> map;
    //private short[] signalSequence;
    private byte[] playSequence;
    private volatile boolean exit;
    int number;

    private static Params params = new Params();

    public Sender(String role) {
        this.role = role;
        map = new HashMap<>();
        map.put("SOS", 1500)   ;
        map.put("Ack", 2500);
        number = 0;
        exit = false;
    }

    @Override
    public void run() {

        if (role.equals("Seeker")) {
            // keep sending SOS
            try {
                MainActivity.log("seeker is sending message: SOS");
                MainActivity.setSeekerBtnState(false);
                MainActivity.setStopSeekBtnState(true);
                playSequence = Utils.convertShortsToBytes( Utils.generateActuateSequence_seed(params.warmSequenceLength, params.signalSequenceLength, params.sampleRate, map.get("SOS"), params.noneSignalLength));

                while(!exit) {
                    MainActivity.log("start to play sequence " + String.valueOf(number++));
                    Utils.play(playSequence);
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else if (role.equals("Helper")) {
            // keep sending ACK
            try {
                MainActivity.log(" helper is sending message: ACK");
                playSequence = Utils.convertShortsToBytes( Utils.generateActuateSequence_seed(params.warmSequenceLength, params.signalSequenceLength, params.sampleRate, map.get("ACK"), params.noneSignalLength));

                while(!exit) {
                    MainActivity.log("start to play sequence " + String.valueOf(number++));
                    Utils.play(playSequence);
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } else {
            MainActivity.log("INVALID ROLE");
        }

    }

    public void stopThread() {
        exit = true;
    }

}
