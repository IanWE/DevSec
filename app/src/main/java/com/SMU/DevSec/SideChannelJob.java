package com.SMU.DevSec;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.TrafficStats;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Process;
import android.os.storage.StorageManager;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import static com.SMU.DevSec.CacheScan.answered;
import static com.SMU.DevSec.CacheScan.mContext;
import static com.SMU.DevSec.CacheScan.notification;
import static com.SMU.DevSec.JobInsertRunnable.insert_locker;
import static com.SMU.DevSec.MainActivity.collect_only;
import static com.SMU.DevSec.MainActivity.infering;

public class SideChannelJob extends Service {
    public static volatile boolean continueRun;
    private static final String TAG = "JobService";
    private static int label=0;
    ArrayList<SideChannelValue> sideChannelValues;
    public static ArrayList<GroundTruthValue> groundTruthValues;
    public static ArrayList<UserFeedback> userFeedbacks;
    public static ArrayList<CompilerValue> compilerValues;
    public static ArrayList<FrontAppValue> frontAppValues;
    int scValueCount = 1;
    int index = 1;
    long cumulativeTime;
    private String currentFront="DevSec";
    private static final String CHANNEL_ID = "e.smu.devsec";
    private static final String description =  "Collecting Data";
    CacheScan cs=null;



    static Lock locker = new ReentrantLock();

    static {
        System.loadLibrary("native-lib"); //jni lib to use libflush
    }
    /**
     *通过通知启动服务
     */
    @TargetApi(Build.VERSION_CODES.N)
    public void setForegroundService()
    {
        //设定的通知渠道名称
        String channelName = CHANNEL_ID;
        //设置通知的重要程度
        int importance = NotificationManager.IMPORTANCE_LOW;
        //构建通知渠道
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, channelName, importance);
        channel.setDescription(description);
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        //在创建的通知渠道上发送通知
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        builder.setSmallIcon(R.mipmap.ic_launcher_foreground) //设置通知图标
                .setContentTitle("DevSec")//设置通知标题
                .setContentText("Scanning")//设置通知内容
                .setOngoing(true)//设置处于运行状态
                .setContentIntent(pendingIntent)
                .setAutoCancel(true); //用户触摸时，自动关闭
        //向系统注册通知渠道，注册后不能改变重要性以及其他通知行为
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
        //将服务置于启动状态 NOTIFICATION_ID指的是创建的通知的ID
        startForeground(111,builder.build());
    }

    @Override
    public int onStartCommand(Intent intent,int flags,int startId){
        //int pid = android.os.Process.myPid(); //get the self pid
        Log.d(TAG, "Job started");
        Toast.makeText(this, "Started Data Collection", Toast.LENGTH_SHORT).show();
        doBackgroundWork();
        return super.onStartCommand (intent,flags,startId);
    }

    /**
     * Method to run data collection in the background
     */
    public void doBackgroundWork() {
        Log.d(TAG, "New Thread Created");
        // Send the job to a different thread
        new Thread(new Runnable() {
                @Override
                public void run() {
                    // Initializing primitives and objects
                    int count = 0;
                    sideChannelValues = new ArrayList<>();
                    groundTruthValues = new ArrayList<>();
                    compilerValues = new ArrayList<>();
                    userFeedbacks = new ArrayList<>();
                    frontAppValues = new ArrayList<>();
                    StorageManager storageManager =
                            (StorageManager) getSystemService(Context.STORAGE_SERVICE);
                    BatteryManager batteryManager =
                            (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
                    // Loop to collect side channel values via API calls
                    while (continueRun) {
                        try {
                            String[] volumes = (String[]) storageManager.getClass()
                                    .getMethod("getVolumePaths", new Class[0])
                                    .invoke(storageManager, new Object[0]);
                            long startTime = System.currentTimeMillis();
                            // Loop for storing the side channel values in a
                            // POJO (SideChannelValue object)
                            for (int j = 0; j < volumes.length; j++) {
                                File f = new File(volumes[j]);
                                SideChannelValue sideChannelValue = new SideChannelValue();
                                sideChannelValue.setSystemTime(System.currentTimeMillis());
                                sideChannelValue.setVolume(j);
                                //cache
                                try {
                                    sideChannelValue.setAllocatableBytes(storageManager.
                                            getAllocatableBytes(storageManager.getUuidForPath(f)));
                                    sideChannelValue.setCacheQuotaBytes(storageManager.
                                            getCacheQuotaBytes(storageManager.getUuidForPath(f)));
                                    sideChannelValue.setCacheSize(storageManager.
                                            getCacheSizeBytes(storageManager.getUuidForPath(f)));
                                }catch (Exception e){
                                    sideChannelValue.setAllocatableBytes(0);
                                    sideChannelValue.setCacheQuotaBytes(0);
                                    sideChannelValue.setCacheSize(0);
                                    Log.d(TAG,e.toString());
                                    Log.d(TAG,"Get Cache info failed");
                                }
                                //disk

                                try {
                                    sideChannelValue.setFreeSpace(f.getFreeSpace());
                                    sideChannelValue.setUsableSpace(f.getUsableSpace());
                                }catch (Exception e){
                                    sideChannelValue.setFreeSpace(0);
                                    sideChannelValue.setUsableSpace(0);
                                    Log.d(TAG,e.toString());
                                    Log.d(TAG,"Get disk info failed");
                                }
                                //CPU time
                                try {
                                    sideChannelValue.setElapsedCpuTime(Process.
                                            getElapsedCpuTime());
                                }catch (Exception e){
                                    sideChannelValue.setElapsedCpuTime(0);
                                    Log.d(TAG,e.toString());
                                    Log.d(TAG,"Get ElapsedCpuTime Failed");
                                }
                                //battery
                                try {
                                    sideChannelValue.setCurrentBatteryLevel(
                                            computeBatteryLevel(getBaseContext()));
                                    sideChannelValue.setBatteryChargeCounter(batteryManager.
                                            getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER));
                                }catch (Exception e){
                                    sideChannelValue.setCurrentBatteryLevel(0);
                                    sideChannelValue.setBatteryChargeCounter(0);
                                    Log.d(TAG,e.toString());
                                    Log.d(TAG,"Get Battery info Failed");
                                }
                                try {
                                    sideChannelValue.setMobileTxBytes(TrafficStats.getMobileTxBytes());
                                    sideChannelValue.setTotalTxBytes(TrafficStats.getTotalTxBytes());
                                    sideChannelValue.setMobileTxPackets(TrafficStats.getMobileTxPackets());
                                    sideChannelValue.setTotalTxPackets(TrafficStats.getTotalTxPackets());
                                    sideChannelValue.setMobileRxBytes(TrafficStats.getMobileRxBytes());
                                    sideChannelValue.setTotalRxBytes(TrafficStats.getTotalRxBytes());
                                    sideChannelValue.setMobileRxPackets(TrafficStats.getMobileRxPackets());
                                    sideChannelValue.setTotalRxPackets(TrafficStats.getTotalRxPackets());
                                }catch (Exception e){
                                    sideChannelValue.setMobileTxBytes(0);
                                    sideChannelValue.setTotalTxBytes(0);
                                    sideChannelValue.setMobileTxPackets(0);
                                    sideChannelValue.setTotalTxPackets(0);
                                    sideChannelValue.setMobileRxBytes(0);
                                    sideChannelValue.setTotalRxBytes(0);
                                    sideChannelValue.setMobileRxPackets(0);
                                    sideChannelValue.setTotalRxPackets(0);
                                    Log.d(TAG,e.toString());
                                    Log.d(TAG,"Get Traffic info Failed");
                                }
                                sideChannelValues.add(sideChannelValue);
                            }
                            // Compute cumulative time to collect 100 sets of side channel values
                            long deltaTime = System.currentTimeMillis() - startTime;
                            cumulativeTime = cumulativeTime + deltaTime;
                            if (((scValueCount % 60) == 0) && index != 1) {//Do not classiy when there is no sc
                                //Log.d(TAG, String.valueOf(sideChannelValues.subList(index-60,index)));
                                //Whether use model to infer
                                if (!collect_only) {
                                    //new Thread(new MonitorFrontEvent(getBaseContext())).start();
                                    if (infering) new Thread(new Classifier(getBaseContext(),
                                            new ArrayList(sideChannelValues.subList(index - 60, index)))).start();
                                }
                                Log.d(TAG, "API Call Batch Count: " + scValueCount +
                                        ", Cumulative Time (ms): " + cumulativeTime);
                            }
                            scValueCount++;
                            index++;

                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                            Log.d("debug", "mhy_IllegalAccessException");
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                            Log.d("debug", "mhy_InvocationTargetException");
                        } catch (NoSuchMethodException e) {
                            e.printStackTrace();
                            Log.d("debug", "mhy_NoSuchMethodException");
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.d("debug", "mhy_Exception");
                            Log.d("debug", "mhy_" + e.toString());
                        }
                        count++;
                        // Log.d(TAG, "run: " + count);
                        // Send side channel values to a new thread and start data collection in another
                        // different thread after collecting 1000 sets of side channel values
                        if (count % 1020 == 0) {
                            locker.lock();
                            new Thread(new JobInsertRunnable(getBaseContext(), sideChannelValues)).start();
                            locker.unlock();
                            Log.d(TAG, "DB Updated");
                            //Clear the updated data
                            insert_locker.lock();//lock until the inserting finished.
                            sideChannelValues = new ArrayList<>();
                            insert_locker.unlock();
                            index = 1;
                            scValueCount = 0;
                            cumulativeTime = 0;
                            count = 0;
                        }
                    }
                }
            }).start();
        // Start scan
        new Thread(new Runnable() {
            @Override
            public void run() {
                //ClassCreate
                try {
                    if(cs==null)
                      cs = new CacheScan(getBaseContext());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //Start Scaning
                scan(0, CacheScan.ranges, CacheScan.offsets, 1, CacheScan.filenames, CacheScan.func_lists, CacheScan.target_lib, CacheScan.target_func);
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (continueRun) {
                        Thread.sleep(1000);
                        cs.Notify();
                    }
                }catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
    }

    public native String scan(int cpu, String[] range, String[] offset, int fork, String[] filename,String[] func_list,String target_lib, String target_func);
    public native void pause();
    /**
     * Method to compute battery level based on API call to BatteryManager
     *
     * @param context: Android activity context for detecting change in battery charge
     * @return
     */
    private float computeBatteryLevel(Context context) {

        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        return level / (float) scale;
    }

    @SuppressLint("CommitPrefEdits")
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("foreground", "onCreate");
        //如果API在26以上即版本为O则调用startForefround()方法启动服务
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            setForegroundService();
        }
    }
    @Override
    public void onDestroy(){
        super.onDestroy ();
        locker.lock();
        new Thread(new JobInsertRunnable(getBaseContext(),sideChannelValues)).start();
        locker.unlock();
        insert_locker.lock();//lock until the inserting finished.
        sideChannelValues = new ArrayList<>();
        insert_locker.unlock();
        Toast.makeText(this, "Job cancelled", Toast.LENGTH_SHORT)
                .show();
        Toast.makeText(this, "Stopped data collection", Toast.LENGTH_SHORT)
                .show();
        SharedPreferences edit = getBaseContext().getSharedPreferences("user",0);
        SharedPreferences.Editor editor = edit.edit();
        editor.putLong("Answered",answered);//record the numbr of notification
        editor.putLong("Notification",notification);
        editor.commit();
        continueRun = false;
        pause();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}