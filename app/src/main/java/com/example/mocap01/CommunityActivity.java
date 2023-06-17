package com.example.mocap01;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class CommunityActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private ListView mListView;
    private List<Post> mPostList;
    private PostAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community);

        mAuth = FirebaseAuth.getInstance();

        mListView = findViewById(R.id.list_view);
        mPostList = new ArrayList<>();
        mAdapter = new PostAdapter();
        mListView.setAdapter(mAdapter);

        DatabaseReference postsRef = FirebaseDatabase.getInstance().getReference("posts");
        postsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mPostList.clear(); // 기존 목록을 초기화

                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    Post post = postSnapshot.getValue(Post.class);
                    mPostList.add(0,post);
                }

                mAdapter.notifyDataSetChanged(); // 어댑터에 데이터 변경을 알림
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(CommunityActivity.this, "Failed to retrieve posts.", Toast.LENGTH_SHORT).show();
            }
        });

        mListView.setOnItemClickListener((parent, view, position, id) -> {
            Post post = mPostList.get(position);
            Intent intent = new Intent(CommunityActivity.this, PostDetailActivity.class);
            intent.putExtra("title", post.getTitle());
            intent.putExtra("content", post.getContent());
            intent.putExtra("name",post.getName());
            intent.putExtra("imageUrl", post.getImageUrl());
            startActivity(intent);
        });
    }

    public void onLogoutClick(View view) {
        mAuth.signOut();
        finish();
    }

    public void onCreatePostClick(View view) {
        Intent intent = new Intent(this, CreatePostActivity.class);
        startActivity(intent);
    }

    private class PostAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mPostList.size();
        }

        @Override
        public Object getItem(int position) {
            return mPostList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.list_item_post, parent, false);
            }

            TextView titleTextView = convertView.findViewById(R.id.text_title);
            TextView nameTextView = convertView.findViewById(R.id.text_name);

            Post post = mPostList.get(position);
            titleTextView.setText(post.getTitle());
            nameTextView.setText("작성자: " + post.getName());

            return convertView;
        }


    }
}



