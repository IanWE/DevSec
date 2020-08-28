package com.SMU.DevSec;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;


import static com.SMU.DevSec.CacheScan.handled;
import static com.SMU.DevSec.SideChannelJob.locker;
import static com.SMU.DevSec.SideChannelJob.userFeedbacks;

public class NotificationClickReceiver extends BroadcastReceiver {
    final String TAG = "Notification";
    @Override
    public void onReceive(Context context, Intent intent) {
        //
        Bundle data = intent.getExtras();
        int event = data.getInt("flag");
        int ignored = data.getInt("ignored");
        locker.lock();
        handled[event] = true;
        locker.unlock();
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(event);

        UserFeedback userFeedback = new UserFeedback();//create a feedback
        switch (ignored){
            case 0:
                Log.d(TAG,"deteled directly");
                break;
            case 1:
                Log.d(TAG,"A Known Event");
                userFeedback.setSystemTime(System.currentTimeMillis());
                userFeedback.setChoice(1);//means you know the event
                locker.lock();
                userFeedbacks.add(userFeedback);
                locker.unlock();
                break;
            case 2:
                Log.d(TAG,"An Unknown Event");
                userFeedback.setSystemTime(System.currentTimeMillis());
                userFeedback.setChoice(0);//means you know the event
                locker.lock();
                userFeedbacks.add(userFeedback);
                locker.unlock();
                break;//sms
        }
    }
}