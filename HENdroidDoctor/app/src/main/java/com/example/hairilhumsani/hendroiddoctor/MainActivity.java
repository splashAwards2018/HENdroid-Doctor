package com.example.hairilhumsani.hendroiddoctor;

import android.Manifest;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import ai.api.AIConfiguration;
import ai.api.AIDataService;
import ai.api.AIServiceException;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Fulfillment;
import ai.api.model.Result;
import ai.kitt.snowboy.SnowboyDetect;

public class MainActivity extends AppCompatActivity {
    static {
        System.loadLibrary("snowboy-detect-android");
    }

    //For override purpose
    String time_reference;

    //Essential
    private DatabaseReference databaseReference;
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private AudioRecord audioRecord = null;
    private AIDataService aiDataService;
    private Boolean shouldDetect;
    private SnowboyDetect snowboyDetect;

    //For Results purpose
    private TextView resultSpeech;
    private Button btnask;
    private Button patientActivity;
    private Button slotActivity;
    private ArrayList<String> arrayList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        int REQUEST_STATIC_PERMISSION = 0;
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, REQUEST_STATIC_PERMISSION);

        resultSpeech = (TextView) findViewById(R.id.result_view);
        btnask = (Button) findViewById(R.id.btn_ask);
        patientActivity = (Button) findViewById(R.id.btnPatient);
        slotActivity = (Button) findViewById(R.id.btnSlot);

        setupNlu();
        setupTts();
        setupAsr();
        setupHotword();
        startHotword();
        getSlotNoti();


        patientActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PatientRecordsActivity.class);
                startActivity(intent);
            }
        });

        slotActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AppointmentActivity.class);
                startActivity(intent);
            }
        });


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void setupAsr() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {

            }

            @Override
            public void onBeginningOfSpeech() {

            }

            @Override
            public void onRmsChanged(float rmsdB) {

            }

            @Override
            public void onBufferReceived(byte[] buffer) {

            }

            @Override
            public void onEndOfSpeech() {

            }

            @Override
            public void onError(int error) {
                Log.e("asr", "ERROR:" + Integer.toString(error));
                startHotword();
            }

            @Override
            public void onResults(Bundle results) {
                List<String> texts = results.getStringArrayList((SpeechRecognizer.RESULTS_RECOGNITION));
                if (texts == null || texts.isEmpty()) {
                    startTts("Please Try Again");
                } else {
                    String text = texts.get(0);
                    startNlu(text);
                }

            }

            @Override
            public void onPartialResults(Bundle partialResults) {

            }

            @Override
            public void onEvent(int eventType, Bundle params) {

            }
        });
    }

    private void startAsr() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {

                final Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en");
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "CHANGE_THIS_TO_LANGUAGE");
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);

                shouldDetect = false;

                speechRecognizer.startListening(recognizerIntent);

            }
        };
        Threadings.runInMainThread(this, runnable);
    }

    private void setupTts() {
        textToSpeech = new TextToSpeech(this, null);
    }

    private void startTts(String text) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (textToSpeech.isSpeaking()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Log.e("tts", e.getMessage());
                    }
                }
                startHotword();
            }
        };
        Threadings.runInBackgroundThread(runnable);
    }

    private void startTtsNoHotword(String text) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (textToSpeech.isSpeaking()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Log.e("tts", e.getMessage());
                    }
                }
            }
        };
        Threadings.runInBackgroundThread(runnable);
    }

    private void setupNlu() {
        String clientAccessToken = "ca48e545151846298b3d20924da0463e";
        AIConfiguration aiConfiguration = new AIConfiguration(clientAccessToken, AIConfiguration.SupportedLanguages.English);
        aiDataService = new AIDataService(aiConfiguration);
    }

    private void startNlu(final String text) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                AIRequest aiRequest = new AIRequest();
                aiRequest.setQuery(text);


                try {
                    AIResponse aiResponse = aiDataService.request(aiRequest);

                    Result result = aiResponse.getResult();
                    Fulfillment fulfillment = result.getFulfillment();
                    String responseText = fulfillment.getSpeech();

                    //Add function if(responseText.equalsIgnoreCase("get_function"))

                    if (responseText.equalsIgnoreCase("get_appointment_function")) {
                        getLastQueryAppointment();
                        responseText = "";

                    }

                    if (responseText.equalsIgnoreCase("get_schedule_slot")) {
                        getSpecificSlot(text);
                        responseText = "";
                    }

                    if (responseText.equalsIgnoreCase("get_symptom_function")) {
                        getSymptoms(text);
                        responseText = "";

                    }


                    final String newRespondText = responseText;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            startTts(newRespondText);
                        }
                    });

                } catch (AIServiceException e) {
                    Log.e("nlu", e.getMessage());
                }
            }
        };
        Threadings.runInBackgroundThread(runnable);
    }

    private void setupHotword() {
        SnowboyUtils.copyAssets(this);


        File snowboyDirectory = SnowboyUtils.getSnowboyDirectory();

        File common = new File(snowboyDirectory, "common.res");
        //File *name* = new File(snowboyDirectory,"*name*".pmdl);


        File voice1 = new File(snowboyDirectory, "Also.pmdl");
        File voice2 = new File(snowboyDirectory, "Another question.pmdl");
        File voice3 = new File(snowboyDirectory, "Another thing.pmdl");
        File voice4 = new File(snowboyDirectory, "Aye.pmdl");
        File voice5 = new File(snowboyDirectory, "Er.pmdl");
        File voice6 = new File(snowboyDirectory, "Erm.pmdl");
        File voice7 = new File(snowboyDirectory, "Hey.pmdl");
        File voice8 = new File(snowboyDirectory, "Next.pmdl");
        File voice9 = new File(snowboyDirectory, "Nurse.pmdl");
        File voice10 = new File(snowboyDirectory, "Oi.pmdl");
        File voice11 = new File(snowboyDirectory, "So.pmdl");
        File voice12 = new File(snowboyDirectory, "Uh.pmdl");
        File voice13 = new File(snowboyDirectory, "Umm.pmdl");


        String models = (String) (voice1.getAbsolutePath() + ","
                + voice2.getAbsolutePath() + ","
                + voice3.getAbsolutePath() + ","
                + voice4.getAbsolutePath() + ","
                + voice5.getAbsolutePath() + ","
                + voice6.getAbsolutePath() + ","
                + voice7.getAbsolutePath() + ","
                + voice8.getAbsolutePath() + ","
                + voice9.getAbsolutePath() + ","
                + voice10.getAbsolutePath() + ","
                + voice11.getAbsolutePath() + ","
                + voice12.getAbsolutePath() + ","
                + voice13.getAbsolutePath());

        snowboyDetect = new SnowboyDetect(common.getAbsolutePath(), models);
        snowboyDetect.setSensitivity("0.30, 0.30, 0.30, 0.30, 0.30, 0.30, 0.30, 0.30, 0.30, 0.30, 0.30, 0.30, 0.30");
        snowboyDetect.applyFrontend(true);
    }

    private void startHotword() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                shouldDetect = true;
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

                int bufferSize = 3200;
                byte[] audioBuffer = new byte[bufferSize];
                audioRecord = new AudioRecord
                        (
                                MediaRecorder.AudioSource.DEFAULT,
                                16000,
                                AudioFormat.CHANNEL_IN_MONO,
                                AudioFormat.ENCODING_PCM_16BIT,
                                bufferSize
                        );

                if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                    Log.e("hotword", "audio record fail to initialize");
                    return;
                }

                audioRecord.startRecording();
                Log.d("hotword", "start listening to hotword");

                while (shouldDetect) {
                    audioRecord.read(audioBuffer, 0, audioBuffer.length);

                    short[] shortArray = new short[audioBuffer.length / 2];
                    ByteBuffer.wrap(audioBuffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortArray);

                    int result = snowboyDetect.runDetection(shortArray, shortArray.length);
                    if (result > 0) {
                        Log.d("hotword", "detected");
                        shouldDetect = false;
                    }
                }
                audioRecord.stop();
                audioRecord.release();
                Log.d("hotword", "stop listening to hotword");

                // TODO: Add action after hotword is detected
                startAsr();
            }
        };
        Threadings.runInBackgroundThread(runnable);
    }

    private void getSlotNoti() {
        databaseReference = FirebaseDatabase.getInstance().getReference("Doctor");

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    //user exists, do something
                    startTtsNoHotword("You have appoinments!");
                } else {
                    //user does not exist, do something else
                    startTtsNoHotword("You have no appoinment!");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    //Get the latest dte appointment from patient
    private void getLastQueryAppointment() {

        databaseReference = FirebaseDatabase.getInstance().getReference().child("Doctor");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (!(dataSnapshot.child("Appointment Date").getValue() == null)) {
                    String messages = dataSnapshot.child("Appointment Date").getValue().toString();
                    startTtsNoHotword(messages);
                    resultSpeech.setText(messages);

                } else {
                    startTtsNoHotword("You have no upcoming appointments");
                    Toast.makeText(MainActivity.this, "No data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                //Handle possible errors.
            }
        });
    }

    private void getSpecificSlot(String time) {
        String time_slot = time.replaceAll("[^\\d.]", "");
        time_slot = time_slot.replace(".", "");
        if (time_slot == "") {
            startTtsNoHotword("Please say the slot again");
        } else {
            int time_int = Integer.parseInt(time_slot);

            String slot[] = new String[]{"9 AM", "10 AM", "11 AM", "1 PM", "2 PM", "3 PM", "4 PM", "5 PM"};


            if (time_int == 9) {
                time_reference = slot[0];
            } else if (time_int == 10) {
                time_reference = slot[1];
            } else if (time_int == 11) {
                time_reference = slot[2];
            } else if (time_int == 1) {
                time_reference = slot[3];
            } else if (time_int == 2) {
                time_reference = slot[4];
            } else if (time_int == 3) {
                time_reference = slot[5];
            } else if (time_int == 4) {
                time_reference = slot[6];
            } else if (time_int == 5) {
                time_reference = slot[7];
            } else {
                time_reference = "";
            }


            databaseReference = FirebaseDatabase.getInstance().getReference().child("Doctor").child(time_reference);
            databaseReference.addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                    String name = dataSnapshot.getValue().toString();
                    if (!dataSnapshot.exists()) {
                        startTtsNoHotword("At " + time_reference + ", you have don't have an appointment");
                        resultSpeech.setText("At " + time_reference + ", you have don't have an appointment");

                    } else {

                        startTtsNoHotword("At " + time_reference + ", you have an appointment with " + name);
                        resultSpeech.setText("At " + time_reference + ", you have an appointment with " + name);
                    }
                }


                @Override
                public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }


    }

    private void getSymptoms(final String name) {
        if (name.contains("patient")) {
            startTtsNoHotword("Please specify your patient name");
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    resultSpeech.setText("Please specify your patient name");

                }
            });

        } else {
            databaseReference = FirebaseDatabase.getInstance().getReference().child("Patient");
            databaseReference.addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                    if (dataSnapshot.exists()) {
                        String nameData = dataSnapshot.getKey();
                        patientCompare(name, nameData);
                    } else {
                        startTtsNoHotword("No data");
                    }
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

        }

    }

    private void patientCompare(final String nameText, final String nameData) {
        if (nameText.contains(nameData)) {
            databaseReference = FirebaseDatabase.getInstance().getReference().child("Patient").child(nameData);
            databaseReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        arrayList.clear();
                        for (DataSnapshot dates : dataSnapshot.getChildren()) {
                            String date = dates.getKey();
                            if (!date.equals("Appointment Date")) {
                                arrayList.add(date);
                            }
                        }
                        if (!arrayList.isEmpty()) {
                            String date = arrayList.get(arrayList.size() - 1);
                            getSymptomThruDate(date, nameData);
                        } else {
                            Log.e("array", "Array List missing");
                        }
                    } else {
                        startTtsNoHotword("There is no data");
                        Toast.makeText(MainActivity.this, "No data", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        } else {
            startTtsNoHotword("Can't compare");
            resultSpeech.setText(nameText + nameData);
        }
    }
//To get symptom
    private void getSymptomThruDate(final String date, final String name) {
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Patient").child(name).child(date).child("Symptom");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override


            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot symptoms : dataSnapshot.getChildren()) {
                    String symptom = symptoms.getValue().toString();
                    arrayList.add(symptom);
                }
                String lastSymptom = arrayList.get(arrayList.size() - 1);
                startTtsNoHotword("On " + date + "," + name + " has a symptom: " + lastSymptom);
                resultSpeech.setText("On " + date + "," + name + " has a symptom: " + lastSymptom);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }
}




