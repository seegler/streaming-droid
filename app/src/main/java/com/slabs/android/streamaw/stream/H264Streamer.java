package com.slabs.android.streamaw.stream;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.preference.PreferenceManager;
import android.util.Base64;

import com.slabs.android.log.LogUtil;
import com.slabs.android.streamaw.libstream.NV21Convertor;
import com.slabs.android.streamaw.media.StreamAtControl;
import com.slabs.android.streamaw.stream.protocol.H264Packetizer;
import com.slabs.android.streamaw.stream.protocol.MediaCodecInputStream;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class H264Streamer {
    private static final String TAG = H264Streamer.class.getName();

    String mMimeType = "video/avc";
    int mVideoEncoder = MediaRecorder.VideoEncoder.H264;
    int mCameraImageFormat = ImageFormat.NV21;
    H264Packetizer mPacketizer = new H264Packetizer();
    private int mTTL = 64;

    ////////////////////////
    //StreamAtCamera mCamera;
    StreamAtControl mStreamAtControl;
    int mRtpPort = 5006;
    int mRtcpPort = 5007;

    //////////////////////
    private H264StreamEncoder mEncoder;
    private MP4Config mConfig;
    private MediaCodec mMediaCodec;
    int[] uVideoResolution = new int[]{800,600};
    int mFrameRate = 20;
    int mBitRate= 2500000;
    private long mTimestamp;
    String mOrigin = "192.168.0.100";
    String mDestination = "192.168.0.104";
    private boolean mStreaming;


    //////////////////////////

    public H264Streamer(StreamAtControl mStreamAtControl) {
        this.mStreamAtControl = mStreamAtControl;
    }

    public void init() throws UnknownHostException {
        long uptime = System.currentTimeMillis();
        mTimestamp = (uptime/1000)<<32 & (((uptime-((uptime/1000)*1000))>>32)/1000); // NTP timestamp
        mPacketizer.setDestination(InetAddress.getByName(mDestination), mRtpPort, mRtcpPort);

        //mPacketizer.getRtpSocket().setOutputStream(mOutputStream, mChannelIdentifier); //Use UDP in RTP Socket

        //mMode = mRequestedMode;
        //mQuality = mRequestedQuality.clone();
        mEncoder = H264StreamEncoder.create(
                PreferenceManager.getDefaultSharedPreferences(mStreamAtControl.getApplicationContext()));
        mConfig = new  MP4Config(mEncoder.getB64SPS(), mEncoder.getB64PPS());

    }

    public void createCameraHere(){

    }

    public void start() throws IOException {
        if(!mStreaming) {
            LogUtil.d(TAG,"Start streaming");
            mPacketizer.setTimeToLive(mTTL);

            byte[] pps = Base64.decode(mConfig.getB64PPS(), Base64.NO_WRAP);
            byte[] sps = Base64.decode(mConfig.getB64SPS(), Base64.NO_WRAP);
            ((H264Packetizer) mPacketizer).setStreamParameters(pps, sps);
            LogUtil.d(TAG, "SDP Info: %s", getSessionDescription());
            encodeWithMediaCodec();
        }
    }

    //https://medium.com/@pkurumbudel/android-system-ipc-mechanisms-3d3b46affa3c
    //https://programmer.help/blogs/binder-ibinder-parcel-aidl-of-android-cross-process-communication-mechanism.html

    private void measureFramerate() {
        final Semaphore lock = new Semaphore(0);

        final Camera.PreviewCallback callback = new Camera.PreviewCallback() {
            int i = 0, t = 0;
            long now, oldnow, count = 0;
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                i++;
                now = System.nanoTime()/1000;
                if (i>3) {
                    t += now - oldnow;
                    count++;
                }
                if (i>20) {
                    mFrameRate = (int) (1000000/(t/count)+1);
                    lock.release();
                }
                oldnow = now;
            }
        };

        mStreamAtControl.getStreamCamera().setPreviewCallback(callback);

        try {
            lock.tryAcquire(2, TimeUnit.SECONDS);
            LogUtil.d(TAG,"Actual framerate: "+mFrameRate);
            //save mFrameRate
        } catch (InterruptedException e) {
            LogUtil.e(TAG, e, e.getMessage());
        }

        mStreamAtControl.getStreamCamera().setPreviewCallback(null);

    }

    public void encodeWithMediaCodec() throws IOException {
        createCameraHere();
        measureFramerate();
        //EncoderDebugger debugger = EncoderDebugger.debug(mSettings, mQuality.resX, mQuality.resY);
        final NV21Convertor convertor = mEncoder.getNV21Convertor();
        mMediaCodec = MediaCodec.createByCodecName(mEncoder.getEncoderName());
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(mMimeType, uVideoResolution[0], uVideoResolution[1]);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mFrameRate);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,mEncoder.getEncoderColorFormat());
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mMediaCodec.start();

        Camera.PreviewCallback callback = new Camera.PreviewCallback() {
            long now = System.nanoTime()/1000, oldnow = now, i=0;
            ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                //LogUtil.d(TAG," onPreviewFrame %s", mMediaCodec.getInputBuffers().length);
                oldnow = now;
                now = System.nanoTime()/1000;
                if (i++>3) {
                    i = 0;
                    //Log.d(TAG,"Measured: "+1000000L/(now-oldnow)+" fps.");
                }
                try {
                    int bufferIndex = mMediaCodec.dequeueInputBuffer(500000);
                    if (bufferIndex>=0) {
                        LogUtil.d(TAG, "Process mediacodec buffer %s", bufferIndex);
                        inputBuffers[bufferIndex].clear();
                        if (data == null) LogUtil.e(TAG,"Symptom of the \"Callback buffer was to small\" problem...");
                        else convertor.convert(data, inputBuffers[bufferIndex]);
                        mMediaCodec.queueInputBuffer(bufferIndex, 0, inputBuffers[bufferIndex].position(), now, 0);
                    } else {
                        LogUtil.e(TAG,"No buffer available !");
                    }
                } finally {
                    mStreamAtControl.getStreamCamera().addCallbackBuffer(data);
                }
            }
        };
        for (int i=0;i<10;i++) mStreamAtControl.getStreamCamera().addCallbackBuffer(new byte[convertor.getBufferSize()]);
        mStreamAtControl.getStreamCamera().setPreviewCallbackWithBuffer(callback);

        mPacketizer.setInputStream(new MediaCodecInputStream(mMediaCodec));
        mPacketizer.start();
        mStreaming =true;
    }

    public void stop(){
        LogUtil.d(TAG,"Stop streaming");
        if(mStreamAtControl.getStreamCamera() !=null){
            mStreamAtControl.getStreamCamera().setPreviewCallback(null);
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

    public String getSessionDescription() {
        StringBuilder sessionDescription = new StringBuilder();
        if (mDestination==null) {
            throw new IllegalStateException("setDestination() has not been called !");
        }
        sessionDescription.append("v=0\r\n");
        // TODO: Add IPV6 support
        sessionDescription.append("o=- "+mTimestamp+" "+mTimestamp+" IN IP4 "+mOrigin+"\r\n");
        sessionDescription.append("s=Unnamed\r\n");
        sessionDescription.append("i=N/A\r\n");
        sessionDescription.append("c=IN IP4 "+mDestination+"\r\n");
        // t=0 0 means the session is permanent (we don't know when it will stop)
        sessionDescription.append("t=0 0\r\n");
        sessionDescription.append("a=recvonly\r\n");
        // Prevents two different sessions from using the same peripheral at the same time
        /*if (mAudioStream != null) {
            sessionDescription.append(mAudioStream.getSessionDescription());
            sessionDescription.append("a=control:trackID="+0+"\r\n");
        }*/
        //if (mVideoStream != null) {
            sessionDescription.append(getVideoSessionDescription());
            sessionDescription.append("a=control:trackID="+1+"\r\n");
        //}
        return sessionDescription.toString();
    }

    public synchronized String getVideoSessionDescription() throws IllegalStateException {
        if (mConfig == null) throw new IllegalStateException("You need to call configure() first !");
        return "m=video "+String.valueOf(mRtpPort)+" RTP/AVP 96\r\n" +
                "a=rtpmap:96 H264/90000\r\n" +
                "a=fmtp:96 packetization-mode=1;profile-level-id="+mConfig.getProfileLevel()+";sprop-parameter-sets="+mConfig.getB64SPS()+","+mConfig.getB64PPS()+";\r\n";
    }

}
