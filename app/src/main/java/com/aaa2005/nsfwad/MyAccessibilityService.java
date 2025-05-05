package com.aaa2005.nsfwad;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.ActivityManager;
import android.content.Context;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import java.util.concurrent.TimeUnit;

import android.net.Uri;
import android.media.AudioAttributes;
import android.media.MediaPlayer;


public class MyAccessibilityService extends AccessibilityService {
    private Handler handler = new Handler();
    private Runnable scanTask;
    private ScreenStateReceiver screenStateReceiver;
    private boolean isScanning = false;


    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        deleteNotificationChannel();

        createNotificationChannel();

        // Register the screen state receiver
        screenStateReceiver = new ScreenStateReceiver(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenStateReceiver, filter);

        // Define the scan task
        scanTask = new Runnable() {
            @Override
            public void run() {
                if (isScanning) {
                    scanScreen();
                }
                handler.postDelayed(this, TimeUnit.SECONDS.toMillis(2)); // Repeat every 2 seconds
            }
        };
        
        // Start the scan task
        isScanning = true;
        handler.post(scanTask);
    }



    private void deleteNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.deleteNotificationChannel("nsfw_detector_channel");
            //Log.d("MyAccessibilityService", "Notification channel deleted.");
        }
    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NotificationChannel channel = new NotificationChannel(
                "nsfw_detector_channel", // Channel ID
                "NSFW Detector Notifications", // Channel name
                NotificationManager.IMPORTANCE_HIGH // Use HIGH importance
            );
            channel.setDescription("Notifications for NSFW detection.");
            channel.enableLights(true); // Enable notification lights
            channel.enableVibration(true); // Enable vibration
            channel.setShowBadge(true); // Show a badge on the app icon

            // Set the custom sound for the channel
            Uri soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.bodi);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
            channel.setSound(soundUri, audioAttributes);


            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
            //Log.d("MyAccessibilityService", "Notification channel created.");
        }
    }

    public void pauseScanning() {
        //Log.d("MyAccessibilityService", "Pausing scanning.");
        isScanning = false;
    }

    public void resumeScanning() {
        //Log.d("MyAccessibilityService", "Resuming scanning.");
        isScanning = true;
    }

    private void scanScreen() {
        //Log.d("MyAccessibilityService", "onAccessibilityEvent called.");
        //if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode == null) {
                //Log.d("MyAccessibilityService", "Root node is null.");
                return;
            }

            // Get the package name of the app that triggered the event
            String packageName = rootNode.getPackageName().toString();

            // Traverse the UI hierarchy and extract text
            traverseNode(rootNode, packageName);
        //}
    }

    private void traverseNode(AccessibilityNodeInfo node, String packageName) {
        if (node == null) {
            return;
        }

        // Get the text from the current node
        CharSequence nodeText = node.getText();
        if (nodeText != null && nodeText.length() > 0) {
            String text = nodeText.toString().toLowerCase();
            //Log.d("MyAccessibilityService", "Text: " + text);

            // Check for suspicious text
            if (containsSuspiciousText(text)) {
                Log.d("MyAccessibilityService", "Suspicious text detected!");
                takeAction(packageName);
            }
        }

        // Traverse child nodes
        for (int i = 0; i < node.getChildCount(); i++) {
            traverseNode(node.getChild(i), packageName);
        }
    }

    private boolean containsSuspiciousText(String text) {
        // List of suspicious keywords
        String[] suspiciousKeywords = {
            // English
            " porn ", " xxx ", " sex ", " nudes ", " naked ", " nude ", " boobs ", " tits ", " pussy", " dick ",
            " cock ", " anal ", " blowjob ", " handjob ", " cum ", " orgasm ", " fetish ", " bdsm", " hentai ",
            " milf ", " slut ", " whore ", " prostitute ", " escort ", " erotic ", " sexting", " masturbate ",
            " masturbation ", " hardcore ", " softcore ", " pr0n ", "s3x", " n00dz ", " b00bs", " t1ts ",
            " p0rn ", " xvideos ", " xnxx ", " redtube ", " youporn ", " pornhub ", " xhamster ", " onlyfans ",
            " chaturbate ", "cam4 ", " myfreecams ", " livejasmin ", " brazzers ", " naughtyamerica ",
            " bangbros ", " free porn ", " sex videos ", " nude pics ", " hot girls ", "sexy photos ",
            " adult content ", "explicit videos ", " porn videos ", " sex chat ", " cam girls ",
    
            // Arabic
            " بورن ", " سكس ", " عارية ", " ثدي "," ممارسة الجنس ",
            " سكـس ", " بــورن ", " ز*** ", " ك*** ", " بورن هاب ", " إكس فيديوز ", " إكس إن إكس إكس ", " ريدتوب ", " يوبورن ",
            " أفلام سكس ", " صور عارية ", " مقاطع إباحية ", " جنس ساخن ", " فتيات عاريات ",
    
        };
        for (String keyword : suspiciousKeywords) {
            if (text.contains(keyword)) {
                Log.d("MyAccessibilityService", "Keyword found: " + keyword);
                return true;
            }
        }
        //Log.d("MyAccessibilityService", "No suspicious text found.");
        return false;
    }

    private long lastAppCloseTime = 0;
    private static final long COOLDOWN_PERIOD = TimeUnit.SECONDS.toMillis(7); // 10 seconds cooldown
    
    private void takeAction(String packageName) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAppCloseTime < COOLDOWN_PERIOD) {
            Log.d("MyAccessibilityService", "Cooldown active. Skipping action.");
            return;
        }
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

        // Close the current app
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        am.killBackgroundProcesses(packageName);
        Log.d("MyAccessibilityService", "Closing the app with package : " + packageName);
        // Show a warning (optional)
        // You can use a Toast or Notification here
        // Launch the home screen
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(homeIntent);
        //Log.d("MyAccessibilityService", "Returning to home screen.");
        lastAppCloseTime = currentTime;
        // Send a notification
        sendNotification("إنذار التكفات", "الى متى ستبقا خاضع لموزتك \n الى متى ستبقا تجلد فيه.");
    }

    

    private void sendNotification(String title, String message) {

        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.bodi);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.release(); // Release the MediaPlayer resources after the sound finishes playing
            }
        });
        mediaPlayer.start(); // Start playing the sound





        //Log.d("MyAccessibilityService", "Attempting to send notification: " + title);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "nsfw_detector_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Use your app's notification icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true);

        // Set the custom sound for devices below Android 8.0
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Uri soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.bodi);
            builder.setSound(soundUri);
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        //notificationManager.notify(1, builder.build()); // Use a unique ID for each notification
        //Log.d("MyAccessibilityService", "Notification sent.");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(1, builder.build());
            }
        } else {
            notificationManager.notify(1, builder.build());
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the scan task when the service is destroyed
        handler.removeCallbacks(scanTask);

        // Unregister the screen state receiver
        if (screenStateReceiver != null) {
            unregisterReceiver(screenStateReceiver);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // No longer needed
    }


    @Override
    public void onInterrupt() {
        // Handle service interruption
    }


    class ScreenStateReceiver extends BroadcastReceiver {
        private MyAccessibilityService service;
    
        public ScreenStateReceiver(MyAccessibilityService service) {
            this.service = service;
        }
    
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                // Screen is turned on, resume scanning
                service.resumeScanning();
            } else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                // Screen is turned off, pause scanning
                service.pauseScanning();
            }
        }
    }
    
}

