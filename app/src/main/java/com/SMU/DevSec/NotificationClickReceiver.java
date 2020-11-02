package com.SMU.DevSec;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;


import static com.SMU.DevSec.CacheScan.answered;
import static com.SMU.DevSec.CacheScan.handled;
import static com.SMU.DevSec.CacheScan.lastactivetime;
import static com.SMU.DevSec.CacheScan.mContext;
import static com.SMU.DevSec.SideChannelJob.locker;
import static com.SMU.DevSec.SideChannelJob.userFeedbacks;

public class NotificationClickReceiver extends BroadcastReceiver {
    final String TAG = "Notification";
    @Override
    public void onReceive(Context context, Intent intent) {
        //
        Bundle data = intent.getExtras();
        long arise = (long) data.get("arise");
        int event = data.getInt("flag");
        String app = data.getString("app");
        int ignored = data.getInt("ignored");

        locker.lock();
        handled[event] = true;
        locker.unlock();
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(event);

        //if(event==1){//if it is camera event
        lastactivetime = Utils.getCurTimeLong();

        UserFeedback userFeedback = new UserFeedback();//create a feedback
        userFeedback.setArisingtime(arise);
        userFeedback.setEvent(event);
        userFeedback.setApp(app);
        userFeedback.setAnsweringtime(Utils.getCurTimeLong());
        switch (ignored){
            case 0:
                Log.d(TAG,"Not sure");
                userFeedback.setChoice(0);//means you know the event
                locker.lock();
                userFeedbacks.add(userFeedback);
                locker.unlock();
                answered++;
                break;
            case 1:
                Log.d(TAG,"Confirmed");
                userFeedback.setChoice(1);//means you know the event
                locker.lock();
                userFeedbacks.add(userFeedback);
                locker.unlock();
                answered++;
                break;
            case 2:
                Log.d(TAG,"Cannot Confirm");
                userFeedback.setChoice(2);//means you know the event
                locker.lock();
                userFeedbacks.add(userFeedback);
                locker.unlock();
                answered++;
                break;//sms
        }

    }
}