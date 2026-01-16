package com.danilaai;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatActivity extends AppCompatActivity {
    
    private TextView chatText;
    private EditText inputText;
    private Button sendButton, backButton;
    private TextView modelInfo;
    
    private ExecutorService executor;
    private Handler mainHandler;
    private StringBuilder chatHistory;
    private String modelPath;
    
    static {
        System.loadLibrary("danilka");
    }
    
    public native String generateResponse(String prompt);
    public native boolean loadModel(String modelPath);
    public native void unloadModel();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        
        modelPath = getIntent().getStringExtra("model_path");
        
        initViews();
        loadModelAndStart();
    }
    
    private void initViews() {
        chatText = findViewById(R.id.chat_text);
        inputText = findViewById(R.id.input_text);
        sendButton = findViewById(R.id.send_button);
        backButton = findViewById(R.id.back_button);
        modelInfo = findViewById(R.id.model_info);
        
        chatText.setMovementMethod(new ScrollingMovementMethod());
        chatHistory = new StringBuilder();
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        
        // Информация о модели
        String fileName = modelPath.substring(modelPath.lastIndexOf("/") + 1);
        modelInfo.setText("🤖 МОДЕЛЬ: " + fileName);
        
        sendButton.setOnClickListener(v -> sendMessage());
        backButton.setOnClickListener(v -> finish());
        
        addSystemMessage("⚡ DANILKA AI АКТИВИРОВАН\nГотов к работе!");
    }
    
    private void loadModelAndStart() {
        chatText.setText("🔄 ЗАГРУЗКА МОДЕЛИ...");
        
        executor.execute(() -> {
            boolean loaded = loadModel(modelPath);
            
            mainHandler.post(() -> {
                if (loaded) {
                    addSystemMessage("✅ МОДЕЛЬ УСПЕШНО ЗАГРУЖЕНА\nЗадавайте вопросы!");
                } else {
                    addSystemMessage("❌ ОШИБКА ЗАГРУЗКИ МОДЕЛИ");
                    Toast.makeText(this, "Неверный формат GGUF", Toast.LENGTH_LONG).show();
                }
            });
        });
    }
    
    private void sendMessage() {
        String prompt = inputText.getText().toString().trim();
        if (prompt.isEmpty()) return;
        
        addMessage("👤 ВЫ", prompt);
        inputText.setText("");
        sendButton.setEnabled(false);
        
        executor.execute(() -> {
            String response = generateResponse(prompt);
            
            mainHandler.post(() -> {
                addMessage("🤖 DANILKA AI", response);
                sendButton.setEnabled(true);
                
                // Прокрутка вниз
                chatText.post(() -> {
                    int scrollAmount = chatText.getLayout().getLineTop(chatText.getLineCount()) 
                                    - chatText.getHeight();
                    if (scrollAmount > 0) {
                        chatText.scrollTo(0, scrollAmount);
                    }
                });
            });
        });
    }
    
    private void addMessage(String sender, String message) {
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        chatHistory.append("\n[").append(time).append("] ").append(sender).append(":\n")
                  .append(message).append("\n");
        chatText.setText(chatHistory.toString());
    }
    
    private void addSystemMessage(String message) {
        chatHistory.append("═".repeat(30)).append("\n")
                  .append(message).append("\n")
                  .append("═".repeat(30)).append("\n");
        chatText.setText(chatHistory.toString());
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.execute(() -> unloadModel());
            executor.shutdown();
        }
    }
                                      }
