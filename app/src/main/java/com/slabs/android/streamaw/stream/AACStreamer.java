package com.slabs.android.streamaw.stream;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;

import com.slabs.android.log.LogUtil;
import com.slabs.android.streamaw.media.StreamAtControl;
import com.slabs.android.streamaw.stream.protocol.AACLATMPacketizer;
import com.slabs.android.streamaw.stream.protocol.MediaCodecInputStream;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class AACStreamer {
    private static final String TAG = AACStreamer.class.getName();

    StreamAtControl mStreamAtControl;
    AACLATMPacketizer mPacketizer = new AACLATMPacketizer();
    int mAdudioSource = MediaRecorder.AudioSource.CAMCORDER;
    String mOrigin = "192.168.0.100";
    String mDestination = "192.168.0.104";
    int mRtpPort = 5004;
    int mRtcpPort = 5005;
    private int mProfile, mSamplingRateIndex, mChannel, mConfig;
    private String mSessionDescription = null;
    private int mSamplingRate=8000;
    private int mBitRate=32000;
    private int mTTL = 64;
    private AudioRecord mAudioRecord;
    private MediaCodec mMediaCodec;
    private Thread mMediaTransferThread;
    private boolean mStreaming;



    public AACStreamer(StreamAtControl control){
        this.mStreamAtControl = mStreamAtControl;
        //isAACAvailable();

    }

    public synchronized String getSessionDescription() throws IllegalStateException {
        return mSessionDescription;
    }
    private void setSessionDescription(int config){
        mSessionDescription = "m=audio "+String.valueOf(mRtpPort)+" RTP/AVP 96\r\n" +
                "a=rtpmap:96 mpeg4-generic/"+mSamplingRate+"\r\n"+
                "a=fmtp:96 streamtype=5; profile-level-id=15; mode=AAC-hbr; config="+Integer.toHexString(config)+"; SizeLength=13; IndexLength=3; IndexDeltaLength=3;\r\n";
    }

    public void init() throws UnknownHostException {

        mSamplingRateIndex =11; //for 8K
        mPacketizer.setDestination(InetAddress.getByName(mDestination), mRtpPort, mRtcpPort);
        //mPacketizer.getRtpSocket().setOutputStream(mOutputStream, mChannelIdentifier);
        mProfile = 2; // AAC LC
        mChannel = 1;
        mConfig = (mProfile & 0x1F) << 11 | (mSamplingRateIndex & 0x0F) << 7 | (mChannel & 0x0F) << 3;
        setSessionDescription(mConfig);

    }

    public void start() throws IOException {
        if(!mStreaming){
            LogUtil.d(TAG,"Start streaming");
            mPacketizer.setTimeToLive(mTTL);
            encodeWithMediaCodec();

        }
    }

    public void stop(){
        LogUtil.d(TAG, "Stop streaming...");
        if(mMediaTransferThread !=null) {
            mMediaTransferThread.interrupt();
            mMediaTransferThread = null;
        }
        if(mAudioRecord !=null){
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;
        }
        if(mMediaCodec != null){
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
        }
        if(mPacketizer!=null) {
            mPacketizer.stop();
        }
        mStreaming=false;
    }

    public void encodeWithMediaCodec() throws IOException {
        final int bufferSize = AudioRecord.getMinBufferSize(mSamplingRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)*2;

        ((AACLATMPacketizer)mPacketizer).setSamplingRate(mSamplingRate);

        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,mSamplingRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        mMediaCodec = MediaCodec.createEncoderByType("audio/mp4a-latm");
        MediaFormat format = new MediaFormat();
        format.setString(MediaFormat.KEY_MIME, "audio/mp4a-latm");
        format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, mSamplingRate);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, bufferSize);
        mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mAudioRecord.startRecording();
        mMediaCodec.start();

        final MediaCodecInputStream inputStream = new MediaCodecInputStream(mMediaCodec);
        final ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();

        mMediaTransferThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int len = 0, bufferIndex = 0;
                try {
                    while (!Thread.interrupted()) {
                        bufferIndex = mMediaCodec.dequeueInputBuffer(10000);
                        if (bufferIndex>=0) {
                            inputBuffers[bufferIndex].clear();
                            len = mAudioRecord.read(inputBuffers[bufferIndex], bufferSize);
                            if (len ==  AudioRecord.ERROR_INVALID_OPERATION || len == AudioRecord.ERROR_BAD_VALUE) {
                                LogUtil.d(TAG,"An error occured with the AudioRecord API !");
                            } else {
                                LogUtil.d(TAG,"Pushing raw audio to the decoder: len="+len+" bs: "+inputBuffers[bufferIndex].capacity());
                                mMediaCodec.queueInputBuffer(bufferIndex, 0, len, System.nanoTime()/1000, 0);
                            }
                        }
                    }
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
        });

        mMediaTransferThread.start();

        // The packetizer encapsulates this stream in an RTP stream and send it over the network
        mPacketizer.setInputStream(inputStream);
        mPacketizer.start();

        mStreaming = true;
    }
}
