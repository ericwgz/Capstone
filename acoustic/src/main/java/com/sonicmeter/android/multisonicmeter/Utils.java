package com.sonicmeter.android.multisonicmeter;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Random;

import com.villoren.java.dsp.convolution.ConvolutionRealD;
import com.villoren.java.dsp.convolution.FilterKernelD;
import com.villoren.java.dsp.fft.FourierTransformD;

public class Utils {

    private static AudioRecord recorder = null;
    private static AudioTrack player  = null;
    private static int mAudioPlayBufferSize = 0;

    private static int lowFreq = 2000;
    private static int highFreq = 6000;

    public static final String TAG = Utils.class.getSimpleName();

    public static ConvolutionRealD convolution = null;

    public static void releaseAudioRecord() {
        if(recorder == null) {
            return;
        }
        //Stop recording, release AudioRecord instance
        if (recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            Utils.getRecorderHandle().stop();
        }
        if (Utils.getRecorderHandle().getState() == AudioRecord.STATE_INITIALIZED) {
            Utils.getRecorderHandle().release();
        }
        recorder = null;
    }

    static boolean audioInitialized()
    {
        return (player != null) && (recorder != null);
    }

    static short[] generateSequence(int len, int seed)
    {
        short[] seq = new short[len];
        Random rand = new Random(seed);
        for (int i = 0; i < seq.length; ++i) {
            seq[i] = (short) (2 * (rand.nextInt(32767) % 2) - 1);
        }
        return seq;
    }

    static short[] generateChirpSequence(int numSample,int sampleRate, int seed, double factor)
    {
        double freq1 = lowFreq;
        double freq2 = highFreq;
        double m =(freq2 - freq1)*sampleRate/(2*numSample); // Calculating alpha/slope
        short[] seq = new short[numSample + 1];
        double fs = 1 / (double)sampleRate;
        double tt = 0;
        for (int i=0;i<=numSample; i++ ) {
            double dVal = Math.cos(2.f * Math.PI * (m * (tt * tt) + freq1 * tt));
            tt += fs;
            seq[i] = (short) Math.round(dVal * factor * 32767); // max positive sample for signed 16 bit integers is 32767
        }
        return seq;
    }

    static short[] generateActuateSequence(int numWarmSample, int numSample, int sampleRate, int seed, double factor, int noneSoundlen)
    {
        double freq = lowFreq;
        short[] seq = new short[numWarmSample + numSample + 2 + noneSoundlen + numWarmSample];
        //None Signal
        for (int i = 0; i < noneSoundlen; i++)
            seq[i] = 0;     ////NoneSound generation
        //Warm Sound
        double fs = 1 / (double)sampleRate;
        for (int i=0;i<=numWarmSample; i++ ) {
            double dVal = Math.cos(2.0 * Math.PI * freq * i * fs);
            seq[noneSoundlen + i] = (short)Math.round(dVal * factor * 32767 * i / numWarmSample); // max positive sample for signed 16 bit integers is 32767
        }
        //ChirpSequence
        short[] seq1 = generateChirpSequence(numSample,sampleRate,seed, factor);
        for (int i=0;i<=numSample; i++ )
            seq[noneSoundlen + numWarmSample + 1 + i] = seq1[i];
        //Cool sound
        double freq1 = highFreq;
        for (int i=0;i<numWarmSample; i++ ) {
            double dVal = Math.cos(2.0 * Math.PI * freq1 * i * fs);
            seq[numWarmSample + numSample + 2 + noneSoundlen +  + i] = (short)Math.round(dVal * factor * 32767 * (numWarmSample-i) / numWarmSample); // max positive sample for signed 16 bit integers is 32767
        }
        return seq;
    }
    /** Generate the signalsequence for the value 1 and 0 with numSample */
    static short[] generateValueSequence(int numSample, int sampleRate, int value){
        double freq1 = lowFreq;
        double freq2 = highFreq;
        double m =(freq2 - freq1)*sampleRate/(2*numSample); // Calculating alpha/slope
        short[] seq = new short[numSample+1];
        double fs = 1 / (double)sampleRate;
        if (value == 1){
            double tt = 0;
            for (int i=0;i<=numSample; i++ ) {
                double dVal = Math.cos(2.0 * Math.PI * (m * (tt * tt) + freq1 * tt));
                tt += fs;
                seq[i] = (short)Math.round(dVal * 32767); // max positive sample for signed 16 bit integers is 32767
            }
        }
        else if (value == 0){
            double tt = numSample/sampleRate;
            for (int i=0;i<=numSample; i++ ) {
                double dVal = Math.cos(2.0 * Math.PI * (m * (tt * tt) + freq1 * tt));
                tt -= fs;
                seq[i] = (short)Math.round(dVal * 32767); // max positive sample for signed 16 bit integers is 32767
            }
        }

        return seq;
    }
    static short[] generateChirpSequence_seed(int numSample,int sampleRate, int seed, double factor)
    {
        assert ( seed < 1024);
        int[] goldcode = generateGoldSequence5(seed);
        short[] seq = new short[(numSample+1) * Params.bitCount];
        short[] seq1 = generateValueSequence(numSample, sampleRate, 1);
//        short[] seq0 = generateValueSequence(numSample, sampleRate, 0);
        short[] seq0 = new short[numSample+1];
        for (int i = 0; i <= numSample; i++)
            seq0[i] = seq1[numSample-i];

        for (int i = 0; i < 31; i++){
            if (goldcode[i] == 1){
                for (int j = 0; j < seq1.length; j++){
                    seq[i * seq1.length + j] = seq1[j];
                }
            }
            else if (goldcode[i] == 0){
                for (int j = 0; j < seq0.length; j++){
                    seq[i * seq0.length + j] = seq0[j];
                }
            }
        }
        return seq;
    }

    public static short[] generateActuateSequence_seed(int numWarmSample, int numSample, int sampleRate, int seed, int noneSoundlen)
    {
        double freq = Params.frequency;
        short[] seq = new short[numWarmSample + numSample * Params.bitCount + noneSoundlen];// + numWarmSample
        //None Signal
        for (int i = 0; i < noneSoundlen; i++)
            seq[i] = 0;     ////NoneSound generation
        //Warm Sound
        double fs = 1 / (double)sampleRate;
        for (int i=0;i<numWarmSample; i++ ) {
            double dVal = Math.cos(2.f * Math.PI * lowFreq * i * fs);
            seq[noneSoundlen + i] = (short)Math.round(dVal * 32767 * i / numWarmSample); // max positive sample for signed 16 bit integers is 32767
        }
        //ChirpSequence
        short[] seq1 = generateSignalSequence_63(seed);
        for (int i=0;i<seq1.length && noneSoundlen+ numWarmSample + i < seq.length; i++ )
            seq[noneSoundlen + numWarmSample + i] = seq1[i];
        //Cool sound
//        double freq1 = highFreq;
//        for (int i=0;i<numWarmSample; i++ ) {
//            double dVal = Math.cos(2.f * Math.PI * freq1 * i * fs);
//            seq[numWarmSample +1+ (numSample+1)*Params.bitCount + noneSoundlen +  + i] = (short)Math.round(dVal * factor * 32767 * (numWarmSample-i) / numWarmSample); // max positive sample for signed 16 bit integers is 32767
//        }
        return seq;
    }

    /** Generate the signalsequence for the value 1 and 0 with 1 carrier cycle */
    static short[]generateCycleSequence(int value){
        double freq = Params.frequency;
        int numSample = Params.sampleRate / Params.frequency;
        short[] seq = new short[numSample+1];
        double fs = 1 / (double)Params.sampleRate;
        int sign = 1;
        if (value == 0) sign = -1;
        for (int i=0;i<=numSample; i++ ) {
            double dVal = sign * Math.cos(2.0 * Math.PI * i * freq * fs);
            seq[i] = (short)Math.round(dVal * 32767); // max positive sample for signed 16 bit integers is 32767
        }

        return seq;
    }

    public static short[] generateSignalSequence_63(int seed)
    {
        assert ( seed < 4096);
        int[] goldcode = generateGoldSequence6(seed);
        short[] seq1 = generateCycleSequence(1);
        short[] seq0 = generateCycleSequence(0);
        short[] seq = new short[seq1.length * goldcode.length];

        for (int i = 0; i < goldcode.length; i++){
            if (goldcode[i] == 1){
                for (int j = 0; j < seq1.length; j++){
                    seq[i * seq1.length + j] = seq1[j];
                }
            }
            else if (goldcode[i] == 0){
                for (int j = 0; j < seq0.length; j++){
                    seq[i * seq0.length + j] = seq0[j];
                }
            }
        }
        return seq;
    }

    static short[] maximizeAmplitude(short[] seq)
    {
        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        short[] res = new short[seq.length];
        int idx = 0;
        for (final int dVal : seq) {
            res[idx++] = (short) ((dVal * 32767));
        }
        return res;
    }
    public static void initPlayer(int sampleRate, int len)
    {
        try{
            mAudioPlayBufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO , AudioFormat.ENCODING_PCM_16BIT);
            player = new AudioTrack(AudioManager.MODE_IN_COMMUNICATION,
                    sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, mAudioPlayBufferSize,
                    AudioTrack.MODE_STREAM);
        }catch (Throwable x){
            MainActivityAcoustic.log("Error Initializing Player: " + x.getMessage());
        }
    }

    public static synchronized void play(byte[] sample)
    {
        //MainActivity.log("Playing");
        int length = sample.length;
        try {
            player.play();
            if(length <= mAudioPlayBufferSize)
                player.write(sample, 0, length);
            else {
                int len = 0;
                while (len < length - mAudioPlayBufferSize){
                    player.write(sample, len, mAudioPlayBufferSize);
                    len += mAudioPlayBufferSize;
                }
                if( len < length){
                    player.write(sample, len, length - len);
                }
            }
            player.stop();
//            player.release();
        } catch (Throwable x) {
            //MainActivity.log("Error playing audio: " + x.getMessage());
            Log.d("Error playing audio:", x.getMessage());

        }

    }

    public static void initRecorder(int sampleRate)
    {
        try {
            int minBufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize);
            //String curMinBufferSize = String.valueOf(minBufferSize);
            //Log.d("recorder curMinBufferSize", curMinBufferSize);
        } catch (Throwable x){
            Log.d("Error: ", "initRecorder() Initializing AudioRecord: " + x.getMessage());
        }

    }

    public static short[] record(int sampleRate, int len) {

        MainActivityAcoustic.log("Recording...");
        int minBufferSize = 0;

        try {
            if (recorder.getState() != AudioRecord.STATE_INITIALIZED)
                throw new Exception("AudioRecord init failed");
            minBufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);//Byte Length
        }catch (Throwable x){
        }
        minBufferSize = minBufferSize / 2;//Convert to Short Length
        if (len <= minBufferSize)
            len = minBufferSize;
        else if (len > minBufferSize){
            if (len - len / minBufferSize > 0){
                len = minBufferSize * (len / minBufferSize + 1);
            }
        }

        short[] sample = new short[len];
        try {

            short[] audioData = new short[minBufferSize];
            int n = 0;
            recorder.startRecording();
            while ( n < len - minBufferSize)
            {
                int numberOfShort = recorder.read(audioData, 0, minBufferSize);
                //MainActivity.log("nnn nnn: " + n);
                for (int i = 0; i < numberOfShort; i++)
                {
                    sample[n + i] = audioData[i];
                }
                n += numberOfShort;
            }
            if(n < len){
                int numberOfShort = recorder.read(audioData, 0, len - n);
                for (int i = 0; i < numberOfShort; i++)
                {
                    sample[n + i] = audioData[i];
                }
                n += numberOfShort;
            }
            recorder.stop();
            MainActivityAcoustic.log("Recorded samples: " + n);
//            saveFile_Internal(convertShortsToBytes(sample), "recorded_sound.wav");
        } catch (Throwable x) {
            MainActivityAcoustic.log("Error reading voice audio: " + x.getMessage());
        }
        /*
         * Frees the thread's resources after the loop completes so that it can be run again
         */
        finally {
//            assert recorder != null;
//            recorder.stop();
//            recorder.release();
        }
        return sample;
    }

    public static int getMinBufferSize(int sampleRate){
        int minBufferSize = 0;
        try {
            if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.d("msg: ", "state is not the same");
                throw new Exception("AudioRecord init failed");
            }

            //Log.d("msg: ", "state is the same");
            minBufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);//Byte Length
        } catch (Throwable x) {
            Log.d("Error: ", "reading voice audio: " + x.getMessage());
        }
        return minBufferSize/2;//return short length
    }

    public static short[] recordBuffer(int minBufferSize) {
        short[] audioData = new short[minBufferSize];
        try {
//            recorder.startRecording();
            int numberOfShort = recorder.read(audioData, 0, minBufferSize);

//            recorder.stop();
        } catch (Exception e) {
            Log.d(TAG, "Error reading voice audio: " + e.getMessage());
        }
        finally {
        }
        return audioData;
    }

    public static AudioRecord getRecorderHandle(){
        if (recorder == null)
            return null;
        return recorder;
    }

    static int estimateStartIndex(short[] sample, short[] model)
    {
        double max = 0;
        int index = -1;
        double sum;

        for(int i = 0; i < sample.length - model.length + 1; i+=100)
        {
            sum = 0.0;
            //QNnorm1 = 0.0;

            for (int k = i; k < i + model.length; k++) {
                sum += (double) model[k - i] * ((double) sample[k]);
                //QNnorm1 += ((double) sample[k]) * ((double) sample[k]);
            }
            //QNnorm1 = Math.sqrt(QNnorm1);

            if (max < sum)
            {
                max = sum;
                index = i;
            }
        }

        return index;
    }

    static int estimateTimeIndex(short[] sample, short[] model, int start, int end)
    {
        double max = -Math.exp(100);
        int index = -1;

        int number_of_taps = model.length;
        double[] buffer = new double[number_of_taps];
        double[] coefficients = new double[number_of_taps];
        int offset = 0;
        int k, j;
        for( k = 0 ; k < number_of_taps ; k++) {
            buffer[k] = 0.0;
            coefficients[k] = (double)model[number_of_taps - 1 - k] / 32767; // matched filter
        }

        if ( end > sample.length )
            end = sample.length;
        if ( start < 0 )
            start = 0;
        double[] outSample = new double[end - start];
        int id = 0;
        for(int i = start; i < end - number_of_taps ; i++)
        {
            buffer[offset] = (double)sample[i] / 32767;
            double output_ = 0.0;
            j = 0;
            for(k = offset, j = 0 ; k >= 0 ; k--, j++)
                output_ += buffer[k] * coefficients[j];

            for( k = number_of_taps - 1; j < number_of_taps ; k--, j++)
                output_ += buffer[k] * coefficients[j];
            outSample[id] = output_;
            id++;
            if(++offset >= number_of_taps)
                offset = 0;
            if (max < Math.abs(output_))
            {
                max = Math.abs(output_);
                index = i;
            }
        }

        return index;
    }
    static int estimateStartIndex3(byte[] sample, byte[] model)
    {
        double PN2max = 0.0;
        int PN2p = 0;

        int len = sample.length;
        int CODELEN = model.length;
        int CLIP_LENGTH = 1000;
        int STEP_SIZE = 20;

        byte[] audioData = sample;
        byte[] PN2 = model;

        int i = 0;
        //run the matched filter to estimate the start time of the server playing the sequence PN2.
        while(i<len-CODELEN+1)
        {
            int step;
            //check how for we should go in each step.
            //if we are near the "coming" of an input signal (a big "hop"), then we should have a larger step size than default (1) to ensure robustness of noise
            if((Math.abs((double)audioData[i])<=0.05) && (Math.abs((double)audioData[i+STEP_SIZE])>=1))
                step = CLIP_LENGTH;
            else
                step = 1;

            double PN2norm1 = 0.0;
            double PN2sum = 0.0;
            double peak = 0.0;
            for(int j = 0; j<CODELEN; j++)
            {
                PN2sum += ((double)audioData[i+j])*((double)PN2[j]);
                PN2norm1 += ((double)audioData[i+j])*((double)audioData[i+j]);
                if(peak< Math.abs((double)audioData[i+j]))
                    peak = Math.abs((double)audioData[i+j]);
            }
            PN2norm1 = Math.sqrt(PN2norm1);

            if(PN2sum<0.0)
            {
                PN2sum = -PN2sum;
            }

            /*
             Both the server and the client will play some audio, and the client will record much stronger signals from itself than the signals coming from the server. The amplitude of audio signals recorded from itself is roughly 1 (you can adjust these thresholds based on your observation), so we need to filter out these signals.
             */
            if((PN2norm1*PN2norm1/(CODELEN*1.0)>=1.0) || (peak>=1.0))
                PN2sum = 0.0;
            if(PN2max<PN2sum)
            {
                PN2max = PN2sum;
                PN2p = i;
            }
            i += step;
        }

        return PN2p;
    }

    static int estimateStartIndex3(short[] sample, short[] model)
    {
        double PN2max = 0.0;
        int PN2p = 0;

        int len = sample.length;
        int CODELEN = model.length;
        int CLIP_LENGTH = 1000;
        int STEP_SIZE = 20;

        short[] audioData = sample;
        short[] PN2 = model;

        int i = 0;
        //run the matched filter to estimate the start time of the server playing
        // the sequence PN2.
        while(i<len-CODELEN+1)
        {
            int step;
            //check how for we should go in each step.
            //if we are near the "coming" of an input signal (a big "hop"),
            // then we should have a larger step size than default (1) to ensure
            // robustness of noise
            if((Math.abs((double)audioData[i])<=0.05) && (Math.abs((double)audioData[i+STEP_SIZE])>=1))
                step = CLIP_LENGTH;
            else
                step = 1;

            double PN2norm1 = 0.0;
            double PN2sum = 0.0;
            double peak = 0.0;
            for(int j = 0; j<CODELEN; j++)
            {
                PN2sum += ((double)audioData[i+j])*((double)PN2[j]);
                PN2norm1 += ((double)audioData[i+j])*((double)audioData[i+j]);
                if(peak< Math.abs((double)audioData[i+j]))
                    peak = Math.abs((double)audioData[i+j]);
            }
            PN2norm1 = Math.sqrt(PN2norm1);

            if(PN2sum<0.0)
            {
                PN2sum = -PN2sum;
            }

            /*
             Both the server and the client will play some audio, and the client will record much stronger signals from itself than the signals coming from the server. The amplitude of audio signals recorded from itself is roughly 1 (you can adjust these thresholds based on your observation), so we need to filter out these signals.
             */
            if((PN2norm1*PN2norm1/(CODELEN*1.0)>=1.0) || (peak>=1.0))
                PN2sum = 0.0;
            if(PN2max<PN2sum)
            {
                PN2max = PN2sum;
                PN2p = i;
            }
            i += step;
        }

        return PN2p;
    }


    public static byte[]  convertShortsToBytes(short[] seq)
    {
        byte[] res = new byte[seq.length * 2];
        int idx = 0;
        for (final short val : seq) {
            // in 16 bit wav PCM, first byte is the low order byte
            res[idx++] = (byte) (val & 0x00ff);
            res[idx++] = (byte) ((val & 0xff00) >>> 8);
        }
        return res;
    }

    static short[] convertBytesToShorts(byte[] sample)
    {
        ByteBuffer bb = ByteBuffer.wrap(sample);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        short[] res = new short[sample.length / 2];
        for (int i=0; i<res.length; i++) res[i] = bb.getShort();
        return res;
    }

    public static void sleep(int milisecs) {
        try {
            Thread.sleep(milisecs);
        } catch (InterruptedException e) {
//            MainActivity.log("Sleep doesn't work " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void saveFile(byte[] data, String filename) throws IOException {
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            String sdCard = Environment.getExternalStorageDirectory().getAbsolutePath();
            File dir = new File(sdCard + "/SonicMeter");
            dir.mkdirs();
            File file = new File(dir, filename);

            try{
                FileOutputStream out = new FileOutputStream(file);
                out.write(data);
                out.flush();
                out.close();
            }
            catch (FileNotFoundException e){
                MainActivityAcoustic.log("File not created " + e.getMessage());
            }
            catch (SecurityException e){
                MainActivityAcoustic.log("Security Exception " );
            }
            catch (Exception e){
                MainActivityAcoustic.log("File io Exception" + e.getMessage());
            }
            MainActivityAcoustic.log("File saved to " + dir + "/" +filename);
        }
        else{
            MainActivityAcoustic.log("Your device has no sdcard!");
        }

    }


    static byte[] readFile(String filename) throws IOException {
        String sdCard = Environment.getExternalStorageDirectory().getAbsolutePath();
        File file = new File(sdCard + "/" + filename);

        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[fis.available()];
        while (fis.read(data) != -1){}
        fis.close();
        return data;
    }

    static byte[] scale_audiodata(byte[] data, double scale){
        byte res[] = new byte[data.length];
        for (int i = 0; i < data.length; i++){
//            double val = (double) data[i] * scale;
            res[i] = (byte)(data[i] >> 1) ;
        }
        return res;
    }

    static void releasePlayer_Recorder(){
        if (player != null) {
            player.release();
            player = null;
        }
        if (recorder != null){
            recorder.release();
            recorder = null;
        }
    }

    public static void setFilter_convolution(short[] model){
        int LEN = 0;
        int i = 0;
        int number_of_taps = model.length;
        while (LEN < number_of_taps){
            LEN = (int) Math.pow(2, i);
            i++;
        }

        double[] filterModel = new double[LEN];
        double[] coefficients = new double[LEN];
        int k;
        for( k = 0 ; k < number_of_taps ; k++)
            coefficients[k] = (double)model[number_of_taps - 1 - k] / 32767; // matched filter

        Arrays.fill(coefficients, number_of_taps, LEN, 0.0);
        double[] inImag = new double[LEN];
        double[] outImag = new double[LEN];
        Arrays.fill(inImag, 0, LEN, 0.0);
        Arrays.fill(outImag, 0, LEN, 0.0);

        FourierTransformD fft = new FourierTransformD(LEN, FourierTransformD.Scale.FORWARD);
        fft.transform(coefficients, inImag, filterModel, outImag, false);

        FilterKernelD filterKernel = new FilterKernelD(convolution);


        for (i = 0; i < LEN; i++){
//            filterKernel.setBin(i, filterModel[i], outImag[i]);
            filterKernel.setBinReal(i, coefficients[i]);
        }
        // Use this filter kernel
        convolution.setFilterKernel(filterKernel);
    }

    public static int estimate_convolution_index(short[] sample, short[] model, int start, int end){
        int LEN = 0;
        int i = 0;
        int number_of_taps = model.length;
        while (LEN < number_of_taps){
            LEN = (int)Math.pow(2, i);
            i++;
        }

        double max = -Math.exp(100);
        int index = -1;
        int j;

        if ( end > sample.length )
            end = sample.length;
        if ( start < 0 )
            start = 0;

        double[] outSample = new double[end - start];
        Arrays.fill(outSample, 0.0);
        double[] in_sam = new double[LEN];
        double[] out_sam = new double[LEN];
        for(i = start; i < end - LEN; i+=LEN/2){
            for (j = 0; j < LEN; j++){
                in_sam[j] = (double)sample[i + j] / 32767;
            }
            //Apply window function
//            HammingWindowD hammingWindowD = new HammingWindowD(LEN);
//            hammingWindowD.apply(in_sam);

            convolution.convolve(in_sam, out_sam);
            //Overlapping add
            for (j = 0; j < LEN; j++)
                outSample[i - start + j] += out_sam[j];

        }

        for(j = 0; j < outSample.length; j++) {
            if (max < outSample[j]) {
                max = outSample[j];
                index = j;
            }
        }
        return start + index;
    }

    // ------------------------ new --------------------------------------------
    public static double estimate_max_similarity(short[] sample, short[] model, int start, int end) {
        int LEN = 0;
        int i = 0;
        int number_of_taps = model.length;
        while (LEN < number_of_taps) {
            LEN = (int)Math.pow(2, i);
            i++;
        }

        double max = -Math.exp(100);
        //int index = -1;
        int j;

        if ( end > sample.length )
            end = sample.length;
        if ( start < 0 )
            start = 0;

        double[] outSample = new double[end - start];
        Arrays.fill(outSample, 0.0);
        double[] in_sam = new double[LEN];
        double[] out_sam = new double[LEN];
        for(i = start; i < end - LEN; i+=LEN/2){
            for (j = 0; j < LEN; j++){
                in_sam[j] = (double)sample[i + j] / 32767;
            }
            //Apply window function
//            HammingWindowD hammingWindowD = new HammingWindowD(LEN);
//            hammingWindowD.apply(in_sam);

            convolution.convolve(in_sam, out_sam);
            //Overlapping add
            for (j = 0; j < LEN; j++)
                outSample[i - start + j] += out_sam[j];

        }

        for(j = 0; j < outSample.length; j++) {
            if (max < outSample[j]) {
                max = outSample[j];
                //index = j;
            }
        }
        return max;
    }

    public static void initConvolution(int sampleLength){
        int N = 0;
        int i = 0;
        while (N < sampleLength){
            N = (int)Math.pow(2, i);
            i++;
        }
        convolution = new ConvolutionRealD(N);
    }

    /**
     * n=5 Gold Sequence Generator
     * @param seed : 1~1024 integer
     * @return int[31] goldcode
     */
    public static int[] generateGoldSequence5(int seed){
        int[] register1 = new int[5];
        int[] register2 = new int[5];
        for (int i = 0; i < 5; i++)
            register1[i] = (seed >> i) & 0x01;
        for (int i = 0; i < 5; i++)
            register2[i] = (seed >> i) & 0x01;

        int[] code1 = new int[31];
//        Arrays.fill(code1, 0);
        //Generate 1st m-sequence (52) of length 31 (1+x^2+x^5)
        for (int i = 0; i < 31; i++){
            int temp = register1[1] + register1[4];
            int k = temp / 2;
            temp = temp - k * 2;
            code1[i] = 2 * register1[4] - 1;//[1,-1]
            if (code1[i] < 0) code1[i] = 0; //[1,0]
            for (int j = 4; j > 0; j--){
                register1[j] = register1[j - 1];
            }
            register1[0] = temp;
        }


        int[] code2 = new int[31];
//        Arrays.fill(code2, 0);
        //Generate 2nd m-sequence (5432) of length 31 (1+x^2+x^3+x^4+x^5)
        for (int i = 0; i < 31; i++){
            int temp = register2[4] + register2[3] + register2[2] + register2[1];
            int k = temp / 2;
            temp = temp - k * 2;
            code2[i] = 2 * register2[4] - 1; //[1,-1]
            if (code2[i] < 0) code2[i] = 0; //[1,0]
            for (int j = 4; j > 0; j--){
                register2[j] = register2[j - 1];
            }
            register2[0] = temp;
        }
        //code1 XOR code2 ->>>>>>> gold code output
        for (int i = 0; i < 31; i++){
            code1[i] = code1[i] ^ code2[i];
        }

        return code1;
    }
    /**
     * n=6 Gold Sequence Generator
     * @param seed : 0~4096 integer
     * @return int[63] goldcode
     */
    public static int[] generateGoldSequence6(int seed){
        int[] register1 = new int[6];
        int[] register2 = new int[6];
        for (int i = 0; i < 6; i++)
            register1[i] = (seed >> i) & 0x01;
        for (int i = 0; i < 6; i++)
            register2[i] = (seed >> i) & 0x01;

        int[] code1 = new int[63];
        //Generate 1st m-sequence (61) of length 63 (1+x^1+x^6)
        for (int i = 0; i < 63; i++){
            int temp = (register1[0] + register1[5]) % 2;
            code1[i] = register1[5];//[1,0]
            for (int j = 5; j > 0; j--){
                register1[j] = register1[j - 1];
            }
            register1[0] = temp;
        }

        int[] code2 = new int[63];
        //Generate 2nd m-sequence (6521)th of length 63
        for (int i = 0; i < 63; i++){
            int temp = (register2[5] + register2[4] + register2[1] + register2[0]) % 2;
            code2[i] = register2[5]; //[1,0]
            for (int j = 5; j > 0; j--){
                register2[j] = register2[j - 1];
            }
            register2[0] = temp;
        }
        //code1 XOR code2 ->>>>>>> gold code output
        for (int i = 0; i < 63; i++){
            code1[i] = code1[i] ^ code2[i];
        }

        return code1;
    }
}
