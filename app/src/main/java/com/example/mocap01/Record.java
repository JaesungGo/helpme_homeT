package com.example.mocap01;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.CalendarView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class Record extends AppCompatActivity {

    CalendarView calendarView;
    TextView today;
    private FirebaseAuth mFirebaseAuth;     //파이어 베이스 인증
    private FirebaseDatabase mFirebaseData = FirebaseDatabase.getInstance();
    private DatabaseReference mDatabaseRef = mFirebaseData.getReference(); //실시간 데이터 베이스
    private ListView listView2;
    private ArrayAdapter<String> adapter;
    List<Object> Array = new ArrayList<Object>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.record);

        today = findViewById(R.id.today);
        calendarView = findViewById(R.id.calendarView);
//        listView2 = findViewById(R.id.listView2);

        mFirebaseAuth = FirebaseAuth.getInstance(); // 파이어베이스 인증 객체 초기화

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        listView2.setAdapter(adapter);

        @SuppressLint("SimpleDateFormat") DateFormat formatter = new SimpleDateFormat("yyyy년MM월dd일");
        Date date = new Date(calendarView.getDate());
        today.setText(formatter.format(date));

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            String day = year + "년" + (month + 1) + "월" + dayOfMonth + "일";
            today.setText(day);
            Log.d("Record", "Today: " + year);
        });

//        mDatabaseRef.child(mFirebaseAuth.getUid()).child("Check2").child("Squat").addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot dataSnapshot) {
//                adapter.clear();
//                for (DataSnapshot messageData : dataSnapshot.getChildren()) {
//                    String msg2 = messageData.getValue().toString();
//                    Array.add(msg2);
//                    adapter.add(msg2);
//                    // child 내에 있는 데이터만큼 반복합니다.
//                }
//                adapter.notifyDataSetChanged();
//                listView2.setSelection(adapter.getCount() - 1);
//            }
//
//            @Override
//            public void onCancelled(DatabaseError databaseError) {
//
//            }
//        });
    }
}