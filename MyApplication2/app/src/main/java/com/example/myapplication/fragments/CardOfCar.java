package com.example.myapplication.fragments;

import static android.content.Context.MODE_PRIVATE;
import static com.example.myapplication.StopwatchService.KEY_ELAPSED_TIME;
import static com.example.myapplication.StopwatchService.PREFS_NAME;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;
import com.example.myapplication.StopwatchService;

import java.io.IOException;
import java.io.InputStream;

public class CardOfCar extends Fragment {
    //sharedpref
    public static final String APP_PREFERENCES = "myProfile";
    public static final String APP_PREFERENCES_IS_RENT = "IsRent";
    public static final String APP_PREFERENCES_ID_RENT_CAR = "IDRentCar";
    public static final String APP_PREFERENCES_CARISOPEN = "CarIsOpen";
    SharedPreferences sPref;
    SharedPreferences.Editor ed;

    //objects
    TextView auto_name, auto_number, auto_gaz, auto_status, auto_price;
    String auto_id;
    ImageView auto_imageBIG;
    ImageButton btnExit;
    Button btnRent;
    Bundle arguments;
    long elapsedTime;

    private BroadcastReceiver timeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if("com.example.UPDATE_TIME".equals(intent.getAction())) {
                elapsedTime = intent.getLongExtra("elapsedTime", 0);

                if(sPref.getString(APP_PREFERENCES_IS_RENT, "NULL").equals("true")
                        && sPref.getString(APP_PREFERENCES_ID_RENT_CAR, "NULL").equals(auto_id)
                        && sPref.getString(APP_PREFERENCES_CARISOPEN, "NULL").equals("true")) {
                    String timeText = "Завершить поездку: " + StopwatchService.formatTime(elapsedTime, false);
                    btnRent.setText(timeText);
                }
            }
        }
    };

    public static CardOfCar newInstance(Bundle bundle) {
        CardOfCar fragment = new CardOfCar();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle bundle) {
        View view = inflater.inflate(R.layout.fragment_card_of_car, container, false);


        Activity activity = getActivity();

        arguments = getArguments();
        sPref = activity.getSharedPreferences(APP_PREFERENCES, MODE_PRIVATE);
        ed = sPref.edit();

        auto_name = view.findViewById(R.id.cocNameCarTV);
        auto_number = view.findViewById(R.id.cocNumberCarTV);
        auto_gaz = view.findViewById(R.id.cocGazCarTV);
        auto_price = view.findViewById(R.id.cocPriceCarTV);
        auto_imageBIG = view.findViewById(R.id.cocImageCarIV);
        auto_status = view.findViewById(R.id.cocStatusCarTV);

        btnExit = view.findViewById(R.id.btnClose);
        btnRent = view.findViewById(R.id.btnRent);

        auto_id = arguments.getString("auto_id", "NULL");
        auto_name.setText(arguments.getString("auto_name", "NULL"));
        auto_number.setText(arguments.getString("auto_number", "NULL"));
        auto_gaz.setText("Уровень топлива: " + arguments.getString("auto_gaz", "NULL") + " %");
        auto_price.setText("Поездка от " + arguments.getString("auto_price", "NULL") + " руб/мин");

        try (InputStream ims = activity.getAssets().open("BIG"+arguments.getString("auto_imageBIG", "NULL"));)
        {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(ims, null, options);

            int targetWidth = auto_imageBIG.getWidth();
            int targetHeight = auto_imageBIG.getHeight();

            if (targetWidth <= 0) targetWidth = 800;
            if (targetHeight <= 0) targetHeight = 600;

            options.inSampleSize = calculateInSampleSize(options, targetWidth, targetHeight);
            options.inJustDecodeBounds = false;

            try (InputStream ims2 = activity.getAssets().open("BIG" + arguments.getString("auto_imageBIG", "NULL"))) {
                Bitmap bitmap = BitmapFactory.decodeStream(ims2, null, options);
                auto_imageBIG.setImageBitmap(bitmap);
            }
        }
        catch(IOException ex)
        {
            Log.d("ERROR", "CANT LOAD IMAGE");
        }

        checkRent();

        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) activity).ResetMarkers();
                FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.FCV_tab, MapFragment.newInstance()).commit();
            }
        });

        btnRent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sPref.getString(APP_PREFERENCES_IS_RENT, "NULL").equals("false")) {
                    ed.putString(APP_PREFERENCES_IS_RENT, "true");
                    ed.putString(APP_PREFERENCES_CARISOPEN, "false");
                    ed.putString(APP_PREFERENCES_ID_RENT_CAR, auto_id);
                }
                else if (sPref.getString(APP_PREFERENCES_IS_RENT, "NULL").equals("true") &&
                        sPref.getString(APP_PREFERENCES_CARISOPEN, "NULL").equals("false")) {
                    ed.putString(APP_PREFERENCES_CARISOPEN, "true");
                    ed.apply();

                    Intent intent = new Intent(getActivity(), StopwatchService.class);
                    intent.setAction("START_TIMER");
                    requireActivity().startService(intent);

                    btnRent.setText("Завершить поездку: 00:00:00");
                    ((MainActivity)getActivity()).setStatusCar(auto_id, "0");
                }
                else if (sPref.getString(APP_PREFERENCES_IS_RENT, "NULL").equals("true") &&
                        sPref.getString(APP_PREFERENCES_CARISOPEN, "NULL").equals("true")) {

                    Intent stopIntent = new Intent(getActivity(), StopwatchService.class);
                    requireActivity().stopService(stopIntent);

                    ed.putString(APP_PREFERENCES_IS_RENT, "false");
                    ed.putString(APP_PREFERENCES_CARISOPEN, "false");
                    ed.putString(APP_PREFERENCES_ID_RENT_CAR, "0");

                    btnRent.setText("Забронировать");

                    //pay
                    ((MainActivity)getActivity()).setStatusCar(auto_id, "1");
                    try {
                        String autoPriceStr = arguments.getString("auto_price", "0");
                        double autoPrice = Double.parseDouble(autoPriceStr);
                        long elapsedTimeMinutes = elapsedTime / 60000;
                        double totalPrice = elapsedTimeMinutes * autoPrice;
                        String formattedPrice = String.format("%,.2f", totalPrice);

                        Toast.makeText(view.getContext(), "Итоговая цена: " + formattedPrice + " руб.", Toast.LENGTH_LONG).show();
                    }
                    catch (NumberFormatException e) {}

                    ((MainActivity) activity).ResetMarkers();
                    FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                    fragmentTransaction.replace(R.id.FCV_tab, MapFragment.newInstance()).commit();
                }

                ed.commit();
                checkRent();
            }
        });

        return view;
    }

    public void checkRent()
    {
        if(sPref.getString(APP_PREFERENCES_IS_RENT, "NULL").equals("false") && sPref.getString(APP_PREFERENCES_ID_RENT_CAR, "NULL").equals("0") && sPref.getString(APP_PREFERENCES_CARISOPEN, "NULL").equals("false"))
        {
            Intent resetIntent = new Intent("com.example.RESET_TIME");
            resetIntent.setPackage(requireActivity().getPackageName());
            requireActivity().sendBroadcast(resetIntent);

        }

        if (arguments.getString("auto_status", "NULL").equals("0") && !sPref.getString(APP_PREFERENCES_ID_RENT_CAR, "NULL").equals(auto_id)) {
            auto_status.setText("Автомобиль недоступен");
            btnRent.setEnabled(false);
            btnRent.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF9C8F4E")));
        }
        else {
            if (sPref.getString(APP_PREFERENCES_IS_RENT, "NULL").equals("true")) {
                if (sPref.getString(APP_PREFERENCES_ID_RENT_CAR, "NULL").equals(auto_id)) {
                    if (sPref.getString(APP_PREFERENCES_CARISOPEN, "NULL").equals("true")) {
                        btnRent.setEnabled(true);
                        btnRent.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFEFCD25")));
                        Intent intent = new Intent(getActivity(), StopwatchService.class);
                        requireActivity().startService(intent);
                        btnRent.setText("Завершить поездку: " + StopwatchService.formatTime(elapsedTime, false));
                    } else {
                        btnRent.setText("Открыть автомобиль");
                        btnRent.setEnabled(true);
                        btnRent.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFEFCD25")));
                        Intent resetIntent = new Intent("com.example.RESET_TIME");
                        resetIntent.setPackage(requireActivity().getPackageName());
                        requireActivity().sendBroadcast(resetIntent);
                    }
                }
                else {
                    btnRent.setText("Завершите текущую поездку");
                    btnRent.setEnabled(false);
                    btnRent.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF9C8F4E")));

                }
            }
            else {
                btnRent.setText("Забронировать");
                Intent resetIntent = new Intent("com.example.RESET_TIME");
                resetIntent.setPackage(requireActivity().getPackageName());
                requireActivity().sendBroadcast(resetIntent);
            }
        }

        ((MainActivity)getActivity()).checkIdForBtn();
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter("com.example.UPDATE_TIME");
        filter.addAction("com.example.RESET_TIME");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireActivity().registerReceiver(
                    timeReceiver,
                    filter,
                    Context.RECEIVER_NOT_EXPORTED
            );
        }
        else {
            requireActivity().registerReceiver(timeReceiver, filter);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        requireActivity().unregisterReceiver(timeReceiver);
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight &&
                    (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        restoreTimerState();
    }

    private void restoreTimerState() {
        if (sPref.getString(APP_PREFERENCES_IS_RENT, "NULL").equals("true")
                && sPref.getString(APP_PREFERENCES_ID_RENT_CAR, "NULL").equals(auto_id)
                && sPref.getString(APP_PREFERENCES_CARISOPEN, "NULL").equals("true")) {

            SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            elapsedTime = prefs.getLong(KEY_ELAPSED_TIME, 0);

            if (elapsedTime > 0) {
                Intent intent = new Intent(getActivity(), StopwatchService.class);
                intent.setAction("START_TIMER");
                requireActivity().startService(intent);
            }

            btnRent.setText("Завершить поездку: " + StopwatchService.formatTime(elapsedTime, false));
        }
    }
}