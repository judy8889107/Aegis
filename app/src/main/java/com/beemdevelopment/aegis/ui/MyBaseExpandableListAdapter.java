package com.beemdevelopment.aegis.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.ImageButton;
import android.widget.TextView;

import com.beemdevelopment.aegis.R;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.LogRecord;

public class MyBaseExpandableListAdapter extends BaseExpandableListAdapter {

    public HashMap<Integer, ArrayList<Struct.urlObject>> group_list;
    public HashMap<Integer, ArrayList<Struct.urlObject>> child_list;
    public Context content;
    public UrlCheckActivity.MyListener myListener;
    boolean isDelClick;


    public MyBaseExpandableListAdapter(HashMap<Integer, ArrayList<Struct.urlObject>> url_database_list, Context content, UrlCheckActivity.MyListener myListener, boolean isDelClick) {
        this.group_list = url_database_list;
        this.preprocess();
        this.myListener = myListener;
        this.content = content;
        this.isDelClick = isDelClick;

    }




    //處理child_list, 若用 remove會影響到同一份物件(建立新物件並重新加入或實現深拷貝)
    public void preprocess() {
        this.child_list = new HashMap<>();
        for (int i = 0; i < group_list.size(); i++) {
            ArrayList<Struct.urlObject> oldObjects = group_list.get(i);
            ArrayList<Struct.urlObject> newObjects = new ArrayList<>();
            for (int j = 0; j < oldObjects.size(); j++) {
                if (oldObjects.get(j).tagName.equals("mainURL")) continue;
                newObjects.add(oldObjects.get(j));
            }
            this.child_list.put(i, newObjects);
        }
    }

    @Override
    public int getGroupCount() {
        return group_list.size();// group個數
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return child_list.get(groupPosition).size(); //回傳群組child個數
    }

    @Override
    public ArrayList<Struct.urlObject> getGroup(int groupPosition) {
        return group_list.get(groupPosition);
    }

    @Override
    public Struct.urlObject getChild(int groupPosition, int childPosition) {
        return child_list.get(groupPosition).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        ViewHolderGroup viewHolderGroup;
        if (convertView == null) {
            convertView = LayoutInflater.from(content).inflate(
                    R.layout.listview_parent_item, parent, false);
            viewHolderGroup = new ViewHolderGroup();
            viewHolderGroup.tv_parent_item = (TextView) convertView.findViewById(R.id.tv_group_parent);
            viewHolderGroup.parent_item_icon = convertView.findViewById(R.id.parent_item_icon);
            viewHolderGroup.parent_item_icon.setTag(groupPosition); //傳遞資訊
            viewHolderGroup.parent_item_icon.setOnClickListener(myListener);
            convertView.setTag(viewHolderGroup);
        } else {
            viewHolderGroup = (ViewHolderGroup) convertView.getTag();
        }
        String parentItem = group_list.get(groupPosition).get(0).text;
        viewHolderGroup.tv_parent_item.setText(parentItem);
        //添加 Tag
        ArrayList<Struct.urlObject> urlObjects = group_list.get(groupPosition);
        viewHolderGroup.tv_parent_item.setTag(urlObjects);

        //展開收合圖示變更
        if (isExpanded) {
            viewHolderGroup.parent_item_icon.setPressed(true);
            viewHolderGroup.parent_item_icon.setTag(groupPosition); //傳遞位置資訊
        } else {
            viewHolderGroup.parent_item_icon.setPressed(false);
            viewHolderGroup.parent_item_icon.setTag(groupPosition); //傳遞位置資訊
        }
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        ViewHolderItem viewHolderItem;
        if (convertView == null) {
            convertView = LayoutInflater.from(content).inflate(
                    R.layout.listview_child_item, parent, false);
            viewHolderItem = new ViewHolderItem();
            viewHolderItem.tv_child_item = (TextView) convertView.findViewById(R.id.tv_group_child);
            convertView.setTag(viewHolderItem);

        } else {
            viewHolderItem = (ViewHolderItem) convertView.getTag();
        }
        Struct.urlObject urlObject = child_list.get(groupPosition).get(childPosition);
        String childItem = urlObject.text;
        viewHolderItem.tv_child_item.setText(childItem);
        viewHolderItem.tv_child_item.setTag(urlObject);

        return convertView;
    }



    //设置子列表是否可选中
    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }


    public static class ViewHolderGroup {
        public TextView tv_parent_item;
        private ImageButton parent_item_icon;
    }

    public static class ViewHolderItem {
        public TextView tv_child_item;
    }

}
