package com.sonicmeter.android.multisonicmeter;

public class Params {
    public static int sampleRate = 48000;

//    public static int sampleRate = 44100;
    static int frequency = 6000;
    static double warmDuration = 0.05; // 0.005 Seconds
    static double signalDuration = 0.05; // Seconds
    public static int warmSequenceLength = (int)(warmDuration * sampleRate);
    public static int signalSequenceLength = (int)(sampleRate / frequency + 1);//(signalDuration * sampleRate);
    static double noneSignalDuration = 0.1; // Seconds
    public static int noneSignalLength = (int)(noneSignalDuration * sampleRate);
    public static int bitCount = 63;


    int playSequenceLength = (warmSequenceLength + signalSequenceLength)*2;
    public static int recordSampleLength = (warmSequenceLength + noneSignalLength + signalSequenceLength * bitCount);

}
