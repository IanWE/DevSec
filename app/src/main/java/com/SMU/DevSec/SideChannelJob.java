/*
This is a foreground service for scanning, side channel information collection and notificaiton poping. 
*/
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
import static com.SMU.DevSec.CacheScan.notification;
import static com.SMU.DevSec.CacheScan.notifying;
import static com.SMU.DevSec.JobInsertRunnable.insert_locker;
import static com.SMU.DevSec.MainActivity.collect_only;
import static com.SMU.DevSec.MainActivity.cs;
import static com.SMU.DevSec.MainActivity.infering;
import static com.SMU.DevSec.MainActivity.lastday;
import static com.SMU.DevSec.MainActivity.day;
import static com.SMU.DevSec.MainActivity.sideChannelValues;
import static com.SMU.DevSec.MainActivity.stage;
import static com.SMU.DevSec.MainActivity.trial;
import static com.SMU.DevSec.MainActivity.uploaded;

public class SideChannelJob extends Service {
    public static volatile boolean continueRun;
    private static final String TAG = "JobService";
    private static int label=0;
    static int[] pattern_filter = null;
    int scValueCount = 1;
    int index = 1;
    long cumulativeTime;
    private String currentFront="DevSec";
    private static final String CHANNEL_ID = "e.smu.devsec";
    private static final String description =  "Collecting Data";
    static Lock locker = new ReentrantLock();
    long start_time = 0;
    Thread thread_collect = null;
    Thread thread_notify = null;
    /**
     *Start service by notification
     */
    @TargetApi(Build.VERSION_CODES.N)
    public void setForegroundService()
    {
        int importance = NotificationManager.IMPORTANCE_LOW;
        //build channel
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_ID, importance);
        Intent intent;
        channel.setDescription(description);
        if(trial==null) {
            SharedPreferences edit = getSharedPreferences("user", 0);
            trial = edit.getString("trialmode", "0");//
        }
        if(trial.equals("1"))//if not completed trial
            intent = new Intent(this, MainActivity.class);
        else
            intent = new Intent(this, TrialModel.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        builder.setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setContentTitle("DevSec")
                .setContentText("Scanning")
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .setDeleteIntent(pendingIntent);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
        startForeground(111,builder.build());
    }

    @Override
    public int onStartCommand(Intent intent,int flags,int startId){
        Log.d(TAG, "Job started");
        start_time = System.currentTimeMillis();
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
                // Initializing primitives and objects
                int count = 0;
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

                        if (((scValueCount % 60) == 0) && index != 1) {//Do not classiy when there is no sc
                            //Log.d(TAG, String.valueOf(sideChannelValues.subList(index-60,index)));
                            //Whether use model to infer
                            /*
                            if (!collect_only) {
                                //new Thread(new MonitorFrontEvent(getBaseContext())).start();
                                if (infering) new Thread(new Classifier(getBaseContext(),
                                        new ArrayList(sideChannelValues.subList(index - 60, index)))).start();
                            }
                            */
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
                    if (count%1020 == 0&&stage==0) {//Do not save collected info when at trial mode
                        new Thread(new JobInsertRunnable(getBaseContext())).start();//start a thread to save data
                        Log.d(TAG, "DB Updated");
                        index = 1;
                        scValueCount = 0;
                        cumulativeTime = 0;
                        count = 0;
                    }
                }//while
            }
        });
        thread_collect.start();


        // Start scan
        if (stage == 0) {
            // Send the job to a different thread
            new Thread(new Runnable() {
                @Override
                public void run() {
                    //ClassCreate
                    try {
                        if (cs == null) {
                            cs = new CacheScan(getBaseContext());//Initialize the CacheScan class
                            Log.d(TAG,"Initialize CacheScan");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //Start Scaning
                    if(pattern_filter==null)
                        pattern_filter = Utils.getArray(getBaseContext(), "ptfilter");//Retrieve an array of pattern filter; since we only need to monitor partial functions, others will get blocked
                    scan(pattern_filter, pattern_filter.length);//start scanning , in native-lib.cpp
                }
            }).start();

            //Start a notification thread.
            thread_notify = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while(cs==null) {
                            Thread.sleep(100);//Waiting until CacheScan class is initialized
                        }
                        while (continueRun) {
                            Thread.sleep(1000);
                            cs.Notify(getBaseContext());//every 1 second to check if it need to send notification
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            thread_notify.start();
        }//
        else{
            new Thread(new Runnable() {
                @Override
                public void run() {
                    //ClassCreate
                    try {
                        if (cs == null)
                            cs = new CacheScan(getBaseContext());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }//trial
    }

    public native void scan(int[] pattern,int length);
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
        setForegroundService();
    }
    @Override
    public void onDestroy(){
        super.onDestroy ();
        final long minutes = (System.currentTimeMillis()-start_time)/(1000*60);
        pause();//pause the scanning
        //locker.lock();
        if(stage==0) {//not in trial mode
            if(minutes>0) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String r = TimerManager.getInstance().uploadTimeCheck(getBaseContext(), minutes);//
                        if (r != null && r.equals("1"))
                            Log.d(TAG, "Running time are uploaded successfully");
                        else
                            Log.d(TAG, "Unable to upload the running time");
                    }
                }).start();
            }
        }
        //locker.unlock();
        SharedPreferences edit = getBaseContext().getSharedPreferences("user",0);
        SharedPreferences.Editor editor = edit.edit();
        editor.putLong("Answered",answered);//record the numbr of notification
        editor.putLong("Notification",notification);
        editor.putLong("lastday",lastday);
        editor.putLong("day",day);
        editor.commit();
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
        if(thread_notify!=null) {
            try {
                thread_notify.interrupt();
                thread_notify.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            thread_notify = null;
        }
        Toast.makeText(this, "Job cancelled", Toast.LENGTH_SHORT)
                .show();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
