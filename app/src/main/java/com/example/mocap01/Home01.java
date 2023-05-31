package com.example.mocap01;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.mocap01.NewsActivity;
import com.example.mocap01.MyAdapter02;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import me.relex.circleindicator.CircleIndicator3;


public class Home01 extends Fragment {


    private ViewPager2 mPager;
    private FragmentStateAdapter pagerAdapter;
    private CircleIndicator3 mIndicator;
    private RecyclerView recyclerView;
    private MyAdapter02 adapter;
    private ArrayList<NewsItem> items = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.home, container, false);

        // ViewPager2 초기화 및 어댑터 설정
        mPager = rootView.findViewById(R.id.viewpager);
        pagerAdapter = new MyAdapter(getActivity(), 4);
        mPager.setAdapter(pagerAdapter);

        // Indicator 설정
        mIndicator = rootView.findViewById(R.id.indicator);
        mIndicator.setViewPager(mPager);
        mIndicator.createIndicators(4, 0);

        recyclerView = rootView.findViewById(R.id.recycler);
        adapter = new MyAdapter02(items, getContext());
        recyclerView.setAdapter(adapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);

        readRss();

        return rootView;
    }
    void readRss() {
        try {
            URL url = new URL("http://rss.hankyung.com/new/news_main.xml");
            NewsActivity.RssFeedTask task = new NewsActivity.RssFeedTask();
            task.execute(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    }



