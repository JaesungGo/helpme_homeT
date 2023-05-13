package com.example.mocap01;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class SquatActivity extends AppCompatActivity {
    private static int TIME_OUT = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_squat);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent squat_intent = new Intent(SquatActivity.this, DetectSquat.class);
                startActivity(squat_intent);
                finish();

            }
        },TIME_OUT);
    }
}