package com.sonicmeter.android.multisonicmeter;

class DataStruct {
    int seed = -1;
    long tc0 = -1, tc1 = -1, tc2 = -1, ts0 = -1, ts1 = -1, ts2 = -1;
    int ready = -1, start = -1, play = -1, calc = -1;

    String buffer = null;

    void setReceivedBuffer(String value){
        buffer = value;
    }
    String getReceivedBuffer(){
        while (buffer == null) Utils.sleep(1);
        return buffer;
    }

//    ValueTypes lastValue;
    long lastUpdateTimestamp;

    void updateValue(String type, String value)
    {
//        lastValue = type;
        lastUpdateTimestamp = System.nanoTime();

        switch (type)
        {
            case "seed":
                seed = Integer.parseInt(value);
//                MainActivity.log("Seed updated: " + value);
                break;

            case "tc0":
                tc0 = Long.parseLong(value);
//                MainActivity.log("Tc0 updated: " + value);
                break;

            case "tc1":
                tc1 = Long.parseLong(value);
//                MainActivity.log("Tc1 updated: " + value);
                break;

            case "tc2":
                tc2 = Long.parseLong(value);
//                MainActivity.log("Tc2 updated: " + value);
                break;

            case "ts0":
                ts0 = Long.parseLong(value);
//                MainActivity.log("Ts0 updated: " + value);
                break;

            case "ts1":
                ts1 = Long.parseLong(value);
//                MainActivity.log("Ts1 updated: " + value);
                break;

            case "ts2":
                ts2 = Long.parseLong(value);
//                MainActivity.log("Ts2 updated: " + value);
                break;

            case "ready":
                ready = Integer.parseInt(value);
//                MainActivity.log("Ready" );
                break;
            case "start":
                start = Integer.parseInt(value);
//                MainActivity.log("Start" );
                break;
            case "play":
                play = Integer.parseInt(value);
//                MainActivity.log("Play" );
                break;
            case "calc":
                calc = Integer.parseInt(value);
//                MainActivity.log("Calculated" );
                break;
        }
    }
}
