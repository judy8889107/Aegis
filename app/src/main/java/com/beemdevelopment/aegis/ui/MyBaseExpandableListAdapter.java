package com.beemdevelopment.aegis.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.beemdevelopment.aegis.R;


import java.util.ArrayList;
import java.util.HashMap;

public class MyBaseExpandableListAdapter extends BaseExpandableListAdapter {

    public HashMap<Integer, ArrayList<Struct.urlObject>> url_database_list;
    public Context content;


    public MyBaseExpandableListAdapter(HashMap<Integer, ArrayList<Struct.urlObject>> url_database_list, Context content) {
        this.url_database_list = url_database_list;
        this.content = content;
    }

    @Override
    public int getGroupCount() {
        return url_database_list.size();//回傳 parent個數
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return url_database_list.get(groupPosition).size(); //回傳群組child個數
    }

    @Override
    public Object getGroup(int groupPosition) {
        return url_database_list.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return url_database_list.get(groupPosition).get(childPosition);
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
            convertView.setTag(viewHolderGroup);
        } else {
            viewHolderGroup = (ViewHolderGroup) convertView.getTag();
        }
        String parentItem = url_database_list.get(groupPosition).get(0).text;
        viewHolderGroup.tv_parent_item.setText(parentItem);
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        ViewHolderItem viewHolderItem;
        if (convertView == null) {
            convertView = LayoutInflater.from(content).inflate(
                    R.layout.listview_parent_item, parent, false);
            viewHolderItem = new ViewHolderItem();
            viewHolderItem.tv_child_item = (TextView) convertView.findViewById(R.id.tv_group_parent);
            convertView.setTag(viewHolderItem);

        } else {
            viewHolderItem = (ViewHolderItem) convertView.getTag();
        }

        String childItem = url_database_list.get(groupPosition).get(childPosition).text;
        viewHolderItem.tv_child_item.setText(childItem);
        return convertView;
    }

    //设置子列表是否可选中
    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }


    private static class ViewHolderGroup {
        private TextView tv_parent_item;
    }

    private static class ViewHolderItem {
        private TextView tv_child_item;
    }
}
