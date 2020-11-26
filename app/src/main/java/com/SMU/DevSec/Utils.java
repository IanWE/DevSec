package com.SMU.DevSec;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.zip.GZIPOutputStream;

import static android.content.Context.MODE_PRIVATE;
import static com.SMU.DevSec.MainActivity.limited_size;

/**
 * Some Utils
 */
public class Utils {
    public static final int SIZETYPE_B = 1;//获取文件大小单位为B的double值
    public static final int SIZETYPE_KB = 2;//获取文件大小单位为KB的double值
    public static final int SIZETYPE_MB = 3;//获取文件大小单位为MB的double值
    public static final int SIZETYPE_GB = 4;//获取文件大小单位为GB的double值
    private static final String TAG = "Utils";
    public static final String DATABASE_FILENAME = "SideScan.db";
    public static final String TEMP_DATABASE = "TempDatabase.db";
    public static boolean compressing=false;
    /**
     * get the size of folder or file
     * @param filePath path
     * @param sizeType the returned type,1 is B、2 is KB、3 is MB、4 is GB
     * @return filesize
     */
    public static double getFolderOrFileSize(String filePath,int sizeType){
        File file=new File(filePath);
        long blockSize=0;
        try {
            if(file.isDirectory()){
                blockSize = getFolderSize(file);
            }else{
                blockSize = getFileSize(file);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG,"Failed to get size");
        }
        return FormetFileSize(blockSize, sizeType);
    }
    /**
     * 调用此方法自动计算指定文件或指定文件夹的大小
     * @param filePath
     * @return 计算好的带B、KB、MB、GB的字符串
     */
    public static String getAutoFolderOrFileSize(String filePath){
        File file=new File(filePath);
        long blockSize=0;
        try {
            if(file.isDirectory()){
                blockSize = getFolderSize(file);
            }else{
                blockSize = getFileSize(file);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("获取文件大小","获取失败!");
        }
        return FormetFileSize(blockSize);
    }
    /**
     * 获取指定文件的大小
     * @param file
     * @return
     * @throws Exception
     */
    private static long getFileSize(File file) throws Exception
    {
        long size = 0;
        if (file.exists()){
            FileInputStream fis = null;
            fis = new FileInputStream(file);
            size = fis.available();
            fis.close();
        }
        else{
            //file.createNewFile();
            Log.e(TAG,"文件不存在!");
        }

        return size;
    }

    /**
     * 获取指定文件夹的大小
     * @param file
     * @return
     * @throws Exception
     */
    private static long getFolderSize(File file) throws Exception
    {
        long size = 0;
        File flist[] = file.listFiles();
        for (int i = 0; i < flist.length; i++){
            if (flist[i].isDirectory()){
                size = size + getFolderSize(flist[i]);
            }
            else{
                size =size + getFileSize(flist[i]);
            }
        }
        return size;
    }
    /**
     * 转换文件大小
     * @param fileSize
     * @return
     */
    private static String FormetFileSize(long fileSize)
    {
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        String wrongSize="0B";
        if(fileSize==0){
            return wrongSize;
        }
        if (fileSize < 1024){
            fileSizeString = df.format((double) fileSize) + "B";
        }
        else if (fileSize < 1048576){
            fileSizeString = df.format((double) fileSize / 1024) + "KB";
        }
        else if (fileSize < 1073741824){
            fileSizeString = df.format((double) fileSize / 1048576) + "MB";
        }
        else{
            fileSizeString = df.format((double) fileSize / 1073741824) + "GB";
        }
        return fileSizeString;
    }
    /**
     * 转换文件大小,指定转换的类型
     * @param fileSize
     * @param sizeType
     * @return
     */
    private static float FormetFileSize(long fileSize,int sizeType)
    {
        DecimalFormat df = new DecimalFormat("#.00");
        float fileSizeLong = 0;
        switch (sizeType) {
            case SIZETYPE_B:
                fileSizeLong=Float.valueOf(df.format((double) fileSize));
                break;
            case SIZETYPE_KB:
                fileSizeLong=Float.valueOf(df.format((double) fileSize / 1024));
                break;
            case SIZETYPE_MB:
                fileSizeLong=Float.valueOf(df.format((double) fileSize / 1048576));
                break;
            case SIZETYPE_GB:
                fileSizeLong=Float.valueOf(df.format((double) fileSize / 1073741824));
                break;
            default:
                break;
        }
        return fileSizeLong;
    }

    public static void zip(File srcFile, File desFile) throws IOException {
        GZIPOutputStream zos = null;
        FileInputStream fis = null;
        try {
            //创建压缩输出流,将目标文件传入
            zos = new GZIPOutputStream(new FileOutputStream(desFile));
            //创建文件输入流,将源文件传入
            fis = new FileInputStream(srcFile);
            byte[] buffer= new byte[1024];
            int len= -1;
            //利用IO流写入写出的形式将源文件写入到目标文件中进行压缩
            while ((len= (fis.read(buffer)))!= -1) {
                zos.write(buffer,0, len);
            }
            Log.d(TAG,"Compressed file successfully");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally{
            if(zos!=null)
                zos.close();
            if(fis!=null)
                fis.close();
        }
    }

    public static boolean checkfile(Context mContext){
        File database = mContext.getDatabasePath(DATABASE_FILENAME);
        float size = 0;
        try {
            size = FormetFileSize(getFileSize(database),3);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(size>limited_size){ //copy file
            //File dest= new File(DATABASE_PATH+TEMP_DATABASE);
            File dest= mContext.getDatabasePath(TEMP_DATABASE);
            if (dest.exists()) {
                dest.delete(); // delete file
            }
            try {
                dest.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            FileChannel srcChannel = null;
            FileChannel dstChannel = null;
            try {
                srcChannel = new FileInputStream(database).getChannel();
                dstChannel = new FileOutputStream(dest).getChannel();
                srcChannel.transferTo(0, srcChannel.size(), dstChannel);
                srcChannel.close();
                dstChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            reinitializeDB(mContext);
            return true;
        }
        return false;
    }

    public static void compress(Context mContext){
        if(compressing)
            return;
        compressing = true;
        File database = mContext.getDatabasePath(TEMP_DATABASE);
        String compressed_filename = getCurTimeLong()/1000+".gz";
        File file = new File(mContext.getFilesDir(),compressed_filename);
        //File file = new File(FILE_PATH,compressed_filename);
        try {
            zip(database, file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        database.delete();
        compressing = false;
    }

    private static void reinitializeDB(Context mContext) {
        // Creating the database file in the app sandbox
        SQLiteDatabase db = mContext.openOrCreateDatabase("SideScan.db",
                MODE_PRIVATE, null);
        Locale locale = new Locale("EN", "SG");
        db.setLocale(locale);
        db.execSQL("delete from " + SideChannelContract.TABLE_NAME);
        db.close();
        Log.d(TAG, "Reinitialized Database");
    }


    public static String readSaveFile(String filename, Context mContext) {
        InputStream inputStream;
        try {
            inputStream = mContext.getResources().getAssets().open(filename);
            byte[] temp = new byte[1024];
            StringBuilder sb = new StringBuilder("");
            int len = 0;
            while ((len = inputStream.read(temp)) > 0) {
                sb.append(new String(temp, 0, len));
            }
            inputStream.close();
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 获取系统时间戳
     * @return
     */
    public static long getCurTimeLong(){
        long time=System.currentTimeMillis();
        return time;
    }
    /**
     * 获取当前时间
     * @param pattern
     * @return
     */
    public static String getCurDate(String pattern){
        SimpleDateFormat sDateFormat = new SimpleDateFormat(pattern);
        return sDateFormat.format(new java.util.Date());
    }

    /**
     * 时间戳转换成字符窜
     * @param pattern
     * @return
     */
    public static String getDateToString(String pattern) {
        Date date = new Date(getCurTimeLong());
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        return format.format(date);
    }

    /**
     * 将字符串转为时间戳
     * @param dateString
     * @param pattern
     * @return
     */
    public static long getStringToDate(String dateString, String pattern) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
        Date date = new Date();
        try{
            date = dateFormat.parse(dateString);
        } catch(ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return date.getTime();
    }

    public static String getVersionName(Context mContext)
    {
        // 获取packagemanager的实例
        PackageManager packageManager = mContext.getPackageManager();
        // getPackageName()是你当前类的包名，0代表是获取版本信息
        PackageInfo packInfo = null;
        try {
            packInfo = packageManager.getPackageInfo(mContext.getPackageName(),0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "0";
        }
        String version = packInfo.versionName;
        return version.replace(".","_");
    }

    public static void saveArray(Context context, int[] intArray, String target) {
        SharedPreferences prefs = context.getSharedPreferences("pattern", Context.MODE_PRIVATE);
        JSONArray jsonArray = new JSONArray();
        for (int b : intArray) {
            jsonArray.put(b);
        }
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(target,jsonArray.toString());
        editor.apply();
    }

    public static int[] getArray(Context context,String target)
    {
        SharedPreferences prefs = context.getSharedPreferences("pattern", Context.MODE_PRIVATE);
        ArrayList<Integer> arrayList = new ArrayList<Integer>();
        try {
            JSONArray jsonArray = new JSONArray(prefs.getString(target, "[]"));
            for (int i = 0; i < jsonArray.length(); i++) {
                arrayList.add(jsonArray.getInt(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        int[] d = new int[arrayList.size()];
        for(int i=0;i<arrayList.size();i++)
            d[i] = arrayList.get(i);
        return d;
    }

    static int sum(int[] l){
        int s = 0;
        for(int i=0;i<l.length;i++){
            s += l[i];
        }
        return s;
    }

    static int pattern_compare(int[] o,int[] t){
        int s = 0;
        for(int i=0;i<o.length;i++){
            if(t[i]==1&&o[i]==1)
                s++;
        }
        return s;
    }

    /**
     * 获取当前手机系统版本号
     *
     * @return  系统版本号
     */
    public static String getSystemVersion() {
        return android.os.Build.VERSION.RELEASE;
    }

    /**
     * 获取手机型号
     *
     * @return  手机型号
     */
    public static String getSystemModel() {
        return android.os.Build.MODEL;
    }

    /**
     * 获取手机厂商
     *
     * @return  手机厂商
     */
    public static String getDeviceBrand() {
        return android.os.Build.BRAND;
    }

    /**
     * 获取CPU型号
     * @return
     */
    public static String getCpuName(){

        String str1 = "/proc/cpuinfo";
        String str2 = "";

        try {
            FileReader fr = null;
            try {
                fr = new FileReader(str1);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            BufferedReader localBufferedReader = new BufferedReader(fr);
            while ((str2=localBufferedReader.readLine()) != null) {
                if (str2.contains("Hardware")) {
                    return str2.split(":")[1];
                }
            }
            localBufferedReader.close();
        } catch (IOException e) {
        }
        return null;
    }

    public static String getBasicInfo(){
        return " SystemModel:"+getSystemModel()+" Android Version:"+getSystemVersion()+" DeviceBrand:"+getDeviceBrand()+" Cpu:"+getCpuName();
    }
}