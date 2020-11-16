package com.SMU.DevSec;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Looper;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.NotificationCompat;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static androidx.core.app.NotificationCompat.FLAG_NO_CLEAR;
import static com.SMU.DevSec.JobInsertRunnable.insert_locker;
import static com.SMU.DevSec.MainActivity.audio;
import static com.SMU.DevSec.MainActivity.camera;
import static com.SMU.DevSec.MainActivity.compilerValues;
import static com.SMU.DevSec.MainActivity.count_threshold;
import static com.SMU.DevSec.MainActivity.filter;
import static com.SMU.DevSec.MainActivity.frontAppValues;
import static com.SMU.DevSec.MainActivity.groundTruthValues;
import static com.SMU.DevSec.MainActivity.lastday;
import static com.SMU.DevSec.MainActivity.day;
import static com.SMU.DevSec.MainActivity.isCollected;
import static com.SMU.DevSec.MainActivity.location;
import static com.SMU.DevSec.MainActivity.pkg_name;
import static com.SMU.DevSec.MainActivity.pkg_permission;
import static com.SMU.DevSec.MainActivity.preset_threshold;
import static com.SMU.DevSec.MainActivity.quering;
import static com.SMU.DevSec.MainActivity.status;
import static com.SMU.DevSec.MainActivity.check;
import static com.SMU.DevSec.MainActivity.trial;
import static com.SMU.DevSec.SideChannelJob.locker;
import static java.util.Objects.*;

public class CacheScan {
    private static final String TAG = "CacheScan";
    String app;
    private int sameapp = 0;
    static boolean ischeckedaddr = false;
    static boolean[] handled = {true, true, true, true};
    volatile static long lastactivetime = 0;
    private String preapp = "DevSec";
    private boolean reset_thresh = false;
    private long lastcamera = 0;
    private long lastaudio = 0;
    static long notification = 0;
    static long answered = 0;
    static boolean notifying = false;//
    boolean filtered = false;
    private static ArrayList<String> target_functions = new ArrayList<String>();
    private final HashMap<String, String> behaviour_map = new HashMap<String, String>();
    private ArrayList<int[]> ALpattern = new ArrayList<int[]>();
    private int[] thresholdforpattern = {10,10};//number of different functions
    private String[] BehaviorList = {"Information", "Camera", "AudioRecorder", "Location"};
    private double audio_threshold_level = 0.25;
    private double camera_threshold_level = 0.25;
    private int[] cleanpattern = {0,0};
    private String AppStringforcheck = "";
    private long firsthit = 0;
    private long setfalse = 0;
    private boolean dismiss = false;
    private boolean exceedtime = false;
    private static File CacheDir;
    public static boolean initializing = false;

    CacheScan(Context mContext) throws IOException {
        //mContext = context;
        init(mContext);
    }
    //6 function
    private void init(Context mContext) throws IOException {
        //exec("cat /proc/`pgrep DevSec`/maps");
        initializing = true;
        int pid = android.os.Process.myPid();
        CacheDir = mContext.getCacheDir();
        //behaviour_map.put("LocationManagerService.java_updateLastLocationLocked", "Location");
        //behaviour_map.put("ContentResolver.java_createSqlQueryBundle", "Information");
        String audioapi = "Audio";
        behaviour_map.put(audioapi, "AudioRecord");
        //behaviour_map.put("_ZN7android5media12IAudioRecord11asInterfaceERKNS_2spINS_7IBinderEEE", "AudioRecorder");
        String cameraapi = "Camera";
        behaviour_map.put(cameraapi, "Camera");
        AssetManager am = mContext.getAssets();
        ArrayList<String> dex = new ArrayList<String>();
        ArrayList<String> filename = new ArrayList<String>();
        ArrayList<String> offset = new ArrayList<String>();
        ArrayList<String> func_list = new ArrayList<String>();
        String[] targets = am.list("targets");
        String temp;
        String vendor = BasicInfo.getDeviceBrand();
        for (String f : requireNonNull(targets)) {
            Log.d("CacheScan", f);//+ " " + f.substring(f.lastIndexOf(".") + 1));
            if (f.substring(f.lastIndexOf(".") + 1).equals("so")) {
                String oat_target = Utils.readSaveFile("targets/" + f, mContext);
                String[] arr = oat_target.split(",");
                StringBuilder funcs = new StringBuilder();
                for (int i = 1; i < arr.length; i++) {
                    target_functions.add(arr[i]);
                    if (i == 1) {//the first function do need a comma
                        funcs.append(arr[i]);
                        continue;
                    }
                    funcs.append(",").append(arr[i]);
                }
                func_list.add(funcs.toString());
                dex.add("");
                filename.add(arr[0]); //get the path of library in the android system;
                //Log.d(TAG, "TTTTTTTTTT "+pid+" funcs:" + funcs.toString() + " " + arr[0] +" "+arr[arr.length - 1].split("\n")[0]);
            } else if (f.substring(f.lastIndexOf(".") + 1).equals("oat")){
                String oat_target = Utils.readSaveFile("targets/" + f, mContext);
                String[] arr = oat_target.split(",");
                String target_oat = arr[0];
                String target_dex = arr[1];
                StringBuilder funcs = new StringBuilder();
                for (int i = 2; i < arr.length; i++) {
                    target_functions.add(arr[i]);
                    if (i == 2) {
                        funcs.append(arr[i]);
                        continue;
                    }
                    funcs.append(",").append(arr[i]);
                }
                func_list.add(funcs.toString());
                dex.add(target_dex);
                filename.add(target_oat);
                //Log.d(TAG, "TTTTTTTTTT "+pid+" funcs:" + funcs.toString() + " " + target_dex +" "+target_oat);
            }
        }
        //static Context mContext;
        String[] dexlist = (String[]) dex.toArray(new String[targets.length]);
        String[] filenames = (String[]) filename.toArray(new String[targets.length]);
        String[] func_lists = (String[]) func_list.toArray(new String[targets.length]);
        //Log.d(TAG, "Target:" + target_func + " " + target_lib);
        SharedPreferences edit = mContext.getSharedPreferences("user",0);
        notification = edit.getLong("Notification",0);
        answered = edit.getLong("Answered",0);
        lastday = edit.getLong("lastday", 0);
        day = edit.getLong("day", 0);

        //ALpattern.add(Utils.getArray(mContext,"1"));
        //ALpattern.add(Utils.getArray(mContext,"2"));
        //for(int i=0;i<ALpattern.size();i++){
            //thresholdforpattern[i] = Utils.sum(ALpattern.get(i));
        //    thresholdforpattern[i] = ALpattern.get(i).length;
        //}
        thresholdforpattern[0] = 10;
        thresholdforpattern[1] = 10;
        init(dexlist, filenames, func_lists);//initiate the JNI function
        //ResetThreshold();
        //if(thresholdforpattern[0]<10)
        //    camera_threshold_level = 0.8;
        //if(thresholdforpattern[1]<10)
        //    audio_threshold_level = 0.8;
        //showToast("Intialized successfully");
        Log.d(TAG,"Threshold Level is at "+camera_threshold_level+"-"+audio_threshold_level);//only output
        initializing = false;
        check = true;
    }

    private String[] exec(int pid, String target) {
        String data = "";
        try {
            Process p = null;
            String command = "grep " + target + " /proc/`pgrep " + MainActivity.NAME + "`/maps"; //还没进内存
            //Log.d(TAG,"TTTTTTTTTTTT"+command);
            p = Runtime.getRuntime().exec(command);
            BufferedReader ie = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String error = null;
            while ((error = ie.readLine()) != null
                    && !error.equals("null")) {
                data += error + "\n";
            }
            String line = null;
            while ((line = in.readLine()) != null
                    && !line.equals("null")) {
                data += line + "\n";
            }
            String[] arr = data.split("\n");
            for (String st : arr) {
                if (st.contains("-xp")) {// && st.split("/").length == 4) {
                    Log.d(TAG, st);
                    data = st;
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data.split(" ");
    }

    public static String[] decompress(String fileName) {
        JarFile jf = null;
        String filename = "";
        String[] jarlist;
        ArrayList<String> dexfiles = new ArrayList<String>();
        try {
            jf = new JarFile(fileName);
            for (Enumeration<JarEntry> e = jf.entries(); e.hasMoreElements(); ) {
                JarEntry je = (JarEntry) e.nextElement();
                Log.d(TAG, "jar file has file:" + je.getName());
                if (je.getName().endsWith(".dex") || je.getName().endsWith(".jar")) {
                    filename = CacheDir + "/" + je.getName();
                    File file = new File(filename);
                    unpack(jf, je, file);
                    Log.d(TAG, "Extract Jar:" + filename);
                    dexfiles.add(filename);
                }
            }
            if (dexfiles.size() == 0) {
                return null;
            }
            return dexfiles.toArray(new String[0]);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;//new String[0];
    }

    /*
    unpack jar
     */
    private static void unpack(JarFile jarFile, JarEntry entry, File file) throws IOException {
        try (InputStream inputStream = jarFile.getInputStream(entry)) {
            try (OutputStream outputStream = new FileOutputStream(file)) {
                int BUFFER_SIZE = 1024;
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }
        }
    }
    /*
    public static void getGroundTruth(int gt){//Should we let the user to choose app?
        String topPackageName = getTopApp();
        GroundTruthValue groundTruthValue = new GroundTruthValue();
        groundTruthValue.setLabels(gt);
        groundTruthValue.setCurrentApp(topPackageName);
        groundTruthValue.setSystemTime(System.currentTimeMillis());
        groundTruthValues.add(groundTruthValue);
    }
    */
    public static String getTopApp(Context mContext) {
        UsageStatsManager mUsageStatsManager = (UsageStatsManager) mContext.getSystemService(Context.USAGE_STATS_SERVICE);//usagestats
        long time = System.currentTimeMillis();
        String topPackageName = "None";
        int gt = 0;
        List<UsageStats> usageStatsList = mUsageStatsManager != null ? mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, time - 3000, time) : null;
        if (usageStatsList != null && !usageStatsList.isEmpty()) {
            SortedMap<Long, UsageStats> usageStatsMap = new TreeMap<>();//e.smu.questlocation
            for (UsageStats usageStats : usageStatsList) {
                usageStatsMap.put(usageStats.getLastTimeUsed(), usageStats);
                //Log.e("TopPackage Name", usageStatsMap.get(usageStatsMap.lastKey()).getPackageName());
            }
            if (!usageStatsMap.isEmpty()) {
                topPackageName = usageStatsMap.get(usageStatsMap.lastKey()).getPackageName();
                //Log.d(TAG, "TopPackage Name is "+topPackageName+' '+//usageStatsMap.get(usageStatsMap.lastKey()).getLastTimeForegroundServiceUsed()+
                //        "Last Time Used:"+usageStatsMap.get(usageStatsMap.lastKey()).getLastTimeUsed()+" Permisson Type:"+name_permisson.get(topPackageName));
            }
        }
        String temp = pkg_name.get(topPackageName);
        if(temp!=null)
            topPackageName = temp;
        return topPackageName;
    }

    private void updateUI(final int result) {
        final long iday;
        if(isCollected) {
            if(System.currentTimeMillis() / (1000 * 60 * 60 * 24) - lastday>0){
                day = day + 1;//(System.currentTimeMillis() / (1000 * 60 * 60 * 24) - lastday);
                lastday = System.currentTimeMillis() / (1000 * 60 * 60 * 24);
            }
        }
        //Log.d(TAG,day+" "+firstday);
        status[result].post(new Runnable() {
            @Override
            public void run() {
                switch (result) {
                    case 0:
                        quering++;
                        status[result].setText("QueryInformation - " + quering);
                        break;
                    case 1:
                        camera++;
                        status[result].setText("Camera - " + camera);
                        break;
                    case 2:
                        audio++;
                        status[result].setText("AudioRecoding - " + audio);
                        break;
                    case 3:
                        location++;
                        status[result].setText("RequestLocation - " + location);
                        break;
                    case 4:
                        if(lastday!=0)
                            status[result+1].setText("Day "+day);
                        status[result].setText("# of Notifications/Answered Today - " + notification + "/" + answered);
                        break;
                }
            }
        });
    }

    private static void unparsedaddr(final int success, final int result) {
        if (success == 0) {
            status[result].post(new Runnable() {
                @Override
                public void run() {
                    switch (result) {
                        case 0:
                            status[result].setText("Unable to parse offset for Queryinformation");
                            break;
                        case 1:
                            status[result].setText("Unable to parse offset for Camera");
                            break;
                        case 2:
                            status[result].setText("Unable to parse offset for Audio");
                            break;
                        case 3:
                            status[result].setText("Unable to parse offset for Location");
                            break;
                    }
                }
            });
        } else {
            status[result].post(new Runnable() {
                @Override
                public void run() {
                    switch (result) {
                        case 0:
                            status[result].setText("QueryInformation - " + quering);
                            break;
                        case 1:
                            status[result].setText("Camera - " + camera);
                            break;
                        case 2:
                            status[result].setText("AudioRecoding - " + audio);
                            break;
                        case 3:
                            status[result].setText("RequestLocation - " + location);
                            break;
                    }
                }
            });
        }
    }

    public void showToast(final Context mContext, final String text) {
        new Thread() {
            @Override
            public void run() {
                super.run();
                Looper.prepare();
                try {
                    Toast.makeText(mContext, text, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e("error", e.toString());
                }
                Looper.loop();
            }
        }.start();
    }

    static int[] getPattern(int c){
        int []p =GetPattern(c);
        ClearPattern(3);
        return p;
    }

    void Notify(Context mContext) { //FrontApp ok, SideCompiler ok, Side_Channel_Info ok, Ground_Truth ok,
        int[] flags = CacheCheck();
        //insert the logs into dataset
        long[] times = GetTimes();
        int[] logs;
        if (times != null&&times.length>0) {
            int[] thresholds = GetThresholds();
            logs = GetLogs();
            ArrayList<CompilerValue> cvs = new ArrayList<CompilerValue>();
            for (int i = 0; i < times.length; i++) {
                CompilerValue cv = new CompilerValue();
                cv.setSystemTime(times[i]);
                cv.setThresholds(thresholds[i]);
                cv.setFunctions(logs[i]);
                cvs.add(cv);
            }
            if(cvs.size()>0) {
                insert_locker.lock();
                compilerValues.addAll(cvs);
                insert_locker.unlock();
            }
        }
        //get app
        app = getTopApp(mContext); // get package name. Even if there is actually an activation, the returned app may still be a None
        int permission_type = 0;
        if(pkg_permission.containsKey(app)){
            permission_type = pkg_permission.get(app);
        }

        if(app.equals(AppStringforcheck)||app.equals("None")){//if app equals to the previous one,
            sameapp++;
        }
        else {
            sameapp = 1;
        }//Log.d(TAG,!app.equals("None")+"  "+!app.toUpperCase().contains("LAUNCHER")+"  "+!app.equals(preapp));
        //if there is an app change, dismiss the first one.
        if(!app.equals("None")&&!app.toUpperCase().contains("LAUNCHER")&&!app.equals(preapp)){
            dismiss = true;
            firsthit = System.currentTimeMillis();
            Log.d(TAG,"Set the dismiss true to ignore the first notification");
        }
        int count = 1;
        // if it is the same application, but 5 seconds has passed over, set the count to 3. (a user shutdown and reopen an app to activate functions.)
        if(app.equals(preapp)&&!dismiss&&System.currentTimeMillis()-setfalse>30000){
            Log.d(TAG,"Set dismiss true");
            dismiss = true;
            exceedtime = true;
            firsthit = System.currentTimeMillis();
        }
        //if some functions are not activated, close the dismiss after 4 seconds
        if(!exceedtime&&dismiss&&System.currentTimeMillis()-firsthit>5000){
            dismiss = false;
            //setfalse = System.currentTimeMillis();
            Log.d(TAG,"Set dismiss back to false");
        }
        AppStringforcheck = app;//previous app
        //On Pixel, app is unable to know whether switched out, when someone switch out and switch back. false positiveness may aise.
        if (app.equals("None")||app.toUpperCase().contains("LAUNCHER")) {
            app = preapp;
        }
        preapp = app;
        long time = System.currentTimeMillis();
        FrontAppValue fa = new FrontAppValue();
        fa.setSystemTime(time);
        fa.setCurrentApp(app);
        insert_locker.lock();
        frontAppValues.add(fa);
        insert_locker.unlock();
        Log.d(TAG, AppStringforcheck + " >>> "+permission_type+" >>>"
                + flags[0] + ":" + flags[1] + ":" + flags[2] + ":" + flags[3] + "."
                + "notification:" + notification +" Sameapp:"+sameapp);
        if (Utils.sum(flags) != 0) {//if some function are activated
            //检查一次, 地址是否都被成功解析
            int length = 4;
            if (!ischeckedaddr) {
                int[] addrs = addr();//get the addresses
                for (int i = 0; i < length; i++) {
                    unparsedaddr(addrs[i], i);
                }
                ischeckedaddr = true;
                thresholdforpattern = GetT();
            }
            updateUI(4);
            for (int i = 0; i < length; i++) {//0-3  5 6
                String cur = target_functions.get(i);
                if (flags[i] != 0){
                    int cmp = 0;
                    if(i==1||i==2){
                        if(sameapp<count){
                            Log.d(TAG,"It is the first activation, skip");
                            ClearPattern(i);
                            continue;
                        }
                        int[] pattern = GetPattern(i);
                        //ClearPattern();
                        //if(!AppStringforcheck.equals("None")&&
                        if((permission_type&i)!=i&&!AppStringforcheck.equals("None")){//if app do not have camera permisson, skip
                            HandleCapture(i);
                            Log.d(TAG,(permission_type&i)+" "+AppStringforcheck+" do not have "+i+" permisson, false positive");
                            ClearPattern(3);
                            continue;
                        }
                        double thforcamera = camera_threshold_level;
                        double thforaudio = audio_threshold_level;
                        //cmp = Utils.pattern_compare(ALpattern.get(i - 1), pattern);
                        cmp = Utils.sum(pattern);
                        if(cmp<=(int)(thresholdforpattern[i-1]*thforcamera)){
                            cleanpattern[i-1]++;
                            HandleCapture(i);
                            if(cleanpattern[i-1]>2){//accumulating
                                cleanpattern[i-1] = 0;
                                ClearPattern(i);
                                Log.d(TAG,"pattern-"+i+" clear : "+cmp);
                            }
                            Log.d(TAG,"pattern did not match pattern-"+i+"; "+cmp+" is less than "+thresholdforpattern[i-1]*thforcamera);
                            continue;
                        }
                        else if(i==2) {//if the pattern meet both camera and audio, present camera only
                            int[] patternX = GetPattern(1);
                            //int sumcpattern = Utils.pattern_compare(ALpattern.get(0), patternX);
                            int sumcpattern = Utils.sum(patternX);
                            if (sumcpattern!=0&&sumcpattern >=(int)(thresholdforpattern[0] * (thforcamera))) {
                                HandleCapture(i);
                                Log.d(TAG, "Camera pattern is " + sumcpattern + ". Skip audio event.");
                                i = 1;
                            }
                        }//avaupdateAudioPortCache
                        ClearPattern(3);
                        cleanpattern[0] = 0;
                        cleanpattern[1] = 0;
                        Log.d(TAG,"pattern match pattern-"+i+"; "+cmp+" reach the threshold "+thresholdforpattern[i-1]);
                        if(dismiss){
                            dismiss = false;
                            exceedtime = false;
                            i = 2;
                            setfalse = System.currentTimeMillis();
                            Log.d(TAG,"Dismiss a activation");
                            continue;
                        }
                    }
                    //record the groundtruth
                    GroundTruthValue groundTruthValue = new GroundTruthValue();
                    groundTruthValue.setSystemTime(time);
                    groundTruthValue.setLabels(i);
                    insert_locker.lock();
                    groundTruthValues.add(groundTruthValue);
                    insert_locker.unlock();
                    int p =0;
                    if(pkg_permission.containsKey(app)){
                        p = pkg_permission.get(app);
                    }
                    if (i==2) {
                        lastaudio = time;
                        if (lastaudio - lastcamera < 2000||(p&i)!=i) {//if the camera follow audio tightly
                            Log.d(TAG, "Skip a audio event" + (lastaudio - lastcamera));
                            ClearPattern(3);
                            HandleCapture(i);
                            continue;
                        }
                    }
                    if (i==1)//if camera is active, we should handle audio api, since it will come with camera api
                    {
                        lastcamera = time;
                        if (lastcamera - lastaudio < 2000||(p&i)!=i) {
                            Log.d(TAG, "Skip a camera event" + (lastcamera - lastaudio));
                            ClearPattern(3);
                            HandleCapture(i);
                            continue;
                        }
                    }
                    HandleCapture(i);
                    updateUI(i);
                    //Log.d(TAG, app + ":" + target_functions.get(i));//&& flags[i]!=0
                    if ((i==1 && handled[i] && time-lastactivetime>3000) ||
                            (i==2 && handled[i]&&time - lastactivetime>1000)){  //Generate only one notification at the same time
                        notification++;
                        locker.lock();
                        handled[i] = false;
                        locker.unlock();
                        //add a user feedback
                        Intent intent = new Intent(mContext, NotificationClickReceiver.class);
                        intentBuild(intent, time, app, i, 0, cmp);
                        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, i + 5, intent, FLAG_UPDATE_CURRENT);

                        Intent intenty = new Intent(mContext, NotificationClickReceiver.class);
                        intentBuild(intenty, time, app, i, 1,cmp);
                        PendingIntent pendingIntenty = PendingIntent.getBroadcast(mContext, i + 10, intenty, FLAG_UPDATE_CURRENT);

                        Intent intentn = new Intent(mContext, NotificationClickReceiver.class);
                        intentBuild(intentn, time, app, i, 2,cmp);
                        PendingIntent pendingIntentn = PendingIntent.getBroadcast(mContext, i + 15, intentn, FLAG_UPDATE_CURRENT);
                        //send notification
                        String textContent = Utils.getDateToString("yyyy-MM-dd HH:mm:ss") + " " + app + " used " + BehaviorList[i];
                        //创建大文本样式
                        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
                        bigTextStyle.setBigContentTitle("DevSec")
                                .setSummaryText(behaviour_map.get(target_functions.get(i)))
                                .bigText(Utils.getDateToString("yyyy-MM-dd HH:mm:ss") + " we found " +
                                        app + " was using " +
                                         BehaviorList[i]+ ". Can you confirm the behaviour?");
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, "behaviour capture")
                                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                                .setPriority(NotificationCompat.PRIORITY_MAX)
                                .setContentTitle("DevSec")
                                .setContentText(textContent)
                                .addAction(0, "Confirm", pendingIntenty)
                                .addAction(0, "Deny", pendingIntentn)
                                .addAction(0, "I'm not sure", pendingIntent)
                                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                                .setGroup("Notification")
                                .setOngoing(true);
//.setAutoCancel(true)
//.setDeleteIntent(pendingIntent)
//.setContentIntent(pendingIntent)
                        builder.setStyle(bigTextStyle);
                        Notification notification = builder.build();
                        NotificationManager mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                        if (mNotificationManager != null) {
                            mNotificationManager.notify(i, notification);
                        }
                    }
                }//if
            }//for i 3
        }
    }

    private void intentBuild(Intent intent,long time,String app,int flag,int ignored,int cmp){
        intent.putExtra("arise",time);
        intent.putExtra("app",app);
        intent.putExtra("flag",flag);
        intent.putExtra("ignored",ignored);
        intent.putExtra("pattern",cmp);
    }

    private void ResetThreshold(Context mContext) {
        if (!reset_thresh) {
            int threshold = getthreshold();
            if (threshold != 0) {
                Log.d(TAG, "The current threshold " + threshold);
                SharedPreferences edit = mContext.getSharedPreferences("user", 0);
                int threshold_pre = edit.getInt("threshold", 3000);
                SharedPreferences.Editor editor = edit.edit();
                Log.d(TAG, "Get the threshold with the lowest count " + threshold_pre);
                if (threshold_pre == 0) {
                    editor.putInt("threshold", threshold);
                    return;
                }
                if (threshold < threshold_pre) {
                    Log.d(TAG, "Found a lower threshold " + threshold);
                    editor.putInt("threshold", threshold);
                } else if (threshold > threshold_pre) {
                    Log.d(TAG, "Current threshold is too big, set it to a previous lower one:" + threshold_pre);
                    setthreshold(threshold_pre);
                }
                reset_thresh = true;
                editor.apply();
            }
        }
    }

    private String[] exec(String target) {
        StringBuilder data = new StringBuilder();
        try {
            java.lang.Process p = null;
            //Log.d(TAG,"TTTTTTTTTTTT"+command);
            p = Runtime.getRuntime().exec(target);
            BufferedReader ie = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String error = null;
            while ((error = ie.readLine()) != null
                    && !error.equals("null")) {
                data.append(error).append("\n");
            }
            String line = null;
            while ((line = in.readLine()) != null
                    && !line.equals("null")) {
                data.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(TAG, data.toString());
        return data.toString().split(" ");
    }

    public static native int[] CacheCheck();
    public native void HandleCapture(int i);
    public native int[] addr();
    public static native int getthreshold();
    public static native int[] GetThresholds();
    public static native long[] GetTimes(); //for compiler
    public static native int[] GetLogs(); //for compiler
    public static native void increase();
    public static native void decrease();
    public static native void setthreshold(int new_thresh);
    //public static native void filteraddr(int index);
    public static native void init(String[] dexlist,String[] filename,String[] func_list);
    public static native int[] GetPattern(int c);
    public static native int[] ClearPattern(int c);
    public static native int[] GetT();
}
