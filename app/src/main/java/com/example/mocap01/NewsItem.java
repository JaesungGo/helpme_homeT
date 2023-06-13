package com.example.mocap01;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
public class NewsItem {
    //보통은 객체지향은 정보의 은닉을 해야하기 때문에, private으로 만든다.
    String title;
    String link;
    String desc;
    String imgUrl;
    String date;

    public NewsItem() { //혹시 몰라서 매개변수가 없는 것도 만들었다.
    }

    public NewsItem (String title, String link, String desc, String imgUrl, String date) {
        this.title = title;
        this.link = link;
        this.desc = desc;
        this.imgUrl = imgUrl;
        this.date = date;
    }

    //Getter & Setter Method..
    //멤버 변수가 private이면 다른 곳에서 접근하기 위해 Getter & Setter Method 필요하다.
    // 자동완성 Alt+Insert 에 Getter & Setter 있다.

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public static List<NewsItem> parseXml(InputStream inputStream) throws XmlPullParserException, IOException {
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
                    if (tagName.equalsIgnoreCase("item")) {
                        currentNewsItem = new NewsItem();
                    } else if (currentNewsItem != null) {
                        if (tagName.equalsIgnoreCase("title")) {
                            currentNewsItem.setTitle(parser.nextText());
                        } else if (tagName.equalsIgnoreCase("link")) {
                            currentNewsItem.setLink(parser.nextText());
                        } else if (tagName.equalsIgnoreCase("description")) {
                            currentNewsItem.setDesc(parser.nextText());
                        } else if (tagName.equalsIgnoreCase("imgUrl")) {
                            currentNewsItem.setImgUrl(parser.nextText());
                        } else if (tagName.equalsIgnoreCase("date")) {
                            currentNewsItem.setDate(parser.nextText());
                        }
                    }
                    break;

                case XmlPullParser.END_TAG:
                    tagName = parser.getName();
                    if (tagName.equalsIgnoreCase("item") && currentNewsItem != null) {
                        newsItemList.add(currentNewsItem);
                        currentNewsItem = null;
                    }
                    break;
            }
            eventType = parser.next();
        }

        return newsItemList;
    }
}


