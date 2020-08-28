package com.SMU.DevSec;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.TextView;

import org.pytorch.IValue;
import org.pytorch.Tensor;

import java.util.ArrayList;
import java.util.Arrays;

import static com.SMU.DevSec.MainActivity.*;
import static com.SMU.DevSec.SideChannelContract.CLASSES;


//import static com.DevSec.acronisdc.MainActivity.module;


class Classifier implements Runnable {
    Context context;
    TextView prediction;
    ArrayList<SideChannelValue> sideChannelValues;
    long startTime;
    //ArrayList<Float> X = new ArrayList<Float>(1020);
    final float[] X = new float[1020];
    final long[] shape = new long[]{1,17,60};

    SQLiteDatabase db;
    private static final String TAG = "PredictRunnable";

    public Classifier(Context context, ArrayList<SideChannelValue> sideChannelValues) {
        this.context = context;
        this.sideChannelValues = sideChannelValues;
    }

    @Override
    public void run() {
        // Start timing the entire process and open the database
        startTime = System.currentTimeMillis();
        for (int i=0;i<60;i++) {
            X[0*60+i]=(float) sideChannelValues.get(i).getVolume();
            X[1*60+i]=(float) sideChannelValues.get(i).getAllocatableBytes();
            X[2*60+i]=(float) sideChannelValues.get(i).getCacheQuotaBytes();
            X[3*60+i]=(float) sideChannelValues.get(i).getCacheSize();
            X[4*60+i]=(float) sideChannelValues.get(i).getFreeSpace();
            X[5*60+i]=(float) sideChannelValues.get(i).getUsableSpace();
            X[6*60+i]=(float) sideChannelValues.get(i).getElapsedCpuTime();
            X[7*60+i]=sideChannelValues.get(i).getCurrentBatteryLevel();
            X[8*60+i]=(float) sideChannelValues.get(i).getBatteryChargeCounter();
            X[9*60+i]=(float) sideChannelValues.get(i).getMobileTxBytes();
            X[10*60+i]=(float) sideChannelValues.get(i).getTotalTxBytes();
            X[11*60+i]=(float) sideChannelValues.get(i).getMobileTxPackets();
            X[12*60+i]=(float) sideChannelValues.get(i).getTotalTxPackets();
            X[13*60+i]=(float) sideChannelValues.get(i).getMobileRxBytes();
            X[14*60+i]=(float) sideChannelValues.get(i).getTotalRxBytes();
            X[15*60+i]=(float) sideChannelValues.get(i).getMobileRxPackets();
            X[16*60+i]=(float) sideChannelValues.get(i).getTotalRxPackets();
        }
        for(int i=X.length-1;i>=0;i--){
            if(i % 60!=0) X[i] = X[i]-X[i-1];
            else X[i]=(float) 0;
        }
        final int result = Infer(X);
        /*
        if(result>0) {
            status[result].post(new Runnable() {
                @Override
                public void run() {
                    switch (result) {
                        case 1:
                            camera++;
                            status[result].setText("Camera - " + camera);
                            break;
                        case 2:
                            audio++;
                            status[result].setText("AudioRecording - " + audio);
                            break;
                        case 3:
                            sms++;
                            status[result].setText("ReadSMS - " + sms);
                            break;
                        case 4:
                            location++;
                            status[result].setText("RequestLocation - " + location);
                            break;
                        case 5:
                            contact++;
                            status[result].setText("ReadContacts - " + contact);
                            break;
                    }
                }
            });
        }
         */
        Log.d(TAG,CLASSES[result]);

    }

    public int Infer(float[] X) {
        Tensor inputTensor = Tensor.fromBlob(X,shape);
        // running the model
        Tensor outputTensor = module.forward(IValue.from(inputTensor)).toTensor();
        // getting tensor content as java array of floats
        final float[][] scores = {outputTensor.getDataAsFloatArray()};
        // searching for the index with maximum score
        float maxScore = -Float.MAX_VALUE;
        int maxScoreIdx = -1;
        Log.d("result", Arrays.toString(outputTensor.getDataAsFloatArray()));
        for (int i = 0; i < scores[0].length; i++) {
            if (scores[0][i] > maxScore) {
                maxScore = scores[0][i];
                maxScoreIdx = i;
            }
        }
        return maxScoreIdx;

    }
}
