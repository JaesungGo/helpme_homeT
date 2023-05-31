package com.example.mocap01;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Mypage01 extends Fragment {

    private ImageView mProfileImage;
    private EditText mNameEditText;
    private Button mSelectImageButton;
    private Button mSaveButton;

    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private DatabaseReference mDatabaseRef;
    private StorageReference mStorageRef;

    private Uri mImageUri;
    private String mProfileImageUrl;

    private ActivityResultLauncher<Intent> mGetContentLauncher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_mypage, container, false);

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        mDatabaseRef = FirebaseDatabase.getInstance().getReference("Users");
        mStorageRef = FirebaseStorage.getInstance().getReference("ProfileImages");

        mProfileImage = rootView.findViewById(R.id.profile_image);
        mNameEditText = rootView.findViewById(R.id.name_edit_text);
        mSelectImageButton = rootView.findViewById(R.id.select_image_button);
        mSaveButton = rootView.findViewById(R.id.save_button);

        mSelectImageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            mGetContentLauncher.launch(intent);
        });

        mSaveButton.setOnClickListener(v -> saveChanges());

        setupActivityResultLauncher();

        loadUserProfile();

        return rootView;
    }

    private void setupActivityResultLauncher() {
        mGetContentLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                Intent data = result.getData();
                mImageUri = data.getData();
                mProfileImage.setImageURI(mImageUri);
            }
        });
    }

    private void saveChanges() {
        final String name = mNameEditText.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(getActivity(), "Please enter a name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mImageUri != null) {
            // 프로필 사진 변경이 있는 경우
            final StorageReference imageRef = mStorageRef.child(mUser.getUid() + ".jpg");
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), mImageUri);
            } catch (IOException e) {
                e.printStackTrace();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, baos);
            byte[] data = baos.toByteArray();

            UploadTask uploadTask = imageRef.putBytes(data);
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    mProfileImageUrl = uri.toString();
                    saveUserChanges(name);
                });
            }).addOnFailureListener(e -> {
                Toast.makeText(getActivity(), "Failed to upload profile image", Toast.LENGTH_SHORT).show();
            });
        } else {
            // 프로필 사진 변경이 없는 경우
            saveUserChanges(name);
        }
    }

    private void saveUserChanges(String name) {
        String uid = mUser.getUid();

        // 사용자 정보 업데이트
        User user = new User(uid, mUser.getEmail(), name, mProfileImageUrl);
        mDatabaseRef.child(uid).setValue(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getActivity(), "Profile saved successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getActivity(), "Failed to save profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadUserProfile() {
        String uid = mUser.getUid();

        // Firebase에서 사용자 정보 가져오기
        mDatabaseRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);

                    // 프로필 사진 표시
                    if (user != null && !TextUtils.isEmpty(user.getProfileImageUrl())) {
                        Glide.with(getActivity())
                                .load(user.getProfileImageUrl())
                                .apply(RequestOptions.circleCropTransform())
                                .into(mProfileImage);
                    }

                    // 이름 표시
                    if (user != null && !TextUtils.isEmpty(user.getName())) {
                        mNameEditText.setText(user.getName());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getActivity(), "Failed to load user profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static class User {
        private String uid;
        private String email;
        private String name;
        private String profileImageUrl;

        public User() {
            // Default constructor required for Firebase
        }

        public User(String uid, String email, String name, String profileImageUrl) {
            this.uid = uid;
            this.email = email;
            this.name = name;
            this.profileImageUrl = profileImageUrl;
        }

        public String getUid() {
            return uid;
        }

        public void setUid(String uid) {
            this.uid = uid;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getProfileImageUrl() {
            return profileImageUrl;
        }

        public void setProfileImageUrl(String profileImageUrl) {
            this.profileImageUrl = profileImageUrl;
        }
    }
}










