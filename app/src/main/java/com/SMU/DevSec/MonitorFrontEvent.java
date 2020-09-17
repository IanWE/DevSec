package com.SMU.DevSec;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.SMU.DevSec.MainActivity.previous_name;
import static com.SMU.DevSec.SideChannelJob.groundTruthValues;

public class MonitorFrontEvent implements Runnable {//deprecated
    private static final String CHANNEL_ID = "AISG";
    Context mContext;

    public MonitorFrontEvent(Context baseContext) {
        this.mContext = baseContext;
    }

    @Override
    public void run() {
        getTopApp();
    }

    private void getTopApp() {
        UsageStatsManager mUsageStatsManager = (UsageStatsManager) mContext.getSystemService(Context.USAGE_STATS_SERVICE);//usagestats
        //Log.e("TopPackage Name", mUsageStatsManager.isAppInactive("e.smu.questlocation")+"");//10
        long time = System.currentTimeMillis();
        List<UsageStats> usageStatsList = mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, time - 3000, time);
        if (usageStatsList != null && !usageStatsList.isEmpty()) {
            SortedMap<Long, UsageStats> usageStatsMap = new TreeMap<>();//e.smu.questlocation
            for (UsageStats usageStats : usageStatsList) {
                usageStatsMap.put(usageStats.getLastTimeUsed(), usageStats);
                //Log.e("TopPackage Name", usageStatsMap.get(usageStatsMap.lastKey()).getPackageName());
            }
            if (!usageStatsMap.isEmpty()) {
                int gt = 0;
                String topPackageName = usageStatsMap.get(usageStatsMap.lastKey()).getPackageName();
                //Log.e("TopPackage Name", topPackageName+' '+//usageStatsMap.get(usageStatsMap.lastKey()).getLastTimeForegroundServiceUsed()+
                //        usageStatsMap.get(usageStatsMap.lastKey()).getLastTimeUsed());
                if (!topPackageName.equals(mContext.getPackageName()) &&
                        //name_permisson.containsKey(topPackageName)&&
                        !topPackageName.equals(previous_name)&&
                        !previous_name.equals(mContext.getPackageName())) {
                        int x = 0;// name_permisson.get(topPackageName);
                        //Log.d("TEST",y+"");
                        switch (x) {//(Integer) name_permisson.get(topPackageName)) {
                            case 0:
                                return;
                            case 1:
                                /*
                                GroundTruthValue groundTruthValue = new GroundTruthValue();
                                groundTruthValue.setLabels("null");//
                                groundTruthValue.setSystemTime(System.currentTimeMillis());
                                groundTruthValues.add(groundTruthValue);
                                 */
                            default:
                                getgt();
                        }
                        //if (getLauncherPackageName(mContext).equals(topPackageName) || "com.othergetTopApp();she.test".equals(topPackageName)) {
                        //    return;
                        //}com.android.launcher3
                    }
                else if(!topPackageName.equals("com.android.settings"))
                    previous_name = topPackageName;
                //else if(topPackageName.equals(mContext.getPackageName())) previous_name = topPackageName;
            }
        }
    }

    private void getgt(){
        Intent intent;
        intent = new Intent(mContext, ActivityforResult.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }
}