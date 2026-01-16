package com.danilaai;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    
    private static final int STORAGE_PERMISSION = 101;
    private static final int FILE_PICKER = 102;
    
    private TextView statusText;
    private ProgressBar progressBar;
    private Button btnLocal, btnDownload, btnBrowser, btnStart;
    private EditText urlInput;
    private LinearLayout modelList;
    private TextView scanText;
    
    private ExecutorService executor;
    private Handler mainHandler;
    private String selectedModelPath;
    private List<File> availableModels = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        checkPermissions();
        startScanAnimation();
    }
    
    private void initViews() {
        statusText = findViewById(R.id.status_text);
        progressBar = findViewById(R.id.progress_bar);
        btnLocal = findViewById(R.id.btn_local);
        btnDownload = findViewById(R.id.btn_download);
        btnBrowser = findViewById(R.id.btn_browser);
        btnStart = findViewById(R.id.btn_start);
        urlInput = findViewById(R.id.url_input);
        modelList = findViewById(R.id.model_list);
        scanText = findViewById(R.id.scan_text);
        
        executor = Executors.newFixedThreadPool(2);
        mainHandler = new Handler(Looper.getMainLooper());
        
        btnLocal.setOnClickListener(v -> openFilePicker());
        btnDownload.setOnClickListener(v -> downloadFromUrl());
        btnBrowser.setOnClickListener(v -> browseHuggingFace());
        btnStart.setOnClickListener(v -> startChat());
        
        updateStatus("⚡ DANILKA AI ULTIMATE ⚡");
    }
    
    private void startScanAnimation() {
        Animation scanAnim = AnimationUtils.loadAnimation(this, R.anim.scan);
        scanText.startAnimation(scanAnim);
        
        executor.execute(() -> {
            try { Thread.sleep(1000); } catch (InterruptedException e) {}
            scanForModels();
        });
    }
    
    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, 
                           Manifest.permission.WRITE_EXTERNAL_STORAGE}, 
                STORAGE_PERMISSION);
        }
    }
    
    private void scanForModels() {
        mainHandler.post(() -> scanText.setText("🔍 СКАНИРОВАНИЕ..."));
        
        executor.execute(() -> {
            availableModels.clear();
            
            // Ищем GGUF во всех папках
            String[] searchPaths = {
                Environment.getExternalStorageDirectory() + "/Download/",
                Environment.getExternalStorageDirectory() + "/",
                getFilesDir() + "/",
                getExternalFilesDir(null) + "/"
            };
            
            for (String path : searchPaths) {
                File dir = new File(path);
                if (dir.exists()) {
                    scanDirectory(dir);
                }
            }
            
            mainHandler.post(() -> {
                scanText.clearAnimation();
                scanText.setText("✅ СКАНИРОВАНИЕ ЗАВЕРШЕНО");
                updateModelList();
                if (availableModels.size() > 0) {
                    updateStatus("Найдено моделей: " + availableModels.size());
                }
            });
        });
    }
    
    private void scanDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file);
            } else if (file.getName().toLowerCase().endsWith(".gguf")) {
                availableModels.add(file);
            }
        }
    }
    
    private void updateModelList() {
        modelList.removeAllViews();
        
        for (File model : availableModels) {
            Button modelBtn = new Button(this);
            modelBtn.setText("🎯 " + model.getName() + "\n📊 " + formatFileSize(model.length()));
            modelBtn.setBackgroundResource(R.drawable.btn_model);
            modelBtn.setTextColor(getResources().getColor(R.color.neon_cyan));
            modelBtn.setAllCaps(false);
            modelBtn.setTextSize(12);
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 5, 0, 5);
            modelBtn.setLayoutParams(params);
            
            modelBtn.setOnClickListener(v -> {
                selectedModelPath = model.getAbsolutePath();
                updateStatus("Выбрана: " + model.getName());
                btnStart.setEnabled(true);
                btnStart.setBackgroundResource(R.drawable.btn_main_active);
            });
            
            modelList.addView(modelBtn);
        }
        
        if (availableModels.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("⚠️ Модели не найдены\nЗагрузите GGUF файл");
            emptyText.setTextColor(getResources().getColor(R.color.neon_purple));
            emptyText.setTextSize(14);
            emptyText.setGravity(View.TEXT_ALIGNMENT_CENTER);
            modelList.addView(emptyText);
        }
    }
    
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        
        try {
            startActivityForResult(Intent.createChooser(intent, "Выберите GGUF"), FILE_PICKER);
        } catch (Exception e) {
            Toast.makeText(this, "Установите файловый менеджер", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_PICKER && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            copyModelFromUri(uri);
        }
    }
    
    private void copyModelFromUri(Uri uri) {
        progressBar.setVisibility(View.VISIBLE);
        updateStatus("Копирование модели...");
        
        executor.execute(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                File modelsDir = new File(getFilesDir(), "models");
                if (!modelsDir.exists()) modelsDir.mkdirs();
                
                String fileName = "model_" + System.currentTimeMillis() + ".gguf";
                File outputFile = new File(modelsDir, fileName);
                
                OutputStream os = new FileOutputStream(outputFile);
                
                byte[] buffer = new byte[8192];
                int length;
                while ((length = is.read(buffer)) > 0) {
                    os.write(buffer, 0, length);
                }
                
                os.close();
                is.close();
                
                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Модель скопирована", Toast.LENGTH_SHORT).show();
                    scanForModels();
                    selectedModelPath = outputFile.getAbsolutePath();
                    btnStart.setEnabled(true);
                    btnStart.setBackgroundResource(R.drawable.btn_main_active);
                });
                
            } catch (Exception e) {
                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void downloadFromUrl() {
        String url = urlInput.getText().toString().trim();
        if (url.isEmpty()) {
            Toast.makeText(this, "Введите URL", Toast.LENGTH_SHORT).show();
            return;
        }
        
        progressBar.setVisibility(View.VISIBLE);
        updateStatus("Загрузка...");
        
        executor.execute(() -> {
            try {
                // Конвертируем ссылку
                String directUrl = convertUrl(url);
                
                URL fileUrl = new URL(directUrl);
                HttpURLConnection conn = (HttpURLConnection) fileUrl.openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                conn.connect();
                
                // Имя файла
                String fileName = "downloaded_" + System.currentTimeMillis() + ".gguf";
                if (directUrl.contains("/")) {
                    String[] parts = directUrl.split("/");
                    String last = parts[parts.length - 1];
                    if (last.contains(".gguf")) {
                        fileName = last.split("\\?")[0];
                    }
                }
                
                File outputFile = new File(getFilesDir() + "/models/", fileName);
                InputStream is = conn.getInputStream();
                FileOutputStream os = new FileOutputStream(outputFile);
                
                byte[] buffer = new byte[8192];
                int length, total = 0;
                int fileSize = conn.getContentLength();
                
                while ((length = is.read(buffer)) > 0) {
                    os.write(buffer, 0, length);
                    total += length;
                    
                    if (fileSize > 0) {
                        final int percent = (total * 100) / fileSize;
                        mainHandler.post(() -> updateStatus("Загрузка: " + percent + "%"));
                    }
                }
                
                os.close();
                is.close();
                
                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Модель загружена!", Toast.LENGTH_SHORT).show();
                    scanForModels();
                    selectedModelPath = outputFile.getAbsolutePath();
                    btnStart.setEnabled(true);
                    btnStart.setBackgroundResource(R.drawable.btn_main_active);
                });
                
            } catch (Exception e) {
                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Ошибка загрузки", Toast.LENGTH_LONG).show();
                    updateStatus("Ошибка: " + e.getMessage());
                });
            }
        });
    }
    
    private String convertUrl(String url) {
        // Google Drive
        if (url.contains("drive.google.com/file/d/")) {
            String id = url.split("/file/d/")[1].split("/")[0];
            return "https://drive.google.com/uc?export=download&id=" + id;
        }
        // Hugging Face
        if (url.contains("huggingface.co/")) {
            return url.replace("/blob/", "/resolve/") + "?download=true";
        }
        return url;
    }
    
    private void browseHuggingFace() {
        String hfUrl = "https://huggingface.co/models?search=gguf&sort=trending";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(hfUrl));
        startActivity(intent);
    }
    
    private void startChat() {
        if (selectedModelPath == null || !new File(selectedModelPath).exists()) {
            Toast.makeText(this, "Выберите модель", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("model_path", selectedModelPath);
        startActivity(intent);
    }
    
    private void updateStatus(String message) {
        statusText.setText(message);
    }
    
    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024.0));
        return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        scanForModels();
    }
  }
