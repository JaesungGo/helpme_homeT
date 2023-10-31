package com.example.mocap01;

import static com.google.android.material.color.utilities.MaterialDynamicColors.error;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.lzyzsd.circleprogress.CircleProgress;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Record01 extends Fragment {

    private CalendarView calendarView;
    private TextView today;
    private TextView dataTextView;
    private TextView avgTextView;
    private FirebaseAuth mFirebaseAuth = FirebaseAuth.getInstance();
    private FirebaseUser currentUser = mFirebaseAuth.getCurrentUser();
    private FirebaseDatabase mFirebaseData = FirebaseDatabase.getInstance();
    private DatabaseReference mDatabaseRef = mFirebaseData.getReference();
    private int savedCsquatSum = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.record, container, false);

        today = rootView.findViewById(R.id.today);
        calendarView = rootView.findViewById(R.id.calendarView);
        dataTextView = rootView.findViewById(R.id.dataTextView);
        avgTextView = rootView.findViewById(R.id.squatAverageTextview);
        CircleProgress circleProgressView = rootView.findViewById(R.id.circleProgress);

        // 날짜 변환
        DateFormat formatter = new SimpleDateFormat("yyyy년MM월dd일");
        Date date = new Date(calendarView.getDate());
        today.setText(formatter.format(date));

        // 캘린더뷰 클릭 이벤트 처리
        AtomicInteger avgCSquat = new AtomicInteger(0); // AtomicInteger를 사용하여 변수 초기화

        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                String day = year + "년" + (month + 1) + "월" + dayOfMonth + "일";
                today.setText(day);

                if (currentUser != null) {
                    String userId = currentUser.getUid();
                    String selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, (month + 1), dayOfMonth);
                    final AtomicInteger csquatSum = new AtomicInteger(0);
                    DatabaseReference exerciseRef = mDatabaseRef.child("Users").child(userId).child("Check2").child(selectedDate);

                    DatabaseReference userRef = mDatabaseRef.child("Users").child(userId).child("Info");

                    exerciseRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                int pullupSum = 0;
                                int pushupSum = 0;
                                int situpSum = 0;
                                int crsquatSum=csquatSum.get();
                                int wsquatSum = 0;


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
                                                        case "CSquat":
                                                            crsquatSum += exerciseData;
                                                            break;
                                                        case "WSquat":
                                                            wsquatSum += exerciseData;
                                                            break;


                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                String result =
                                        "정확한 스쿼트 갯수" + "\n"
                                                + "--> " + crsquatSum + "개" + "\n\n"
                                                + "잘못된 스쿼트 갯수" + "\n"
                                                + "--> " + wsquatSum + "개";
                                int totalSum = crsquatSum + wsquatSum;
                                int csquatPercentage = (int) ((float) crsquatSum / totalSum * 100);

                                exerciseRef.child("csquatsum").setValue(crsquatSum);
                                dataTextView.setText(result);
                                circleProgressView.setProgress(csquatPercentage);

                                userRef.child("goalsquat").addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot goalSnapshot) {
                                        int ctsquat = csquatSum.get();
                                        if (goalSnapshot.exists()) {
                                            int goalsquat = goalSnapshot.getValue(Integer.class);

                                            // Compare goalsquat with csquatSum and display the result in avgTextView
                                            String avgText = "스쿼트 목표: " + goalsquat + "개\n";
                                            if (ctsquat >= goalsquat) {
                                                avgText += "목표 달성: 성공";
                                            } else {
                                                avgText += "목표 달성: 실패";
                                            }
                                            avgTextView.setText(avgText);
                                        } else {
                                            avgTextView.setText("목표 스쿼트 갯수를 설정하세요.");
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        avgTextView.setText("목표 스쿼트를 불러오지 못했습니다.");
                                    }
                                });

                            } else {
                                dataTextView.setText("운동기록이 없습니다");
                                circleProgressView.setProgress(0);

                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            dataTextView.setText("운동기록을 불러오지 못했습니다");
                            avgTextView.setText("목표 스쿼트를 불러오지 못했습니다.");
                        }
                    });

                }

            }
        });
        return rootView;
    }
}