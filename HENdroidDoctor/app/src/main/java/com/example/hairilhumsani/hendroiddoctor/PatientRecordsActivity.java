package com.example.hairilhumsani.hendroiddoctor;

import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class PatientRecordsActivity extends AppCompatActivity {

    private DatabaseReference databaseReference;

    private ListView patientList;
    private ArrayList<String> arrayList = new ArrayList<>();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_records);

        ActionBar ab = getSupportActionBar();
        ab.setTitle("Patient");

        setList();
    }

    private void setList()
    {
        final ArrayAdapter arrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, arrayList);
        patientList = (ListView) findViewById(R.id.list_patient);
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Patient");
        patientList.setAdapter(arrayAdapter);

       databaseReference.addValueEventListener(new ValueEventListener() {
           @Override
           public void onDataChange(DataSnapshot dataSnapshot) {
               arrayList.clear();
               for (DataSnapshot names: dataSnapshot.getChildren()) {
                   String name = names.getKey();
                   arrayList.add(name);
               }
               arrayAdapter.notifyDataSetChanged();
           }

           @Override
           public void onCancelled(DatabaseError databaseError) {

           }
       });
       patientList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
           @Override
           public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
               String names = arrayList.get(position);

               Intent intent = new Intent(PatientRecordsActivity.this,PatientDate.class);
               intent.putExtra("name",names);

               startActivity(intent);

           }
       });


    }
}
