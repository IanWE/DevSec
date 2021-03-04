/*
Stages for Trial mode. Now we only eliminate those unavailable functions.
*/
package com.SMU.DevSec;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import static com.SMU.DevSec.MainActivity.pkg_permission;
import static com.SMU.DevSec.MainActivity.stage;

public class TrialModelStages {
    Context mContext;
    final String TAG = "CacheScan_TrialMode";
    private volatile String[] apps = new String[5];
    boolean stop = false;
    int s = 0;
    int s_1 = 0;
    private int seconds = 10;
    Thread thread;

    TrialModelStages(Context context) {
        mContext = context;
    }

    public int checkconducted() {
        if (CacheScan.getthreshold() == 9999)
            return 2;
        int[] pattern_filter = getFilter();
        Log.d(TAG, "The Number of Filtered Addresses in Camera and Audio:" + Utils.sum(pattern_filter));
        Utils.saveArray(mContext, pattern_filter, "ptfilter");
        return 1;
    }

    /**
     * Method to get last used applications
     *
     */
    public int GetLastApps(){
        int i = 0;
        while(true){
            String app = CacheScan.getTopApp(mContext);
            apps[i] = app;
            i = (i+1)%apps.length;
            if(stop)
                break;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    /**
     * Method to start trial
     *
     */
    public void startDialog() {
        final AlertDialog alertDialog = new AlertDialog.Builder(mContext).create();
        long time = 0;
        alertDialog.show();
        alertDialog.setCancelable(false);
        Window window = alertDialog.getWindow();
        if (window != null) {
            window.setContentView(R.layout.trial_stage);
            window.setGravity(Gravity.CENTER);
            TextView tvContent = window.findViewById(R.id.stage_content);
            //Log.d(TAG,str);
            SpannableStringBuilder ssb = new SpannableStringBuilder();
            ssb.append("You are at the trial mode. We will need " + seconds + " seconds to check the functions. Please do not use camera or audiorecording function during this period.");
            thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            trial1();//eliminate functions those keep poping, in native-lib/native-lib.cpp
                        }
                    });
            thread.start();
            time = System.currentTimeMillis();

            tvContent.setMovementMethod(ScrollingMovementMethod.getInstance());
            tvContent.setText(ssb);
            Button next = window.findViewById(R.id.next_stage);
            final long finalTime = time;
            next.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(s==0){//check the result after 10 seconds
                        long lefttime = System.currentTimeMillis()- finalTime;
                        if(lefttime<seconds*1000){
                            showToast("Please try again in "+(seconds-lefttime/1000)+" seconds.");
                            return;
                        }
                    }
                    int r = checkconducted();
                    SharedPreferences edit = mContext.getSharedPreferences("user", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = edit.edit();
                    if(r==0){
                        showToast("Please follow the instruction to complete the trial.");
                    }
                    if(r==2){
                        try {
                            thread.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        editor.putString("trialmodel","2");
                        editor.commit();
                        showToast("Sorry, your phone is not compatible with our experiment.");
                        // stop service
                        Intent intent = new Intent(mContext, AfterTrialModel.class);
                        mContext.startActivity(intent);
                    }
                    if(r==1) {
                        showToast("Thanks, you have completed all the trials.");
                        stop = true;
                        stage = 0;
                        try {
                            thread.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        editor.putString("trialmodel","1");
                        editor.putLong("lastday",System.currentTimeMillis() / (1000 * 60 * 60 * 24));
                        editor.putLong("day",1);
                        editor.commit();
                        alertDialog.cancel();
                    }
                }
            });
        }
    }

    public void showToast(final String text) {
        new Thread() {
            @Override
            public void run() {
                super.run();
                Looper.prepare();
                try {
                    Toast.makeText(mContext, text, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e("error", e.toString());
                }
                Looper.loop();
            }
        }.start();
    }

    public static native void trial1();
    public static native int[] getFilter();
    //public static native void flush(int c);
}
