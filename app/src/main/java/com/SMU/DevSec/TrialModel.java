package com.SMU.DevSec;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.PersistableBundle;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import static com.SMU.DevSec.MainActivity.pkg_permission;
import static com.SMU.DevSec.SideChannelJob.continueRun;
import static com.SMU.DevSec.MainActivity.stage;

public class TrialModel extends AppCompatActivity {
    final String TAG="TrialModel";
    int[] flags = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.trial_model_layout);
        LogcatHelper.getInstance(getBaseContext()).start();
        Button start_button = findViewById(R.id.start);
        //final TrialModelStages tms = new TrialModelStages(TrialModel.this);
        start_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CacheScan.CacheCheck() == null) {//check if initiated well.
                    showToast("App is initializing, try it a bit later");
                }
                else {
                    TrialModelStages.getInstance(TrialModel.this).startDialog();
                }
            }
        });

        Button close_button = findViewById(R.id.close);
        close_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent stop=new Intent (getBaseContext(),SideChannelJob.class);
                stopService(stop);
                finishAffinity();
                System.exit(0);
            }
        });

        stage = 1;//start the service
        Intent begin = new Intent(this, SideChannelJob.class);
        if (!continueRun) {
            continueRun = true;
            startForegroundService(begin);
            Log.d(TAG, "Job scheduled");
            Toast.makeText(this, "Job is scheduling", Toast.LENGTH_SHORT)
                    .show();
        }
    }

    public void showToast(final String text) {
        new Thread() {
            @Override
            public void run() {
                super.run();
                Looper.prepare();
                try {
                    Toast.makeText(getBaseContext(), text, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e("error", e.toString());
                }
                Looper.loop();
            }
        }.start();
    }

    public void onDestroy() {
        //finishAffinity();
        //if(TrialModelStages.getInstance(TrialModel.this) != null) {   mDialog.dismiss();  }
        super.onDestroy();
    }
}