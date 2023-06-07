package com.example.mocap01;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class ImgActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView profileImageView;
    private Uri imageUri;
    private StorageReference storageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mypage);

        profileImageView = findViewById(R.id.profile_image);

        storageReference = FirebaseStorage.getInstance().getReference();

        profileImageView.setOnClickListener(v -> openGallery());

        // 기존에 설정된 프로필 사진을 가져오기 위해 다운로드 메소드 호출
        downloadProfileImage();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void uploadProfileImage() {
        if (imageUri != null) {
            StorageReference profileImageRef = storageReference.child("profileImages/" + FirebaseAuth.getInstance().getCurrentUser().getUid() + ".jpg");

            profileImageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        // 업로드 성공 시 처리 로직
                        Toast.makeText(ImgActivity.this, "프로필 사진이 업로드되었습니다.", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        // 업로드 실패 시 처리 로직
                        Toast.makeText(ImgActivity.this, "프로필 사진 업로드에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void downloadProfileImage() {
        StorageReference profileImageRef = storageReference.child("profileImages/" + FirebaseAuth.getInstance().getCurrentUser().getUid() + ".jpg");

        profileImageRef.getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    // 다운로드 성공 시 처리 로직
                    // uri를 사용하여 이미지를 로드하거나 표시하는 작업을 수행할 수 있습니다.
                })
                .addOnFailureListener(e -> {
                    // 다운로드 실패 시 처리 로직
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            profileImageView.setImageURI(imageUri);

            uploadProfileImage();
        }
    }
}
