package com.example.mocap01;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class CreatePostActivity extends AppCompatActivity {

    private EditText editTextTitle;
    private EditText editTextContent;
    private ImageView imageView;
    private Button buttonAttach;
    private Button buttonCreate;

    private DatabaseReference mDatabase;
    private StorageReference mStorage;
    private FirebaseAuth mAuth;

    private Uri imageUri;

    private ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null && data.getData() != null) {
                        imageUri = data.getData();
                        imageView.setImageURI(imageUri);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mStorage = FirebaseStorage.getInstance().getReference();

        editTextTitle = findViewById(R.id.editTextTitle);
        editTextContent = findViewById(R.id.editTextContent);
        imageView = findViewById(R.id.imageView);
        buttonAttach = findViewById(R.id.buttonAttach);
        buttonCreate = findViewById(R.id.buttonCreate);

        buttonAttach.setOnClickListener(v -> attachImage());

        buttonCreate.setOnClickListener(v -> createPost());
    }

    private void attachImage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void createPost() {
        String title = editTextTitle.getText().toString().trim();
        String content = editTextContent.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            Toast.makeText(this, "제목을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(content)) {
            Toast.makeText(this, "내용을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        DatabaseReference usersRef = mDatabase.child("Users").child(userId);

        usersRef.get().addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
            @Override
            public void onSuccess(DataSnapshot dataSnapshot) {
                String name = dataSnapshot.child("name").getValue(String.class);

                String postId = mDatabase.child("posts").push().getKey();

                if (postId != null) {
                    StorageReference imageRef = mStorage.child("post_images").child(postId + ".jpg");

                    if (imageUri != null) {
                        // 이미지가 선택된 경우, 이미지를 Firebase Storage에 업로드
                        imageRef.putFile(imageUri)
                                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                        // 이미지 업로드 성공 시, 다운로드 URL을 가져옴
                                        imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                            @Override
                                            public void onSuccess(Uri uri) {
                                                // 다운로드 URL을 사용하여 게시물 객체 생성
                                                Post post = new Post(postId, title, content, uri.toString(), name);

                                                // 게시물을 Firebase Database에 저장
                                                savePost(post);
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Toast.makeText(CreatePostActivity.this, "이미지 업로드 실패", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(CreatePostActivity.this, "이미지 업로드 실패", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        // 이미지가 선택되지 않은 경우, 게시물 객체 생성 (이미지 URL은 null)
                        Post post = new Post(postId, title, content, null, name);

                        // 게시물을 Firebase Database에 저장
                        savePost(post);
                    }
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(CreatePostActivity.this, "사용자 정보 가져오기 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void savePost(Post post) {
        DatabaseReference postsRef = FirebaseDatabase.getInstance().getReference("posts");
        String postId = postsRef.push().getKey();

        if (postId != null) {
            postsRef.child(postId).setValue(post)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(CreatePostActivity.this, "게시물이 저장되었습니다.", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(CreatePostActivity.this, "게시물 저장에 실패했습니다.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}
