package com.example.peertut;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class MessageAdapter extends ArrayAdapter<Message> {
    private static final int TYPE_SENT     = 0;
    private static final int TYPE_RECEIVED = 1;

    private String currentUserId;

    public MessageAdapter(Context context, String currentUserId) {
        super(context, 0, new ArrayList<>());
        this.currentUserId = currentUserId;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        Message msg = getItem(position);
        return (currentUserId != null
                && msg.getSenderId() != null
                && currentUserId.equals(msg.getSenderId()))
                ? TYPE_SENT
                : TYPE_RECEIVED;
    }

    public void addMessage(Message message) {
        add(message);
        notifyDataSetChanged();
    }

    private static class ViewHolder {
        TextView messageText;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int type = getItemViewType(position);
        ViewHolder holder;

        if (convertView == null) {
            int layoutRes = (type == TYPE_SENT)
                    ? R.layout.item_message_sent
                    : R.layout.item_message_received;
            convertView = LayoutInflater.from(getContext())
                    .inflate(layoutRes, parent, false);

            holder = new ViewHolder();
            holder.messageText = convertView.findViewById(R.id.messageText);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // At this point holder.messageText should never be null,
        // assuming your XML is correctly set up.
        Message message = getItem(position);
        holder.messageText.setText(message.getMessage());

        return convertView;
    }
}
