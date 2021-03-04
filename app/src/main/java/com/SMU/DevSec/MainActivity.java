/*
This is the entry file of the application. 
The whole logic is :
1. Ask for 'Usage Stats' permisson, and do items check if applicable.
2. Ask for registration if applicable.
3. If registered but not finish the trial mode, turn to the trial mode if applicable.
4. Once finish the trail mode, application will schedule a foreground service to start scanning.
*/
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
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "DevSec";
    private static final int PERMISSIONS_REQUEST = 0;
    public static HashMap<String, String> pkg_name = new HashMap<>(); //Package to AppName
    public static HashMap<String, Integer> pkg_permission = new HashMap<>(); //Package to Permission
    CrashHandler crashHandler = CrashHandler.getInstance(); //Register a CrashHandler
    TimerManager timerManager = TimerManager.getInstance(); //Uploader
    private static final int ready = 0;
    public static String previous_name = "";
    static boolean infering = true;
    // Number of Events
    static int camera = 0;
    static int audio = 0;
    static int location = 0;
    static int quering = 0;
    static TextView[] status = new TextView[6];
    static boolean uploaded = false;
    static boolean collect_only = true;
    Intent intent;
    static boolean check = false;
    static Switch[] mSwitch = {null}; //there will be a memory leak warning if we do not use list
    static EditText[] jobStatus = {null};
    private static Toast toast;
    private PermissionRequire permissionRequire;
    public static final int SIZE_LIMIT = 1000; //Upload the compressed database, once reach "SIZE_LIMIT" MB
    public static final int TIME_INTERVAL = 10; //check the size every "TIME_INTERVAL" minutes
    public static boolean isCollected = false;
    static String trial;
    static long lastday = 0;
    static long day = 1;
    ImageView imageView;
    Dialog imgdialog;
    ImageView image;
    static int count_threshold = 1;
    static int preset_threshold = 0;
    static CacheScan cs=null;
    static int stage = 0;
    String name;

    public static ArrayList<SideChannelValue> sideChannelValues = new ArrayList<>();
    public static ArrayList<GroundTruthValue> groundTruthValues = new ArrayList<>();
    public static ArrayList<UserFeedback> userFeedbacks = new ArrayList<>();
    public static ArrayList<CompilerValue> compilerValues = new ArrayList<>();
    public static ArrayList<FrontAppValue> frontAppValues = new ArrayList<>();
    static {
        System.loadLibrary("native-lib"); //jni lib to use libflush
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        crashHandler.init(MainActivity.this);//register crashhandler
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_backup);
        toast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);
        //Request Usage Stat permission and check if successfully
        permissionRequire = new PermissionRequire(MainActivity.this);
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = 0;
        if (appOps != null) {
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    mode = appOps.unsafeCheckOpNoThrow("android:get_usage_stats",
                            Process.myUid(), getPackageName());
                }
            }
            boolean granted = mode == AppOpsManager.MODE_ALLOWED;
            if (!granted) {
                permissionRequire.startDialog();
            }
        }
        //Items check
        ItemsCheck itemsCheck = new ItemsCheck(MainActivity.this);
        if(!itemsCheck.isAgreed())
            itemsCheck.startDialog();
        // Initialize database
        initializeDB();
        getAllAppInfos();
        //Switch
        mSwitch[0] = (Switch) findViewById(R.id.btn_schedule_job);
        jobStatus[0] = (EditText) findViewById(R.id.job_status);
        SharedPreferences edit = getSharedPreferences("user", 0);
        name = edit.getString("RSA", "None");
        trial = edit.getString("trialmode", "0");
        lastday = edit.getLong("lastday",0);
        day = edit.getLong("day",1);
        if(check){//every time entering the app, check the running status.
            if(!SideChannelJob.continueRun) {
                mSwitch[0].setChecked(false);
                checkRunStatus(SideChannelJob.continueRun);
            }
            else {
                mSwitch[0].setChecked(true);
                checkRunStatus(SideChannelJob.continueRun);
                CacheScan.ischeckedaddr = false;
            }
        }

        //Register
        if (name!=null&&name.equals("None")) {
            Toast.makeText(getBaseContext(), "Please register first.", Toast.LENGTH_SHORT)
                    .show();
            intent = new Intent(MainActivity.this, Register.class);
            startActivity(intent);//start register page
            mSwitch[0].setChecked(false);
        }

        //scheduleJob cancelJob
        mSwitch[0].setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if(name==null||name.equals("None")) { //Load the RSA key to check if registered
                        SharedPreferences edit = getSharedPreferences("user", 0);
                        name = edit.getString("RSA", "None");
                    }
                    if (name!=null&&name.equals("None")) { //If name == "None", then turn to register
                        Toast.makeText(getBaseContext(), "Please register first.", Toast.LENGTH_SHORT)
                                .show();
                        intent = new Intent(MainActivity.this, Register.class);
                        startActivity(intent);//start register page
                        mSwitch[0].setChecked(false);
                    } else
                        scheduleJob(buttonView);
                } else {
                    boolean d = cancelJob(buttonView);
                    if(!d){
                        mSwitch[0].setChecked(true);
                    }
                }
            }
        });
        status[0] = findViewById(R.id.query);
        status[1] = findViewById(R.id.camera);
        status[2] = findViewById(R.id.audio);
        status[3] = findViewById(R.id.location);
        status[4] = findViewById(R.id.notification);
        status[5] = findViewById(R.id.dayx);

        createNotificationChannel(); //register channel
        //image event
        imageView = (ImageView) findViewById(R.id.example);
        imgdialog = new Dialog(MainActivity.this,R.style.FullActivity);
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        attributes.width = WindowManager.LayoutParams.MATCH_PARENT;
        attributes.height = WindowManager.LayoutParams.MATCH_PARENT;
        imgdialog.getWindow().setAttributes(attributes);
        image = getImageView();
        imgdialog.setContentView(image);
        //click listener
        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imgdialog.dismiss();
            }
        });
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imgdialog.show();
            }
        });
        //start
        if(trial.equals("1")) {
            timerManager.schedule_upload(getBaseContext());//Schedule the upload
            mSwitch[0].setChecked(true);
        }
    }

    public void onResume() {
        super.onResume();
        if(name==null||name.equals("None")) {
            SharedPreferences edit = getSharedPreferences("user", 0);
            name = edit.getString("RSA", "None");
        }
        //check permisson
        permissionRequire = new PermissionRequire(MainActivity.this);
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = 0;
        boolean granted;//=false;
        if (appOps != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    mode = appOps.unsafeCheckOpNoThrow("android:get_usage_stats",
                            Process.myUid(), getPackageName());
                }
        }
        granted = mode == AppOpsManager.MODE_ALLOWED;
        if(granted&&name!=null&&!name.equals("None")&&!trial.equals("1")) {
            intent = new Intent(MainActivity.this, TrialModel.class); //enter trial mode
            startActivity(intent);
        }
        if(granted&&name!=null&&!name.equals("None")&&trial.equals("1")) {
            if(!SideChannelJob.continueRun)
                timerManager.schedule_upload(getBaseContext());
                mSwitch[0].setChecked(true);
        }
    }

    //Dynamic ImageView
    private ImageView getImageView(){
        ImageView imageView = new ImageView(this);
        imageView.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        @SuppressLint("ResourceType") InputStream is = getResources().openRawResource(R.drawable.example);
        Drawable drawable = BitmapDrawable.createFromStream(is, null);
        imageView.setImageDrawable(drawable);
        return imageView;
    }

    public static void showToast(String message, int time) {
        if (toast != null) {
            toast.setText(message);
            toast.setDuration(time);
            toast.show();
        }
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        CharSequence name = getString(R.string.notification_channel_name);
        String description = "Notification for captured behaviours";
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel channel = new NotificationChannel("behaviour capture", name, importance);
        channel.setDescription(description);
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    /*
     * AppInfo
     */
    protected void getAllAppInfos() {
        if(pkg_permission!=null&&!pkg_permission.isEmpty())
            return;
        final PackageManager packageManager = getPackageManager();
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> ResolveInfos = packageManager.queryIntentActivities(
                intent, 0);
        final PackageManager pm = getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ResolveInfo ri : ResolveInfos) {
            String path = null;
            String packageName = ri.activityInfo.packageName;
            String appName = ri.loadLabel(packageManager).toString();
            pkg_name.put(packageName,appName);
            pkg_permission.put(appName,read_permissions(packageName));
        }
    }

    /**
     * Method to read permissions
     *
     * @param packageName: package Name
     */
    public int read_permissions(String packageName) {
        int[] judge = {0, 0};
        int type = 0;
        PackageManager pm = getPackageManager();
        PackageInfo info;
        try {
            int pmnumber = 0;
            boolean flag = (PackageManager.PERMISSION_GRANTED ==
                    pm.checkPermission("android.permission.RECORD_AUDIO", packageName));
            if(flag)
                type+=2;
            flag = (PackageManager.PERMISSION_GRANTED ==
                    pm.checkPermission("android.permission.CAMERA", packageName));
            if(flag)
                type+=1;
            return type;
            } catch (Exception ex) {
            ex.printStackTrace();
        }
        return 0;
    }

    /**
     * Method to check permissions
     */
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.d(TAG, "RequestPermission");
        if (PERMISSIONS_REQUEST != requestCode) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }
        boolean areAllPermissionsGranted = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                areAllPermissionsGranted = false;
                Log.d(TAG, "Not All Permission");
                break;
            }
        }
        if (areAllPermissionsGranted) {
            Log.d(TAG, "onRequestPermissionsResult: Required permissions granted");
        }
    }

    /**
     * Method to check if the job scheduler is running
     *
     * @param isRunning: flag to indicate if the job scheduler is running
     */
    public static void checkRunStatus(boolean isRunning) {
        if (isRunning) {
            jobStatus[0].setText("Stop Detection");
        } else {
            SpannableString spanString = new SpannableString("Start Detection!!!");
            ForegroundColorSpan span = new ForegroundColorSpan(Color.RED);
            spanString.setSpan(span, 15, 18, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
            jobStatus[0].setText(spanString);
        }
    }

    /**
     * Method to start the job scheduler
     *
     * @param v: the current view at which this method is called
     */
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
        if(!check) {
            Toast.makeText(this, "Please try it later.", Toast.LENGTH_SHORT)
                    .show();
            return false;
        }
        stopService(stop);
        SideChannelJob.continueRun = false;
        checkRunStatus(SideChannelJob.continueRun);
        Log.d(TAG, "Job cancelled");
        return true;
    }

    /**
     * Method to initialize database
     */
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

        sSQL = "CREATE TABLE IF NOT EXISTS " + SideChannelContract.USER_FEEDBACK+ " (" +
                SideChannelContract.Columns.ARISINGTIME + " INTEGER NOT NULL, " +
                SideChannelContract.Columns.EVENT + " INTEGER, "+
                SideChannelContract.Columns.CURRENT_APP + " INTEGER, "+
                SideChannelContract.Columns.ANSWERINGTIME + " INTEGER, "+
                SideChannelContract.Columns.CHOICES + " INTEGER, "+
                SideChannelContract.Columns.PATTERN + " INTEGER); ";
        db.execSQL(sSQL);

        sSQL = "CREATE TABLE IF NOT EXISTS " + SideChannelContract.SIDE_COMPILER+ " (" +
                SideChannelContract.Columns.SYSTEM_TIME + " INTEGER NOT NULL, " +
                SideChannelContract.Columns.THRESHOLDS + " INTEGER," +
                SideChannelContract.Columns.FUNCTIONS + " INTEGER); ";
        db.execSQL(sSQL);

        sSQL = "CREATE TABLE IF NOT EXISTS " + SideChannelContract.FRONT_APP+ " (" +
                SideChannelContract.Columns.SYSTEM_TIME + " INTEGER NOT NULL, " +
                SideChannelContract.Columns.CURRENT_APP + " TEXT); ";
        db.execSQL(sSQL);

        String V = "Version"+Utils.getVersionName(getBaseContext());
        sSQL = "CREATE TABLE IF NOT EXISTS " + V +  " (" +
                SideChannelContract.Columns.SYSTEM_TIME + " )";//1600592322078 1600591910848
        db.execSQL(sSQL);
        db.close();
    }
}

