package com.example.ksoo.mp4builder;


import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/*
~ Nilesh Deokar @nieldeokar on 09/17/18 8:11 AM
*/

public class PcmToAacAndMp4 {

    private static final String TAG = PcmToAacAndMp4.class.getSimpleName();

    private static final int SAMPLE_RATE = 16000;
    private static final int SAMPLE_RATE_INDEX = 8;
    private static final int CHANNELS = 1;
    private static final int BIT_RATE = 16000;

    private final int bufferSize;
    private final MediaCodec mediaCodec;
    private final OutputStream outputStream;



    MediaMuxer muxer = null;
    int audioTrackIndex = 0;
    PcmToAacAndMp4(OutputStream outputStream) throws IOException {

        this.bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        this.mediaCodec = createMediaCodec(this.bufferSize);
        this.outputStream = outputStream;

        this.mediaCodec.start();
    }

    public void run() {
        File outputFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "pcm.mp4");
        if (outputFile.exists()) outputFile.delete();

        try {
            muxer = new MediaMuxer(outputFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }
//        MediaFormat audioFormat = MediaFormat.createAudioFormat("audio/mp4a-latm", SAMPLE_RATE, 1);
//        audioFormat.setInteger(MediaFormat.KEY_IS_ADTS, 1);
//        audioFormat.setByteBuffer("csd-0", ByteBuffer.allocate(2).put(new byte[]{(byte) 0x11, (byte)0x90}));

//        audioTrackIndex = muxer.addTrack(audioFormat);
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        ByteBuffer[] codecInputBuffers = mediaCodec.getInputBuffers();
        ByteBuffer[] codecOutputBuffers = mediaCodec.getOutputBuffers();

        String audiofilePath = Environment.getExternalStorageDirectory().getPath() + "/" + "recording.raw";
        File audioinputFile = new File(audiofilePath);
        FileInputStream audioFis = null;
        try {
            audioFis = new FileInputStream(audioinputFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        boolean doneSubmittingInput = false;
        int timestamp = 0;

        try {
            int offset = 0;
            while (!doneSubmittingInput) {

//                boolean success = handleCodecInput(audioRecord, mediaCodec, codecInputBuffers, Thread.currentThread().isAlive());
                int codecInputBufferIndex = mediaCodec.dequeueInputBuffer(10 * 1000);

                if (codecInputBufferIndex != MediaCodec.INFO_TRY_AGAIN_LATER) {
                    ByteBuffer codecBuffer = codecInputBuffers[codecInputBufferIndex];
                    codecBuffer.clear();

                    byte[] tempBuffer = new byte[codecBuffer.capacity()];
                    int readBytes = 0;
                    try {
                        readBytes = audioFis.read(tempBuffer, 0, codecBuffer.capacity());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    codecBuffer.put(tempBuffer, offset, readBytes);
                    timestamp++;
                    if(readBytes < codecBuffer.capacity()) {
                        mediaCodec.queueInputBuffer(codecInputBufferIndex, 0, readBytes,
                                timestamp * 64000 * 2, MediaCodec.BUFFER_FLAG_END_OF_STREAM);

                        doneSubmittingInput = true;
                    }
                    else {
                        mediaCodec.queueInputBuffer(codecInputBufferIndex, 0, readBytes,
                                timestamp * 64000 * 2, 0);
                    }
                }


                while(true) {
                    int codecOutputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 1000);
                    if (codecOutputBufferIndex >= 0) {
                        ByteBuffer encoderOutputBuffer = codecOutputBuffers[codecOutputBufferIndex];
                        encoderOutputBuffer.position(bufferInfo.offset);
                        encoderOutputBuffer.limit(bufferInfo.offset + bufferInfo.size);


                        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            bufferInfo.size = 0;
                        }
                        if(bufferInfo.size > 0) {
//                            bufferInfo.presentationTimeUs = ++timestamp * 100000;
                            muxer.writeSampleData(audioTrackIndex, encoderOutputBuffer, bufferInfo);
                        }
                        if(bufferInfo.size != 0) {
                            byte[] header = createAdtsHeader(bufferInfo.size - bufferInfo.offset);
                            outputStream.write(header);

                            byte[] data = new byte[encoderOutputBuffer.remaining()];
                            encoderOutputBuffer.get(data);
                            outputStream.write(data);
                        }

                        encoderOutputBuffer.clear();

                        mediaCodec.releaseOutputBuffer(codecOutputBufferIndex, false);
                        if((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0){
                            break;
                        }
                    } else if (codecOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        codecOutputBuffers = mediaCodec.getOutputBuffers();
                    } else if (codecOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
//                    encodeStart = true;
                        audioTrackIndex = muxer.addTrack(mediaCodec.getOutputFormat());
                        muxer.start();
                    }
                    else if (codecOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        break;
                    }

                }
            }
        } catch (IOException e) {
            Log.w(TAG, e);
        } finally {
            mediaCodec.stop();
            mediaCodec.release();
            muxer.stop();
            muxer.release();

            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    boolean encodeStart = false;

    private byte[] createAdtsHeader(int length) {
        int frameLength = length + 7;
        byte[] adtsHeader = new byte[7];

        adtsHeader[0] = (byte) 0xFF; // Sync Word
        adtsHeader[1] = (byte) 0xF1; // MPEG-4, Layer (0), No CRC
        adtsHeader[2] = (byte) ((MediaCodecInfo.CodecProfileLevel.AACObjectLC - 1) << 6);
        adtsHeader[2] |= (((byte) SAMPLE_RATE_INDEX) << 2);
        adtsHeader[2] |= (((byte) CHANNELS) >> 2);
        adtsHeader[3] = (byte) (((CHANNELS & 3) << 6) | ((frameLength >> 11) & 0x03));
        adtsHeader[4] = (byte) ((frameLength >> 3) & 0x7FF);
        adtsHeader[5] = (byte) (((frameLength & 0x07) << 5) | 0x1f);
        adtsHeader[6] = (byte) 0xFC;

        return adtsHeader;
    }


    private MediaCodec createMediaCodec(int bufferSize) throws IOException {
        MediaCodec mediaCodec = MediaCodec.createEncoderByType("audio/mp4a-latm");
        MediaFormat mediaFormat = new MediaFormat();

        mediaFormat.setString(MediaFormat.KEY_MIME, "audio/mp4a-latm");
        mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, SAMPLE_RATE);
        mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, CHANNELS);
        mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, bufferSize);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);

        try {
            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (Exception e) {
            Log.w(TAG, e);
            mediaCodec.release();
            throw new IOException(e);
        }

        return mediaCodec;
    }

    interface OnRecorderFailedListener {
        void onRecorderFailed();

        void onRecorderStarted();
    }
}
