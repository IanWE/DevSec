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
    Context mContext;
    private static TimerManager INSTANCE = null;
    private final String DATABASE_PATH = "/data/data/com.SMU.DevSec/databases/";
    private final String DATABASE_FILENAME = "SideScan.db";
    private String name;
    private final String TAG = "TimeManager";
    String requestUrl = "http://202.161.45.163:80/upload/";
    private boolean scheduled = false;

    //时间间隔(一天)
    private static final long PERIOD_DAY = 24 * 60 * 60 * 1000;

    private TimerManager(Context context){
        init(context);
    }
    /**
     * 初始化目录
     */
    private void init(Context context) {
        mContext = context;
    }

    public static TimerManager getInstance(Context context){
        if(INSTANCE==null)
            INSTANCE = new TimerManager(context);
        return INSTANCE;
    }

    public void schedule() {
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
                //final File file = new File(DATABASE_PATH + DATABASE_FILENAME);
                //Log.d("uploading", file.getName());
                if (getwifistate() == 2) {
                    uploadFile();
                } else
                    Log.i("uploading", "No WIFI connected");
            }
        }, date, PERIOD_DAY);
    }

    public void schedule_upload() {
        if(scheduled)
            return;
        scheduled = true;
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
                //final File file = new File(DATABASE_PATH + DATABASE_FILENAME);
                //Log.d("uploading", file.getName());
                if (getwifistate() == 2) {
                    uploadFile();
                    uploadLogs();
                } else {
                    Log.i("uploading", "No WIFI connected");
                    //if there are more than 30 files, stop service;
                }
                File[] files = getfilelist();
                if(files.length>=50&&SideChannelJob.continueRun){
                    Intent stop=new Intent (mContext,SideChannelJob.class);
                    if(!check) {
                        return;
                    }
                    mContext.stopService(stop);
                    SideChannelJob.continueRun = false;
                    mSwitch.post(new Runnable() {
                        @Override
                        public void run() {
                            mSwitch.setChecked(false);
                        }
                    });
                    MainActivity.checkRunStatus(SideChannelJob.continueRun);
                    Log.d(TAG, "Job cancelled");
                }
            }
        }, 60*1000, TIME_INTERVAL*60*1000);
    }

    // 增加或减少天数
    private Date addDay(Date date, int num) {
        Calendar startDT = Calendar.getInstance();
        startDT.setTime(date);
        startDT.add(Calendar.DAY_OF_MONTH, num);
        return startDT.getTime();
    }

    private int getwifistate() {
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

    void uploadLogs() {
        SharedPreferences edit = mContext.getSharedPreferences("user", 0);//Get name
        name = edit.getString("RSA", "None");
        if(!name.equals("None")) {
            Log.i("uploading", "upload logs start");
            //上传文件
            File file = new File(mContext.getFilesDir(),"log.txt");
            if(!file.exists()){
                Log.d(TAG,"File not exists");
                return;
            }
            if (getwifistate() != 0) {
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

    void uploadFile() {
        SharedPreferences edit = mContext.getSharedPreferences("user", 0);//Get name
        name = edit.getString("RSA", "None");
        if(!name.equals("None")) {
            Log.i("uploading", "upload start");
            //上传文件
            File[] files = getfilelist();
            if (files == null) {
                return;
            }
            if (getwifistate() == 2) {
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

    String getCode(String name){
        if(getwifistate()!=0)
            return SocketHttpRequester.getCode(requestUrl,name);
        return null;
    }

    File[] getfilelist(){
        final File filedir = mContext.getFilesDir();
        if (!filedir.exists()) {//判断路径是否存在
            return null;
        }
        //Compress
        //Log.d("uploading", filedir.getName());
        final File[] files = filedir.listFiles();
        return files;
    }
}





