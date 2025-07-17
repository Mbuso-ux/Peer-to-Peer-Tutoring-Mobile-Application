package com.example.peertut;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TutorAiChatAdapter extends RecyclerView.Adapter<TutorAiChatAdapter.ChatViewHolder> {
    private final List<AiChatEntry> chatEntries;

    public TutorAiChatAdapter(List<AiChatEntry> chatEntries) {
        this.chatEntries = chatEntries;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes = viewType == 0 ? 
            R.layout.ai_chat_user_bubble : 
            R.layout.ai_chat_bot_bubble;
            
        View view = LayoutInflater.from(parent.getContext())
            .inflate(layoutRes, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        holder.chatContent.setText(chatEntries.get(position).getContent());
    }

    @Override
    public int getItemViewType(int position) {
        return chatEntries.get(position).isFromUser() ? 0 : 1;
    }

    @Override
    public int getItemCount() { return chatEntries.size(); }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView chatContent;
        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            chatContent = itemView.findViewById(R.id.ai_chat_text);
        }
    }
}