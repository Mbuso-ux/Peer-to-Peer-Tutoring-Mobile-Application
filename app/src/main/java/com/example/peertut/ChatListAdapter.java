package com.example.peertut;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.List;

public class ChatListAdapter extends ArrayAdapter<ChatListItem> {
    public ChatListAdapter(Context context, List<ChatListItem> items) {
        super(context, 0, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ChatListItem item = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_chat, parent, false);
        }
        
        TextView nameView = convertView.findViewById(R.id.chatName);
        nameView.setText(item.getOtherUserName());
        
        return convertView;
    }
}