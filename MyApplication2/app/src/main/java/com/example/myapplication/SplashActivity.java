package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.myapplication.database.DataBase;
import com.example.myapplication.registration.Registration;
import com.yandex.runtime.connectivity.internal.ConnectivitySubscription;

import java.util.ArrayList;
import java.util.List;

public class SplashActivity extends AppCompatActivity {
    DataBase db;
    SharedPreferences sPref;
    public static final String APP_PREFERENCES = "myProfile";
    public static final String DATA_SAVE = "DATA_SAVE";
    private static final int REQUEST_CODE_LOCATION = 100;
    private static final int REQUEST_CODE_NOTIFICATIONS = 101;
    boolean data_save;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        sPref = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);

        data_save = sPref.getBoolean(DATA_SAVE, false);

        db = new DataBase(this);
        db.create_db();

        if (isOnline(this)) {
            new Handler(Looper.getMainLooper()).postDelayed(this::checkPermissionsAndStart, 1000);
        } else {
            showNoInternetDialog();
        }

    }

    private void showNoInternetDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Нет подключения к интернету")
                .setMessage("Для работы приложения требуется интернет-соединение")
                .setPositiveButton("Закрыть приложение", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void checkPermissionsAndStart() {
        if (checkLocationPermissions()) {
            if (isLocationEnabled()) {
                startActivityMain();
            } else {
                showEnableLocationDialog();
            }
        } else {
            requestNecessaryPermissions();
        }
    }

    private boolean checkLocationPermissions() {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isLocationEnabled() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private void showEnableLocationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Требуется включить GPS")
                .setMessage("Для работы приложения необходимо включить геолокацию")
                .setPositiveButton("Настройки", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Закрыть приложение", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_LOCATION) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                startActivityMain();
            } else {
                showPermissionRationale();
            }
        }
    }

    public void startActivityMain()
    {
        if(data_save) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
        else {
            Intent intent2 = new Intent(this, Registration.class);
            startActivity(intent2);
            finish();
        }
    }

    private void showPermissionRationale() {
        new AlertDialog.Builder(this)
                .setTitle("Требуются разрешения")
                .setMessage("Для работы приложения необходимы разрешения на доступ к местоположению")
                .setPositiveButton("Закрыть приложение", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    public void requestNecessaryPermissions()
    {
        List<String> permissionsToRequest = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(android.Manifest.permission.POST_NOTIFICATIONS);
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_CODE_LOCATION
            );
        } else {
            startActivityMain();
        }
    }

    public static boolean isOnline(Context context)
    {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting())
        {
            return true;
        }
        return false;
    }
}