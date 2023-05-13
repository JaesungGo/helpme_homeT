package com.example.mocap01;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.os.Bundle;

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MypageActivity extends AppCompatActivity {

    private FirebaseAuth mFirebaseAuth;     //파이어 베이스 인증
    private FirebaseDatabase mFirebaseData = FirebaseDatabase.getInstance();
    private DatabaseReference mDatabaseRef = mFirebaseData.getReference(); //실시간 데이터 베이스

    private ChildEventListener mChild;

    private ListView listView2;
    private ArrayAdapter<String> adapter;
    List<Object> Array = new ArrayList<Object>();
    private Button sdData1;
    private EditText etData1;
    public String msgData1;

    ImageView home, train, record, mypage;
    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mypage);

        sdData1 = (Button)findViewById(R.id.button2);
        etData1 = (EditText)findViewById(R.id.editTextText2);
        listView2 = (ListView) findViewById(R.id.listView2);

        initDatabase();

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<String>());
        listView2.setAdapter(adapter);

        mFirebaseAuth = FirebaseAuth.getInstance();
        mDatabaseRef = FirebaseDatabase.getInstance().getReference("UserAccount");

        sdData1.setOnClickListener((v -> {
            msgData1 = etData1.getText().toString();
            mDatabaseRef.child(mFirebaseAuth.getUid()).child("check").push().child("count").setValue(msgData1);
            Toast.makeText(MypageActivity.this,"업로드성공",Toast.LENGTH_SHORT).show();
        }));

        //mDatabaseRef = mFirebaseData.getReference("UserAccount"); // 변경값을 확인할 child 이름
        mDatabaseRef.child(mFirebaseAuth.getUid()).child("check").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                adapter.clear();
                for (DataSnapshot messageData : dataSnapshot.getChildren()) {
                    String msg2 = messageData.getValue().toString();
                    Array.add(msg2);
                    adapter.add(msg2);
                    // child 내에 있는 데이터만큼 반복합니다.
                }
                adapter.notifyDataSetChanged();
                listView2.setSelection(adapter.getCount() - 1);
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
    private void initDatabase() {

        mFirebaseData = FirebaseDatabase.getInstance();

        mDatabaseRef = mFirebaseData.getReference("log");
        mDatabaseRef.child("log").setValue("check");

        mChild = new ChildEventListener() {

            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        mDatabaseRef.addChildEventListener(mChild);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDatabaseRef.removeEventListener(mChild);
    }

}
