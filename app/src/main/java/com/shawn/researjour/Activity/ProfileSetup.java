package com.shawn.researjour.Activity;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.shawn.researjour.R;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.Calendar;
import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileSetup extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private CircleImageView profileImage;
    private EditText fullName,dob_picker,university;
    private Spinner gender,researcherRole;
    private Button nextBtn;
    private ProgressDialog loadingBar;

    //fireBase
    private FirebaseAuth mAuth;
    private DatabaseReference UsersRef;
    private StorageReference UserProfileImageRef;
    private String currentUserID;

    //constants for image pick
    private int PReqCode=1;
    private static final int REQUESCODE =1 ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_setup);

        //init fireBase
        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserID);
        UserProfileImageRef = FirebaseStorage.getInstance().getReference().child("Profile Images");

        //finding id
        profileImage=findViewById(R.id.profile_image);
        fullName=findViewById(R.id.full_name_id);
        university=findViewById(R.id.universityText_id);
        researcherRole=findViewById(R.id.roleSpinner_id);
        gender=findViewById(R.id.gender_spinner);
        dob_picker=findViewById(R.id.dobText_id);
        nextBtn= findViewById(R.id.profile_next_button_id);
        loadingBar = new ProgressDialog(this);

        //array adapter for spinner items
        ArrayAdapter<CharSequence> researcherRoleAdapter = ArrayAdapter.createFromResource(this,
                R.array.role_spinnerItems, android.R.layout.simple_spinner_item);
        researcherRoleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        researcherRole.setAdapter(researcherRoleAdapter);
        researcherRole.setOnItemSelectedListener(this);

        ArrayAdapter<CharSequence> genderAdapter = ArrayAdapter.createFromResource(this,
                R.array.gender_spinnerItems, android.R.layout.simple_spinner_item);
        researcherRoleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        gender.setAdapter(genderAdapter);
        gender.setOnItemSelectedListener(this);

        /*login text watcher for empty edit text field*/
        fullName.addTextChangedListener(loginTextWatcher);
        university.addTextChangedListener(loginTextWatcher);
        dob_picker.addTextChangedListener(loginTextWatcher);

        //calling check permission method
        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            if (Build.VERSION.SDK_INT >= 22) {
                    checkAndRequestForPermission();
            }
            }
        });

        //fetching user profile image from fireBase
        UsersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if(dataSnapshot.exists())
                {
                    if (dataSnapshot.hasChild("profileimage"))
                    {
                        String image = dataSnapshot.child("profileimage").getValue().toString();
                        Picasso.get().load(image).placeholder(R.drawable.profile_image).into(profileImage);
                    }
                    else
                    {
                        Toast.makeText(ProfileSetup.this, "Please select profile image first.", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        //date picker
        dob_picker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                datePicker();
            }
        });

        //next button intent
        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                SaveAccountSetupInformation();
            }
        });

    }

    //data picker method
    private void datePicker() {
        //calender instance for picking the date of birth
        Calendar mCalender=Calendar.getInstance();
        final int year=mCalender.get(Calendar.YEAR);
        final int month=mCalender.get(Calendar.MONTH);
        final int day=mCalender.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog=new DatePickerDialog(ProfileSetup.this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                month=month+1;
                String date=dayOfMonth+"/"+month+"/"+year;
                dob_picker.setText(date);
            }
        },year,month,day);
        datePickerDialog.show();
    }

    //on activity result method
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==REQUESCODE && resultCode==RESULT_OK && data!=null)
        {
            //Uri imageUri = data.getData();

            //init crop image activity
            CropImage.activity()
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(1, 1)
                    .start(this);
        }

        if(requestCode==CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE)
        {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if(resultCode == RESULT_OK)
            {
                loadingBar.setTitle("Profile Image");
                loadingBar.setMessage("Please wait, while we updating your profile image...");
                loadingBar.show();
                loadingBar.setCanceledOnTouchOutside(true);

                Uri resultUri = result.getUri();
                StorageReference filePath = UserProfileImageRef.child(currentUserID + ".jpg");

                filePath.putFile(resultUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        if (taskSnapshot.getMetadata() != null) {
                            if (taskSnapshot.getMetadata().getReference() != null) {
                                Task<Uri> result = taskSnapshot.getStorage().getDownloadUrl();
                                result.addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        String downloadUrl = uri.toString();
                                        UsersRef.child("profileimage").setValue(downloadUrl).addOnCompleteListener(new OnCompleteListener<Void>()
                                        {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task)
                                            {
                                                if(task.isSuccessful())
                                                {
                                                    Intent selfIntent = new Intent(ProfileSetup.this, ProfileSetup.class);
                                                    startActivity(selfIntent);

                                                    Toast.makeText(ProfileSetup.this, "Profile Image stored to Firebase Database Successfully...", Toast.LENGTH_SHORT).show();
                                                    loadingBar.dismiss();
                                                }
                                                else {
                                                    String message = task.getException().getMessage();
                                                    Toast.makeText(ProfileSetup.this, "Error Occured: " + message, Toast.LENGTH_SHORT).show();
                                                    loadingBar.dismiss();
                                                }
                                            }

                                        });

                                    }
                                });
                            }
                        }
                    }
                });

            } else
            {
                Toast.makeText(this, "Error Occured: Image can not be cropped. Try Again.", Toast.LENGTH_SHORT).show();
                loadingBar.dismiss();
            }
        }
    }

    //spinner item selected listener
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String researcherRoleInput = parent.getItemAtPosition(position).toString();
        String genderInput = parent.getItemAtPosition(position).toString();

        HashMap userMap = new HashMap();
        if(parent.getId() == R.id.roleSpinner_id)
        {
            userMap.put("researcherRole", researcherRoleInput);
        }
        else if(parent.getId() == R.id.gender_spinner)
        {
            userMap.put("gender", genderInput);
        }

        UsersRef.updateChildren(userMap).addOnCompleteListener(new OnCompleteListener() {
            @Override
            public void onComplete(@NonNull Task task)
            {
                if(task.isSuccessful())
                {
                }
                else
                {
                    String message =  task.getException().getMessage();
                    Toast.makeText(ProfileSetup.this, "Error Occured: " + message, Toast.LENGTH_SHORT).show();
                    loadingBar.dismiss();
                }
            }
        });

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    //check permission method
    private void checkAndRequestForPermission() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {

                Toast.makeText(this,"Please accept for required permission",Toast.LENGTH_SHORT).show();
            }

            else
            {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PReqCode);
            }

        }
        else
            openGallery();

    }

    //open gallery method
    private void openGallery() {

        //TODO: open gallery intent and wait for user to pick an image !

        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent,REQUESCODE);
    }

    //saving account info to fireBase
    private void SaveAccountSetupInformation() {

        String universtiyInput = university.getText().toString();
        String fullnameInput = fullName.getText().toString();
        String dobInput = dob_picker.getText().toString();

        if(TextUtils.isEmpty(fullnameInput))
        {
            Toast.makeText(this, "Please write your username...", Toast.LENGTH_SHORT).show();
        }
        if(TextUtils.isEmpty(universtiyInput))
        {
            Toast.makeText(this, "Please write your full name...", Toast.LENGTH_SHORT).show();
        }
        if(TextUtils.isEmpty(dobInput))
        {
            Toast.makeText(this, "Please write your country...", Toast.LENGTH_SHORT).show();
        }
        else
        {
            loadingBar.setTitle("Saving Information");
            loadingBar.setMessage("Please wait, while we are creating your new Account...");
            loadingBar.show();
            loadingBar.setCanceledOnTouchOutside(true);

            HashMap userMap = new HashMap();
            userMap.put("fullname", fullnameInput);
            userMap.put("university", universtiyInput);
            userMap.put("date of birth", dobInput);

            UsersRef.updateChildren(userMap).addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task)
                {
                    if(task.isSuccessful())
                    {
                        SendUserToCategorySelection();
                        Toast.makeText(ProfileSetup.this, "your Account is created Successfully.", Toast.LENGTH_LONG).show();
                        loadingBar.dismiss();
                    }
                    else
                    {
                        String message =  task.getException().getMessage();
                        Toast.makeText(ProfileSetup.this, "Error Occured: " + message, Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                    }
                }
            });
        }
    }

    /*method for login text watcher*/
    private TextWatcher loginTextWatcher=new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String fullNameInput=fullName.getText().toString().trim();
            String universityInput=university.getText().toString().trim();
            String dob_pickerInput=dob_picker.getText().toString().trim();

            //setting the button enabled if the edit text field is not empty
            nextBtn.setEnabled(!fullNameInput.isEmpty() && !universityInput.isEmpty() && !dob_pickerInput.isEmpty());
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    //send user to category selection
    private void SendUserToCategorySelection()
    {
        Intent mainIntent = new Intent(ProfileSetup.this, CategorySelection.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }

}
