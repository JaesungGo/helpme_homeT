package com.example.mocap01;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.squareup.picasso.Picasso;

public class PostDetailActivity extends AppCompatActivity {

    private TextView textViewTitle;
    private TextView textViewContent;
    private ImageView imageView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        textViewTitle = findViewById(R.id.textViewTitle);
        textViewContent = findViewById(R.id.textViewContent);
        imageView = findViewById(R.id.imageView);

        // 인텐트에서 게시물 데이터 가져오기
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String title = extras.getString("title");
            String content = extras.getString("content");
            String imageUrl = extras.getString("imageUrl");

            textViewTitle.setText(title);
            textViewContent.setText(content);

            // Picasso 또는 다른 이미지 로딩 라이브러리를 사용하여 이미지 로드 및 표시
            Picasso.get().load(imageUrl).into(imageView);
        }
    }
}




