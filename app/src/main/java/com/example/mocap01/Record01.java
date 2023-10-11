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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


public class Record01 extends Fragment {

    private CalendarView calendarView;
    private TextView today;
    private TextView dataTextView;
    private FirebaseAuth mFirebaseAuth = FirebaseAuth.getInstance();     //파이어 베이스 인증
    private FirebaseUser currentUser = mFirebaseAuth.getCurrentUser();
    private FirebaseDatabase mFirebaseData = FirebaseDatabase.getInstance();
    private DatabaseReference mDatabaseRef = mFirebaseData.getReference(); //실시간 데이터 베이스

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.record, container, false);

        today = rootView.findViewById(R.id.today);
        calendarView = rootView.findViewById(R.id.calendarView);
        dataTextView = rootView.findViewById(R.id.dataTextView);

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

                if (currentUser != null) {
                    String userId = currentUser.getUid();
                    //String selectedDate = year + "-0" + (month + 1) + "-0" + dayOfMonth;
                    String selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, (month + 1), dayOfMonth);


                    DatabaseReference database = FirebaseDatabase.getInstance().getReference();
                    DatabaseReference exerciseRef = database.child("Users").child(userId).child("Check2").child(selectedDate);

                    exerciseRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                int pullupSum = 0;
                                int pushupSum = 0;
                                int situpSum = 0;
                                int squatSum = 0;

                                for (DataSnapshot exerciseSnapshot : snapshot.getChildren()) {
                                    String exerciseName = exerciseSnapshot.getKey();
                                    Object value = exerciseSnapshot.getValue();
                                    if (value != null) {
                                        if (value instanceof HashMap) {
                                            HashMap<String, Object> exerciseDataMap = (HashMap<String, Object>) value;

                                            for (Map.Entry<String, Object> entry : exerciseDataMap.entrySet()) {
                                                String exerciseDataString = entry.getValue().toString();

                                                // Remove commas from the exerciseDataString
                                                exerciseDataString = exerciseDataString.replaceAll("[,\\s]", ""); // 쉼표와 공백 제거

                                                int startIndex = exerciseDataString.indexOf("=") + 1;

                                                if (startIndex >= 0) {
                                                    String exerciseDataNumber = exerciseDataString.substring(startIndex).trim();
                                                    int exerciseData = Integer.parseInt(exerciseDataNumber);

                                                    switch (exerciseName) {
                                                        case "Pullup":
                                                            pullupSum += exerciseData;
                                                            break;
                                                        case "Pushup":
                                                            pushupSum += exerciseData;
                                                            break;
                                                        case "Situp":
                                                            situpSum += exerciseData;
                                                            break;
                                                        case "Squat":
                                                            squatSum += exerciseData;
                                                            break;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                String result = "Pullup 합 : " + pullupSum + "\n"
                                        + "Pushup 합 : " + pushupSum + "\n"
                                        + "Situp 합 : " + situpSum + "\n"
                                        + "Squat 합 : " + squatSum;

                                dataTextView.setText(result);
                            } else {
                                dataTextView.setText("No data found for this date.");
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            dataTextView.setText("Failed to fetch data from Firebase.");
                        }
                    });

                }

            }
        });
        return rootView;
    }
}
