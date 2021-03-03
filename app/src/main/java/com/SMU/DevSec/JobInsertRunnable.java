package com.SMU.DevSec;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.SMU.DevSec.MainActivity.compilerValues;
import static com.SMU.DevSec.MainActivity.frontAppValues;
import static com.SMU.DevSec.MainActivity.groundTruthValues;
import static com.SMU.DevSec.MainActivity.sideChannelValues;
import static com.SMU.DevSec.MainActivity.userFeedbacks;
import static com.SMU.DevSec.Utils.DATABASE_FILENAME;
import static com.SMU.DevSec.Utils.DATABASE_PATH;

class JobInsertRunnable implements Runnable {
    Context context;
    SQLiteDatabase db;
    ContentValues values;
    long startTime;
    static Lock insert_locker = new ReentrantLock();

    private static final String TAG = "JobInsertRunnable";
    /**
     * Constructor for this class

     * @param context:           Android activity context for opening the database
     */
    public JobInsertRunnable(Context context) {
        this.context = context;
        /*
        this.groundTruthValues = groundTruthValues;
        this.userFeedbacks = userFeedbacks;
        this.compilerValues = compilerValues;
        this.frontAppValues = frontAppValues;

         */
    }

    /**
     * Method to perform the operation in a different thread (from the Runnable interface)
     */
    public void run() {
        // Start timing the entire process and open the database
        insert_locker.lock();//locked here, in case that other thread delete the ArrayList
        startTime = System.currentTimeMillis();
        db = context.openOrCreateDatabase("SideScan.db", Context.MODE_PRIVATE, null);
        // DB transaction for faster batch insertion of data into database
        db.beginTransaction();
        if(sideChannelValues!=null&&sideChannelValues.size()!=0) {
            values = new ContentValues();
            for (SideChannelValue sideChannelValue : sideChannelValues) {
                values.put(SideChannelContract.Columns.SYSTEM_TIME,
                        sideChannelValue.getSystemTime());
                values.put(SideChannelContract.Columns.VOLUME,
                        sideChannelValue.getVolume());
                values.put(SideChannelContract.Columns.ALLOCATABLE_BYTES,
                        sideChannelValue.getAllocatableBytes());
                values.put(SideChannelContract.Columns.CACHE_QUOTA_BYTES,
                        sideChannelValue.getCacheQuotaBytes());
                values.put(SideChannelContract.Columns.CACHE_SIZE,
                        sideChannelValue.getCacheSize());
                values.put(SideChannelContract.Columns.FREE_SPACE,
                        sideChannelValue.getFreeSpace());
                values.put(SideChannelContract.Columns.USABLE_SPACE,
                        sideChannelValue.getUsableSpace());
                values.put(SideChannelContract.Columns.ELAPSED_CPU_TIME,
                        sideChannelValue.getElapsedCpuTime());
                values.put(SideChannelContract.Columns.CURRENT_BATTERY_LEVEL,
                        sideChannelValue.getCurrentBatteryLevel());
                values.put(SideChannelContract.Columns.BATTERY_CHARGE_COUNTER,
                        sideChannelValue.getBatteryChargeCounter());
                values.put(SideChannelContract.Columns.MOBILE_TX_BYTES,
                        sideChannelValue.getMobileTxBytes());
                values.put(SideChannelContract.Columns.TOTAL_TX_BYTES,
                        sideChannelValue.getTotalTxBytes());
                values.put(SideChannelContract.Columns.MOBILE_TX_PACKETS,
                        sideChannelValue.getMobileTxPackets());
                values.put(SideChannelContract.Columns.TOTAL_TX_PACKETS,
                        sideChannelValue.getTotalTxPackets());
                values.put(SideChannelContract.Columns.MOBILE_RX_BYTES,
                        sideChannelValue.getMobileRxBytes());
                values.put(SideChannelContract.Columns.TOTAL_RX_BYTES,
                        sideChannelValue.getTotalRxBytes());
                values.put(SideChannelContract.Columns.MOBILE_RX_PACKETS,
                        sideChannelValue.getMobileRxPackets());
                values.put(SideChannelContract.Columns.TOTAL_RX_PACKETS,
                        sideChannelValue.getTotalRxPackets());
                db.insert(SideChannelContract.TABLE_NAME, null, values);
            }
            sideChannelValues = new ArrayList<>();
        }
        // Ground Truth insertion
        if(groundTruthValues!=null&&groundTruthValues.size()!=0) {
            values = new ContentValues();
            for (GroundTruthValue groundTruthValue : groundTruthValues) {
                values.put(SideChannelContract.Columns.SYSTEM_TIME,
                        groundTruthValue.getSystemTime());
                values.put(SideChannelContract.Columns.LABELS,
                        groundTruthValue.getLabels());
                db.insert(SideChannelContract.GROUND_TRUTH, null, values);
            }
            groundTruthValues = new ArrayList<>();
        }

        if(userFeedbacks!=null&&userFeedbacks.size()!=0) {
            values = new ContentValues();
            for (UserFeedback userFeedback : userFeedbacks) {
                values.put(SideChannelContract.Columns.ARISINGTIME,
                        userFeedback.getArisingtime());
                values.put(SideChannelContract.Columns.EVENT,
                        userFeedback.getEvent());
                values.put(SideChannelContract.Columns.CURRENT_APP,
                        userFeedback.getApp());
                values.put(SideChannelContract.Columns.ANSWERINGTIME,
                        userFeedback.getAnsweringtime());
                values.put(SideChannelContract.Columns.CHOICES,
                        userFeedback.getChoice());
                values.put(SideChannelContract.Columns.PATTERN,
                        userFeedback.getPattern());
                db.insert(SideChannelContract.USER_FEEDBACK, null, values);
            }
            userFeedbacks = new ArrayList<>();
        }

        if(compilerValues!=null&&compilerValues.size()!=0) {
            values = new ContentValues();
            for (CompilerValue compilerValue: compilerValues) {
                values.put(SideChannelContract.Columns.SYSTEM_TIME,
                        compilerValue.getSystemTime());
                values.put(SideChannelContract.Columns.THRESHOLDS,
                        compilerValue.getThresholds());
                values.put(SideChannelContract.Columns.FUNCTIONS,
                        compilerValue.getFunctions());
                //Log.d("XXXXXXXXX",compilerValue.getSystemTime()+"   "+
                //        compilerValue.getFunctions());
                db.insert(SideChannelContract.SIDE_COMPILER, null, values);
            }
            compilerValues = new ArrayList<>();
        }

        if(frontAppValues!=null&&frontAppValues.size()!=0) {
            values = new ContentValues();
            for (FrontAppValue frontAppValue: frontAppValues) {
                values.put(SideChannelContract.Columns.SYSTEM_TIME,
                        frontAppValue.getSystemTime());
                values.put(SideChannelContract.Columns.CURRENT_APP,
                        frontAppValue.getCurrentApp());
                db.insert(SideChannelContract.FRONT_APP, null, values);
            }
            frontAppValues = new ArrayList<>();
        }
        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();
        long deltaTime = System.currentTimeMillis() - startTime;
        insert_locker.unlock();
        boolean ifcompress = Utils.checkfile(context);//get the size of db
        if(ifcompress) {//if the db is larger than limit size, compress it.
            Utils.compress(context);
        }
        Log.d(TAG, "Time taken for DB storage (ms): " + deltaTime);
    }
}