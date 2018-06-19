package com.example.hairilhumsani.hendroiddoctor;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

public class PatientDetails extends AppCompatActivity {

    private DatabaseReference databaseReference;

    private ListView symptomsList;
    private ArrayList<String> arrayList = new ArrayList<>();

    private TextView symptomsName;
    private TextView checkUp;
    private TextView temperature;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_details);

        symptomsName = (TextView) findViewById(R.id.symptomsText);
        checkUp = (TextView) findViewById(R.id.checkNo);
        temperature = (TextView) findViewById(R.id.tempNo);

        Intent intent = getIntent();
        String name = intent.getStringExtra("name");
        String date = intent.getStringExtra("date");

        symptomsName(name, date);
        tempNo(name, date);
        checkUp(name, date);
        getImagesByGlide(name, date);


        symptomsName.setText(name + "'s Symptoms");
    }

    private void symptomsName(String name, String date) {
        final ArrayAdapter arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, arrayList);
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Patient").child(name).child(date).child("Symptom");
        symptomsList = (ListView) findViewById(R.id.symptomsList);
        symptomsList.setAdapter(arrayAdapter);

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot symptoms : dataSnapshot.getChildren()) {
                    String symptom = symptoms.getValue().toString();
                    arrayAdapter.add(symptom);
                }


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void tempNo(String name, String date) {
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Patient").child(name).child(date).child("Temperature");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String temp = dataSnapshot.getValue().toString();
                    temperature.setText(temp);
                } else {
                    temperature.setText("Haven't take temperature");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void checkUp(String name, String date) {
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Patient").child(name).child(date).child("Checkup");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String checkNo = dataSnapshot.getValue().toString();
                    checkUp.setText(checkNo);

                } else {
                    checkUp.setText("Haven't check up");
                }


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void getImagesByGlide(String name, String date) {
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Patient").child(name).child(date).child("Image");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String imageUrl;
                if (dataSnapshot.exists()) {
                    imageUrl = dataSnapshot.getValue().toString();
                } else {
                    imageUrl = "";
                }
                    StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl("gs://splashawards2018-b98bd.appspot.com/" + imageUrl +".jpg");
                    ImageView imageView = (ImageView) findViewById(R.id.patient_facial);
                    Glide.with(PatientDetails.this)
                            .using(new FirebaseImageLoader())
                            .load(storageReference)
                            .into(imageView);
            }


            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
