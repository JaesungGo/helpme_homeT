package com.example.mocap01;

import android.os.AsyncTask;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class NewsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MyAdapter02 newsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycle);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        NewsAsyncTask newsAsyncTask = new NewsAsyncTask();
        newsAsyncTask.execute();
    }

    private class NewsAsyncTask extends AsyncTask<Void, Void, List<NewsItem>> {

        @Override
        protected List<NewsItem> doInBackground(Void... voids) {
            List<NewsItem> newsItemList = null;
            try {
                URL url = new URL("http://www.mbn.co.kr/rss/health/");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = connection.getInputStream();
                    newsItemList = NewsItem.parseXml(inputStream);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return newsItemList;
        }

        @Override
        protected void onPostExecute(List<NewsItem> newsItemList) {
            if (newsItemList != null) {
                newsAdapter = new MyAdapter02(newsItemList, NewsActivity.this, new MyAdapter02.OnItemClickListener() {
                    @Override
                    public void onItemClick(NewsItem item) {
                        // 아이템 클릭 이벤트 처리
                    }
                });
                recyclerView.setAdapter(newsAdapter);
            }
        }
    }
}






