package com.example.mocap01;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class ResultRecord extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_record); // YourOtherActivity의 레이아웃 파일을 설정

        // wrongList 데이터를 받아옴
        Intent intent = getIntent();
        ArrayList<ParcelablePair<Integer, Object>> receivedWrongList = intent.getParcelableArrayListExtra("wrongList");

        // ListView 초기화
        ListView listView = findViewById(R.id.result_listview); // ListView의 ID에 맞게 변경

        // 데이터를 순차적으로 표시하기 위해 사용할 어댑터 생성
        ArrayAdapter<ParcelablePair<Integer, Object>> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, receivedWrongList);

        // ListView에 어댑터 설정
        listView.setAdapter(adapter);
    }

//    private List<String> formatData(List<ParcelablePair<Integer, Object>> dataList) {
//        List<String> formattedData = new ArrayList<>();
//        for (ParcelablePair<Integer, Object> pair : dataList) {
//            // 원하는 형식으로 데이터 포맷팅
//            String formattedItem = "First: " + pair.getFirst() + ", Second: " + pair.getSecond();
//            formattedData.add(formattedItem);
//        }
//        return formattedData;
//    }
}
