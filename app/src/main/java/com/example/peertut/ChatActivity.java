package com.example.peertut;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class ChatActivity extends AppCompatActivity {
    private ListView messagesListView;
    private EditText messageInput;
    private ImageButton sendButton;
    private DatabaseReference messagesRef;
    private String chatId, currentUserId, otherUserId;
    private MessageAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatId = getIntent().getStringExtra("chatId");
        otherUserId = getIntent().getStringExtra("otherUserId");
        currentUserId = FirebaseAuth.getInstance().getUid();
        messagesRef = FirebaseDatabase.getInstance().getReference("chatRooms").child(chatId).child("messages");

        initializeViews();
        setupChat();
    }

    private void initializeViews() {
        messagesListView = findViewById(R.id.messagesListView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        TextView chatTitle = findViewById(R.id.chatTitle);

        FirebaseDatabase.getInstance().getReference("users").child(otherUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String name = snapshot.child("name").getValue(String.class);
                        chatTitle.setText("Chat with " + (name != null ? name : "User"));
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });

        sendButton.setOnClickListener(v -> sendMessage());
    }

    private void setupChat() {
        adapter = new MessageAdapter(this, currentUserId);
        messagesListView.setAdapter(adapter);

        messagesRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildId) {
                Message message = snapshot.getValue(Message.class);
                if (message != null) {
                    adapter.addMessage(message);
                    messagesListView.setSelection(adapter.getCount() - 1);
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildId) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildId) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void sendMessage() {
        String messageText = messageInput.getText().toString().trim();
        if (!messageText.isEmpty()) {
            String messageId = messagesRef.push().getKey();
            Message message = new Message(currentUserId, messageText);
            messagesRef.child(messageId).setValue(message);
            messageInput.setText("");
        }
    }
}