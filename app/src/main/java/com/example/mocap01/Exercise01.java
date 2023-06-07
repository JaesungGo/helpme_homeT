package com.example.mocap01;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;


public class Exercise01 extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.exercise, container, false);

        // Find the squat button in exercise.xml
        ImageButton squatButton = view.findViewById(R.id.squat);
        ImageButton pullupButton = view.findViewById(R.id.pullup);
        ImageButton pushupButton = view.findViewById(R.id.pushup);
        ImageButton situpButton = view.findViewById(R.id.situp);

        // Set a click listener for the squat button
        squatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), SquatActivity.class);
                startActivity(intent);
            }
        });
        pullupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), PullUpActivity.class);
                startActivity(intent);
            }
        });
        pushupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), PushUpActivity.class);
                startActivity(intent);
            }
        });
        situpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), SitUpActivity.class);
                startActivity(intent);
            }
        });

        return view;
    }
}

