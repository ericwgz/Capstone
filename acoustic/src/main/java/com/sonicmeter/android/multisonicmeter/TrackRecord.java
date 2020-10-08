package com.sonicmeter.android.multisonicmeter;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;



public class TrackRecord {

//    private static Logger log = LoggerFactory.getLogger(TrackRecord.class);

    private final long id = System.nanoTime();

    // maximum seconds to buffer
    private static int AUDIO_RING_BUFFER_SEC = 2;

    private ReentrantLock lock;

    private BlockingQueue<short[]> audioList;
    private volatile short[] AUDIO_BUFFER;

    private int CAPACITY = Params.recordSampleLength * 6;

    private int totalSamples = 0;

    private int head = 0;

    private int tail = 0;

    public TrackRecord() {
        // size the buffer
        AUDIO_BUFFER = new short[CAPACITY];
//        for (int i = 0; i < CAPACITY; i++)
//            AUDIO_BUFFER[i] = 0;
        // use a "fair" lock
        lock = new ReentrantLock(true);
        audioList = new LinkedBlockingQueue<>();
    }

    /**
     * Adds a sample to the AUDIO_BUFFER.
     *
     * @param sample
     * @return true if added successfully and false otherwise
     */
    public boolean add(short sample) {
        // reposition the tail
        tail = (tail + 1) % CAPACITY;
        // add the sample to the tail
        AUDIO_BUFFER[tail] = sample;
        // added!
        return true;
    }

    /**
     * Adds an array of samples to the AUDIO_BUFFER.
     *
     * @param samples
     */
    public synchronized void addSamples(short[] samples) {
        try {
            lock.lock();
            if (totalSamples >= CAPACITY)
            {
                audioList.take();
                totalSamples -= samples.length;
            }
            audioList.put(samples);
            totalSamples += samples.length;

        } catch (InterruptedException ie) {
            ie.printStackTrace();
        } finally {
            if (lock.isHeldByCurrentThread())
                lock.unlock();
        }
/*

        try {
            boolean ans = lock.tryLock();//10, TimeUnit.MILLISECONDS
            if (ans) {
                for (short sample : samples) {
                    if (!add(sample)) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
*/
    }
    /**
     * Gets an array of samples from the buffer.
     *
     * @param length : length of sample
     * @return sample[]
     */
    public short[] getSamples(int length) {
        short[] samples = null;

        try {
            lock.lock();

            int audioBufferNumber = audioList.size();
            if (audioBufferNumber <= 0)
            {
                return samples;
            }
            // Get total samples
            int one_buffer_len = audioList.element().length;
            int startBlockNum = 0;
            int realSampleBlockNum = 0;

            // Calculate start block number and create sample buffer.
            if (length > totalSamples) {
                startBlockNum = 0;
                samples = new short[totalSamples];
            } else {
                startBlockNum = (totalSamples - length) / one_buffer_len;
                samples = new short[(audioBufferNumber - startBlockNum) * one_buffer_len];
            }

            // Fill sample buffer
            int bufferIdx = 0;
            int fillSamples = 0;
            for (short[] buffer : audioList) {
                if (bufferIdx < startBlockNum) {
                    bufferIdx++;
                    continue;
                }

                System.arraycopy(buffer, 0, samples, fillSamples, buffer.length);
                fillSamples += buffer.length;

                if (fillSamples >= samples.length)
                    break;

                bufferIdx++;
            }
            return samples;
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if (lock.isHeldByCurrentThread())
                lock.unlock();
        }

        return samples;
/*
        short[] samples = new short[length];
        try {
            lock.lock();//tryLock(10, TimeUnit.MILLISECONDS);
            for (int i = 0; i < length; i++){
                samples[length - 1 - i] = AUDIO_BUFFER[(CAPACITY + tail - i) % CAPACITY];
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (lock.isLocked()) {
                lock.unlock();
            }
        }
        return samples;
*/
    }

    public boolean releaseLock(){
        boolean ret = false;
        try {
            if (lock.isLocked()) {
                lock.unlock();
                ret = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }
}
