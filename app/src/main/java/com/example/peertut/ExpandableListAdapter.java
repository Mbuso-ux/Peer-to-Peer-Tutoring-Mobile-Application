package com.example.peertut;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

public class ExpandableListAdapter extends BaseExpandableListAdapter {

    private Context context;
    private List<String> headers;
    private Map<String, List<Map<String, String>>> childMap;

    public ExpandableListAdapter(Context context, List<String> headers, Map<String, List<Map<String, String>>> childMap) {
        this.context = context;
        this.headers = headers;
        this.childMap = childMap;
    }

    @Override
    public int getGroupCount() {
        return headers.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return childMap.get(headers.get(groupPosition)).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return headers.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return childMap.get(headers.get(groupPosition)).get(childPosition);
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
    public boolean hasStableIds() { return false; }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        String header = headers.get(groupPosition);
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(android.R.layout.simple_expandable_list_item_1, null);
        }
        ((TextView) convertView.findViewById(android.R.id.text1)).setText(header);
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        Map<String, String> item = childMap.get(headers.get(groupPosition)).get(childPosition);

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, null);
        }

        ((TextView) convertView.findViewById(android.R.id.text1)).setText(item.get("title"));
        ((TextView) convertView.findViewById(android.R.id.text2)).setText(item.get("description"));

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) { return true; }
}
