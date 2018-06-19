package com.example.hairilhumsani.hendroiddoctor;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class PatientDate extends AppCompatActivity {
    private DatabaseReference databaseReference;

    private ListView datetList;
    private ArrayList<String> arrayList = new ArrayList<>();
    private TextView dateNotice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_date);

        Intent intent = getIntent();
        String name = intent.getStringExtra("name");
        this.getSupportActionBar().setTitle(name);

        getDate(name);
        getAppointmentDate(name);
    }

    private void getDate(final String name) {
        final ArrayAdapter arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, arrayList);
        datetList = (ListView) findViewById(R.id.date_list);
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Patient").child(name);
        datetList.setAdapter(arrayAdapter);

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                arrayList.clear();
                for (DataSnapshot dates : dataSnapshot.getChildren()) {
                    String date = dates.getKey();
                    if (!date.equals("Appointment Date")) {
                        arrayList.add(date);
                    }

                }
                arrayAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        datetList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String date = arrayList.get(position);

                Intent intent = new Intent(PatientDate.this, PatientDetails.class);
                intent.putExtra("name", name);
                intent.putExtra("date", date);

                startActivity(intent);

            }
        });


    }

    private void getAppointmentDate(final String name) {
        dateNotice = (TextView) findViewById(R.id.tv_noticeDate);
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Patient").child(name).child("Appointment Date");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                    String date = dataSnapshot.getValue().toString();
                        dateNotice.setText(date);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
