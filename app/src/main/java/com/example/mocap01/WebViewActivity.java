package com.example.mocap01;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class WebViewActivity extends AppCompatActivity {

    private WebView webView;
    private String newsUrl;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news);

        // 인텐트에서 전달받은 링크 가져오기
        newsUrl = getIntent().getStringExtra("newsUrl");

        // 웹뷰 설정
        webView = findViewById(R.id.webView);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true); // 자바스크립트 사용 허용

        // WebViewClient 설정 (웹 페이지 이벤트 처리)
        webView.setWebViewClient(new WebViewClient());

        // WebChromeClient 설정 (프로그레스 바 표시 등)
        webView.setWebChromeClient(new WebChromeClient());

        // 토스트로 newsUrl 표시
        Log.d("WebViewActivity", "newsUrl: " + newsUrl);

        // 웹뷰에 해당 기사 로드
        webView.loadUrl(newsUrl);
    }

    // 뒤로 가기 버튼을 눌렀을 때 웹뷰에서 이전 페이지로 이동
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}

