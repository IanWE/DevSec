package com.SMU.DevSec;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.SMU.DevSec.MainActivity.sideChannelValues;
import static com.SMU.DevSec.SideChannelJob.insert_locker;

class JobInsertRunnable implements Runnable {
    Context context;
    SQLiteDatabase db;
    ContentValues values;
    long startTime;

    private static final String TAG = "JobInsertRunnable";
    /**
     * Constructor for this class

     * @param context:           Android activity context for opening the database
     */
    public JobInsertRunnable(Context context) {
        this.context = context;
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
        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();
        long deltaTime = System.currentTimeMillis() - startTime;
        insert_locker.unlock();
        boolean ifcompress = Utils.checkfile(context);//get the size of db
        if(ifcompress) {//if the db is large than the limited size, compress it.
            Utils.compress(context);
        }
        Log.d(TAG, "Time taken for DB storage (ms): " + deltaTime);
    }
}