package com.example.hairilhumsani.hendroiddoctor;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class AppointmentActivity extends AppCompatActivity {

    private DatabaseReference databaseReference;

    private ListView datetList;
    private ArrayList<String> arrayList = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment);

        getAppointmentDate();
    }

    private void getAppointmentDate() {
        final ArrayAdapter arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, arrayList);
        datetList = (ListView) findViewById(R.id.appointment_date_list);
        final String times[] = new String[]{"9 AM", "10 AM", "11 AM", "1 PM", "2 PM", "3 PM", "4 PM", "5 PM"};

        databaseReference = FirebaseDatabase.getInstance().getReference().child("Doctor");
        datetList.setAdapter(arrayAdapter);

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                arrayList.clear();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot dates : dataSnapshot.getChildren()) {
                        String date = dates.getKey();
                        if (!date.equals("Appointment Date")) {
                            arrayList.add(date);

                        }
                        arrayAdapter.notifyDataSetChanged();
                    }

                } else {
                    Toast.makeText(AppointmentActivity.this, "No data", Toast.LENGTH_SHORT).show();
                }

            }


            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        datetList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String time = arrayList.get(position);

                Intent intent = new Intent(AppointmentActivity.this, AppointmentResult.class);
                intent.putExtra("time", time);
                startActivity(intent);
            }
        });


    }
}
