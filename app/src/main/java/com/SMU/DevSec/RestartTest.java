package com.SMU.DevSec;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


public class RestartTest extends AppCompatActivity {
    final String TAG = "RestartTest";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.restart_application);
        Button restart_button = findViewById(R.id.recheck);
        TimerManager.getInstance(getBaseContext()).uploadLogs();
        restart_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences edit = getBaseContext().getSharedPreferences("user", Context.MODE_PRIVATE);
                String name = edit.getString("trialmodel", "0");
                if (name.equals("1")) {
                    showToast("You have passed the check");
                    SharedPreferences.Editor editor = edit.edit();
                    editor.putLong("day",System.currentTimeMillis()/(1000*24*60*60));//STORE THE FIRST DAY
                    editor.commit();
                    Log.d(TAG,"Set the day "+System.currentTimeMillis()/(1000*24*60*60));
                    showNormalDialog();
                }
                if (name.equals("2")) {
                    showToast("Sorry, the device is not compatible with our experiment");
                    finishAffinity();
                    System.exit(0);
                    return;
                }
                if(name.equals("0")){
                    SharedPreferences.Editor editor = edit.edit();
                    editor.putBoolean("Conducted",false);
                    editor.commit();
                    Intent intent = new Intent(RestartTest.this, TrialModel.class);
                    startActivity(intent);
                }
                //LogcatHelper.getInstance(getBaseContext()).start();
                //stop service
            }
        });

        Button check_button = findViewById(R.id.checkpass);
        check_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences edit = getBaseContext().getSharedPreferences("user", Context.MODE_PRIVATE);
                String name = edit.getString("trialmodel", "0");
                if (name.equals("0")) {
                    showToast("We are still analysing logs");
                    return;
                }
                if (name.equals("1")) {
                    SharedPreferences.Editor editor = edit.edit();
                    editor.putLong("day",System.currentTimeMillis()/(1000*24*60*60));//STORE THE FIRST DAY
                    editor.commit();
                    showToast("You have passed the check");
                    Log.d(TAG,"Set the day "+edit.getLong("day",0)+"");
                    showNormalDialog();

                }
                if (name.equals("2")) {
                    showToast("Sorry, the device is not compatible with our experiment");
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    finishAffinity();
                    System.exit(0);
                    return;
                }
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

    private void showNormalDialog(){
        /* @setIcon 设置对话框图标
         * @setTitle 设置对话框标题
         * @setMessage 设置对话框消息提示
         * setXXX方法返回Dialog对象，因此可以链式设置属性
         */
        final AlertDialog.Builder normalDialog =
                new AlertDialog.Builder(RestartTest.this);
        //normalDialog.setIcon(R.drawable.icon_dialog);
        //normalDialog.setTitle("我是一个普通Dialog");
        normalDialog.setMessage("Congratulations! You phone has passed the hardware test. You are entering the user study phase.");
        normalDialog.setPositiveButton("Proceed",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(RestartTest.this, MainActivity.class);
                        startActivity(intent);
                    }
                });
        // 显示
        normalDialog.show();
    }
}
