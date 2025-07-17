package com.example.peertut;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private List<ChatMessage> messages;

    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        public TextView textView;

        public ChatViewHolder(View v) {
            super(v);
            textView = v.findViewById(android.R.id.text1);
        }
    }

    public ChatViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ChatViewHolder(v);
    }

    public void onBindViewHolder(ChatViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);
        holder.textView.setText(msg.sender + ": " + msg.message);
    }

    public int getItemCount() {
        return messages.size();
    }
}
