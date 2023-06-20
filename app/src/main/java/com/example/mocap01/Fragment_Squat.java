package com.example.mocap01;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

public class Fragment_Squat extends Fragment {

    public static class Fragment_s1 extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            ViewGroup rootView = (ViewGroup) inflater.inflate(
                    R.layout.frame_squat, container, false);

            TextView textView = rootView.findViewById(R.id.textView1);
            textView.setText("Fragment 1");

            return rootView;
        }

    }

    public static class Fragment_s2 extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            ViewGroup rootView = (ViewGroup) inflater.inflate(
                    R.layout.frame_squat, container, false);

            TextView textView = rootView.findViewById(R.id.textView2);
            textView.setText("Fragment 2");

            return rootView;
        }

    }

    public static class Fragment_s3 extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            ViewGroup rootView = (ViewGroup) inflater.inflate(
                    R.layout.frame_squat, container, false);

            TextView textView = rootView.findViewById(R.id.textView3);
            textView.setText("Fragment 3");


            return rootView;
        }

    }

}