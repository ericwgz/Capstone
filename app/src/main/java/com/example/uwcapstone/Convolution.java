package com.example.uwcapstone;

import com.villoren.java.dsp.convolution.AbstractConvolutionD;
import com.villoren.java.dsp.convolution.ConvolutionRealD;
import com.villoren.java.dsp.convolution.FilterKernelD;
import com.villoren.java.dsp.fft.FourierTransformD;

import java.util.Arrays;

public class Convolution {

    private ConvolutionRealD mConvolutionRealD;
    private short[] mModel;

    Convolution(short[] model) {
        mModel = model;
        setFilter_convolution();
    }

    private void setFilter_convolution() {
        int LEN = 0;
        int i = 0;
        int number_of_taps = mModel.length;
        while (LEN < number_of_taps){
            LEN = (int) Math.pow(2, i);
            i++;
        }

        double[] filterModel = new double[LEN];
        double[] coefficients = new double[LEN];
        int k;
        for ( k = 0 ; k < number_of_taps ; k++) {
            coefficients[k] = (double)mModel[number_of_taps - 1 - k] / 32767; // matched filter
        }
        Arrays.fill(coefficients, number_of_taps, LEN, 0.0);
        double[] inImag = new double[LEN];
        double[] outImag = new double[LEN];
        Arrays.fill(inImag, 0, LEN, 0.0);
        Arrays.fill(outImag, 0, LEN, 0.0);

        FourierTransformD fft = new FourierTransformD(LEN, FourierTransformD.Scale.FORWARD);
        fft.transform(coefficients, inImag, filterModel, outImag, false);

        FilterKernelD filterKernel = new FilterKernelD(mConvolutionRealD);

        for (i = 0; i < LEN; i++) {
//            filterKernel.setBin(i, filterModel[i], outImag[i]);
            filterKernel.setBinReal(i, coefficients[i]);
        }
        // Use this filter kernel
        mConvolutionRealD.setFilterKernel(filterKernel);
    }

    double estimate_max_similarity(short[] sample, int start, int end) {

        if (mConvolutionRealD == null) {
            setFilter_convolution();
        }

        int LEN = 0;
        int i = 0;
        int number_of_taps = mModel.length;
        while (LEN < number_of_taps){
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

            mConvolutionRealD.convolve(in_sam, out_sam);
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
}
