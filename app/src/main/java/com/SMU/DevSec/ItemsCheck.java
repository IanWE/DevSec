/*
This class will show some clauses for user to check.
*/
package com.SMU.DevSec;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
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

public class ItemsCheck {
    Context mContext;
    final String TAG = "ItemsCheck";

    ItemsCheck(Context context){
        mContext = context;
    }
    /**
     * 是否是首次进入APP
     */
    public boolean isAgreed() {
        SharedPreferences edit = mContext.getSharedPreferences("itemscheck", 0);
        return edit.getBoolean("agreed", false);
    }

    /**
     * 保存首次进入APP状态
     */
    public void Agreed() {
        SharedPreferences preferences = mContext.getSharedPreferences("itemscheck", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor=preferences.edit();
        editor.putBoolean("agreed", true);
        editor.apply();
    }

    public void finished(){
        System.exit(0);
    }

    public void startDialog() {
        final AlertDialog alertDialog = new AlertDialog.Builder(mContext).create();
        alertDialog.show();
        alertDialog.setCancelable(false);
        Window window = alertDialog.getWindow();
        if (window != null) {
            window.setContentView(R.layout.item_check);
            window.setGravity(Gravity.CENTER);

            TextView tvContent = window.findViewById(R.id.tv_content);
            TextView tvCancel = window.findViewById(R.id.tv_cancel);
            TextView tvAgree = window.findViewById(R.id.tv_agree);
            String str = readSaveFile("items.txt",mContext);
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
            //tvContent.setText(ssb);

            //tvContent.setMovementMethod(LinkMovementMethod.getInstance());
            tvContent.setMovementMethod(ScrollingMovementMethod.getInstance());
            tvContent.setText(ssb);

            tvCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    alertDialog.cancel();
                    finished();
                }
            });

            tvAgree.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Agreed();
                    alertDialog.cancel();
                }
            });
        }

    }

}
