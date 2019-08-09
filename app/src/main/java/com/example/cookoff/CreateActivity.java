package com.example.cookoff;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class CreateActivity extends AppCompatActivity {

    private CircleImageView setupImg;
    private Toolbar setupToolbar;
    private EditText createName;
    private Button createBtn;
    private Task<Uri> mainImageURI = null;
    private ProgressBar progbar;

    private StorageReference storageReference;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firebaseFirestor;
    private String user_id;
    private static Uri download_uri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create);

        Toolbar setupToolbar = (Toolbar) findViewById(R.id.setupToolbar);
        setSupportActionBar(setupToolbar);
        getSupportActionBar().setTitle("Account Setup");


        mAuth = FirebaseAuth.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();
        firebaseFirestor = FirebaseFirestore.getInstance();

        setupImg = findViewById(R.id.createImage);
        createName = (EditText) findViewById(R.id.createName);
        createBtn = (Button) findViewById(R.id.createBtn);
        progbar = (ProgressBar) findViewById(R.id.progbar);

        user_id = mAuth.getCurrentUser().getUid();

        firebaseFirestor.collection("Users").document(user_id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                if (task.isSuccessful()) {

                    if (task.getResult().exists()) {

                        String name = task.getResult().getString("name");
                        String image = task.getResult().getString("image");

                        createName.setText(name);


                        RequestOptions placeholderRequest = new RequestOptions();
                        placeholderRequest.placeholder(R.drawable.defaultprofilepic);
                        Toast.makeText(CreateActivity.this, image, Toast.LENGTH_SHORT).show();
                        Glide.with(CreateActivity.this).setDefaultRequestOptions(placeholderRequest).load(image).into(setupImg);

                    } else {

                        Toast.makeText(CreateActivity.this, "Data doesn't exist", Toast.LENGTH_SHORT).show();

                    }


                } else {

                    String error = task.getException().getMessage();
                    Toast.makeText(CreateActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();

                }
            }
        });

        user_id = mAuth.getCurrentUser().getUid();

        setupImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                    String user_name = createName.getText().toString();

                    if (ContextCompat.checkSelfPermission(CreateActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                        // Toast.makeText(CreateActivity.this,"Permission denied",Toast.LENGTH_LONG).show();
                        // Manifest must be an array
                        ActivityCompat.requestPermissions(CreateActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

                    } else {

                        Toast.makeText(CreateActivity.this, "Select a profile picture!", Toast.LENGTH_LONG).show();
                        CropImage.activity()
                                .setGuidelines(CropImageView.Guidelines.ON)
                                .start(CreateActivity.this);

                    }

                } else {

                    bringImageSelector();

                }

            }

            private void bringImageSelector() {

                Toast.makeText(CreateActivity.this, "Select a profile picture!", Toast.LENGTH_LONG).show();
                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .start(CreateActivity.this);
            }
        });


        createBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final String userName = createName.getText().toString();

                if (!TextUtils.isEmpty(userName) && mainImageURI != null) {

                    final String user_id = mAuth.getCurrentUser().getUid();
                    StorageReference image_path = storageReference.child("profile_images").child(user_id + ".jpg");
                    progbar.setVisibility(View.VISIBLE);

                    image_path.putFile(mainImageURI).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                            if (task.isSuccessful()) {




                                Map<String, String> userMap = new HashMap<>();
                                userMap.put("name", userName);
                                userMap.put("image", download_uri.toString());

                                firebaseFirestor.collection("Users").document(user_id).set(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {

                                        if (task.isSuccessful()) {

                                            Intent mainIntent = new Intent(CreateActivity.this, MainActivity.class);
                                            startActivity(mainIntent);
                                            finish();

                                        } else {

                                            String error = task.getException().getMessage();
                                            Toast.makeText(CreateActivity.this, "Error:" + error, Toast.LENGTH_LONG).show();


                                        }
                                        progbar.setVisibility(View.INVISIBLE);
                                    }
                                });


                            } else {
                                String error = task.getException().getMessage();
                                Toast.makeText(CreateActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                                progbar.setVisibility(View.INVISIBLE);
                            }


                        }
                    });


                }

            }
        });


    }

    private void storeFirestore(@NonNull Task<UploadTask.TaskSnapshot> task, String user_name) {

        Task<Uri> download_uri;

        if (task != null) {

            download_uri = task.getResult().getStorage().getDownloadUrl();
        } else {

            download_uri = mainImageURI;

        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {

                mainImageURI = result.getUri();
                setupImg.setImageURI(mainImageURI);

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }

    }
}
