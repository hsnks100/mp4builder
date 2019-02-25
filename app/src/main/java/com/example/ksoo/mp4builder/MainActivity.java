package com.example.ksoo.mp4builder;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.net.Uri;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0)
        {
            for (int i=0; i<grantResults.length; ++i)
            {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED)
                {
                    // 하나라도 거부한다면.
                    new AlertDialog.Builder(this).setTitle("알림").setMessage("권한을 허용해주셔야 앱을 이용할 수 있습니다.")
                            .setPositiveButton("종료", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
//                                        m_oMainActivity.finish();
                                }
                            }).setNegativeButton("권한 설정", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    .setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
                            getApplicationContext().startActivity(intent);
                        }
                    }).setCancelable(false).show();

                    return;
                }
            }
        }
    }
    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    public static final String AUDIO_RECORDING_FILE_NAME = "recording.raw"; // Input PCM file
    public static final String COMPRESSED_AUDIO_FILE_NAME = "builder.mp4"; // Output MP4 file
    public static final String COMPRESSED_AUDIO_FILE_MIME_TYPE = "audio/mp4a-latm";
    public static final int COMPRESSED_AUDIO_FILE_BIT_RATE = 128000; // 128kbps
    public static final int SAMPLING_RATE = 16000;
    public static final int CODEC_TIMEOUT_IN_MS = 5000;
    public static final int BUFFER_SIZE = 88200;

    void r2() {
//        MediaExtractor me = new MediaExtractor();
//        try {
//            me.setDataSource(Environment.getExternalStorageDirectory() + "/" + "video.h264");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        MediaMuxer muxer = null;
        File outputFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + COMPRESSED_AUDIO_FILE_NAME);
        if (outputFile.exists()) outputFile.delete();

        try {
            muxer = new MediaMuxer(outputFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }

        MediaFormat videoFormat = MediaFormat.createVideoFormat("video/avc", 1280, 720);
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, 4000000);

        videoFormat.setByteBuffer("csd-0", ByteBuffer.wrap(new byte[] {
                0x00, 0x00, 0x00, 0x01, 0x67, 0x42, 0x00, 0x1F, (byte)0x9D, (byte)0xB8, 0x14, 0x01, 0x6E, (byte)0x9B, (byte)0x80,
                (byte)0x80, (byte)0x80, (byte)0x81}));
        videoFormat.setByteBuffer("csd-1", ByteBuffer.wrap(new byte[] {
                0x00, 0x00, 0x00, 0x01, 0x68, (byte)0xCE, 0x3C, (byte)0x80 }));


//        videoFormat.setByteBuffer("csd-0", ByteBuffer.wrap(new byte[] {
//                0x00, 0x00, 0x00, 0x01, 0x67, 0x42, 0x00, 0x1F, (byte)0x9D, (byte)0xA8, 0x14, 0x01, 0x6E, (byte)0x9B, (byte)0x80,
//                (byte)0x80, (byte)0x80, (byte)0x81}));
//        videoFormat.setByteBuffer("csd-1", ByteBuffer.wrap(new byte[] {
//                0x00, 0x00, 0x00, 0x01, 0x68, (byte)0xCE, 0x3C, (byte)0x80 }));
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 25);
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 12);
        int videoTrackIndex = muxer.addTrack(videoFormat);
        ByteBuffer inputBuffer = ByteBuffer.allocate(1024*1024*15);
        boolean finished = false;
        String videofilePath = Environment.getExternalStorageDirectory().getPath() + "/" + "video.h264";
        File videoinputFile = new File(videofilePath);
        FileInputStream videoFis = null;
        try {
            videoFis = new FileInputStream(videoinputFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        muxer.start();
        ByteBuffer bb = ByteBuffer.allocate(1024 * 1024 * 15);
        byte[] tempBuffer = new byte[1000000];
        bb.mark();
        // read all
        while(true) {
            try {
                int bb2 = videoFis.read(tempBuffer, 0, tempBuffer.length);
                if(bb2 != -1) {
                    bb.put(tempBuffer, 0, bb2);
                }
                else {
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        int fileSize = bb.position();
        bb.reset();

        // to find 00 00 00 01 sequence.
        int mata[][] = new int[5][256];
        mata[0][0] = 1;
        mata[1][0] = 2;
        mata[2][0] = 3;
        mata[3][1] = 4;
        int step = 0;
        // find first key frame.
//        while(bb.hasRemaining()) {
//            step = mata[step][(bb.get() & 0xFF)];
//            if(step < 4) {
//                step = mata[step][(bb.get() & 0xFF)];
//            }
//            else if(step == 4) {
//                bb.position(bb.position() - 4);
//                break;
//            }
//        }
        step = 0;

        int frameIndex = 0;
        int writeSize = 0;
        while(!finished) {
            int nextPosition = -1;
            int offset = bb.position();
            bb.get(); bb.get(); bb.get(); bb.get();
            byte nalUnit = bb.get();
            while(bb.position() < fileSize) {
                if(step < 4) {
                    step = mata[step][(bb.get() & 0xFF)];
                    nextPosition = bb.position();
                }
                else if(step == 4) {
                    boolean nalVersion = true;
                    byte g = bb.get();
                    if((g & 0x60) == 0x60) {
                        nextPosition = bb.position() - 5;
                        break;
                    }
                }
            }
//            Log.d("TAG", "" + nextPosition + "..." + fileSize);
            step = 0;
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int rNalUnit = (nalUnit & 0x1f);
            Log.d(",,", String.format("nal value: %d", rNalUnit));
            bufferInfo.flags = (nalUnit & 0x1f) == 5 ? MediaCodec.BUFFER_FLAG_KEY_FRAME : 0;
            bufferInfo.offset = offset;
            bufferInfo.size = nextPosition - offset;
            writeSize += bufferInfo.size;
            bufferInfo.presentationTimeUs = 137 + (frameIndex++) * 1000 * 1000 / 25;
            inputBuffer.mark();
            inputBuffer.position(bufferInfo.offset);
            int oldLimit = inputBuffer.limit();
            inputBuffer.limit(bufferInfo.offset + bufferInfo.size);
            muxer.writeSampleData(videoTrackIndex, inputBuffer, bufferInfo);
            inputBuffer.reset();
            inputBuffer.limit(oldLimit);
            if(nextPosition >= fileSize) {
                Log.d("TAG", "" + nextPosition + "..." + fileSize);
                break;
            }
            bb.position(nextPosition);
        }
        muxer.stop();
        muxer.release();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
        };

        if(!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

        r2();

    }
}
