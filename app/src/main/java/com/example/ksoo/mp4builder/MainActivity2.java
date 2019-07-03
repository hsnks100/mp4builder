package com.example.ksoo.mp4builder;


import android.Manifest;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

/*
~ Nilesh Deokar @nieldeokar on 09/18/18 6:25 PM
*/

import java.io.File;

import static com.example.ksoo.mp4builder.MainActivity.hasPermissions;

public class MainActivity2 extends AppCompatActivity implements View.OnClickListener {

    AudioRecording mAudioRecording;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO
        };

        if(!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

        Button btnStart = findViewById(R.id.btnStart);
        Button btnStop = findViewById(R.id.btnStop);

        btnStart.setOnClickListener(this);
        btnStop.setOnClickListener(this);

        mAudioRecording = new AudioRecording();

    }

    private void startRecording() {
        AudioRecording.OnAudioRecordListener onRecordListener = new AudioRecording.OnAudioRecordListener() {
            @Override
            public void onRecordFinished() {
                Log.d("MAIN","onFinish ");
            }

            @Override
            public void onError(int e) {
                Log.d("MAIN","onError "+e);
            }

            @Override
            public void onRecordingStarted() {
                Log.d("MAIN","onStart ");

            }
        };

        File filePath = new File(Environment.getExternalStorageDirectory() + "/" + System.currentTimeMillis() + ".aac");
        mAudioRecording.setOnAudioRecordListener(onRecordListener);
        mAudioRecording.setFile(filePath);
        mAudioRecording.startRecording();
    }

    private void stopRecording() {
        if( mAudioRecording != null){
            mAudioRecording.stopRecording(false);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btnStart:
                startRecording();
                break;
            case R.id.btnStop:
                stopRecording();
                break;
        }
    }
}
