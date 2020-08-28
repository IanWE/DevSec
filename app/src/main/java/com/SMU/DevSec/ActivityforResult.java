package com.SMU.DevSec;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import static com.SMU.DevSec.SideChannelJob.groundTruthValues;

public class ActivityforResult extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.perm_selection_layout);
/*
        Button button1 = (Button) findViewById(R.id.buttona1);
        button1.setOnClickListener(new View.OnClickListener() {
                                      @Override
                                      public void onClick(View v) {
                                          GroundTruthValue groundTruthValue = new GroundTruthValue();
                                          groundTruthValue.setLabels(1);
                                          groundTruthValue.setSystemTime(System.currentTimeMillis());
                                          groundTruthValues.add(groundTruthValue);
                                          finish();
                                      }
                                  });

        Button button2 = (Button) findViewById(R.id.buttona2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GroundTruthValue groundTruthValue = new GroundTruthValue();
                groundTruthValue.setLabels(2);
                groundTruthValue.setSystemTime(System.currentTimeMillis());
                groundTruthValues.add(groundTruthValue);
                finish();
            }
        });

        Button button3 = (Button) findViewById(R.id.buttona3);
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GroundTruthValue groundTruthValue = new GroundTruthValue();
                groundTruthValue.setLabels(3);
                groundTruthValue.setSystemTime(System.currentTimeMillis());
                groundTruthValues.add(groundTruthValue);
                finish();
            }
        });

        Button button4 = (Button) findViewById(R.id.buttona4);
        button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GroundTruthValue groundTruthValue = new GroundTruthValue();
                groundTruthValue.setLabels(4);
                groundTruthValue.setSystemTime(System.currentTimeMillis());
                groundTruthValues.add(groundTruthValue);
                finish();
            }
        });

        Button button5 = (Button) findViewById(R.id.buttona5);
        button5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GroundTruthValue groundTruthValue = new GroundTruthValue();
                groundTruthValue.setLabels(5);
                groundTruthValue.setSystemTime(System.currentTimeMillis());
                groundTruthValues.add(groundTruthValue);
                finish();
            }
        });

        Button button0 = (Button) findViewById(R.id.buttona0);
        button0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GroundTruthValue groundTruthValue = new GroundTruthValue();
                groundTruthValue.setLabels(0);
                groundTruthValue.setSystemTime(System.currentTimeMillis());
                groundTruthValues.add(groundTruthValue);
                finish();
            }
        });

        Button buttonback = (Button) findViewById(R.id.title_back_2);
        buttonback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GroundTruthValue groundTruthValue = new GroundTruthValue();
                groundTruthValue.setLabels(0);
                groundTruthValue.setSystemTime(System.currentTimeMillis());
                groundTruthValues.add(groundTruthValue);
                Intent intent = new Intent(ActivityforResult.this,MainActivity.class);
                //启动intent对应的Activity
                startActivity(intent);//let it close itself.
            }
        });
    */
    }
}