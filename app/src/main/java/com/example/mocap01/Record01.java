package com.example.mocap01;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class Record01 extends Fragment {

    private CalendarView calendarView;
    private TextView today;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.record, container, false);

        today = rootView.findViewById(R.id.today);
        calendarView = rootView.findViewById(R.id.calendarView);

        // 날짜 변환
        DateFormat formatter = new SimpleDateFormat("yyyy년MM월dd일");
        Date date = new Date(calendarView.getDate());
        today.setText(formatter.format(date));

        // 캘린더뷰 클릭 이벤트 처리
        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                String day = year + "년" + (month + 1) + "월" + dayOfMonth + "일";
                today.setText(day);
            }
        });

        return rootView;
    }
}