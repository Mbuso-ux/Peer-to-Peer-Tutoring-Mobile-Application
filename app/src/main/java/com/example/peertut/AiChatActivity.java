package com.example.peertut;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class AiChatActivity extends AppCompatActivity {
    private RecyclerView aiConversationList;
    private TutorAiChatAdapter chatAdapter;
    private final List<AiChatEntry> chatHistory = new ArrayList<>();
    private GenerativeModelFutures model;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        // Initialize Gemini 2.0 Flash with proper configuration
        // Initialize Gemini 2.0 Flash
        GenerativeModel gm = new GenerativeModel(
                "gemini-2.0-flash",
                "AIzaSyCIvXx9e3TFXjmlh5x4nx9fZRBTCuKwAe4"
        );
        model = GenerativeModelFutures.from(gm);

        setupChatInterface();
        showWelcomeMessage();
    }

    public static class NetworkUtils {
        public static boolean isOnline(Context context) {
            ConnectivityManager cm = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            return (netInfo != null && netInfo.isConnected());
        }
    }

    private void setupChatInterface() {
        aiConversationList = findViewById(R.id.ai_conversation_list);
        aiConversationList.setLayoutManager(new LinearLayoutManager(this));
        chatAdapter = new TutorAiChatAdapter(chatHistory);
        aiConversationList.setAdapter(chatAdapter);
        findViewById(R.id.ai_done_button).setOnClickListener(v -> {
            extractAndReturnFilters();
        });



        findViewById(R.id.ai_send_button).setOnClickListener(v -> {
            EditText inputField = findViewById(R.id.ai_input_field);
            String userInput = inputField.getText().toString().trim();

            if (userInput.isEmpty()) {
                Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!NetworkUtils.isOnline(this)) {
                showError("No internet connection");
                return;
            }

            addChatEntry(userInput, true);
            inputField.setText("");
            processUserRequest(userInput);
        });
    }
    private void extractAndReturnFilters() {
        String latestFilters = "";
        // Get last AI response from chat history
        for (int i = chatHistory.size()-1; i >= 0; i--) {
            AiChatEntry entry = chatHistory.get(i);
            if (!entry.isUser()) {
                String[] parts = entry.getText().split("\\|", 2);
                if (parts.length > 0) latestFilters = parts[0].trim();
                break;
            }
        }
        returnWithFilters(latestFilters);
    }
    private void processUserRequest(String query) {
        Content content = new Content.Builder()
                .addText("You are an expert tutor AI helping a student with academic work. " +
                        "Provide clear, detailed explanations for their query: '" + query + "'. " +
                        "For conceptual questions, break down the answer with examples. " +
                        "For homework problems, guide them to the solution step-by-step. " +
                        "For exam prep, suggest effective study strategies. " +
                        "Maintain a friendly, encouraging tone throughout.")
                .build();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                try {
                    String responseText = result.getText();
                    if (responseText == null || responseText.isEmpty()) {
                        showError("I didn't quite get that. Could you rephrase?");
                        return;
                    }

                    Log.d("AI_RESPONSE", responseText);
                    runOnUiThread(() -> {
                        // Format the response for better readability
                        String formattedResponse = responseText
                                .replace(". ", ".\n")
                                .replace("• ", "\n• ")
                                .replace("- ", "\n- ")
                                .replace("1. ", "\n1. ");
                        addChatEntry(formattedResponse, false);
                    });
                } catch (Exception e) {
                    Log.e("AI_ERROR", "Response parsing failed", e);
                    showError("Having trouble processing that. Try asking differently.");
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e("AI_ERROR", "Generation failed", t);
                showError("I'm having some technical difficulties. Please try again soon!");
            }
        }, ContextCompat.getMainExecutor(this));
    }


    private void handleAiOutput(String response) {
        String[] parts = response.split("\\|", 2);
        String filters = "";
        String explanation = response;

        if (parts.length == 2) {
            filters = parts[0].trim();
            explanation = parts[1].trim();
        } else {
            Log.w("AI_PARSE", "Unexpected response format: " + response);
        }

        addChatEntry(explanation, false);
    }

    private void returnWithFilters(String filters) {
        Intent resultData = new Intent();
        // Ensure non-null value using empty string as default
        resultData.putExtra("searchFilters", filters != null ? filters : "");
        setResult(RESULT_OK, resultData);
        finish();
    }

    private void showError(String message) {
        runOnUiThread(() ->
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        );
    }

    private void addChatEntry(String text, boolean isUser) {
        chatHistory.add(new AiChatEntry(text, isUser));
        chatAdapter.notifyItemInserted(chatHistory.size() - 1);
        aiConversationList.smoothScrollToPosition(chatHistory.size() - 1);
    }

    private void showWelcomeMessage() {
        String welcomeMsg = "Hello! I'm your Personal Learning Companion 🤓\n\n" +
                "I can help you with:\n" +
                "• Understanding difficult concepts\n" +
                "• Solving homework problems\n" +
                "• Preparing for exams\n" +
                "• Finding study resources\n\n" +
                "Try asking me about:\n" +
                "- \"Explain calculus derivatives\"\n" +
                "- \"Help with my chemistry assignment\"\n" +
                "- \"Best way to study for history finals\"\n" +
                "- \"Python programming examples\"";
        addChatEntry(welcomeMsg, false);
    }

    @Override
    public Executor getMainExecutor() {
        return super.getMainExecutor();
    }
}