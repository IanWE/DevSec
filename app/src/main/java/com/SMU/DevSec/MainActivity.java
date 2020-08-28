package com.SMU.DevSec;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AppOpsManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.pytorch.Module;

import static com.SMU.DevSec.CacheScan.decrease;
import static com.SMU.DevSec.CacheScan.increase;


//public class MainActivity extends AppCompatActivity implements View.OnClickListener {
public class MainActivity extends AppCompatActivity {
    private List<AppInfo> appInfo;
    public static final String NAME = "DevSec";
    public static final String TAG = "MainActivity";
    private static final int PERMISSIONS_REQUEST = 0;
    public static HashMap<String, Integer> name_permisson = new HashMap<>();
    private String packageName;
    EditText jobStatus;
    private String DATABASE_PATH;
    private final String DATABASE_FILENAME = "SideScan.db";

    private static final int ready = 0;
    public static String previous_name = "";
    static Module module = null;
    static boolean infering = true;
    static int camera = 0;
    static int audio = 0;
    //static int sms = 0;
    static int location = 0;
    //static int contact = 0;
    static int quering = 0;
    static TextView[] status = new TextView[5];
    static boolean uploaded = false;
    static boolean collect_only = true;
    TimerManager uploader = new TimerManager(this);
    Intent intent;
    static boolean check = false;
    private static Toast toast;
    private ItemsCheck itemsCheck;
    private Button buttoninc,buttondec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);

        itemsCheck = new ItemsCheck(MainActivity.this);
        if(!itemsCheck.isAgreed())
            itemsCheck.startDialog();
        // Initialize database
        initializeDB();
        getAllAppInfos();
        DATABASE_PATH =  getBaseContext().getFilesDir().getPath()+"databases/";
        // Loading model
        try {
            module = Module.load(assetFilePath(this, "model.pt"));
        } catch (IOException e) {
            Log.e("Pytorch", "Error reading assets", e);
        }
        //Switch
        final Switch mSwitch = (Switch) findViewById(R.id.btn_schedule_job);
        jobStatus = (EditText) findViewById(R.id.job_status);

        if(check){
            mSwitch.setChecked(true);
            CacheScan.ischeckedaddr = false;
        }
        // 添加监听 scheduleJob cancelJob
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    SharedPreferences edit = getSharedPreferences("user", 0);
                    String name = edit.getString("RSA", "None");
                    if (name.equals("None")) {
                        Toast.makeText(getBaseContext(), "Please register first.", Toast.LENGTH_SHORT)
                                .show();
                        intent = new Intent(MainActivity.this, Register.class);
                        startActivity(intent);//start register page
                        mSwitch.setChecked(false);
                    } else
                        scheduleJob(buttonView);
                } else {
                    boolean d = cancelJob(buttonView);
                    if(!d){
                        mSwitch.setChecked(true);
                    }
                }
            }
        });
        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            int succeed = 1;
            @Override
            public void onClick(View v) {
                new Thread(){
                    public void run() {
                        uploader.uploadFile();
                    }
                }.start();
                Toast.makeText(getBaseContext(), "Uploading database.", Toast.LENGTH_SHORT)
                          .show();
                }
        });
        status[0] = findViewById(R.id.query);
        status[1] = findViewById(R.id.camera);
        status[2] = findViewById(R.id.audio);
        status[3] = findViewById(R.id.location);
        status[4] = findViewById(R.id.threshold);
        uploader.schedule();
        Button buttonr1 = (Button) findViewById(R.id.buttonr);
        buttonr1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Register.class);
                startActivity(intent);
            }
        });
        createNotificationChannel(); //register channel
        //mannually adjust the threshold
        //buttoninc = (Button) findViewById(R.id.increase);
        //buttondec = (Button) findViewById(R.id.decrease);
        //buttoninc.setOnClickListener(this);
        //buttondec.setOnClickListener(this);

        //register
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = 0;
        if (appOps != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                mode = appOps.unsafeCheckOpNoThrow("android:get_usage_stats",
                        Process.myUid(), getPackageName());
            }
        }
        boolean granted = mode == AppOpsManager.MODE_ALLOWED;
        if (!granted) {
            Toast.makeText(getBaseContext(), "Please grant the permisson.", Toast.LENGTH_SHORT)
                    .show();
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivity(intent);
        }
    }

    public static void showToast(String message, int time) {
        //一般不需要做非空判断，除非APP被篡改过
        if (toast != null) {
            toast.setText(message);
            //设置显示时长
            toast.setDuration(time);
            toast.show();
        }
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
    }

    /*
     * 得到手机中所有应用信息的列表
     * AppInfo
     */
    protected void getAllAppInfos() {
        //List<AppInfo> list = new ArrayList<AppInfo>();
        // 得到应用的packgeManager
        final PackageManager packageManager = getPackageManager();
        //创建一个主界面的intent
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        // 得到包含应用信息的列表
        List<ResolveInfo> ResolveInfos = packageManager.queryIntentActivities(
                intent, 0);
        final PackageManager pm = getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        //遍历
        for (ResolveInfo ri : ResolveInfos) {
            String path = null;
            // 得到包名
            String packageName = ri.activityInfo.packageName;
            /*
            // 得到应用名称
            String appName = ri.loadLabel(packageManager).toString();
            //得到路径
            for (ApplicationInfo pk : packages) {
                if (packageName.equals(pk.packageName)) {
                    path = pk.sourceDir;
                }
            }
             */
            //添加到list
            int permisson_type = read_permissons(packageName);
            //AppInfo appInfo = new AppInfo(appName, packageName, path, permisson_type);
            //list.add(appInfo);
            name_permisson.put(packageName, permisson_type);//Appinfo is not needed for now
            name_permisson.remove("com.android.settings");
        }
    }

    // Get Apps' permission
    public int read_permissons(String packageName) {
        int[] judge = {0, 0};
        int type = 0;
        PackageManager pm = getPackageManager();
        PackageInfo info;
        try {
            int pmnumber = 0;
            boolean flag = (PackageManager.PERMISSION_GRANTED ==
                    pm.checkPermission("android.permission.RECORD_AUDIO", packageName));
            if(flag)
                type+=1;
            flag = (PackageManager.PERMISSION_GRANTED ==
                    pm.checkPermission("android.permission.CAMERA", packageName));
            if(flag)
                type+=2;

            /*
//String result = null;
            String[] packagePermissions = info.requestedPermissions;
            Log.d(TAG, info.packageName);
            if (packagePermissions != null) {
                for (String packagePermission : packagePermissions) {
                    if(packagePermission.equals("android.permisson.CAMERA"))
                        pmnumber += 1;

                    if(packagePermission.equals("android.permission.RECORD_AUDIO"))
                    /*
                    if ((packagePermission.equals("android.permission.READ_SMS"))
                            || (packagePermission.equals("android.permission.READ_CONTACTS"))
                            || (packagePermission.equals("android.permission.RECORD_AUDIO"))
                            || (packagePermission.equals("android.permission.CAMERA")))
                        judge[0] = 2;
                    if (packagePermission.contains("LOCATION"))
                        judge[1] = 1;
                    pmnumber = judge[0] + judge[1];
                    Log.v("result", packageName + packagePermission);

                }
            } else {
                Log.d(TAG, info.packageName + ": no permissions");
            }*/
            return type;
            } catch (Exception ex) {
            ex.printStackTrace();
        }
        return 0;
    }

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
    public void checkRunStatus(boolean isRunning) {
        if (isRunning) {
            jobStatus.setText("Cancel Job");
        } else {
            jobStatus.setText("Schedule Job");
        }
    }

    public void scheduleJob(View v) {
        SideChannelJob.continueRun = true;
        checkRunStatus(SideChannelJob.continueRun);
        // Building the job to be passed to the job scheduler
        Intent begin = new Intent(this, SideChannelJob.class);
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(begin);
            Log.d(TAG, "Job scheduled");
            Toast.makeText(this, "Job is scheduling", Toast.LENGTH_SHORT)
                    .show();
        }
        else{
            Log.d(TAG, "Job scheduling failed");
            Toast.makeText(this, "Job scheduling failed", Toast.LENGTH_SHORT)
                    .show();
        }
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
                SideChannelContract.Columns.CURRENT_APP + " TEXT, " +
                SideChannelContract.Columns.SYSTEM_TIME + " INTEGER NOT NULL, " +
                SideChannelContract.Columns.LABELS + " TEXT); ";
        Log.d(TAG,sSQL);
        db.execSQL(sSQL);

        sSQL = "CREATE TABLE IF NOT EXISTS " + SideChannelContract.USER_FEEDBACK+ " (" +
                SideChannelContract.Columns.SYSTEM_TIME + " INTEGER NOT NULL, " +
                SideChannelContract.Columns.CHOICES + " INTEGER); ";
        Log.d(TAG,sSQL);
        db.execSQL(sSQL);
        db.close();
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
/*
    public void onClick(View v) {
        if(!check){
            showToast("Try it after starting",0);
            return;
        }
        switch (v.getId()) {
            case R.id.increase:
                increase();
                CacheScan.unparsedaddr();
                Log.d(TAG,"Increase the threshold");
                break;
            case R.id.decrease:
                decrease();
                CacheScan.unparsedaddr();
                Log.d(TAG,"Decrease the threshold");
                break;
            default:
                break;
        }
    }
 */
}

