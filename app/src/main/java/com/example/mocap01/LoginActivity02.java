package com.example.mocap01;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LoginActivity02 extends AppCompatActivity {

    private Button mLoginBtn;
    private TextView mResigettxt;
    private EditText mEmailText, mPasswordText;
    private FirebaseAuth firebaseAuth;

    private DatabaseReference mDatabaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        firebaseAuth =  FirebaseAuth.getInstance();
        //버튼 등록하기
        mResigettxt = findViewById(R.id.btn_register);
        mLoginBtn = findViewById(R.id.btn_login);
        mEmailText = findViewById(R.id.et_email);
        mPasswordText = findViewById(R.id.et_pass);


        //가입 버튼이 눌리면
        mResigettxt.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                //intent함수를 통해 register액티비티 함수를 호출한다.
                startActivity(new Intent(LoginActivity02.this,RegisterActivity02.class));

            }
        });

        //로그인 버튼이 눌리면
        mLoginBtn.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                String email = mEmailText.getText().toString().trim();
                String pwd = mPasswordText.getText().toString().trim();

                if (email.isEmpty() || pwd.isEmpty()) {
                    Toast.makeText(LoginActivity02.this, "이메일과 비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show();
                    return;
                }

                firebaseAuth.signInWithEmailAndPassword(email,pwd)
                        .addOnCompleteListener(LoginActivity02.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if(task.isSuccessful()){
                                    Intent intent = new Intent(LoginActivity02.this, MainActivity.class);
                                    startActivity(intent);

                                }else{
                                    Toast.makeText(LoginActivity02.this,"로그인 오류",Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

            }
        });
    }
}

