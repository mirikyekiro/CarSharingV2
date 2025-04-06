package com.example.myapplication;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class StopwatchService extends Service {
    private static final String TAG = "StopwatchService";
    public static final String PREFS_NAME = "StopwatchPrefs";
    public static final String KEY_ELAPSED_TIME = "elapsedTime";
    private static final String KEY_START_TIME = "startTime";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "stopwatch_channel";

    private Handler handler;
    private long startTime = 0;
    private long elapsedTime = 0;
    private boolean isRunning = false;
    private boolean isReceiverRegistered = false;
    private long lastDisplayedMinutes = -1;

    private BroadcastReceiver resetReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            resetTimer();
        }
    };

    private Runnable updateRunnable  = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                elapsedTime = System.currentTimeMillis() - startTime;
                sendTimeUpdate(elapsedTime);
                long currentMinutes = TimeUnit.MILLISECONDS.toMinutes(elapsedTime);

                sendTimeUpdate(elapsedTime);

                if (currentMinutes != lastDisplayedMinutes) {
                    updateNotification(elapsedTime);
                    lastDisplayedMinutes = currentMinutes;
                }
                handler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
        registerResetReceiver();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Stopwatch Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Stopwatch Service Channel");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private void registerResetReceiver() {
        IntentFilter filter = new IntentFilter("com.example.RESET_TIME");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            registerReceiver(resetReceiver, filter, RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(resetReceiver, filter);
        }
        isReceiverRegistered = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "START_TIMER".equals(intent.getAction())) {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            elapsedTime = prefs.getLong(KEY_ELAPSED_TIME, 0);
            startTime = System.currentTimeMillis() - elapsedTime;
            isRunning = true;

            updateNotification(TimeUnit.MILLISECONDS.toMinutes(elapsedTime));
            startForeground(NOTIFICATION_ID, createNotification(TimeUnit.MILLISECONDS.toMinutes(elapsedTime)));

            handler.post(updateRunnable);
        }
        return START_STICKY;
    }

    private void resetTimer() {
        handler.removeCallbacks(updateRunnable);
        elapsedTime = 0;
        startTime = System.currentTimeMillis();
        saveElapsedTime(elapsedTime);
        sendTimeUpdate(elapsedTime);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        handler.removeCallbacksAndMessages(null);
        stopForeground(true);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(NOTIFICATION_ID);
        manager.cancelAll();

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(KEY_ELAPSED_TIME, 0);
        editor.putLong(KEY_START_TIME, System.currentTimeMillis());
        editor.apply();

        if (isReceiverRegistered) {
            unregisterReceiver(resetReceiver);
            isReceiverRegistered = false;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void updateNotification(long elapsedMillis) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedMillis);
        String notificationText = String.format(Locale.getDefault(),
                "Автомобиль открыт: %d мин", minutes);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("CarSharing")
                .setContentText(notificationText)
                .setSmallIcon(R.drawable.car_icon)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.car_icon))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();

        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.notify(NOTIFICATION_ID, notification);
    }

    private void saveElapsedTime(long elapsedTime) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(KEY_ELAPSED_TIME, elapsedTime);
        editor.putLong(KEY_START_TIME, startTime);
        editor.apply();
    }

    private void sendTimeUpdate(long elapsedTime) {
        Intent intent = new Intent("com.example.UPDATE_TIME");
        intent.setPackage(getPackageName());
        intent.putExtra("elapsedTime", elapsedTime);
        sendBroadcast(intent);
        saveElapsedTime(elapsedTime);
    }

    private Notification createNotification(long minutes) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "stopwatch_service",
                    "Stopwatch Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Stopwatch Service Channel");
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("CarSharing")
                .setContentText("Автомобиль открыт: " + minutes + " мин")
                .setSmallIcon(R.drawable.car_icon)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();
    }

    public static String formatTime(long milliseconds, boolean isNotification) {
        int hours = (int) (milliseconds / 3600000);
        int minutes = (int) (milliseconds % 3600000) / 60000;
        int seconds = (int) (milliseconds % 60000) / 1000;

        if (isNotification) {
            return String.format(Locale.getDefault(), "%d мин", minutes);
        }
        else
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }
}
