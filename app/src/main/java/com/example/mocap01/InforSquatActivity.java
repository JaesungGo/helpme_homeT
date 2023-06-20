package com.example.mocap01;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import me.relex.circleindicator.CircleIndicator3;

public class InforSquatActivity extends AppCompatActivity {

    private Button ContBtn , CancelBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_infor_squat);

        ViewPager2 viewPager = findViewById(R.id.viewpager);
        FragmentStateAdapter pagerAdapter = new Adapter_Squat(this, 3);
        viewPager.setAdapter(pagerAdapter);

        CircleIndicator3 indicator = findViewById(R.id.indicator);
        indicator.setViewPager(viewPager);
        indicator.createIndicators(3, 0);

        ContBtn = findViewById(R.id.btn_continue);
        CancelBtn = findViewById(R.id.btn_cancel);

        CancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(InforSquatActivity.this, Exercise01.class);
                startActivity(intent);
            }
        });

        ContBtn.setEnabled(false); // 초기에 비활성화 상태로 설정

        // ViewPager2의 페이지 변경 이벤트 감지
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateContinueButtonState(position);
            }
        });

        ContBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(InforSquatActivity.this, SquatActivity.class);
                startActivity(intent);
            }
        });


    }


    private void updateContinueButtonState(int position) {
        if (position >= 2) {
            ContBtn.setEnabled(true);
            ContBtn.setBackgroundColor(getResources().getColor(R.color.button_enabled_color));

        } else {
            ContBtn.setEnabled(false);
            ContBtn.setBackgroundColor(getResources().getColor(R.color.button_disabled_color));

        }
    }

}