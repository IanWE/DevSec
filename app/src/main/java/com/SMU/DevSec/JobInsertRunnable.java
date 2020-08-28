package com.SMU.DevSec;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;

class JobInsertRunnable implements Runnable {
    Context context;
    ArrayList<SideChannelValue> sideChannelValues;
    ArrayList<GroundTruthValue> groundTruthValues;
    ArrayList<UserFeedback> userFeedbacks;
    SQLiteDatabase db;
    ContentValues values;
    long startTime;

    private static final String TAG = "JobInsertRunnable";
    /**
     * Constructor for this class
     *
     * @param context:           Android activity context for opening the database
     * @param sideChannelValues: ArrayList of SideChannelValue objects to be inserted into the database
     */
    public JobInsertRunnable(Context context, ArrayList<SideChannelValue> sideChannelValues,
                             ArrayList<GroundTruthValue> groundTruthValues,ArrayList<UserFeedback> userFeedbacks) {
        this.context = context;
        this.sideChannelValues = sideChannelValues;
        this.groundTruthValues = groundTruthValues;
        this.userFeedbacks = userFeedbacks;
    }

    /**
     * Method to perform the operation in a different thread (from the Runnable interface)
     */
    public void run() {
        // Start timing the entire process and open the database
        startTime = System.currentTimeMillis();
        db = context.openOrCreateDatabase("SideScan.db", Context.MODE_PRIVATE, null);
        // DB transaction for faster batch insertion of data into database
        db.beginTransaction();
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
        // Ground Truth insertion
        if(groundTruthValues.size()!=0) {
            values = new ContentValues();
            for (GroundTruthValue groundTruthValue : groundTruthValues) {
                values.put(SideChannelContract.Columns.SYSTEM_TIME,
                        groundTruthValue.getSystemTime());
                values.put(SideChannelContract.Columns.CURRENT_APP,
                        groundTruthValue.getCurrentApp());
                values.put(SideChannelContract.Columns.LABELS,
                        groundTruthValue.getLabels());
                db.insert(SideChannelContract.GROUND_TRUTH, null, values);
            }
        }
        if(userFeedbacks.size()!=0) {
            values = new ContentValues();
            for (UserFeedback userFeedback : userFeedbacks) {
                values.put(SideChannelContract.Columns.SYSTEM_TIME,
                        userFeedback.getSystemTime());
                values.put(SideChannelContract.Columns.CHOICES,
                        userFeedback.getChoice());
                db.insert(SideChannelContract.USER_FEEDBACK, null, values);
            }
        }

        db.setTransactionSuccessful();
        db.endTransaction();

        db.close();

        long deltaTime = System.currentTimeMillis() - startTime;
        Log.d(TAG, "Time taken for DB storage (ms): " + deltaTime);
    }
}