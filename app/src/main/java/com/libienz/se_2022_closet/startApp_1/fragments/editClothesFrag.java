package com.libienz.se_2022_closet.startApp_1.fragments;

import static android.app.Activity.RESULT_OK;
import static com.libienz.se_2022_closet.startApp_1.util.FirebaseReference.userRef;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentResultListener;
import androidx.viewpager2.widget.ViewPager2;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.libienz.se_2022_closet.R;
import com.libienz.se_2022_closet.startApp_1.data.Clothes;

import java.util.ArrayList;

public class editClothesFrag extends Fragment {

    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private StorageReference storageReference = storage.getReference().child("clothes").child(user.getUid());
    private String ClothesKey;
    private Uri imguri;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_edit_clothes, container, false);

        ClothesKey = getArguments().getString("ClothesKey");

        ImageView editImg_iv = view.findViewById(R.id.editImg_iv);
        EditText editTag_et = view.findViewById(R.id.editTag_et);
        EditText editInfo_et = view.findViewById(R.id.editInfo_et);

        //수정 전 원본 데이터 불러오기
        userRef.child(user.getUid()).child("Clothes").child(ClothesKey).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Clothes clothes = snapshot.getValue(Clothes.class);

                imguri = Uri.parse(clothes.getimg());
                //editTag_et.setText(clothes.gettag());
                editInfo_et.setText(clothes.getClothesInfo());

                StorageReference submitReference = storageReference.child(ClothesKey + ".png");
                submitReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        if (getActivity() == null) return;
                        Glide.with(editClothesFrag.this).load(uri).into(editImg_iv);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        //갤러리에서 사진을 선택하면 해당 사진을 앱으로 불러와 이미지뷰에 띄움
        ActivityResultLauncher<Intent> activityResult = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null){
                            imguri = result.getData().getData();
                            editImg_iv.setImageURI(imguri);
                        }
                    }
                });

        //이미지 업로드 버튼을 클릭하면 갤러리에 접근하는 메소드를 호출하도록 함, 성민님 코드 참고했음 (BoardAddActivity.java 57:60)
        Button editClothesImg_btn = (Button) view.findViewById(R.id.editClothesImg_btn);
        editClothesImg_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent();
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/");
                activityResult.launch(galleryIntent);
            }
        });

        //수정 완료 버튼을 눌러 의류를 수정함
        Button doneeditClothes_btn = (Button) view.findViewById(R.id.doneeditClothes_btn);
        doneeditClothes_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){

                //유저 정보 확인 후 의류 수정
                if (user != null){
                    //editClothes(user.getUid(), imguri.toString(), editTag_et.getText().toString(), editInfo_et.getText().toString(), container);
                    Toast.makeText(container.getContext(), "수정 완료되었습니다", Toast.LENGTH_SHORT).show();

                    //수정 완료된 의류의 키값을 열람 프래그먼트에 넘김
                    Bundle bundle = new Bundle();
                    bundle.putString("ClothesKey", ClothesKey);

                    readClothesFrag readClothesFrag = new readClothesFrag();
                    readClothesFrag.setArguments(bundle);

                    //수정된 의류 정보를 열람하도록 함
                    getParentFragmentManager().beginTransaction().replace(R.id.readClothes_fg, readClothesFrag).commit();
                }
            }
        });

        return view;
    }

    //파이어베이스 이미지 업로드 메소드
    private void uploadToFirebase(Uri uri, String idToken, String ClothesKey, ViewGroup container){
        StorageReference imgref = storage.getReference().child("clothes").child(idToken).child(ClothesKey + ".png");
        UploadTask uploadTask = imgref.putFile(uri);

        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast failedmsg = Toast.makeText(container.getContext(), "Failed to Edit Img", Toast.LENGTH_SHORT);
                failedmsg.show();
            }
        });
    }

    public void editClothes(String idToken, String Img, ArrayList<String> Tag, String Info, ViewGroup container){
        Clothes clothes = new Clothes(Img, Tag, Info);

        //파이어베이스 리얼타임 데이터베이스에 의류 정보 저장
        userRef.child(idToken).child("Clothes").child(ClothesKey).setValue(clothes);

        //파이어베이스 스토리지에 의류 사진 저장
        uploadToFirebase(imguri, idToken, ClothesKey, container);
    }
}