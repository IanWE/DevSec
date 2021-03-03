package com.SMU.DevSec;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Time;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.CRC32;

import static android.content.Context.MODE_PRIVATE;
import static com.SMU.DevSec.JobInsertRunnable.insert_locker;
import static com.SMU.DevSec.MainActivity.TIME_INTERVAL;
import static com.SMU.DevSec.MainActivity.check;
import static com.SMU.DevSec.MainActivity.mSwitch;
import static com.SMU.DevSec.MainActivity.showToast;

public class TimerManager {
    /**
     * @param args
     */
    private static TimerManager INSTANCE = null;
    private final String DATABASE_PATH = "/data/data/com.SMU.DevSec/databases/";
    private final String DATABASE_FILENAME = "SideScan.db";
    private String name = "None";//
    private String adler = "None";
    private final String TAG = "TimeManager";
    //static String requestUrl = "http://202.161.45.163:80/upload/";
    static String requestUrl = "http://139.180.153.71/";
    private boolean scheduled = false;
    private boolean scheduled1 = false;

    //时间间隔(一天)
    private static final long PERIOD_DAY = 24 * 60 * 60 * 1000;
    private TimerManager(){
        //do something
    }
    /**
     * Initialize
     */
    public static TimerManager getInstance(){
        if(INSTANCE==null)
            INSTANCE = new TimerManager();
        return INSTANCE;
    }

    /**
     * Schedule to upload data and logs
     */
    void schedule_upload(final Context mContext) {
        if(scheduled)
            return;
        scheduled = true;
        SharedPreferences edit = mContext.getSharedPreferences("user", 0);//Get name
        name = edit.getString("RSA", "None");
        adler = edit.getString("adler", "None");
        Log.d(TAG,"schedule to check weather upload every 10 mins");
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0); //0点
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        Date date = calendar.getTime(); //第一次执行定时任务的时间
        //如果第一次执行定时任务的时间 小于当前的时间
        //此时要在 第一次执行定时任务的时间加一天，以便此任务在下个时间点执行。如果不加一天，任务会立即执行。
        if (date.before(new Date())) {
            date = this.addDay(date, 1);
        }
        Timer timer = new Timer();
        //安排指定的任务在指定的时间开始进行重复的固定延迟执行。
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (getwifistate(mContext) == 2) {
                    uploadFile(mContext);
                    uploadLogs(mContext);
                } else {
                    Log.i("uploading", "No WIFI connected");
                    //if there are more than 30 files, stop service;
                }
                File[] files = getfilelist(mContext);
                if(files!=null&&files.length>=100&&SideChannelJob.continueRun){
                    Intent stop=new Intent (mContext,SideChannelJob.class);
                    if(!check) {
                        return;
                    }
                    mContext.stopService(stop);
                    SideChannelJob.continueRun = false;
                    mSwitch[0].post(new Runnable() {
                        @Override
                        public void run() {
                            mSwitch[0].setChecked(false);
                        }
                    });
                    showToast(mContext,"You have more than 100 files need to be uploaded");
                    MainActivity.checkRunStatus(SideChannelJob.continueRun);
                    Log.d(TAG, "Job cancelled");
                }
            }
        }, 3*60*1000, TIME_INTERVAL*60*1000);
    }

    // 增加或减少天数
    private Date addDay(Date date, int num) {
        Calendar startDT = Calendar.getInstance();
        startDT.setTime(date);
        startDT.add(Calendar.DAY_OF_MONTH, num);
        return startDT.getTime();
    }

    private int getwifistate(Context mContext) {
        int result = 0; // Returns connection type. 0: none; 1: mobile data; 2: wifi
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
            if (capabilities != null) {
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    result = 2;
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    result = 1;
                }
            }
        }
        return result;
    }

    void uploadLogs(Context mContext) {
        if(name.equals("None")) {
            SharedPreferences edit = mContext.getSharedPreferences("user", 0);//Get name
            name = edit.getString("RSA", "None");
            adler = edit.getString("adler", "None");
        }
        if(!name.equals("None")) {
            Log.i("uploading", "upload logs start");
            //上传文件
            File file = new File(mContext.getFilesDir(),"log.txt");
            if(!file.exists()){
                Log.d(TAG,"File not exists");
                return;
            }
            if (getwifistate(mContext) != 0) {
                if (file.getName().endsWith("txt") && SocketHttpRequester.uploadFile(file, requestUrl, name).equals("Success")) {
                    //reinitializeDB();
                    file.delete();
                    Log.i("uploading", "Uploaded Logs Successfully");
                    //showToast("Uploaded successfully.", 0);
                } else
                    Log.i("uploading", "Uploading Logs failed");
            } else {
                Log.i("uploading", "No WIFI connected");
                //showToast("No WIFI connected.", 0);
            }
        }
    }

    void uploadFile(Context mContext) {
        if(name.equals("None")) {
            SharedPreferences edit = mContext.getSharedPreferences("user", 0);//Get name
            name = edit.getString("RSA", "None");
            adler = edit.getString("adler", "None");
        }
        if(!name.equals("None")) {
            Log.i("uploading", "upload start");
            //上传文件
            File[] files = getfilelist(mContext);
            if (files == null) {
                return;
            }
            if (getwifistate(mContext) == 2) {
                for (File file : files) {
                    if (file.getName().endsWith("gz") && SocketHttpRequester.uploadFile(file, requestUrl, name).equals("Success")) {
                        //reinitializeDB();
                        file.delete();
                        Log.i("uploading", "Uploaded Successfully");
                        //showToast("Uploaded successfully.", 0);
                    } else
                        Log.i("uploading", "Uploading failed or no file");
                }//for
            } else {
                Log.i("uploading", "No WIFI connected");
                //showToast("No WIFI connected.", 0);
            }
        }
    }

    String getCode(Context mContext,String name){
        if(getwifistate(mContext)!=0)
            return SocketHttpRequester.getCode(requestUrl,name);
        return null;
    }

    String uploadTimeCheck(Context mContext,long tm){
        if(name.equals("None")) {
            SharedPreferences edit = mContext.getSharedPreferences("user", 0);//Get name
            name = edit.getString("RSA", "None");
            adler = edit.getString("adler", "None");
        }
        if(!name.equals("None")&&getwifistate(mContext)!=0&&tm>=0)
            return SocketHttpRequester.uploadRunningTime(requestUrl+"timecheck/"+adler+"/"+tm);
        return null;
    }

    private File[] getfilelist(Context mContext){
        final File filedir = mContext.getFilesDir();
        if (!filedir.exists()) {//判断路径是否存在
            return null;
        }
        //Compress
        //Log.d("uploading", filedir.getName());
        return filedir.listFiles();
    }

    private void showToast(final Context mContext, final String text) {
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

}





