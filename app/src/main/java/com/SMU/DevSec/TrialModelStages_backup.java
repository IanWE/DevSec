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

public class TrialModelStages_backup {
    private static TrialModelStages_backup INSTANCE = null;
    final String TAG = "CacheScan_ItemsCheck";
    volatile String[] apps = new String[5];
    boolean stop = false;
    int s = 0;
    int s_1 = 0;
    int seconds = 10;

    public TrialModelStages_backup() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                GetLastApps();
            }
        }).start();
    }

    public static TrialModelStages_backup getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new TrialModelStages_backup();
        }
        return INSTANCE;
    }

    public int checkconducted(Context mContext,int c) {
        if(c==0) {
            //stage1
            //if(s_1==0)
            //    return 1;
            if(CacheScan.getthreshold()==9999)
                return 2;
            int[] pattern_filter = getFilter();
            Log.d(TAG, "The Number of Filtered Addresses in Camera and Audio:" + Utils.sum(pattern_filter));
            Utils.saveArray(mContext, pattern_filter, "ptfilter");
            return 1;
        }
        //stage2
        if (c==2||c==1) {
            if(apps.length==0)
                return 0;
            for(int i=apps.length-1;i>=0;i--){
                int permission_type = 0;
                if (pkg_permission.containsKey(apps[i]))
                    permission_type = pkg_permission.get(apps[i]);
                Log.d(TAG,"Application:"+apps[i]+", Permisson:"+permission_type);
                if ((permission_type & c) == c) {//check the permisson 1 camera 2 audio
                    int[] pattern = CacheScan.getPattern(c);
                    //sum the pattern
                    int sum = Utils.sum(pattern);
                    Log.d(TAG,"Activated Function:"+sum);
                    if (sum == 0)//if no activations
                        return 2;
                    Utils.saveArray(mContext, pattern, c + "");
                    return 1;
                }
            }
        }
        return 0;
    }

    public int GetLastApps(){
        int i = 0;
        while(true){
            String app = CacheScan.getTopApp();
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

    public void startDialog(final Context mContext) {
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
            if(s==0){
                //if(s_1!=0) {
                    //ssb.append("You are at stage 1. We will need " + seconds + " seconds to check the functions. Please switch to the home page and wait for "+seconds+" seconds; then switch back.");
                ssb.append("You are at the trial mode. We will need " + seconds + " seconds to check the functions. Please do not use camera or audiorecording function during this period.");
                new Thread(new Runnable() {
                        @Override
                        public void run() {
                            trial1();//
                        }
                    }).start();
                    time = System.currentTimeMillis();
                //} else{
                //    ssb.append("    You are at stage 1-2. Please try to launch 3 apps you use everyday (such as Instagram, Telegram and WhatsApp, etc.)," +
                //            " and keep them for few seconds. This can help eliminate some false notifications.\n" +
                //            "    It should be noted that do not use the camera and recording functions of these apps during the stage 1-2. " +
                //            "\n    After that, please switch back and click the \"Next\" button");
                //}
            }
            if (s == 1)
                ssb.append("(Important!) You are at stage 2. Please turn on the Camera for 3 seconds and close it; then switch back and click the \"Next\" button");
            else if(s==2)
                ssb.append("(Important!) You are at stage 3. Please turn on the Whatsapp(or Telegram/Wechat) and use the in-app recording function for 3 seconds and close it; then switch back and click the \"Next\" button");
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
                            showToast(mContext,"Please try again in "+(seconds-lefttime/1000)+" seconds.");
                            return;
                        }
                    }
                    int r = checkconducted(mContext,s);
                    SharedPreferences edit = mContext.getSharedPreferences("user", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = edit.edit();
                    if(r==0){
                        showToast(mContext,"Please follow the instruction to complete the trial.");
                    }
                    if(r==2){
                        editor.putString("trialmodel","2");
                        editor.commit();
                        showToast(mContext,"Sorry, your phone is not compatible with our experiment.");
                        // stop service
                        Intent intent = new Intent(mContext, AfterTrialModel.class);
                        mContext.startActivity(intent);
                    }
                    if(r==1) {
                        if (s == 0) {//finish directly, do not enter the next step.
                            /*
                            //if(s_1==0){
                            //    s_1 = 1;
                            //    alertDialog.cancel();
                            //    getInstance(mContext).startDialog();
                            //    return;
                            //}
                            s = 1;
                            stage = 2;//flag to start scan
                            alertDialog.cancel();
                            getInstance(mContext).startDialog();
                            return;
                             */
                            showToast(mContext,"Thanks, you have completed all the trials.");
                            stop = true;
                            stage = 0;
                            editor.putString("trialmodel","1");
                            editor.putLong("lastday",System.currentTimeMillis() / (1000 * 60 * 60 * 24));
                            editor.putLong("day",1);
                            editor.commit();
                            alertDialog.cancel();
                        }
                        if (s == 1) {
                            s = 2;
                            flush(s);
                            alertDialog.cancel();
                            getInstance().startDialog(mContext);
                            return;
                        }
                        if (s == 2) {
                            showToast(mContext,"Thanks, you have completed all the trials.");
                            stop = true;
                            editor.putString("trialmodel","1");
                            editor.putLong("lastday",System.currentTimeMillis() / (1000 * 60 * 60 * 24));
                            editor.putLong("day",0);
                            editor.commit();
                            alertDialog.cancel();
                            Intent intent = new Intent(mContext, AfterTrialModel.class);
                            mContext.startActivity(intent);
                        }
                    }
                }
            });
        }
    }

    public void showToast(final Context mContext, final String text) {
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
    public static native void flush(int c);
}
