/*
 * Copyright 2011 - Churn Labs, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.churnlabs.ffmpegsample;

import java.util.Date;

import android.graphics.Bitmap;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioRecord.OnRecordPositionUpdateListener;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;

public class MainActivity extends SDLActivity {
	static String TAG = "FFMPEGSample";
	
	private native static boolean nativeClassInit();
	//private native void openFile();
	//private native int drawFrame(Bitmap bitmap, byte[] bytes, int iLenArr);
	//private native void drawFrameAt(Bitmap bitmap, int secs);
	
	private native int openStreaming(int iDx,Surface s, String path);
	private native int addStreamingPath(int iDx, String path);
	private native int updateSurface(int iDx, Surface s);
	
	private native int pauseStreaming(int iDx);
	private native int resumeStreaming(int iDx);
	private native int closeStreaming(int iDx);
	
//	private native void startRecord(int bufferSize);
//	private native void recordAudio(byte[] bytes, int bufSize);
//	private native void endRecord();
	
	private native int startRecord(int fd);
	private native int isRecording();
	private native void recordAudio(byte[] bytes, int bufSize);
	private native void endRecord();
	
	private Bitmap mBitmap;
	private int mSecs = 0;
	private long mLastTs;
	ImageView iv;
	boolean mIsPause = true;
	boolean mIsStop = false;
	
	AudioTrack track;
	int bufferSize;
	byte[] bytes;
	
	private AudioRecord recorder;
	int bufferRecSize;
	byte[] bytesRec;
	
	MediaRecorder mediaRecorder;
	
    static {
    	//System.loadLibrary("SDL2");
    	System.loadLibrary("ffmpegutils");
    	if (!nativeClassInit())
			throw new RuntimeException("Native Init Failed");
    }
    
    final Runnable run = new Runnable(){

		@Override
		public void run() {
//			long lDelta = System.currentTimeMillis() - mLastTs;
//			drawFrame(mBitmap);
//			iv.invalidate();
//			//drawFrameAt(mBitmap, mSecs);
//			if(false == mIsPause)
//				iv.postDelayed(this, (long) (1000/30.f));
//			mLastTs = System.currentTimeMillis();
		}};
    
    
    @Override
	protected void onPause() {
    	mIsPause = true;
    	//iv.removeCallbacks(run);
    	pauseStreaming(0);
    	pauseStreaming(1);
    	super.onPause();
	}
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		mIsPause = false;
		//iv.removeCallbacks(run);
		//iv.postDelayed(run, (long) (1000/30.f));
		if(null != track){
			track.stop();
		}
		resumeStreaming(0);
    	resumeStreaming(1);
	}
	
	@Override
	protected void onPostResume() {
		// TODO Auto-generated method stub
		super.onPostResume();
		
		updateSurface(0, SDLActivity.getNativeSurface());
		updateSurface(1, SDLActivity.getNativeSurface2());
	}

	RelativeLayout holder , holder2;
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);

        holder = (RelativeLayout)findViewById(R.id.surface_holder);
        //mLayout = new AbsoluteLayout(this);
        holder.addView(mSurface2);
        holder2 = (RelativeLayout)findViewById(R.id.surface_holder2);
        holder2.addView(mSurface);
        
        iv = (ImageView)findViewById(R.id.frame);
        iv.setScaleType(ScaleType.FIT_CENTER);
        mBitmap = Bitmap.createBitmap(640, 360, Bitmap.Config.ARGB_8888);
        
        //iv.postDelayed(run, (long) (1000/30.f));
        mLastTs = System.currentTimeMillis();
        
        Button btn = (Button)findViewById(R.id.frame_adv);
        btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				//drawFrame(mBitmap);
				//ImageView i = (ImageView)findViewById(R.id.frame);
				//iv.setImageBitmap(mBitmap);
				beginLiveView();
				holder.bringToFront();
			}
		});
        
        Button btnAdd1 = (Button)findViewById(R.id.btn_add1);
        btnAdd1.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				addStreamingPath(0, "mp4:amazons3/wowza2.s3.tokyo/liveorigin/mystream_"+(idx++)+".mp4");
			}
		});
        
        Button btnPause1 = (Button)findViewById(R.id.btn_pause1);
        btnPause1.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				pauseStreaming(0);
			}
		});
        
        Button btnResume1 = (Button)findViewById(R.id.btn_resume1);
        btnResume1.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				resumeStreaming(0);
			}
		});
        
        Button btn_live2 = (Button)findViewById(R.id.frame_live2);
        btn_live2.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				//drawFrame(mBitmap);
				//ImageView i = (ImageView)findViewById(R.id.frame);
				//iv.setImageBitmap(mBitmap);
				beginLiveView2();
				holder2.bringToFront();
			}
		});
        
        Button btnPause2 = (Button)findViewById(R.id.btn_pause2);
        btnPause2.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				pauseStreaming(1);
			}
		});
        
        Button btnResume2 = (Button)findViewById(R.id.btn_resume2);
        btnResume2.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				resumeStreaming(1);
			}
		});
        
        Button btn_liveall = (Button)findViewById(R.id.frame_liveAll);
        btn_liveall.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				//drawFrame(mBitmap);
				//ImageView i = (ImageView)findViewById(R.id.frame);
				//iv.setImageBitmap(mBitmap);
				beginLiveViewAll();
			}
		});
        
        Button btn_fwd = (Button)findViewById(R.id.frame_fwd);
        btn_fwd.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
//				mSecs += 5;
//				drawFrameAt(mBitmap, mSecs);
//				ImageView i = (ImageView)findViewById(R.id.frame);
//				i.setImageBitmap(mBitmap);
				beginToTalk();
			}
		});
        
        Button btn_back = (Button)findViewById(R.id.frame_back);
        btn_back.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
//				mSecs -= 5;
//				drawFrameAt(mBitmap, mSecs);
//				ImageView i = (ImageView)findViewById(R.id.frame);
//				i.setImageBitmap(mBitmap);
				mIsStop = true;
				Log.i(TAG, "btn_back(), click");
				closeStreaming(0);
				closeStreaming(1);
				//System.exit(0);
			}
		});
        
        Button btn_stop1 = (Button)findViewById(R.id.btn_stop1);
        btn_stop1.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mIsStop = true;
				Log.i(TAG, "btn_stop1(), click");
				closeStreaming(0);
			}
		});
        
        Button btn_stop2 = (Button)findViewById(R.id.btn_stop2);
        btn_stop2.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mIsStop = true;
				Log.i(TAG, "btn_stop2(), click");
				closeStreaming(1);
			}
		});
    }
    
    int idx =1;
    
    private void beginLiveView(){
    	mIsStop = false;
         
         new Thread(){
         	public void run(){     
         		idx = 1;
         		//openStreaming(0, SDLActivity.getNativeSurface(), "rtmp://54.250.149.50/live-origin-record/mystream5_aac");
         		openStreaming(0, SDLActivity.getNativeSurface(), "rtmp://54.250.149.50/vods3/_definst_/mp4:amazons3/wowza2.s3.tokyo/liveorigin/mystream_0.mp4");
         	}
         }.start();
         
         new Thread(){
         	public void run(){
         		try {
 					Thread.sleep(2000);
 				} catch (InterruptedException e) {
 					// TODO Auto-generated catch block
 					e.printStackTrace();
 				}
         		long beginTs = -1;
         		
         		while(idx < 1){
         			if(-1 == beginTs){
     					beginTs = System.currentTimeMillis();;
     				}
     				long lDelta = System.currentTimeMillis() - beginTs;
     				//Log.i(TAG, "lDelta:"+lDelta+", idx:"+idx);
     				if(lDelta > 200*idx){
     					addStreamingPath(0, "mp4:amazons3/wowza2.s3.tokyo/liveorigin/mystream_"+(idx++)+".mp4");
     				}
         		}
         		
         		//addStreamingPath(0, "rtmp://54.250.149.50/vods3/_definst_/mp4:amazons3/wowza2.s3.tokyo/liveorigin/mystream_0.mp4");
         	}
         }.start();
    }
    
    private void beginLiveView2(){
    	mIsStop = false;
        
        new Thread(){
        	public void run(){
        		//openStreaming(SDLActivity.getNativeSurface2(), "rtmp://192.168.2.224:1935/myapp/mystream");
                openStreaming(1, SDLActivity.getNativeSurface2(), "rtmp://54.250.149.50/vods3/_definst_/mp4:amazons3/wowza2.s3.tokyo/liveorigin/sample.mp4");
        		//openStreaming(1, SDLActivity.getNativeSurface2(), "rtmp://54.250.149.50/live-origin-record/mystream5_aac");
        		
        	}
        }.start();
    }
    
    private void beginLiveViewAll(){
    	mIsStop = false;
        int sampleRate = 48000;
        bufferSize = AudioTrack.getMinBufferSize(sampleRate,AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
        track = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
//        //Play audio clip
        track.play();
        
        iv.setImageBitmap(mBitmap);
        //iv.setPadding(-300, 0, 300, 0);
        //iv.setScaleType(ScaleType.FIT_CENTER);
        
        new Thread(){
        	public void run(){
        		//openStreaming("rtmp://54.250.149.50/vods3/_definst_/mp4:amazons3/wowza2.s3.tokyo/liveorigin/mystream_0.mp4");
        		openStreaming(1, SDLActivity.getNativeSurface2(), "rtmp://54.250.149.50/vods3/_definst_/mp4:amazons3/wowza2.s3.tokyo/liveorigin/sample.mp4");
        		//openStreaming(SDLActivity.getNativeSurface2(), "rtmp://192.168.2.106:1935/myapp/mystream");
        		
        	}
        }.start();
        
        new Thread(){
        	public void run(){
        		try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        		//openStreaming("rtmp://54.250.149.50/vods3/_definst_/mp4:amazons3/wowza2.s3.tokyo/liveorigin/mystream_0.mp4");
        		idx = 1;
                openStreaming(0, SDLActivity.getNativeSurface(), "rtmp://54.250.149.50/vods3/_definst_/mp4:amazons3/wowza2.s3.tokyo/liveorigin/mystream_0.mp4");
        	}
        }.start();
        
        new Thread(){
        	public void run(){
        		try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        		long beginTs = -1;
        		while(idx < 100){
        			if(-1 == beginTs){
    					beginTs = System.currentTimeMillis();;
    				}
    				long lDelta = System.currentTimeMillis() - beginTs;
    				//Log.i(TAG, "lDelta:"+lDelta+", idx:"+idx);
    				if(lDelta > 200*idx){
    					addStreamingPath(0, "mp4:amazons3/wowza2.s3.tokyo/liveorigin/mystream_"+(idx++)+".mp4");
    				}
        		}
        		
        		//addStreamingPath(0, "rtmp://54.250.149.50/vods3/_definst_/mp4:amazons3/wowza2.s3.tokyo/liveorigin/mystream_0.mp4");
        	}
        }.start();
    }
    
    private void beginToTalk(){
    	if(0 < isRecording()){
    		Log.i(TAG, "beginToTalk(), is recording");
    		return;
    	}
    	mIsStop = false;
    	final int sampleRateRec = 16000; 
        bufferRecSize = AudioRecord.getMinBufferSize(sampleRateRec, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
        Log.i(TAG, "beginToTalk(), rec bufferSize:"+bufferRecSize);
        new Thread(){
        	public void run(){
        		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        		bytesRec = new byte[bufferRecSize];	
        		OnRecordPositionUpdateListener positionUpdater = new OnRecordPositionUpdateListener()
                {
                    @Override
                    public void onPeriodicNotification(AudioRecord recorder)
                    {
                        Date d = new Date();
                        Log.d(TAG, "periodic notification " + d.toLocaleString() + " mili " + d.getTime());
                    }

                    @Override
                    public void onMarkerReached(AudioRecord recorder)
                    {
                        Log.d(TAG, "marker reached");
                    }
                };
                
                if(null != recorder && recorder.getRecordingState() != recorder.RECORDSTATE_STOPPED){
                	recorder.stop();
                	try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                }
                
                recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRateRec, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferRecSize);
                recorder.setPositionNotificationPeriod(sampleRateRec);
                recorder.setRecordPositionUpdateListener(positionUpdater);
                
        		//boolean bBeginFFmpeg = false;
        		recorder.startRecording();
        		while(false == mIsPause && false == mIsStop/* && false == bEndLoop*/){
        			int samplesRead = recorder.read(bytesRec, 0, bufferRecSize);
        			Log.i(TAG, "record, samplesRead:"+samplesRead);
        			if(samplesRead != AudioRecord.ERROR_INVALID_OPERATION)
        				recordAudio(bytesRec, samplesRead);
        		}
        		recorder.stop();
        		endRecord();
        		Log.i(TAG, "record end");
        	}
        }.start();
        
        new Thread(){
			public void run(){
				if(0 < startRecord(0)){
					mIsStop = true;
				}
				Log.i(TAG, "startRecord---- exit");
			}
		}.start();
    }
    
    public void playSound(int size) {  
    	//Log.i(TAG, "playSound");
        if(track.getPlayState()!=AudioTrack.PLAYSTATE_PLAYING)
            track.play();
        //Log.i(TAG, "playSound 1");
        track.write(bytes, 0, size);
        //Log.i(TAG, "playSound 2");
    }
    int samplesRead = 0;
    
    public byte[] getRecordSoundBuf(){
    	do{
    		samplesRead = recorder.read(bytesRec, 0, bufferRecSize);
    	}while(samplesRead != AudioRecord.ERROR_INVALID_OPERATION);
		Log.i(TAG, "getRecordSoundBuf, samplesRead:"+samplesRead);
		return bytesRec;
    }
    
    public int getRecordSampleRead(){
    	return samplesRead;
    }
    
    boolean bEndLoop = false;
    public void stopFeedPipe(){
    	bEndLoop = true;
    }
    
    public void test(){
    	//Log.i(TAG, "test");
    	//Log.i(TAG, "run+++");
    }
    
	ParcelFileDescriptor getPipeFD()
	{
	    final String FUNCTION = "getPipeFD";
	    //FileDescriptor outputPipe = null;
	    ParcelFileDescriptor[] pipe = null;
	    try
	    {
	        pipe = ParcelFileDescriptor.createPipe();
	        
	        //outputPipe = pipe[1].getFileDescriptor();
	    }
	    catch(Exception e)
	    {
	        Log.e("ProcessTest", FUNCTION + " : " + e.getMessage());
	    }

	    return pipe[1];
	}
	
	ParcelFileDescriptor[] getPipe()
	{
	    final String FUNCTION = "getPipeFD";
	    //FileDescriptor outputPipe = null;
	    ParcelFileDescriptor[] pipe = null;
	    try
	    {
	        pipe = ParcelFileDescriptor.createPipe();
	        
	        //outputPipe = pipe[1].getFileDescriptor();
	    }
	    catch(Exception e)
	    {
	        Log.e("ProcessTest", FUNCTION + " : " + e.getMessage());
	    }

	    return pipe;
	}
}