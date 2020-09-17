package com.SMU.DevSec;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AfterTrialModel extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.after_trai_model);
        new Thread(){
            public void run() {
                TimerManager.getInstance(getBaseContext()).uploadLogs();
            }
        }.start();
        //Set Conducted
        SharedPreferences preferences = getBaseContext().getSharedPreferences("user", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor=preferences.edit();
        editor.putBoolean("Conducted", true);
        editor.commit();

        Button finish_button = findViewById(R.id.exit);
        finish_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    showToast("The app will be closed in 3 seconds; ");
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                finishAffinity();
                //stop service
                System.exit(0);
            }
        });
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
