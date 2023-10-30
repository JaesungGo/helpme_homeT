package com.example.mocap01;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ResultRecord extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_record);

        // wrongList 데이터를 받아옴
        Intent intent = getIntent();
        ArrayList<ParcelablePair<Integer, Object>> receivedWrongList = intent.getParcelableArrayListExtra("wrongList");

        // ListView 초기화
        ListView listView = findViewById(R.id.result_listview);

        // 데이터를 순차적으로 표시하기 위해 사용할 어댑터 생성
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, formatData(receivedWrongList));

        // ListView에 어댑터 설정
        listView.setAdapter(adapter);

        // 버튼 초기화 및 클릭 이벤트 설정
        Button saveButton = findViewById(R.id.result_back);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveDataToFirebase(formatData(receivedWrongList));
            }
        });
    }

    private List<String> formatData(List<ParcelablePair<Integer, Object>> dataList) {
        List<String> formattedData = new ArrayList<>();
        for (ParcelablePair<Integer, Object> pair : dataList) {
            String message1;
            switch (pair.getFirst()) {
                case 1:
                    message1 = " - 올바른 자세입니다. ";
                    break;
                case 2:
                    message1 = " - 엉덩이가 너무 내려갔어요. ";
                    break;
                case 3:
                    message1 = " - 무릎 사이가 너무 좁아요. ";
                    break;
                default:
                    message1 = " ";
            }
            String formattedItem = pair.getSecond().toString() + message1;
            formattedData.add(formattedItem);
        }
        return formattedData;
    }

    private void saveDataToFirebase(List<String> formattedData) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference userRef = database.getReference("Users");
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());
        String today2 = new SimpleDateFormat("HH:mm", Locale.KOREA).format(new Date());
        DatabaseReference squatsRef3 = userRef.child("Check2").child(today).child("Recode").child(today2);

        squatsRef3.setValue(formattedData).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(ResultRecord.this, "데이터 저장에 성공하였습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ResultRecord.this, "데이터 저장에 실패하였습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
