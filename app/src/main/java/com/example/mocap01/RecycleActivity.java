package com.example.mocap01;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class RecycleActivity extends AppCompatActivity implements MyAdapter02.OnItemClickListener {

    private RecyclerView recyclerView;
    private MyAdapter02 adapter;
    private List<NewsItem> newsItemList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycle);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        newsItemList = new ArrayList<>();
        adapter = new MyAdapter02(newsItemList, this);
        adapter.setOnItemClickListener(this);
        recyclerView.setAdapter(adapter);

        // 뉴스 데이터 로드
        loadNewsData();

        Button logoutButton = findViewById(R.id.logout_button);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // 현재 액티비티 종료
            }
        });
    }

    private void loadNewsData() {
        // 뉴스 정보를 가져오는 AsyncTask 실행
        new FetchNewsTask().execute();
    }

    private class FetchNewsTask extends AsyncTask<Void, Void, List<NewsItem>> {

        @Override
        protected List<NewsItem> doInBackground(Void... voids) {
            List<NewsItem> newsItemList = new ArrayList<>();

            try {
                URL url = new URL("https://fs.jtbc.co.kr/RSS/sports.xml");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = connection.getInputStream();

                newsItemList = parseXml(inputStream);

                inputStream.close();
                connection.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(RecycleActivity.this, "네트워크 연결 오류", Toast.LENGTH_SHORT).show();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
                Toast.makeText(RecycleActivity.this, "XML 파싱 오류", Toast.LENGTH_SHORT).show();
            }

            return newsItemList;
        }

        @Override
        protected void onPostExecute(List<NewsItem> newsItemList) {
            super.onPostExecute(newsItemList);

            if (newsItemList != null && !newsItemList.isEmpty()) {
                // 뉴스 아이템 리스트를 업데이트합니다.
                updateNewsList(newsItemList);
            } else {
                Toast.makeText(RecycleActivity.this, "뉴스 정보를 가져오지 못했습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateNewsList(List<NewsItem> newsItemList) {
        this.newsItemList.clear();
        this.newsItemList.addAll(newsItemList);
        adapter.notifyDataSetChanged();
    }

    private List<NewsItem> parseXml(InputStream inputStream) throws XmlPullParserException, IOException {
        List<NewsItem> newsItemList = new ArrayList<>();

        XmlPullParserFactory xmlFactoryObject = XmlPullParserFactory.newInstance();
        XmlPullParser parser = xmlFactoryObject.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        parser.setInput(inputStream, null);

        int eventType = parser.getEventType();
        NewsItem currentNewsItem = null;

        while (eventType != XmlPullParser.END_DOCUMENT) {
            String tagName;
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    tagName = parser.getName();
                    if (tagName.equals("item")) {
                        currentNewsItem = new NewsItem();
                    } else if (currentNewsItem != null) {
                        if (tagName.equals("title")) {
                            currentNewsItem.setTitle(parser.nextText());
                        } else if (tagName.equals("description")) {
                            currentNewsItem.setDesc(parser.nextText());
                        } else if (tagName.equals("link")) {
                            currentNewsItem.setLink(parser.nextText());
                        } else if (tagName.equals("pubDate")) {
                            currentNewsItem.setDate(parser.nextText());
                        } else if (tagName.equals("enclosure")) {
                            String imageUrl = parser.getAttributeValue(null, "url");
                            currentNewsItem.setImgUrl(imageUrl);
                        }
                    }
                    break;

                case XmlPullParser.END_TAG:
                    tagName = parser.getName();
                    if (tagName.equals("item") && currentNewsItem != null) {
                        newsItemList.add(currentNewsItem);
                        currentNewsItem = null;
                    }
                    break;
            }

            eventType = parser.next();
        }

        return newsItemList;
    }

    @Override
    public void onItemClick(NewsItem item) {
        String newsUrl = item.getLink();

        // WebViewActivity로 이동하는 인텐트 생성
        Intent intent = new Intent(RecycleActivity.this, WebViewActivity.class);
        intent.putExtra("newsUrl", newsUrl);
        Log.d("RecycleActivity", "newsUrl: "+ newsUrl);
        startActivity(intent);
    }
}
