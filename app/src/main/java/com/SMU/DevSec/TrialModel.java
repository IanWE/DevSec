/*
Trial mode to do the device check.
*/
package com.SMU.DevSec;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
        stage = 1;//start the service
        final TrialModelStages tms = new TrialModelStages(TrialModel.this);
        //final TrialModelStages_backup tms = new TrialModelStages_backup(TrialModel.this);
        start_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CacheScan.CacheCheck() == null) {//check if initiated well.
                    showToast("App is initializing, try few seconds later");
                }
                else if(stage==1){
                    tms.startDialog();
                }
                else {
                    showToast("You have completed the test");
                    Intent intent = new Intent(getBaseContext(), AfterTrialModel.class);
                    startActivity(intent);
                }
            }
        });

        Button close_button = findViewById(R.id.close);
        close_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if(stage==0) {
                    Intent intent = new Intent(getBaseContext(), AfterTrialModel.class);
                    startActivity(intent);
                }
                else{
                    Log.d(TAG,"You haven't finished the test.");
                    showToast("You haven't finished the test.");
                }
                //Intent stop=new Intent (getBaseContext(),SideChannelJob.class);
                //stopService(stop);
                //finishAffinity();
                //System.exit(0);
            }
        });

        Intent begin = new Intent(this, SideChannelJob.class);//start
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
        //if(TrialModelStages_backup.getInstance(TrialModel.this) != null) {   mDialog.dismiss();  }
        super.onDestroy();
    }
}
