package com.example.myapplication.registration;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;
import com.example.myapplication.database.DataBase;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;

public class FragmentCertificate extends Fragment {
    public static final String APP_PREFERENCES = "myProfile";
    public static final String DATA_SAVE = "DATA_SAVE";
    public static final String APP_PREFERENCES_LOGIN= "Login";
    public static final String APP_PREFERENCES_MAIL = "Mail";
    public static final String APP_PREFERENCES_PHONE = "Phone";
    public static final String APP_PREFERENCES_PASSWORD = "Password";
    DataBase db;
    SharedPreferences sPref;
    Button btnNext, btnDateOfBirthSpinner, btnDateOfIssueSpinner, btnDateOfExpSpinner;
    EditText editPlaceOfBirth, editIssuedBy, editCode, editResidence, editCategory;
    TextInputLayout placeOfBirthError, issuedByError, codeError, residenceError, categoryError;

    int btn = 0;

    private DatePickerDialog datePickerDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_certificate, container, false);

        Activity activity = getActivity();
        db = new DataBase(activity);

        sPref = activity.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);

        btnNext = view.findViewById(R.id.btnNext);
        btnDateOfBirthSpinner = view.findViewById(R.id.btnDateOfBirthSpinner);
        btnDateOfIssueSpinner = view.findViewById(R.id.btnDateOfIssueSpinner);
        btnDateOfExpSpinner = view.findViewById(R.id.btnDateOfExpSpinner);

        editPlaceOfBirth = view.findViewById(R.id.editPlaceOfBirth);
        editIssuedBy = view.findViewById(R.id.editIssuedBy);
        editCode = view.findViewById(R.id.editCode);
        editResidence = view.findViewById(R.id.editResidence);
        editCategory = view.findViewById(R.id.editCategory);

        codeError = view.findViewById(R.id.codeError);
        categoryError = view.findViewById(R.id.categoryError);

        EditText[] edList = {editPlaceOfBirth, editIssuedBy, editCode, editResidence, editCategory};

        CustomTextWatcher2 textWatcher = new CustomTextWatcher2(edList, btnNext);
        for (EditText editText : edList) editText.addTextChangedListener(textWatcher);

        btnDateOfBirthSpinner.setText(getTodayDate());
        btnDateOfIssueSpinner.setText(getTodayDate());
        btnDateOfExpSpinner.setText(getTodayDate());

        initDatePicker();

        btnDateOfBirthSpinner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDatePicker();
                btn = 1;
            }
        });

        btnDateOfIssueSpinner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDatePicker();
                btn = 2;
            }
        });

        btnDateOfExpSpinner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDatePicker();
                btn = 3;
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editCode.length() != 10) {
                    codeError.setError("Длина кода должна быть ровно 10!");
                }
                else if (!editCategory.getText().toString().equals("A") &&
                        !editCategory.getText().toString().equals("B") &&
                        !editCategory.getText().toString().equals("C")) {
                    codeError.setError(null);
                    categoryError.setError("Доступные категории: A, B, C");
                }
                else {
                    //проверка на наличие удостоверения в бд

                    categoryError.setError(null);
                    closeKeyboard();
                    Toast.makeText(activity, "Удостоверение загружено!",Toast.LENGTH_LONG).show();

                    db.addUser(sPref.getString(APP_PREFERENCES_LOGIN, "NULL"),
                            sPref.getString(APP_PREFERENCES_MAIL, "NULL"),
                            sPref.getString(APP_PREFERENCES_PHONE, "NULL"),
                            sPref.getString(APP_PREFERENCES_PASSWORD, "NULL"));
                    SaveData();

                    activity.startActivityForResult(new Intent(activity, MainActivity.class), 1);
                    activity.finish();
                }
            }
        });



        return view;
    }

    private String getTodayDate() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        return makeDateString(day, month, year);
    }

    private void initDatePicker()
    {
        DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                String date = makeDateString(dayOfMonth, month, year);

                switch (btn)
                {
                    case 1 : btnDateOfBirthSpinner.setText(date);break;
                    case 2 : btnDateOfIssueSpinner.setText(date);break;
                    case 3 : btnDateOfExpSpinner.setText(date);break;
                }
            }
        };

        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        int style = com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialCalendar;

        datePickerDialog = new DatePickerDialog(requireContext(), style, dateSetListener, year, month, day);
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
    }

    private String makeDateString(int day, int month, int year)
    {
        return getMonthFormat(month) + " " + day + " " + year;
    }

    private String getMonthFormat(int month)
    {
        String[] months = new String[] {"Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"};
        return months[month];
    }

    public void openDatePicker()
    {
        datePickerDialog.show();
    }

    private void SaveData(){
        Activity activity = getActivity();

        sPref = activity.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sPref.edit();

        ed.putBoolean(DATA_SAVE, true);
        ed.commit();
    }

    private void closeKeyboard(){
        View view = this.getActivity().getCurrentFocus();
        if(view !=null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}

class CustomTextWatcher2 implements TextWatcher {
    View v;
    EditText[] edList;
    public CustomTextWatcher2(EditText[] edList, Button v) {
        this.v = v;
        this.edList = edList;
    }
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        for (EditText editText : edList) {
            if (editText.getText().toString().trim().length() <= 0) {
                v.setEnabled(false);
                v.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF9C8F4E")));
                break;
            } else {
                v.setEnabled(true);
                v.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFEFCD25")));
            }
        }
    }
}