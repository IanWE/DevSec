package com.SMU.DevSec;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import static com.SMU.DevSec.CacheScan.mContext;
import static com.SMU.DevSec.MainActivity.trial;

public class TrialModelCheck implements Runnable {
    Context mContext;
    String code = null;
    //no need to get the pass from the server, so remove this class
    public TrialModelCheck(Context context) {
        this.mContext = context;
    }
    @Override
    public void run() {
        SharedPreferences edit = mContext.getSharedPreferences("user", Context.MODE_PRIVATE);
        String name = edit.getString("adler", "None");
        if (!name.equals("None")) {
            code = TimerManager.getInstance().getCode(mContext,name);
            if (code == null) {
                Log.d("TrialModelCheck", "Request Failed");
                return;
            }
            Log.d("TrialModelCheck", code);

            code = code.split("F")[0].split("T")[0].split("P")[0];//For example, it return a code with 1F3_4P120
            if (code != null && (code.equals("1")||code.equals("2"))) {
                SharedPreferences.Editor editor = edit.edit();
                editor.putString("trialmodel", code);
                trial = code;
                editor.apply();
            }
        }
    }
}
