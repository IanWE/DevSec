package com.SMU.DevSec;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Looper;
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

import androidx.core.app.NotificationCompat;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static com.SMU.DevSec.MainActivity.audio;
import static com.SMU.DevSec.MainActivity.camera;
import static com.SMU.DevSec.MainActivity.location;
import static com.SMU.DevSec.MainActivity.name_permisson;
import static com.SMU.DevSec.MainActivity.quering;
import static com.SMU.DevSec.MainActivity.showToast;
import static com.SMU.DevSec.MainActivity.status;
import static com.SMU.DevSec.MainActivity.check;
import static com.SMU.DevSec.SideChannelJob.groundTruthValues;
import static com.SMU.DevSec.SideChannelJob.locker;
import static java.util.Objects.*;

public class CacheScan {
    private static final String TAG = "CacheScan";
    static Context mContext;
    static String target_lib = "services.odex";
    static String target_func = "0";
    static String []target;
    static String []ranges;
    static String []filenames;
    static String []offsets;
    static String []func_lists;

    public static boolean ischeckedaddr = false;
    static boolean[] handled = {true,true,true};
    private long lastactivetime = 0;
    private String preapp = "";
    private long preventcamera = 0;
    private long preventaudio = 0;
    private String cameraapi = "CameraManager.java_connectCameraServiceLocked";
    private String audioapi = "_ZN7android5media12IAudioRecordC2Ev";
    //static volatile int semaphore=1;

    static ArrayList<String> target_functions = new ArrayList<String>();
    final HashMap<String,String> behaviour_map = new HashMap<String,String>();

    CacheScan(Context context) throws IOException {
        mContext = context;
        init();
    }
//6 function
    private void init() throws IOException {
        int pid = android.os.Process.myPid();
        behaviour_map.put("LocationManagerService.java_updateLastLocationLocked", "Location");
        behaviour_map.put("ContentResolver.java_createSqlQueryBundle", "Information");
        behaviour_map.put(audioapi,"AudioRecorder");
        behaviour_map.put(cameraapi,"Camera");
        behaviour_map.put("CameraManager.java","Camera");
        AssetManager am = mContext.getAssets();
        ArrayList<String> range = new ArrayList<String>();
        ArrayList<String> filename = new ArrayList<String>();
        ArrayList<String> offset = new ArrayList<String>();
        ArrayList<String> func_list = new ArrayList<String>();
        String[] targets = am.list("targets");
        String temp;
        /*
        int l = targets.length;
        for(int i=0;i<l;i++)//move so file to the end
            if (targets[i].equals("z_camera.so")) {
                Log.d(TAG,"Move "+targets[i]+" to the list's end");
                temp = targets[i];
                for(int j=i+1;j<targets.length;j++) {
                    targets[j - 1] = targets[j];
                    //Log.d(TAG, "Move " + targets[j] + " to "+targets[j-1]);
                }
                targets[targets.length-1] = temp;
                l--;
            }
         */
        for (String f : requireNonNull(targets)) {
            Log.d("CacheScan", f );//+ " " + f.substring(f.lastIndexOf(".") + 1));
            if (f.substring(f.lastIndexOf(".") + 1).equals("so")) {
                //String[] arr = exec(pid, f);//unable to grep file since some reason
                String oat_target = Utils.readSaveFile("targets/"+f,mContext);
                String[] arr = oat_target.split("," );
                offset.add("");
                StringBuilder funcs= new StringBuilder();
                for(int i=1;i<arr.length;i++){
                    target_functions.add(arr[i]);
                    if(i==1) {
                        funcs.append(arr[i]);
                        continue;
                    }
                    funcs.append(",").append(arr[i]);
                }
                func_list.add(funcs.toString());
                range.add("");
                offset.add("");
                filename.add(arr[0]); //get the path of library in the android system;
                //Log.d(TAG, "TTTTTTTTTT "+pid+" funcs:" + funcs.toString() + " " + arr[0] +" "+arr[arr.length - 1].split("\n")[0]);
            }
            else if(f.substring(f.lastIndexOf(".") + 1).equals("oat")||
                    f.substring(f.lastIndexOf(".") + 1).equals("odex")){
                String oat_target = Utils.readSaveFile("targets/"+f,mContext);
                String[] arr = oat_target.split("," );
                String target_oat = arr[0];
                String target_dex = arr[1];
                StringBuilder funcs= new StringBuilder();
                for(int i=2;i<arr.length;i++){
                    target_functions.add(arr[i]);
                    if(i==2) {
                        funcs.append(arr[i]);
                        continue;
                    }
                    funcs.append(",").append(arr[i]);
                }
                func_list.add(funcs.toString());
                range.add(target_dex);
                offset.add("");
                filename.add(target_oat);
                //Log.d(TAG, "TTTTTTTTTT "+pid+" funcs:" + funcs.toString() + " " + target_dex +" "+target_oat);
            }
        }
        ranges = (String[]) range.toArray(new String[targets.length]);
        offsets = (String[]) offset.toArray(new String[targets.length]);
        filenames = (String[]) filename.toArray(new String[targets.length]);
        func_lists = (String[]) func_list.toArray(new String[targets.length]);
        //Log.d(TAG, "Target:" + target_func + " " + target_lib);
    }

    private String[] exec(int pid, String target) {
        String data = "";
        try {
            Process p = null;
            String command = "grep " + target + " /proc/`pgrep "+MainActivity.NAME+"`/maps"; //还没进内存
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
                if (st.contains("-xp")){// && st.split("/").length == 4) {
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
        ArrayList<String> dexfiles = new ArrayList<String>();
        try {
            jf = new JarFile(fileName);
            for (Enumeration<JarEntry> e = jf.entries(); e.hasMoreElements(); ) {
                JarEntry je = (JarEntry) e.nextElement();
                if (je.getName().endsWith(".dex")) {
                    filename = "data/data/com.SMU.DevSec/cache/" + je.getName();
                    File file = new File(filename);
                    unpack(jf, je, file);
                    Log.d("Extract Jar:",filename);
                    dexfiles.add(filename);
                }
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
    public static void unpack(JarFile jarFile, JarEntry entry, File file) throws IOException {
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
    private static String getTopApp() {
        check = true;
        UsageStatsManager mUsageStatsManager = (UsageStatsManager) mContext.getSystemService(Context.USAGE_STATS_SERVICE);//usagestats
        //Log.e("TopPackage Name", mUsageStatsManager.isAppInactive("e.smu.questlocation")+"");//10
        long time = System.currentTimeMillis();
        String topPackageName = null;
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
        return topPackageName;
    }

    private void updateUI(final int result){
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
                    }
                }
            });
        }

    private static void unparsedaddr(final int success,final int result){
        if(success==0){
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
        }
        else {
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

    public static void unparsedaddr() {
        while(getthreshold()==0) {//wait untill threshold initialized
            Log.d(TAG,"sleeppppppppppppppppp");
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        final int[] thds = thd();
        final int threshold = getthreshold();
        if (thds != null) {
            status[4].post(new Runnable() {
                @Override
                public void run() {//
                    status[4].setText("Cache - " + thds[0] + ", Mem - " + thds[2] + " \nThreshold - " + threshold);
                }
            });
        }
    }

    public void showToast(final String text) {
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
    public void Notify() {
        int[] flags = CacheCheck();
        if (flags != null) {
            Log.d(TAG,flags[0]+":"+flags[1]+":"+flags[2]+":"+flags[3]+":"+flags[4]);
            //检查一次, 地址是否都被成功解析
            if (!ischeckedaddr) {
                int[] addrs = addr();
                for (int i = 0; i < addrs.length; i++) {
                    unparsedaddr(addrs[i], i);
                }
                unparsedaddr();
                ischeckedaddr = true;
                showToast("Job scheduled successfully");
            }
            String app = getTopApp();//增加一个判断， 不要反复弹notification
            for (int i = 0; i < flags.length - 1; i++) {
                if (app == null) app = "Unknown App";//如果前台app不发生变化，则不继续弹？
                String cur = target_functions.get(i);
                if (flags[i] != 0 || (cur.equals(cameraapi) && flags[flags.length - 1] != 0)) {//(cur.equals(cameraapi) && app.toUpperCase().contains("CAMERA"))) {//in case system camera do not activate api
                    //Integer permison_type;
                    //int type = 0;
                    //permison_type = name_permisson.get(app);
                    //if(permison_type!=null){
                    //    type = permison_type;
                    //}
                    //if((cur.equals("AudioManager.java_getIdForAudioFocusListener")&&((type&1)!=1))||
                    //        (cur.equals("CAMERA.java_setHasPreviewCallback")&&((type&2)!=2))) {
                    /*
                    if(cur.equals(audioapi)){//if audio is active, we should handle camera api, since it may come with camera api
                        //HandleCapture(i - 1);
                        preventcamera = Utils.getCurTimeLong();
                        if(preventcamera-preventaudio<1000) {//if the camera follow audio tightly
                            Log.d(TAG,"Skip a audio event"+(preventcamera-preventaudio));
                            HandleCapture(i);
                            continue;
                        }
                    }
                */

                    if (cur.equals(cameraapi))//if camera is active, we should handle audio api, since it will come with camera api
                    {
                        //HandleCapture(i + 1);
                        //preventaudio = Utils.getCurTimeLong();
                        HandleCapture(flags.length - 1);
                        //if(preventaudio-preventcamera<3000) {
                        //    Log.d(TAG,"Skip a camera event"+(preventcamera-preventaudio));
                        //    HandleCapture(i);
                        //    continue;
                    }
                    GroundTruthValue groundTruthValue = new GroundTruthValue();
                    groundTruthValue.setSystemTime(System.currentTimeMillis());
                    groundTruthValue.setCurrentApp(app);
                    groundTruthValue.setLabels(SideChannelContract.CLASSES[i]);
                    locker.lock();
                    groundTruthValues.add(groundTruthValue);
                    locker.unlock();

                    HandleCapture(i);
                    updateUI(i);
                    //Store the action to database
                    Log.d(TAG, app + ":" + target_functions.get(i));
                    if ((cur.equals(cameraapi) && handled[i] &&
                            (Utils.getCurTimeLong() - lastactivetime) > 2000) ||
                            (cur.equals(audioapi) && handled[i])) {  //Generate only one notification at the same time
                        //Log.d(TAG,"TTTTTTTT permisson camera:"+((type&2)==2)+" audio:"+((type&1)==1));
                        locker.lock();
                        handled[i] = false;
                        locker.unlock();
                        Intent intent = new Intent(mContext, NotificationClickReceiver.class);
                        intent.putExtra("flag", i);
                        intent.putExtra("ignored", 0);
                        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, i + 5, intent, FLAG_UPDATE_CURRENT);

                        Intent intenty = new Intent(mContext, NotificationClickReceiver.class);
                        intenty.putExtra("flag", i);
                        intenty.putExtra("ignored", 1);
                        PendingIntent pendingIntenty = PendingIntent.getBroadcast(mContext, i + 10, intenty, FLAG_UPDATE_CURRENT);

                        Intent intentn = new Intent(mContext, NotificationClickReceiver.class);
                        intentn.putExtra("flag", i);
                        intentn.putExtra("ignored", 2);
                        PendingIntent pendingIntentn = PendingIntent.getBroadcast(mContext, i + 15, intentn, FLAG_UPDATE_CURRENT);
                        //send notification
                        String textContent = Utils.getDateToString("yyyy-MM-dd HH:mm:ss") + " " + app + ":" + behaviour_map.get(target_functions.get(i));
                        //创建大文本样式
                        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
                        bigTextStyle.setBigContentTitle("DevSec")
                                .setSummaryText(behaviour_map.get(target_functions.get(i)))
                                .bigText(Utils.getDateToString("yyyy-MM-dd HH:mm:ss") + " " +
                                        app + " used " +
                                        behaviour_map.get(target_functions.get(i)));
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, "behaviour capture")
                                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                                .setPriority(NotificationCompat.PRIORITY_MAX)
                                .setContentTitle("DevSec")
                                .setContentText(textContent)
                                .setContentIntent(pendingIntent)
                                .addAction(0, "Yes, I know", pendingIntenty)
                                .addAction(0, "Not Sure", pendingIntentn)
                                .setDeleteIntent(pendingIntent)
                                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                                .setGroup("Notification")
                                .setAutoCancel(true);

                        builder.setStyle(bigTextStyle);
                        Notification notification = builder.build();
                        NotificationManager mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                        mNotificationManager.notify(i, notification);
                    }
                    if (cur.equals(cameraapi)) {
                        lastactivetime = Utils.getCurTimeLong();
                    }
                }
            }
        }
    }
    public native int[] CacheCheck();
    public native void HandleCapture(int i);
    public native int[] addr();
    public static native int[] thd();
    public static native int getthreshold();
    public static native void increase();
    public static native void decrease();
}
