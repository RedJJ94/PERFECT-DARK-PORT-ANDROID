package com.perfectdark.port;

import org.libsdl.app.SDLActivity;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.perfectdark.port.R;
import java.io.File;

public class MainActivity extends SDLActivity {
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final int SETTINGS_REQUEST_CODE = 100;
    private ImageButton backButton;
    
    static {
        System.loadLibrary("SDL2");
        System.loadLibrary("pd");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        android.util.Log.i("PerfectDark", "MainActivity onCreate start");
        
        super.onCreate(savedInstanceState);
        android.util.Log.i("PerfectDark", "MainActivity super.onCreate complete");
        
        // Keep screen on and hide system UI
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        // Handle display cutouts (remove white bars)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode = 
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        
        hideSystemUI();
        
        // Setup touch controls with back button listener
        setupTouchControls();
        
        // No external storage permissions needed with SAF + app-scoped storage
        initializeGame();
        android.util.Log.i("PerfectDark", "MainActivity onCreate complete");
    }
    
    private void setupTouchControls() {
        // Add overlay with back button
        addGameOverlay();
    }
    
    private void addGameOverlay() {
        // Inflate the overlay layout
        View overlay = LayoutInflater.from(this).inflate(R.layout.game_overlay, mLayout, false);
        
        // Get the back button and set click listener
        backButton = overlay.findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSettings();
            }
        });
        
        // Add overlay to the layout on top of the SDL surface
        mLayout.addView(overlay);
    }
    
    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivityForResult(intent, SETTINGS_REQUEST_CODE);
    }
    
    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        decorView.setSystemUiVisibility(uiOptions);
    }
    
    private boolean checkPermissions() { return true; }
    
    private void requestPermissions() { /* no-op */ }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // No storage permissions requested; proceed regardless
    }
    
    private void initializeGame() {
        android.util.Log.i("PerfectDark", "initializeGame start");
        
        // Create data directory in external storage
        File dataDir = new File(getExternalFilesDir(null), "data");
        android.util.Log.i("PerfectDark", "Data dir: " + dataDir.getAbsolutePath());
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        
        // Initialize native game
        android.util.Log.i("PerfectDark", "Calling nativeInit");
        nativeInit(dataDir.getAbsolutePath());
        
        android.util.Log.i("PerfectDark", "initializeGame complete");
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        nativeDestroy();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SETTINGS_REQUEST_CODE) {
            // Settings were potentially changed, you could reload game settings here if needed
            hideSystemUI();
        }
    }

    // Native methods
    public native void nativeInit(String dataPath);
    public native void nativeStartGame();
    public native void nativeDestroy();
}
