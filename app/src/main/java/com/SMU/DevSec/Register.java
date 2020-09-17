package com.SMU.DevSec;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Adler32;

import androidx.appcompat.app.AppCompatActivity;

import static com.SMU.DevSec.SideChannelJob.groundTruthValues;

public class Register extends AppCompatActivity{
        private String email;
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.register_layout);

            Button button1 = (Button) findViewById(R.id.buttonr1);
            button1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditText editText1 =(EditText)findViewById(R.id.email);
                    email=editText1.getText().toString();
                    if(isEmail(email))
                    {
                        save_information(email);
                        Toast.makeText(getBaseContext(), "Register Successfully.", Toast.LENGTH_SHORT)
                            .show();
                        finish();
                    }
                    else
                        Toast.makeText(getBaseContext(), "Please input a valid email.", Toast.LENGTH_SHORT)
                            .show();
                }});
        }

        private void save_information(String email){
            try {
                String source = email;
                String result;

                Adler32 adl = new Adler32();
                adl.update(email.getBytes());
                source = email+"_"+adl.getValue();
                //Log.e("RSA", source);
                //String s="abc";
                //byte[] b=s.getByte();
                //String s1=new String(b);

                InputStream inPublic = getResources().getAssets().open("public.pem");
                PublicKey publicKey = RSA.loadPublicKey(inPublic);
                byte[] encryptByte = RSA.encryptData(source.getBytes(), publicKey);
                // 为了方便观察吧加密后的数据用base64加密转一下，要不然看起来是乱码,所以解密是也是要用Base64先转换
                String afterencrypt = Base64Utils.encode(encryptByte);
                Log.d("RSA", afterencrypt);

                SharedPreferences preferences=getSharedPreferences("user", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor=preferences.edit();
                editor.putString("RSA", afterencrypt);
                editor.putString("adler",adl.getValue()+"");
                editor.commit();

            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    public static boolean isEmail(String string) {
        if (string == null)
            return false;
        String regEx1 = "^([a-z0-9A-Z]+[-|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$";
        Pattern p;
        Matcher m;
        p = Pattern.compile(regEx1);
        m = p.matcher(string);
        if (m.matches())
            return true;
        else
            return false;
    }
}

