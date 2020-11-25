package com.SMU.DevSec;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


import org.pytorch.Module;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "DevSec";
    private static final int PERMISSIONS_REQUEST = 0;
    CrashHandler crashHandler = CrashHandler.getInstance();
    static boolean infering = true;
    static int camera = 0;
    static int audio = 0;
    static int sms = 0;
    static int location = 0;
    static int contact = 0;
    static int calendar = 0;
    static TextView[] status = new TextView[6];
    static boolean uploaded = false;
    static boolean collect_only = true;//Here to set whether launch Deep Learning Model
    Switch mSwitch = null;
    EditText jobStatus = null;
    static long lastday = 0;
    static long day = 1;
    static int stage = 0;
    public static ArrayList<SideChannelValue> sideChannelValues = new ArrayList<>();
    static Module module;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        crashHandler.init(MainActivity.this);//register crashhandler
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeDB();
        // Loading model
        try {
            module = Module.load(assetFilePath(this, "model.pt"));
        } catch (IOException e) {
            Log.e("Pytorch", "Error reading assets", e);
        }
        //Switch
        mSwitch = (Switch) findViewById(R.id.btn_schedule_job);
        jobStatus = (EditText) findViewById(R.id.job_status);
        if(!SideChannelJob.continueRun) {
            mSwitch.setChecked(false);
            checkRunStatus(SideChannelJob.continueRun);
        }
        else {
            mSwitch.setChecked(true);
            checkRunStatus(SideChannelJob.continueRun);
        }

        SharedPreferences edit = getSharedPreferences("user", 0);
        String name = edit.getString("RSA","None");
        if (name!=null&&name.equals("None")) {
            Toast.makeText(getBaseContext(), "Please register first.", Toast.LENGTH_SHORT)
                    .show();
            Intent intent = new Intent(MainActivity.this, Register.class);
            startActivity(intent);//start register page
            mSwitch.setChecked(false);
        }
        // 添加监听 scheduleJob cancelJob
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    SharedPreferences edit = getSharedPreferences("user", 0);
                    String name = edit.getString("RSA", "None");
                    if (name!=null&&name.equals("None")) {
                        Toast.makeText(getBaseContext(), "Please register first.", Toast.LENGTH_SHORT)
                                .show();
                        Intent intent = new Intent(MainActivity.this, Register.class);
                        startActivity(intent);//start register page
                        mSwitch.setChecked(false);
                    } else
                        scheduleJob(buttonView);
                } else {
                    boolean d = cancelJob(buttonView);
                }
            }
        });
        status[0] = findViewById(R.id.sms);
        status[1] = findViewById(R.id.camera);
        status[2] = findViewById(R.id.audio);
        status[3] = findViewById(R.id.location);
        status[4] = findViewById(R.id.callhistory);
        status[5] = findViewById(R.id.calendar);
        //createNotificationChannel(); //register channel
    }

    public void onResume() {
        super.onResume();
    }

    /**
     * Method to check if the job scheduler is running
     *
     * @param isRunning: flag to indicate if the job scheduler is running
     */
    public void checkRunStatus(boolean isRunning) {
        if (isRunning) {
            jobStatus.setText("Stop Detection");
        } else {
            SpannableString spanString = new SpannableString("Start Detection!!!");
            ForegroundColorSpan span = new ForegroundColorSpan(Color.RED);
            spanString.setSpan(span, 15, 18, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
            jobStatus.setText(spanString);
        }
    }

    public void scheduleJob(View v) {
        SideChannelJob.continueRun = true;
        checkRunStatus(SideChannelJob.continueRun);
        // Building the job to be passed to the job scheduler
        Intent begin = new Intent(this, SideChannelJob.class);
        startForegroundService(begin);
        Log.d(TAG, "Job scheduled");
        Toast.makeText(this, "Job is scheduling", Toast.LENGTH_SHORT)
                .show();
    }
    /**
     * Method to stop the job scheduler
     *
     * @param v: the current view at which this method is called
     */
    public boolean cancelJob(View v) {
        Intent stop=new Intent (this,SideChannelJob.class);
        stopService(stop);
        SideChannelJob.continueRun = false;
        checkRunStatus(SideChannelJob.continueRun);
        Log.d(TAG, "Job cancelled");
        return true;
    }

    void initializeDB() {
        // Creating the database file in the app sandbox
        SQLiteDatabase db = getBaseContext().openOrCreateDatabase("SideScan.db",
                MODE_PRIVATE, null);
        Locale locale = new Locale("EN", "SG");
        db.setLocale(locale);

        // Creating the schema of the database
        String sSQL = "CREATE TABLE IF NOT EXISTS " + SideChannelContract.TABLE_NAME + " (" +
                //SideChannelContract.Columns._ID + " INTEGER PRIMARY KEY NOT NULL, " +
                SideChannelContract.Columns.TIMESTAMP + " DATETIME DEFAULT (datetime('now', 'localtime')), " +
                SideChannelContract.Columns.SYSTEM_TIME + " INTEGER NOT NULL, " +
                SideChannelContract.Columns.VOLUME + " INTEGER, " +
                SideChannelContract.Columns.ALLOCATABLE_BYTES + " INTEGER, " +
                SideChannelContract.Columns.CACHE_QUOTA_BYTES + " INTEGER, " +
                SideChannelContract.Columns.CACHE_SIZE + " INTEGER, " +
                SideChannelContract.Columns.FREE_SPACE + " INTEGER, " +
                SideChannelContract.Columns.USABLE_SPACE + " INTEGER, " +
                SideChannelContract.Columns.ELAPSED_CPU_TIME + " INTEGER, " +
                SideChannelContract.Columns.CURRENT_BATTERY_LEVEL + " REAL, " +
                SideChannelContract.Columns.BATTERY_CHARGE_COUNTER + " INTEGER, " +
                SideChannelContract.Columns.MOBILE_TX_BYTES + " INTEGER, " +
                SideChannelContract.Columns.TOTAL_TX_BYTES + " INTEGER, " +
                SideChannelContract.Columns.MOBILE_TX_PACKETS + " INTEGER, " +
                SideChannelContract.Columns.TOTAL_TX_PACKETS + " INTEGER, " +
                SideChannelContract.Columns.MOBILE_RX_BYTES + " INTEGER, " +
                SideChannelContract.Columns.TOTAL_RX_BYTES + " INTEGER, " +
                SideChannelContract.Columns.MOBILE_RX_PACKETS + " INTEGER, " +
                SideChannelContract.Columns.TOTAL_RX_PACKETS + " INTEGER);";
        db.execSQL(sSQL);

        sSQL = "CREATE TABLE IF NOT EXISTS " + SideChannelContract.GROUND_TRUTH + " (" +
                SideChannelContract.Columns.SYSTEM_TIME + " INTEGER NOT NULL, " +
                SideChannelContract.Columns.LABELS + " INTEGER); ";
        db.execSQL(sSQL);
    }

    public static String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }
        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }
}

