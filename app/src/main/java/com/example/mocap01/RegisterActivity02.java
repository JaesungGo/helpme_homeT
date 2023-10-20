package com.example.mocap01;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;

public class RegisterActivity02 extends AppCompatActivity {

    private DatabaseReference mDatabaseRef;
    private EditText mEmailText, mPasswordText, mPasswordcheckText, mName, mHeightText, mWeightText;
    private Button mregisterBtn;
    private ImageView mProfileImage;
    private Button mChooseImageBtn;
    private Uri mImageUri;
    private StorageReference mStorageRef;
    private FirebaseAuth firebaseAuth;
    private Spinner mGenderSpinner;
    private static final int PICK_IMAGE_REQUEST = 1;
    private DatePicker mDatePicker; // DatePicker 선언
    private String selectedGender;
    private String selectedBirthday;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Firebase 접근 설정
        firebaseAuth = FirebaseAuth.getInstance();
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        mStorageRef = FirebaseStorage.getInstance().getReference("profile_images");

        mEmailText = findViewById(R.id.et_email);
        mPasswordText = findViewById(R.id.et_pass);
        mPasswordcheckText = findViewById(R.id.et_checkpass);
        mregisterBtn = findViewById(R.id.btn_register);
        mName = findViewById(R.id.et_name);
        mProfileImage = findViewById(R.id.profile_image);
        mChooseImageBtn = findViewById(R.id.btn_choose_image);
        mGenderSpinner = findViewById(R.id.spinner_gender);
        mDatePicker = findViewById(R.id.datepicker); // DatePicker와 레이아웃 연결
        mHeightText = findViewById(R.id.heightEditText);
        mWeightText = findViewById(R.id.weightEditText);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.gender_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mGenderSpinner.setAdapter(adapter);

        mChooseImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImageChooser();
            }
        });

        mGenderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                selectedGender = parentView.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // 사용자가 아무 것도 선택하지 않은 경우
                Toast.makeText(getApplicationContext(), "성별을 선택해주세요.", Toast.LENGTH_SHORT).show();
            }
        });
        mregisterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = mEmailText.getText().toString().trim();
                String pwd = mPasswordText.getText().toString().trim();
                String pwdcheck = mPasswordcheckText.getText().toString().trim();
                String height = mHeightText.getText().toString().trim(); // 키
                String weight = mWeightText.getText().toString().trim(); // 몸무게

                if (email.isEmpty() || pwd.isEmpty() || pwdcheck.isEmpty() || height.isEmpty() || weight.isEmpty()) {
                    Toast.makeText(RegisterActivity02.this, "이메일, 비밀번호, 키, 몸무게를 모두 입력해주세요", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (pwd.equals(pwdcheck)) {
                    final ProgressDialog mDialog = new ProgressDialog(RegisterActivity02.this);
                    mDialog.setMessage("가입중입니다...");
                    mDialog.show();

                    if (mImageUri != null) {
                        StorageReference fileReference = mStorageRef.child(System.currentTimeMillis() + "." + getFileExtension(mImageUri));
                        fileReference.putFile(mImageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                Task<Uri> downloadUriTask = taskSnapshot.getStorage().getDownloadUrl();
                                downloadUriTask.addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri downloadUri) {
                                        String profileImageUrl = downloadUri.toString();
                                        selectedBirthday = getSelectedDate(); // 선택한 생년월일 가져오기
                                        saveUserToFirebase(email, pwd, profileImageUrl, selectedGender, selectedBirthday,height,weight);
                                    }
                                });
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(RegisterActivity02.this, "프로필 사진 업로드 실패", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        String profileImageUrl = "https://firebasestorage.googleapis.com/v0/b/opencv-620cd.appspot.com/o/default_profile_image.png?alt=media&token=de5fdd6b-3695-4008-b32e-7b4b24e0f701&_gl=1*czy4xs*_ga*MjA3MjY4MjI0Ny4xNjc5OTY4MzYx*_ga_CW55HF8NVT*MTY4NjEyNDQ3MC4xNS4xLjE2ODYxMjRNSVC4MC4wLjA."; // 특정 이미지 URL로 대체
                        selectedBirthday = getSelectedDate(); // 선택한 생년월일 가져오기
                        saveUserToFirebase(email, pwd, profileImageUrl, selectedGender, selectedBirthday,height,weight);
                    }
                } else {
                    Toast.makeText(RegisterActivity02.this, "비밀번호가 틀렸습니다. 다시 입력해 주세요.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void openImageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "프로필사진 선택"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            mImageUri = data.getData();
            Glide.with(this).load(mImageUri).into(mProfileImage);
        }
    }

    private String getFileExtension(Uri uri) {
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    private String getSelectedDate() {
        int day = mDatePicker.getDayOfMonth();
        int month = mDatePicker.getMonth() + 1;
        int year = mDatePicker.getYear();
        return year + "-" + month + "-" + day;
    }

    private void saveUserToFirebase(String email, String pwd, String profileImageUrl, String gender, String birthday, String height, String weight) {
        firebaseAuth.createUserWithEmailAndPassword(email, pwd).addOnCompleteListener(RegisterActivity02.this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    String uid = user.getUid();
                    String name = mName.getText().toString().trim();

                    HashMap<String, Object> hashMap = new HashMap<>();
                    hashMap.put("uid", uid);
                    hashMap.put("email", email);
                    hashMap.put("password", pwd);
                    hashMap.put("name", name);
                    hashMap.put("profileImageUrl", profileImageUrl);
                    hashMap.put("gender", gender);
                    hashMap.put("birthday", birthday);
                    hashMap.put("height", height); // 키
                    hashMap.put("weight", weight); // 몸무게

                    DatabaseReference reference = mDatabaseRef.child("Users").child(uid).child("Info");
                    reference.setValue(hashMap);

                    Intent intent = new Intent(RegisterActivity02.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                    Toast.makeText(RegisterActivity02.this, "회원가입에 성공하셨습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(RegisterActivity02.this, "이미 존재하는 아이디입니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
