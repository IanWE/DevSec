package com.SMU.DevSec;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static android.content.Context.MODE_PRIVATE;
import static com.SMU.DevSec.MainActivity.SIZE_LIMIT;

/**
 * @CreateBy HaiyuKing
 * @Used android 获取文件夹或文件的大小 以B、KB、MB、GB 为单位
 * @参考资料 http://blog.csdn.net/jiaruihua_blog/article/details/13622939
 */
public class Utils {
    public static final int SIZETYPE_B = 1;//获取文件大小单位为B的double值
    public static final int SIZETYPE_KB = 2;//获取文件大小单位为KB的double值
    public static final int SIZETYPE_MB = 3;//获取文件大小单位为MB的double值
    public static final int SIZETYPE_GB = 4;//获取文件大小单位为GB的double值
    private static final String TAG = "Utils";
    public static final String DATABASE_PATH = "/data/data/com.SMU.DevSec/databases/";
    private final String FILE_PATH = "/data/data/com.SMU.DevSec/files/";
    public static final String DATABASE_FILENAME = "SideScan.db";
    public static final String TEMP_DATABASE = "TempDatabase.db";
    /**
     * 获取指定文件或指定文件夹的的指定单位的大小
     * @param filePath 文件路径
     * @param sizeType 获取大小的类型1为B、2为KB、3为MB、4为GB
     * @return double值的大小
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
            Log.e("获取文件大小","获取失败!");
        }
        return FormetFileSize(blockSize, sizeType);
    }
    /**
     * 调用此方法自动计算指定文件或指定文件夹的大小
     * @param filePath 文件路径
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
            zos.close();
            fis.close();
        }
    }

    public static boolean checkfile(Context mContext){
        File database = new File(DATABASE_PATH + DATABASE_FILENAME);
        float size = 0;
        try {
            size = FormetFileSize(getFileSize(database),3);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(size>SIZE_LIMIT){ //copy file
            File dest= new File(DATABASE_PATH+TEMP_DATABASE);
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
        File database = new File(DATABASE_PATH + TEMP_DATABASE);
        String compressed_filename = getCurTimeLong()/1000+".gz";
        File file = new File(mContext.getFilesDir(),compressed_filename);
        //File file = new File(FILE_PATH,compressed_filename);
        try {
            zip(database, file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        database.delete();
    }

    private static void reinitializeDB(Context mContext) {
        // Creating the database file in the app sandbox
        SQLiteDatabase db = mContext.openOrCreateDatabase("SideScan.db",
                MODE_PRIVATE, null);
        Locale locale = new Locale("EN", "SG");
        db.setLocale(locale);
        db.execSQL("delete from " + SideChannelContract.TABLE_NAME);
        db.execSQL("delete from " + SideChannelContract.GROUND_TRUTH);
        db.execSQL("delete from " + SideChannelContract.USER_FEEDBACK);
        db.execSQL("delete from " + SideChannelContract.SIDE_COMPILER);
        db.execSQL("delete from " + SideChannelContract.FRONT_APP);
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
}