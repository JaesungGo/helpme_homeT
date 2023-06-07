package com.example.mocap01;


import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class NewsActivity extends AppCompatActivity {

    RecyclerView recyclerView;

    static ArrayList<NewsItem> items= new ArrayList<>();

    static MyAdapter02 adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);

        recyclerView=findViewById(R.id.recycler);
        adapter= new MyAdapter02(items,this);
        recyclerView.setAdapter(adapter);

        //리사이클러의 배치관리자 설정
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);

        //대량의 데이터 추가 작업
        readRss();
    }//onCreate Method ..

    //rss xml문서 읽어와서 파싱하는 작업 메소드
    void readRss(){

        try {
            URL url=new URL("http://rss.hankyung.com/new/news_main.xml");
//실디바이스 중에서 oreo버전 이상에서는 보안강화로 인해 https만 허용하도록 함..

            //스트림역할하여 데이터 읽어오기 : 인터넷 작업은 반드시 퍼미션 작성해야함.
            //Network작업은 반드시 별도의 Thread만 할 수 있다.
            //별도의 Thread 객체 생성
            RssFeedTask task= new RssFeedTask();
            task.execute(url); //doInBackground()메소드가 발동[thread의 start()와 같은 역할]
        } catch (MalformedURLException e) { e.printStackTrace();}

    }// readRss Method ..

    //이너 클래스
    static class RssFeedTask extends AsyncTask<URL, Void, String>{

        //Thread의 run()메소드와 같은 역할
        @Override
        protected String doInBackground(URL... urls) { //...는 여러개를 받는 의미, 만약 task.execute(url, url2, url3); 보내면 urls[3]로 받는다.
            //전달받은 URL 객체
            URL url= urls[0];

            //해임달(URL)에게 무지개로드(Stream) 열도록..
            try {
                InputStream is= url.openStream();

                //읽어온 xml를 파싱(분석)해주는 객체 생성
                XmlPullParserFactory factory= XmlPullParserFactory.newInstance();
                XmlPullParser xpp= factory.newPullParser();

                //utf-8은 한글도 읽어오기 위한 인코딩 방식
                xpp.setInput(is, "utf-8");
                int eventType= xpp.getEventType();

                NewsItem item= null;
                String tagName= null;

                while (eventType != XmlPullParser.END_DOCUMENT){
                    switch (eventType){
                        case XmlPullParser.START_DOCUMENT:
                            break;
                        case XmlPullParser.START_TAG:
                            tagName=xpp.getName();
                            Log.i("TAG", tagName);

                            if(tagName.equals("item")){
                                item= new NewsItem();
                            }else if(tagName.equals("title")){
                                xpp.next();
                                if(item!=null) item.setTitle(xpp.getText());
                            }else if(tagName.equals("link")){
                                xpp.next();
                                if(item!=null) item.setLink(xpp.getText());
                            }else if(tagName.equals("description")){
                                xpp.next();
                                if(item!=null) item.setDesc(xpp.getText());
                            }else if(tagName.equals("image")){
                                xpp.next();
                                if(item!=null) item.setImgUrl(xpp.getText());
                            }else if(tagName.equals("pubDate")){
                                xpp.next();
                                if(item!=null) item.setDate(xpp.getText());
                            }
                            break;
                        case XmlPullParser.TEXT:
                            break;
                        case XmlPullParser.END_TAG:
                            tagName=xpp.getName();
                            if(tagName.equals("item")){

                                Log.i("SSS", item.getTitle());
                                //읽어온 기사 한개를 대량의 데이터에 추가
                                items.add(item);
                                item=null;

                                //리사이클러의 아답터에게 데이터가
                                //변경되었다는 것을 통지(화면 갱신)
                                //UI변경작업을 하고 싶다면..
                                publishProgress();//onProgressUpdate()라는 메소드 실행

                            }
                            break;
                    }
                    eventType= xpp.next();
                }//while

                //파싱 작업이 완료되었다!!
                //테스트로 Toastㄹ 보여주기, 단 별도 스레드는
                //UI작업이 불가! 그래서 runOnUiThread()를 이용했었음.
                //이 UI작업을 하는 별도의 메소드로
                //결과를 리턴하는 방식을 사용

            } catch (IOException e) {e.printStackTrace();} catch (XmlPullParserException e) {e.printStackTrace();}

            return "파싱종료"; // 리턴 값은 onPostExecute(String s) s에 전달된다.
        }//doInBackground()

        //doInBackground() 작업 도중에
        //publichProgress()라는 메소드를
        //호출하면 자동으로 실행되는 메소드
        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            //이곳에서도 UI변경작업이 가능함.
            adapter.notifyItemInserted(items.size());//새로 추가한 것은 마지막에 추가하는 내용
            //전체[notifyDataSetChanged()]를 바꾸면 속도가 좋지않다. 추가한 것만 바꾸자.

        }

        //doInBackground메소드가 종료된 후
        //UI작업을 위해 자동 실행되는 메소드
        //runOnUiThread()와 비슷한 역할
        //@Override
        //protected void onPostExecute(String s) { //매개 변수 s에 들어오는 값음 doIBackground()의 return 값이다.
            //super.onPostExecute(s);

            //리사이클러에서 보여주는 데이터를 가진
            //아답터에게 데이터가 변경되었다고 공지
            //adapter.notifyDataSetChanged();


            //이 메소드 안에서는 UI변경 작업 가능
            //Toast.makeText(NewsActivity.this, s+":"+items.size(), Toast.LENGTH_SHORT).show();
        //}
    }//RssFeedTask class
}//MainActivity class ..
