package com.SMU.DevSec;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.Context.MODE_PRIVATE;
import static com.SMU.DevSec.MainActivity.showToast;

public class TimerManager {
    /**
     * @param args
     */
    Context mContext;

    private final String DATABASE_PATH = "/data/data/com.SMU.DevSec/databases/";
    private final String DATABASE_FILENAME = "SideScan.db";
    private String name;
    String requestUrl = "http://139.180.153.72:5000";

    public TimerManager(Context context) {
        mContext = context;
    }

    //时间间隔(一天)
    private static final long PERIOD_DAY = 24 * 60 * 60 * 1000;

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
                } else Log.i("uploading", "No WIFI connected");
            }
        }, date, PERIOD_DAY);
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

    private void uploadFile(File file) {
        Log.i("uploading", "upload start");
        //上传文件
        if(SocketHttpRequester.uploadFile(file, requestUrl, name)==200) {
            reinitializeDB();
            showToast("Uploaded successfully.",0);
        }
    };


    void uploadFile() {
        SharedPreferences edit = mContext.getSharedPreferences("user", 0);//Get name
        name = edit.getString("RSA", "None");
        if(!name.equals("None")){
            Log.i("uploading", "upload start");
            //上传文件
            final File file = new File(DATABASE_PATH + DATABASE_FILENAME);
            Log.d("uploading", file.getName());
            if (getwifistate() == 2) {
                if (SocketHttpRequester.uploadFile(file, requestUrl, name) == 200) {
                    reinitializeDB();
                    showToast("Uploaded successfully.",0);
                }
            } else {
                Log.i("uploading", "No WIFI connected");
                showToast("No WIFI connected.", 0);
            }
        }
    }

    private void reinitializeDB() {
        // Creating the database file in the app sandbox
        SQLiteDatabase db = mContext.openOrCreateDatabase("SideScan.db",
                MODE_PRIVATE, null);
        Locale locale = new Locale("EN", "SG");
        db.setLocale(locale);
        db.execSQL("delete from " + SideChannelContract.TABLE_NAME);
        db.execSQL("delete from " + SideChannelContract.GROUND_TRUTH);
        db.execSQL("delete from " + SideChannelContract.USER_FEEDBACK);
        db.close();
        Log.d("uploading", "Reinitialized Database");
    }
}





