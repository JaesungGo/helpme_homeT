package com.example.mocap01;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import me.relex.circleindicator.CircleIndicator3;

public class Home01 extends Fragment {

    private TextView nameTextView;
    private ImageView photoImageView;
    private FirebaseAuth firebaseAuth;
    private Context context; // 컨텍스트 참조 보존을 위한 변수

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.home, container, false);
        context = getContext(); // 컨텍스트 참조 보존

        nameTextView = rootView.findViewById(R.id.nameTextView);
        photoImageView = rootView.findViewById(R.id.photoImageView);

        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference().child("Users").child(uid).child("Info");

            databaseRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists() && context != null) { // 컨텍스트가 null이 아닌지 확인
                        String name = snapshot.child("name").getValue(String.class);
                        String photoUrl = snapshot.child("profileImageUrl").getValue(String.class);

                        nameTextView.setText("안녕하세요, " + name + "님!");
                        Glide.with(context).load(photoUrl).into(photoImageView);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // 데이터베이스 에러 처리
                    // 에러 로그 출력 또는 사용자에게 오류 메시지 표시 등의 작업을 수행할 수 있습니다.
                    Log.e("Home01", "Database Error: " + error.getMessage());
                }
            });
        }

        // ViewPager2 초기화 및 어댑터 설정
        ViewPager2 viewPager = rootView.findViewById(R.id.viewpager);
        FragmentStateAdapter pagerAdapter = new MyAdapter(getActivity(), 4);
        viewPager.setAdapter(pagerAdapter);

        // Indicator 설정
        CircleIndicator3 indicator = rootView.findViewById(R.id.indicator);
        indicator.setViewPager(viewPager);
        indicator.createIndicators(4, 0);

        // community 버튼 초기화
        Button communityButton = rootView.findViewById(R.id.community);
        Button newsButton = rootView.findViewById(R.id.news);

        // community 버튼 클릭 이벤트 처리
        communityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // CommunityActivity 실행
                Intent intent = new Intent(getActivity(), CommunityActivity.class);
                startActivity(intent);
            }
        });

        newsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // CommunityActivity 실행
                Intent intent = new Intent(getActivity(), RecycleActivity.class);
                startActivity(intent);
            }
        });

        return rootView;
    }
}







