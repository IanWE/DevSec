package com.SMU.DevSec;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import static com.SMU.DevSec.SideChannelJob.continueRun;

public class TrialModel extends AppCompatActivity {
    final String TAG="TrialModel";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.trial_model_layout);

        LogcatHelper.getInstance(getBaseContext()).start();
        Intent begin = new Intent(this, SideChannelJob.class);
        if (Build.VERSION.SDK_INT >= 26&&!continueRun) {
            continueRun = true;
            startForegroundService(begin);
            Log.d(TAG, "Job scheduled");
            Toast.makeText(this, "Job is scheduling", Toast.LENGTH_SHORT)
                    .show();
        }

        Button finish_button = findViewById(R.id.finish);
        finish_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences edit = getBaseContext().getSharedPreferences("user", Context.MODE_PRIVATE);
                String name = edit.getString("trialmodel", "0");
                //LogcatHelper.getInstance(getBaseContext()).start();
                Intent stop=new Intent (getBaseContext(),SideChannelJob.class);
                stopService(stop);
                continueRun = false;
                Log.d(TAG,"Stop logging the test");
                LogcatHelper.getInstance(getBaseContext()).stop();
                Intent intent = new Intent(TrialModel.this, AfterTrialModel.class);
                startActivity(intent);
                //stop service
            }
        });
    }

    public void onDestroy() {
        finishAffinity();
        super.onDestroy();
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
}
