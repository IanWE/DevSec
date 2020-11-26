package com.SMU.DevSec;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.TrafficStats;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Process;
import android.os.storage.StorageManager;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import static com.SMU.DevSec.MainActivity.infer_only;
import static com.SMU.DevSec.MainActivity.sideChannelValues;

public class SideChannelJob extends Service {
    public static volatile boolean continueRun;
    private static final String TAG = "DevSec_Service";
    int scValueCount = 1;
    int index = 1;
    long cumulativeTime;
    private static final String CHANNEL_ID = "com.SMU.DevSec";
    private static final String description =  "Collecting Data";
    long start_time = 0;
    Thread thread_collect = null;
    static Lock insert_locker = new ReentrantLock();


    @TargetApi(Build.VERSION_CODES.N)
    public void setForegroundService()
    {
        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_ID, importance);
        Intent intent;
        channel.setDescription(description);
        intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        //在创建的通知渠道上发送通知
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        builder.setSmallIcon(R.mipmap.ic_launcher_foreground) //设置通知图标
                .setContentTitle("DevSec")//设置通知标题
                .setContentText("Scanning")//设置通知内容
                .setOngoing(true)//设置处于运行状态
                .setContentIntent(pendingIntent)
                .setDeleteIntent(pendingIntent);
                //.setAutoCancel(true); //用户触摸时，自动关闭
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
        doBackgroundWork();
        Toast.makeText(this, "Job scheduled successfully", Toast.LENGTH_SHORT)
                .show();
        return super.onStartCommand (intent,flags,startId);
    }

    /**
     * Method to run data collection in the background
     */
    public void doBackgroundWork() {
        Log.d(TAG, "New Thread Created");
        thread_collect = new Thread(new Runnable() {
            @Override
            public void run() {
                int count = 0;
                StorageManager storageManager =
                        (StorageManager) getSystemService(Context.STORAGE_SERVICE);
                BatteryManager batteryManager =
                        (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
                // Loop to collect side channel values via API calls
                sideChannelValues = new ArrayList<>();
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
                            } catch (Exception e) {
                                sideChannelValue.setAllocatableBytes(0);
                                sideChannelValue.setCacheQuotaBytes(0);
                                sideChannelValue.setCacheSize(0);
                                Log.d(TAG, e.toString());
                                Log.d(TAG, "Get Cache info failed");
                            }
                            //disk
                            try {
                                sideChannelValue.setFreeSpace(f.getFreeSpace());
                                sideChannelValue.setUsableSpace(f.getUsableSpace());
                            } catch (Exception e) {
                                sideChannelValue.setFreeSpace(0);
                                sideChannelValue.setUsableSpace(0);
                                Log.d(TAG, e.toString());
                                Log.d(TAG, "Get disk info failed");
                            }
                            //CPU time
                            try {
                                sideChannelValue.setElapsedCpuTime(Process.
                                        getElapsedCpuTime());
                            } catch (Exception e) {
                                sideChannelValue.setElapsedCpuTime(0);
                                Log.d(TAG, e.toString());
                                Log.d(TAG, "Get ElapsedCpuTime Failed");
                            }
                            //battery
                            try {
                                sideChannelValue.setCurrentBatteryLevel(
                                        computeBatteryLevel(getBaseContext()));
                                sideChannelValue.setBatteryChargeCounter(batteryManager.
                                        getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER));
                            } catch (Exception e) {
                                sideChannelValue.setCurrentBatteryLevel(0);
                                sideChannelValue.setBatteryChargeCounter(0);
                                Log.d(TAG, e.toString());
                                Log.d(TAG, "Get Battery info Failed");
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
                            } catch (Exception e) {
                                sideChannelValue.setMobileTxBytes(0);
                                sideChannelValue.setTotalTxBytes(0);
                                sideChannelValue.setMobileTxPackets(0);
                                sideChannelValue.setTotalTxPackets(0);
                                sideChannelValue.setMobileRxBytes(0);
                                sideChannelValue.setTotalRxBytes(0);
                                sideChannelValue.setMobileRxPackets(0);
                                sideChannelValue.setTotalRxPackets(0);
                                Log.d(TAG, e.toString());
                                Log.d(TAG, "Get Traffic info Failed");
                            }
                            insert_locker.lock();
                            sideChannelValues.add(sideChannelValue);
                            insert_locker.unlock();
                        }
                        // Compute cumulative time to collect 100 sets of side channel values
                        long deltaTime = System.currentTimeMillis() - startTime;
                        cumulativeTime = cumulativeTime + deltaTime;
                        if (((scValueCount % 60) == 0) && index != 1) {
                            //Log.d(TAG, String.valueOf(sideChannelValues.subList(index-60,index)));
                            //infering events
                            new Thread(new Classifier(getBaseContext(),
                                    new ArrayList(sideChannelValues.subList(index - 60, index)))).start();
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
                        if(!infer_only)
                            new Thread(new JobInsertRunnable(getBaseContext())).start();//store the dataset
                        else
                            sideChannelValues = new ArrayList<>();
                        Log.d(TAG, "DB Updated");
                        //Clear the updated data
                        index = 1;
                        scValueCount = 0;
                        cumulativeTime = 0;
                        count = 0;
                    }
                }//while
            }
        });
        thread_collect.start();
    }

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
        Log.d(TAG, "onCreate");
        //如果API在26以上即版本为O则调用startForefround()方法启动服务
        setForegroundService();
    }

    @Override
    public void onDestroy(){
        super.onDestroy ();
        continueRun = false;
        //make sure threads are stopped
        if(thread_collect!=null) {
            try {
                thread_collect.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            thread_collect = null;
        }
        if(!infer_only)
            new Thread(new JobInsertRunnable(getBaseContext())).start();//store the dataset
        else
            sideChannelValues = new ArrayList<>();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}