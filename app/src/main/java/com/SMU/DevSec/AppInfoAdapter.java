package com.SMU.DevSec;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppInfoAdapter extends BaseAdapter{
    private List<AppInfo> data;
    private Context context;
    private Map<Integer,Boolean> map=new HashMap<>();
    public AppInfoAdapter(List<com.SMU.DevSec.AppInfo> data, Context context){
        this.data= data;
        this.context= context;
    }
    private boolean onBind;
    private int checkedPosition = -1;

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return 0;
    }
    public int getCheckedPosition() {
        return checkedPosition;
    }
    //返回带数据当前行的Item视图对象
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view;
        //1. 如果convertView是null, 加载item的布局文件
        if(convertView==null) {
            Log.e("TAG", "getView() load layout");
            view = View.inflate(context, R.layout.item_main, null);
        }else {
        view =convertView;//复用历史缓存对象
    }

         //2. 得到当前行数据对象
        AppInfo appInfo = data.get(position);
        //3. 得到当前行需要更新的子View对象
        ImageView imageView = (ImageView) view.findViewById(R.id.item_icon);
        TextView textView = (TextView) view.findViewById(R.id.item_name);
        //4. 给视图设置数据
        //imageView.setImageDrawable(appInfo.getIcon());
        textView.setText(appInfo.getAppName());
        //返回convertView
            final CheckBox checkBox=(CheckBox)view.findViewById(R.id.item_check);
            checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (checkBox.isChecked()){
                        map.clear();
                        map.put(position,true);
                        checkedPosition = position;
                        Log.i("po","position"+checkedPosition);
                    }else {
                        map.remove(position);
                        if (map.size() == 0) {
                            checkedPosition = -1; //-1 代表一个都未选择
                        }
                    }
                    if (!onBind) {
                        notifyDataSetChanged();
                    }
                }
            });
            onBind = true;
            if(map!=null&&map.containsKey(position)){
                checkBox.setChecked(true);
            }else {
                checkBox.setChecked(false);
            }
            onBind = false;

        return view;
    }
}
