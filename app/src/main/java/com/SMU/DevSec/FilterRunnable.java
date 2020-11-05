package com.SMU.DevSec;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import static com.SMU.DevSec.MainActivity.count_threshold;
import static com.SMU.DevSec.MainActivity.filter;
import static com.SMU.DevSec.MainActivity.preset_threshold;
import static com.SMU.DevSec.MainActivity.trial;

public class FilterRunnable implements Runnable {

    Context mContext;
    String code = null;

    public FilterRunnable(Context context) {
        this.mContext = context;
    }
    @Override
    public void run() {
        SharedPreferences edit = mContext.getSharedPreferences("user", Context.MODE_PRIVATE);
        String name = edit.getString("adler", "None");
        if (!name.equals("None")) {
            code = TimerManager.getInstance().getCode(mContext,name);
            if (code==null) {
                Log.d("FilterCheck", "Request Failed");
                String filter_string = edit.getString("filter",null);
                String temp_threshold = edit.getString("count_threshold",null);
                String temp_pre_threshold = edit.getString("preset_threshold",null);
                if(temp_threshold!=null)
                    count_threshold = Integer.parseInt(temp_threshold);
                if(temp_pre_threshold!=null)
                    preset_threshold = Integer.parseInt(temp_pre_threshold);
                saveFilter(filter_string);
                return;
            }
            Log.d("FilterCheck", code);
            if(code.contains("F")){
                String code_f = code.split("F")[1].split("T")[0].split("P")[0];//For example, it return a code with 1F3_4T3
                SharedPreferences.Editor editor = edit.edit();
                editor.putString("filter", code_f);
                editor.apply();
                saveFilter(code_f);
            }
            if(code.contains("T")){
                String code_t = code.split("T")[1].split("P")[0];//For example, it return a code with 1F3_4T3
                SharedPreferences.Editor editor = edit.edit();
                editor.putString("count_threshold", code_t);
                editor.apply();
                count_threshold = Integer.parseInt(code_t);
            }
            if(code.contains("P")){
                String code_t = code.split("P")[1];//For example, it return a code with 1F3_4T3
                SharedPreferences.Editor editor = edit.edit();
                editor.putString("preset_threshold", code_t);
                editor.apply();
                preset_threshold = Integer.parseInt(code_t);
            }
            trial = edit.getString("trialmodel", "0");
            String code_x = code.split("F")[0].split("T")[0].split("P")[0];
            if (!trial.equals(code_x)) {//if the code has changed
                Log.d("FilterCheck", "The code has changed to "+code_x);
                SharedPreferences.Editor editor = edit.edit();
                editor.putString("trialmodel", code_x);
                editor.apply();
            }

        }
    }

    public void saveFilter(String filter_string){
        if(filter_string!=null) {
            String[] filters = filter_string.split("_");
            for(String each:filters){
                int flt = Integer.parseInt(each);
                filter[flt] = 1;
            }
        }
    }
}
