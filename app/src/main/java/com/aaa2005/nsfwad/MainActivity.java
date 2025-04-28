package com.aaa2005.nsfwad;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;



public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkAndRequestPermissions();

        // Open Accessibility settings
        //Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        //startActivity(intent);
    }

    private void checkAndRequestPermissions() {
        String[] permissions = {
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.KILL_BACKGROUND_PROCESSES,
            Manifest.permission.RECEIVE_BOOT_COMPLETED
        };
    
        boolean allPermissionsGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }
    
        if (!allPermissionsGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("MainActivity", "Permission granted: " + permissions[i]);
                } else {
                    Log.d("MainActivity", "Permission denied: " + permissions[i]);
                }
            }
        }
    }
}

