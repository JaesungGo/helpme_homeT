package com.example.mocap01;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
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
    TextView dataTextView;

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
        //listView2 = (ListView) findViewById(R.id.listView2);
        dataTextView = findViewById(R.id.dataTextView);

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<String>());
        listView2.setAdapter(adapter);
    }
}



//날짜변환
//        DateFormat formatter = new SimpleDateFormat("yyyy년MM월dd일");
//        Date date = new Date(calendarView.getDate());
//        today.setText(formatter.format(date));
//
//        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
//
//            @Override
//            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
//                String day;
//                day = year + "년" + (month+1) + "월" + dayOfMonth + "일";
//                today.setText(day);
//            }
//        });
//
//        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
//            @Override
//            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
//                String day = year + "년" + (month + 1) + "월" + dayOfMonth + "일";
//                today.setText(day);
//
//                String currentDate = year + "-" + (month + 1) + "-" + dayOfMonth;
//                String databasePath = "UserAccount/" + uid + "/Check2/Squat/" + currentDate;
//
//                DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference(databasePath);
//                databaseRef.addValueEventListener(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                        if (dataSnapshot.exists()) {
//                            String data = dataSnapshot.getValue(String.class);
//                            dataTextView.setText(data);
//                        } else {
//                            dataTextView.setText("No data found for this date.");
//                        }
//                    }
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError databaseError) {
//                        dataTextView.setText("Failed to fetch data from Firebase.");
//                    }
//                });
//            }
//        });
//    }
//}
//
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
//            @Override
//            public void onCancelled(DatabaseError databaseError) {
//
//            }
//        });

