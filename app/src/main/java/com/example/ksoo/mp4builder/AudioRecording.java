package com.example.ksoo.mp4builder;

import java.io.File;

import android.util.Log;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/*
~ Nilesh Deokar @nieldeokar on 09/18/18 6:24 PM
*/

public class AudioRecording {

    private static final String TAG = "AudioRecording";
    private File file;
    private OnAudioRecordListener onAudioRecordListener;
    private long mStartingTimeMillis = 0;
    private static final int IO_ERROR = 1;
    private static final int RECORDER_ERROR = 2;
    public static final int FILE_NULL = 3;

    private Thread mRecordingThread;

    public AudioRecording() {
    }

    public void setOnAudioRecordListener(OnAudioRecordListener onAudioRecordListener) {
        this.onAudioRecordListener = onAudioRecordListener;
    }

    public void setFile(String filePath) {
        this.file = new File(filePath);
    }
    public void setFile(File filePath) {
        this.file = filePath;
    }

    // Call this method from Activity onStartButton Click to start recording
    public synchronized void startRecording() {
        if(file == null) {
            onAudioRecordListener.onError(FILE_NULL );
            return;
        }
        mStartingTimeMillis = System.currentTimeMillis();
        try {
            if(mRecordingThread != null) stopRecording(true);


            mRecordingThread = new Thread(new AudioRecordThread(outputStream(file),new AudioRecordThread.OnRecorderFailedListener() {
                @Override
                public void onRecorderFailed() {
                    onAudioRecordListener.onError(RECORDER_ERROR);
                    stopRecording(true);
                }

                @Override
                public void onRecorderStarted() {
                    onAudioRecordListener.onRecordingStarted();
                }
            }));
            mRecordingThread.setName("AudioRecordingThread");



            mRecordingThread.start();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // Call this method from Activity onStopButton Click to stop recording
    public synchronized void stopRecording(Boolean cancel){

        Log.d(TAG, "Recording stopped ");

        if(mRecordingThread != null){

            mRecordingThread.interrupt();
            mRecordingThread = null;


            if (file.length() == 0L) {
                onAudioRecordListener.onError(IO_ERROR);
                return;
            }

            // total recorded time
            long mElapsedMillis = (System.currentTimeMillis() - mStartingTimeMillis);

            if (!cancel) {
                onAudioRecordListener.onRecordFinished();
            } else {
                deleteFile();
            }
        }
    }

    private void deleteFile() {
        if (file != null && file.exists())
            Log.d(TAG, String.format("deleting file success %b ", file.delete()));
    }

    private OutputStream outputStream(File file) {
        if (file == null) {
            throw new RuntimeException("file is null !");
        }
        OutputStream outputStream;
        try {
            outputStream = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(
                    "could not build OutputStream from" + " this file " + file.getName(), e);
        }
        return outputStream;
    }

    public interface OnAudioRecordListener {

        void onRecordFinished();

        void onError(int errorCode);

        void onRecordingStarted();

    }
}
