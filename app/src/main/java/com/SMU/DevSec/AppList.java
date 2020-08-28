package com.SMU.DevSec;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class AppList extends AppCompatActivity  {

    private ListView lv_main;
    private List<AppInfo> data;
    private AppInfoAdapter adapter;
    private int pos;
    private String[] listdata=new String[5];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.applist_layout);
        Intent intent = getIntent();
        listdata[0] = intent.getStringExtra("Permission");
        listdata[1] = intent.getStringExtra("hour");
        listdata[2] = intent.getStringExtra("min");

        Button titleBack1 = (Button) findViewById(R.id.title_back_3);
        Button checked = (Button) findViewById(R.id.title_check);
        //初始化成员变量
        lv_main = (ListView) findViewById(R.id.lv_main);
        data = getAllAppInfos();
        adapter = new AppInfoAdapter(data,this);
        //显示列表
        lv_main.setAdapter(adapter);

        //隐藏标题栏
        ActionBar actionBar = getSupportActionBar();
        if(actionBar!=null){
            actionBar.hide();
        }
        titleBack1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        checked.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v){

                pos = adapter.getCheckedPosition();
                if (pos != -1) {
                    final String name = data.get(pos).getAppName();
                    AlertDialog.Builder dialog = new AlertDialog.Builder(AppList.this);
                    dialog.setTitle("Are you sure?");
                    dialog.setMessage("You selected "+name);
                    dialog.setCancelable(false);
                    dialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            System.out.println(pos);
                            Log.i("p",name);
                            String path = data.get(pos).getPath();
                            listdata[3] = name;
                            listdata[4] = path;
                            Toast.makeText(AppList.this, name + " Successful", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                    dialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
                    dialog.show();
                }else {
                    Toast.makeText(AppList.this,"Please Select App", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //给LitView设置Item的长按监听
//        lv_main.setOnItemLongClickListener(this);
    }

    /*
     * 得到手机中所有应用信息的列表
     * AppInfo
     *  Drawable icon  图片对象
     *  String appName
     *  String packageName
     */
    protected List<AppInfo> getAllAppInfos() {

        List<AppInfo> list = new ArrayList<AppInfo>();
        // 得到应用的packgeManager
        final PackageManager packageManager = getPackageManager();
//         创建一个主界面的intent
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        // 得到包含应用信息的列表
        List<ResolveInfo> ResolveInfos = packageManager.queryIntentActivities(
                intent, 0);
        final PackageManager pm = getPackageManager();
        List<ApplicationInfo> packages =  pm.getInstalledApplications(PackageManager.GET_META_DATA);
//         遍历
        for (ResolveInfo ri: ResolveInfos) {
//            String source = ri.activityInfo.sourceDir;
            String path =null;
            // 得到包名
            String packageName = ri.activityInfo.packageName;
//            Log.i("r",packageName);
            // 得到图标
            Drawable icon = ri.loadIcon(packageManager);
            // 得到应用名称
            String appName = ri.loadLabel(packageManager).toString();
            //得到路径
            for (ApplicationInfo pk: packages) {
                if(packageName.equals(pk.packageName)){
                    path = pk.sourceDir;
//                    Log.i("i",path);
                }
            }
//             添加到list
            //AppInfo appInfo = new AppInfo(appName, packageName, path);
            //list.add(appInfo);
         }
        return list;
    }

}
