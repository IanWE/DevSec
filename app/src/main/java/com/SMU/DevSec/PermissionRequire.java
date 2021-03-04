/*
This is the file to require permisson
*/
package com.SMU.DevSec;

import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Process;
import android.provider.Settings;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import static com.SMU.DevSec.Utils.readSaveFile;

public class PermissionRequire {
    Context mContext;
    final String TAG = "ItemsCheck";

    PermissionRequire(Context context){
        mContext = context;
    }
    /**
     * 保存首次进入APP状态
     */
    public void startDialog() {
        final AlertDialog alertDialog = new AlertDialog.Builder(mContext).create();
        alertDialog.show();
        alertDialog.setCancelable(false);
        Window window = alertDialog.getWindow();
        if (window != null) {
            window.setContentView(R.layout.permisson_require);
            window.setGravity(Gravity.CENTER);

            TextView tvContent = window.findViewById(R.id.tv_p_content);
            TextView tvgrant = window.findViewById(R.id.tv_p_grant);
            String str = "Please grant the permission \"Usage Stats\" by:\n" +
                    "(1) Clicking the button above;\n" +
                    "(2) When next screen appear, select our app \"DevSec\" from the list of apps;\n" +
                    "(3) Toggling to turn on \"Permit usage access\"; and\n" +
                    "(4) Clicking the left arrow at the top left of the screen to return to our app.";
            //Log.d(TAG,str);
            SpannableStringBuilder ssb = new SpannableStringBuilder();
            ssb.append(str);
            /*
            int start = str.indexOf("License to Use");//第一个出现的位置
            StyleSpan span = new StyleSpan(Typeface.BOLD);
            int end = str.indexOf(": SMU grants");
            ssb.setSpan(span, start, end, 0);

            start = str.indexOf("User Information");//第一个出现的位置
            end = str.indexOf(": By downloading");
            ssb.setSpan(span, start, end, 1);

            start = str.indexOf("Acceptable Use");//第一个出现的位置
            end = str.indexOf(": The user agrees not");
            ssb.setSpan(span, start, end, 2);

            start = str.indexOf("Indemnification");//第一个出现的位置
            end = str.indexOf(": The user agrees to");
            ssb.setSpan(span, start, end, 3);

            start = str.indexOf("Warranties/Representations/Guarantees");//第一个出现的位置
            end = str.indexOf(": SMU provides");
            ssb.setSpan(span, start, end, 4);
            */
            tvContent.setText(ssb);
            //tvContent.setMovementMethod(LinkMovementMethod.getInstance());
            tvContent.setMovementMethod(ScrollingMovementMethod.getInstance());
            tvContent.setText(ssb);

            tvgrant.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                    mContext.startActivity(intent);
                    new Thread() {
                        public void run() {
                            while(!getpermisson()) {
                                try {
                                    Thread.sleep(500);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            alertDialog.cancel();
                        }
                    }.start();
                }
            });
        }
    }
    boolean getpermisson(){
        AppOpsManager appOps = (AppOpsManager) mContext.getSystemService(Context.APP_OPS_SERVICE);
        boolean granted = false;
        int mode = 0;
        if (appOps != null) {
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    mode = appOps.unsafeCheckOpNoThrow("android:get_usage_stats",
                            Process.myUid(), mContext.getPackageName());
                }
            }
            granted = mode == AppOpsManager.MODE_ALLOWED;
        }
        return granted;
    }

}
