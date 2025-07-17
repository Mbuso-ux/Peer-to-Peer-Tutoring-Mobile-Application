package com.example.peertut;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class ChatListActivity extends AppCompatActivity {
    private ListView chatListView;
    private ChatListAdapter adapter;
    private DatabaseReference userChatsRef, usersRef;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        currentUserId = FirebaseAuth.getInstance().getUid();
        userChatsRef = FirebaseDatabase.getInstance().getReference("userChats").child(currentUserId);
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        chatListView = findViewById(R.id.chatListView);

        loadChatList();


// Modified version with error handling:
        chatListView.setOnItemClickListener((parent, view, position, id) -> {
            ChatListItem item = adapter.getItem(position);
            if (item != null) {
                Intent intent = new Intent(ChatListActivity.this, ChatActivity.class);
                intent.putExtra("chatId", item.getChatId());
                intent.putExtra("otherUserId", item.getOtherUserId());
                startActivity(intent);
            } else {
                Toast.makeText(this, "Invalid chat selection", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadChatList() {
        userChatsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ChatListItem> chatList = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String chatId = ds.getKey();
                    String[] participants = chatId.split("_");

                    // Validate chatId format
                    if (participants.length != 2) {
                        Log.e("ChatList", "Invalid chatId format: " + chatId);
                        continue;
                    }

                    String otherUserId = participants[0].equals(currentUserId) ? participants[1] : participants[0];

                    usersRef.child(otherUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String name = snapshot.child("name").getValue(String.class);
                            chatList.add(new ChatListItem(chatId, otherUserId, name != null ? name : "User"));

                            // Update adapter once all items are added
                            if (adapter == null) {
                                adapter = new ChatListAdapter(ChatListActivity.this, chatList);
                                chatListView.setAdapter(adapter);
                            } else {
                                adapter.clear();
                                adapter.addAll(chatList);
                                adapter.notifyDataSetChanged();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("ChatList", "Error loading user: " + error.getMessage());
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ChatList", "Error loading chats: " + error.getMessage());
            }
        });
    }
}