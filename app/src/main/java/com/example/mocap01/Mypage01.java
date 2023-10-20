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
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
    private DatePicker mDatePicker;
    private EditText mHeightEdit;
    private EditText mWeightEdit;
    private Button mSelectImageButton;
    private Button mSaveButton;
    private TextView mGenderTextView;
    private TextView mBirthdayTextView;

    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private DatabaseReference mDatabaseRef;
    private StorageReference mStorageRef;

    private Uri mImageUri;
    private String mProfileImageUrl;

    private ActivityResultLauncher<Intent> mGetContentLauncher;
    private String mGender;  // 변수 추가: 성별 정보를 저장
    private String mBirthday;  // 변수 추가: 생년월일 정보를 저장

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_mypage, container, false);

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        mDatabaseRef = FirebaseDatabase.getInstance().getReference("Users").child(mUser.getUid()).child("Info");
        mStorageRef = FirebaseStorage.getInstance().getReference("ProfileImages");

        mProfileImage = rootView.findViewById(R.id.profile_image);
        mNameEditText = rootView.findViewById(R.id.name_edit_text);
        mSelectImageButton = rootView.findViewById(R.id.select_image_button);
        mHeightEdit = rootView.findViewById(R.id.heightEditText);
        mWeightEdit = rootView.findViewById(R.id.weightEditText);
        mSaveButton = rootView.findViewById(R.id.save_button);
        mGenderTextView = rootView.findViewById(R.id.genderTextView);
        mBirthdayTextView = rootView.findViewById(R.id.birthdayTextView);

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
        final String height = mHeightEdit.getText().toString().trim();
        final String weight = mWeightEdit.getText().toString().trim();


        if (TextUtils.isEmpty(name)) {
            Toast.makeText(getActivity(), "이름을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mImageUri != null) {
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
                    updateUserProfile(name,mGender,mBirthday, height, weight);
                });
            }).addOnFailureListener(e -> {
                Toast.makeText(getActivity(), "이미지 업로드에 실패하였습니다", Toast.LENGTH_SHORT).show();
            });
        } else {
            updateUserProfile(name,mGender,mBirthday, height, weight);
        }
    }

    private void updateUserProfile(String name,String gender,String birthday, String height, String weight) {
        String uid = mUser.getUid();

        User user = new User(uid, mUser.getEmail(), name, mProfileImageUrl);
        user.setHeight(height);
        user.setWeight(weight);
        user.setGender(gender);
        user.setBirthday(birthday);

        mDatabaseRef.setValue(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getActivity(), "프로필이 성공적으로 저장되었습니다", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getActivity(), "프로필 저장에 실패하였습니다", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadUserProfile() {
        String uid = mUser.getUid();

        mDatabaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    mGender = snapshot.child("gender").getValue(String.class);
                    mBirthday = snapshot.child("birthday").getValue(String.class);
                    String height = snapshot.child("height").getValue(String.class);
                    String weight = snapshot.child("weight").getValue(String.class);

                    mHeightEdit.setText(height);
                    mWeightEdit.setText(weight);

                    mGenderTextView.setText(mGender);
                    mBirthdayTextView.setText(mBirthday);

                    String name = snapshot.child("name").getValue(String.class);
                    mNameEditText.setText(name);

                    String profileImageUrl = snapshot.child("profileImageUrl").getValue(String.class);
                    if (!TextUtils.isEmpty(profileImageUrl)) {
                        Glide.with(getActivity())
                                .load(profileImageUrl)
                                .apply(RequestOptions.circleCropTransform())
                                .into(mProfileImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getActivity(), "유저 정보 불러오기 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static class User {
        private String uid;
        private String email;
        private String name;
        private String profileImageUrl;
        private String height;
        private String weight;
        private String gender;
        private String birthday;

        public User() {
            // Default constructor required for calls to DataSnapshot.getValue(User.class)
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

        public String getHeight() {
            return height;
        }

        public void setHeight(String height) {
            this.height = height;
        }

        public String getWeight() {
            return weight;
        }

        public void setWeight(String weight) {
            this.weight = weight;
        }
        public String getGender() {
            return gender;
        }

        public void setGender(String gender) {
            this.gender = gender;
        }

        public String getBirthday() {
            return birthday;
        }

        public void setBirthday(String birthday) {
            this.birthday = birthday;
        }
    }
}
