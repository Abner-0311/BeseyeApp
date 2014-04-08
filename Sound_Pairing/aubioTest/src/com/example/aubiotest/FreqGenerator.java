package com.example.aubiotest;

import static com.example.aubiotest.AubioTestConfig.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class FreqGenerator {
	final static String TAG = AubioTestActivity.TAG;
	final static private double TWO_PI = 2 * Math.PI;
	final static private double HALF_PI = 0.5 * Math.PI;
	//final static private double ONE_THRID_PI = Math.PI / 3;
	//final static private double ONE_SIXTH_PI = Math.PI / 6;
	
	final static private double TARGET_ANGLE = HALF_PI/128;
	//final static private double BASE_ANGLE = HALF_PI - TARGET_ANGLE;
	
    //static private int sCurCodePos = -1;
    
    static private FreqGenerator sFreqGenerator;
    
    private FreqGenerator(){}
    
    public static FreqGenerator getInstance(){
    	if(null == sFreqGenerator)
    		sFreqGenerator= new FreqGenerator();
    	
    	return sFreqGenerator;
    }
    
    private OnPlayToneCallback mOnPlayToneCallback;
    private AudioTrack mToneAudioTrack;
    
//    public void setSampleRate(int iSampleRate){
//    	if(0 < iSampleRate)
//    		siSampleRatePlay = iSampleRate;
//    }
//    
//    public void setDuration(float fDuration){
//    	if(0 < fDuration)
//    		sfDuration = fDuration;
//    }
    
    public void setOnPlayToneCallback(OnPlayToneCallback cb){
    	mOnPlayToneCallback = cb;
    }
    
    static public interface OnPlayToneCallback{
    	public void onStartGen(String strCode);
    	public void onStopGen(String strCode);
    	
    	public void onCurFreqChanged(double dFreq);
    	public void onErrCorrectionCode(String strCode, String strEC, String strEncodeMark);
    }
    
//    public void playCode(final String strCodeInput, final boolean bNeedEncode){
//    	final Thread thread = new Thread(new Runnable() {
//            public void run() {
//            	String strEncode = "";
//            	//try {
//            		strEncode = bNeedEncode?encode(strCodeInput):strCodeInput;
////				} catch (WriterException e) {
////					// TODO Auto-generated catch block
////					e.printStackTrace();
////				}
//            	
//            	final String strECCode = strEncode.substring(strCodeInput.length());
//            	String strEncodeMark = encodeConsecutiveDigits(strEncode);
//            	
//            	if(null != mOnPlayToneCallback)
//            		mOnPlayToneCallback.onErrCorrectionCode(strCodeInput, strECCode,strEncodeMark);
//            	
//            	String strCode = bNeedEncode?(PREFIX_DECODE+strEncode+POSTFIX_DECODE):strEncode;
//            	int iCodeLen = strCode.length();
//            	float duration = TONE_DURATION;
//            	int numSamples = (int) (duration * SAMPLE_RATE_PLAY);
////            	if(numSamples < bufferSize){
////            		Log.i(TAG, "run(), make numSamples from "+numSamples+" to "+bufferSize );
////            		numSamples = bufferSize;
////            	}
//            	
//            	int iTotalSamples = numSamples*iCodeLen;
//            	
//            	double[] sample = new double[iTotalSamples];
//            	byte[] generatedSnd = new byte[2 * iTotalSamples];
//            	
//            	if(null == mToneAudioTrack)
//            		mToneAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
//            									SAMPLE_RATE_PLAY, 
//								                AudioFormat.CHANNEL_CONFIGURATION_MONO,
//								                AudioFormat.ENCODING_PCM_16BIT, 
//								                generatedSnd.length,
//								                AudioTrack.MODE_STREAM);
//            	
//            	
//            	int i, iCurCodeIndex = 0;
//            	double dFreq = 0.0;
//                for (i = 0; i < iTotalSamples; ++i) {
//                	if(0 == i % numSamples){
//                		iCurCodeIndex = i/numSamples;
//                		//Log.i(TAG, "run(), strCode["+iCurCodeIndex+"] = "+strCode.charAt(iCurCodeIndex));
//                		dFreq = sAlphabetTable.get(strCode.substring(iCurCodeIndex, iCurCodeIndex+1));
//                		//Log.i(TAG, "run(), freq:"+dFreq );
//                	}
//                    sample[i] = Math.sin(TWO_PI * (i) * dFreq / SAMPLE_RATE_PLAY);
//                    //Log.i(TAG, "run(), sample["+i+"] = "+sample[i] );
//                }
//                
////	              int idx = 0;
////	              int ramp = numSamples / 25 ;                                    // Amplitude ramp as a percent of sample count
////	              i=0;
////	              for (final double dVal : sample) {
////	            	  if(i % numSamples == 0)
////	            		  i=0;
////	            	  
////	            	  double dRatio = 1.d;
////	  	              if(i< ramp){
////	  	            	  dRatio = i/ramp;// Ramp amplitude up (to avoid clicks)
////	  	              }else if(i >= (numSamples - ramp) && i< numSamples){
////	  	            	  dRatio = (numSamples-i)/ramp;// Ramp down to zero
////	  	              }
////	  	      			
////	  	              final short val = (short) ((dVal * 32767 * dRatio));
////	  	      			
////	  	      		  // in 16 bit wav PCM, first byte is the low order byte
////	  	      		  generatedSnd[idx++] = (byte) (val & 0x00ff);
////	  	      		  generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
////
////	            	  i++;
////	              }
//	              
//
//                // convert to 16 bit pcm sound array
//                // assumes the sample buffer is normalised.
//                int idx = 0;
//                for (final double dVal : sample) {
//                    // scale to maximum amplitude
//                    final short val = (short) ((dVal * 32767));
//                    // in 16 bit wav PCM, first byte is the low order byte
//                    generatedSnd[idx++] = (byte) (val & 0x00ff);
//                    generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
//
//                }
//            	
//                mToneAudioTrack.write(generatedSnd, 0, generatedSnd.length);
//	            
//	            while(mToneAudioTrack.getState() != AudioTrack.STATE_INITIALIZED){
//	            	try {
//						Thread.sleep(10);
//					} catch (InterruptedException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//	            }
//	            	
//	            if(null != mOnPlayToneCallback)
//            		mOnPlayToneCallback.onStartGen(strCodeInput);
//	            
//	            //if(audioTrack.getPlayState() != AudioTrack.PLAYSTATE_PLAYING)
//            	mToneAudioTrack.play();
//            
//            	if(null != mOnPlayToneCallback)
//            		mOnPlayToneCallback.onCurFreqChanged(dFreq);
//            	
//            	if(null != mOnPlayToneCallback)
//            		mOnPlayToneCallback.onStopGen(strCodeInput);
//            }
//        });
//        thread.start();
//    }
//    
//    synchronized public void stopPlay(){
//    	if(null !=mToneAudioTrack && mToneAudioTrack.getPlayState() != AudioTrack.PLAYSTATE_STOPPED){
//    		mToneAudioTrack.stop();
//    		mToneAudioTrack = null;
//    	}
//    }
    
    static public String getECCode(String strCode){
    	String strRet = "";
    	String strEncode = "";
    	//try {
    		strEncode = encode(strCode);
//		} catch (WriterException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
    	
    	strRet = strEncode.substring(strCode.length());
    	return strRet;
    }
    
    private Thread mPlayCodeThread = null;
    private boolean mbStopPlayCodeThread = false;
    static private class EncodeItm{
    	public EncodeItm(String strCodeInput, String strECCode, String strEncodeMark, String strEncode) {
			super();
			this.strCodeInput = strCodeInput;
			this.strECCode = strECCode;
			this.strEncodeMark = strEncodeMark;
			this.strEncode = strEncode;
		}
		String strCodeInput;
    	String strECCode;
    	String strEncodeMark;
    	String strEncode;
    }
    final private List<EncodeItm> mlstEncodeList = new ArrayList<EncodeItm>();
    
    synchronized public void playCode2(final String strCodeInput, final boolean bNeedEncode){
    	String strEncode = "";
    	//try {
    		strEncode = bNeedEncode?encode(strCodeInput):strCodeInput;
//		} catch (WriterException e) {
//			e.printStackTrace();
//		}
    	
    	final String strECCode = strEncode.substring(strCodeInput.length());
    	
    	String strEncodeMark = encodeConsecutiveDigits(strEncode);
    	//String strEncodeMark = encodeConsecutiveDigits(strCodeInput)/*+DIVIDER*/+encodeConsecutiveDigits(strECCode);
    	
    	String strCode = bNeedEncode?(PREFIX_DECODE+(PRE_EMPTY?"X":"")+strEncodeMark+""+POSTFIX_DECODE+sCodeTable.get(sCodeTable.size()-4)):strEncode;
    	
    	synchronized(mlstEncodeList){
    		mlstEncodeList.add(new EncodeItm(strCodeInput, strECCode, strEncodeMark, strCode));
    		mlstEncodeList.notify();
    	}
    	
    	if(null == mPlayCodeThread){
    		mPlayCodeThread = new Thread(new Runnable() {
                public void run() {
                	//android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                	
                	mbStopPlayCodeThread = false;
                	final int iMinBufSize = AudioTrack.getMinBufferSize(SAMPLE_RATE_PLAY, 
					    								                AudioFormat.CHANNEL_CONFIGURATION_MONO,
					    								                AudioFormat.ENCODING_PCM_16BIT);
                	
                	
                	Log.i(TAG, "run(), iMinBufSize "+iMinBufSize+", siSampleRatePlay = "+SAMPLE_RATE_PLAY);
                	
                	double[] sample = new double[iMinBufSize/2];
                	byte[] generatedSnd = new byte[iMinBufSize];
                	
                	if(null == mToneAudioTrack)
                		mToneAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
	                									SAMPLE_RATE_PLAY, 
	    								                AudioFormat.CHANNEL_CONFIGURATION_MONO,
	    								                AudioFormat.ENCODING_PCM_16BIT, 
	    								                generatedSnd.length,
	    								                AudioTrack.MODE_STREAM);
                	
                	while(mToneAudioTrack.getState() != AudioTrack.STATE_INITIALIZED){
    	            	try {
    						Thread.sleep(10);
    					} catch (InterruptedException e) {
    						e.printStackTrace();
    					}
    	            }
    	            	
                	mToneAudioTrack.play();
                	
                	String strLastCode = null;
                	
                	while(!mbStopPlayCodeThread){
                		synchronized(mlstEncodeList){
                    		if(0 == mlstEncodeList.size()){
                    			try {
                    				Log.e(TAG, "run(), mPlayCodeThread begin to wait" );
    								mlstEncodeList.wait();
    								Log.e(TAG, "run(), mPlayCodeThread exit to wait" );
    							} catch (InterruptedException e) {
    								e.printStackTrace();
    							}
                    		}
                    		
                    		{
                    			try {
                    				Thread.sleep(1500L);
    							} catch (InterruptedException e) {
    								e.printStackTrace();
    							}
                    		}
                    		
                    		EncodeItm itmCode = mlstEncodeList.remove(0);
                    		
                    		//I suspect that there is redundant inputcode from sender, workaround temporarily
                    		if(null != strLastCode && strLastCode.equals(itmCode.strCodeInput)){
                    			Log.e(TAG, "run(), ++++++++++++++++++++++++++++++++++++++++++++++++++++++ same as strLastCode:"+strLastCode );
                    			continue;
                    		}
                    		strLastCode = itmCode.strCodeInput;
                    		
                    		if(null != mOnPlayToneCallback)
                        		mOnPlayToneCallback.onStartGen(itmCode.strCodeInput);
                    		
                    		if(null != mOnPlayToneCallback)
                        		mOnPlayToneCallback.onErrCorrectionCode(itmCode.strCodeInput, itmCode.strECCode, itmCode.strEncodeMark);
                    		
                    		int iCodeLen = itmCode.strEncode.length();
                        	float duration = TONE_DURATION;
                        	int numSamples = (int) ((duration * SAMPLE_RATE_PLAY)*SILENCE_RATIO);
                        	int realNumSample = (int) ((duration * SAMPLE_RATE_PLAY));
                        	int iTotalSamples = numSamples*iCodeLen;
                        	int iBufLen = sample.length;
                        	
                        	int i, iCurCodeIndex = 0;
                        	double dFreq = 0.0;
                        	int iDeltaRange = (int) (0.1*realNumSample);
                        	int iDoubleDeltaRange = iDeltaRange*4;
                        	//Log.i(TAG, "run(), iDeltaRange = "+iDeltaRange);
                        	
                        	float fBaseAmpRatio = 1.0f;
                        	
                            for (i = 0; i < iTotalSamples; ++i) {
                            	if(0 < i && 0 == i % iBufLen){
                            		writeTone(sample, generatedSnd, iBufLen);
                            		java.util.Arrays.fill(sample,(short) 0);
                            		java.util.Arrays.fill(generatedSnd,(byte) 0);
                            	}
                            	
                            	if(0 == i % numSamples){
                            		iCurCodeIndex = i/numSamples;
                            		//Log.i(TAG, "run(), strCode["+iCurCodeIndex+"] = "+strCode.charAt(iCurCodeIndex));
                            		String strMatch = itmCode.strEncode.substring(iCurCodeIndex, iCurCodeIndex+1);
                            		Double dValue = sAlphabetTable.get(strMatch);
                            		dFreq = (null == dValue)?0.0:dValue;
                            		
                            		if(AMP_TUNE){
                            			fBaseAmpRatio = 1.0f;
                            			int idxFound = sCodeTable.indexOf(strMatch);
                            			if(0 <= idxFound && idxFound < AMP_BASE_RATIO.length){
                            				fBaseAmpRatio = AMP_BASE_RATIO[idxFound];
                            				Log.d(TAG, "run(), fBaseAmpRatio= "+fBaseAmpRatio+" for "+strMatch);
                            			}
                            		}
                            	}
                            	
                            	int iCurPos = (i % numSamples);
//                            	int iBaseDelta  = (realNumSample - iDeltaRange*2 );
//                            	double dBaseVal = Math.sin(BASE_ANGLE);
//                            	double dBaseRestVal = (1 - Math.sin(BASE_ANGLE));
                            	
                            	if(realNumSample < iCurPos){
                            		sample[i% iBufLen] = 0;
                            	}else{
                            		float fRatio = 1.0f;
                            		
                            		/*if(iDeltaRange >= iCurPos){
                            			fRatio = (float) (0.10f + 0.90f * Math.sin(HALF_PI * (((float) iCurPos) / iDeltaRange)));
                            		}else */if((realNumSample - iDoubleDeltaRange) <= iCurPos){
                            			fRatio = (float) (0.10f + 0.90f * ( 1 - Math.sin(HALF_PI * (((float) (iCurPos - (realNumSample - iDoubleDeltaRange) )) /(iDoubleDeltaRange)))));
                            		}/*else
                            			fRatio = (float) (0.2f + 0.8f * ((Math.sin(BASE_ANGLE + TARGET_ANGLE*(((float) iCurPos) / iBaseDelta)) - dBaseVal)/dBaseRestVal));
                            			*/
                            		//Log.i(TAG, "run(), iCurPos = "+iCurPos+", fRatio = "+fRatio );
                            		
                            		sample[i% iBufLen] = fBaseAmpRatio * fRatio * Math.sin(TWO_PI * (i) * dFreq / SAMPLE_RATE_PLAY);
                            	}
                                
                            }
                            
                            if(0 < iTotalSamples%iBufLen){
                            	writeTone(sample, generatedSnd, iTotalSamples%iBufLen);
                            	java.util.Arrays.fill(sample,(short) 0);
                        		java.util.Arrays.fill(generatedSnd,(byte) 0);
                            }
                            
//                        	if(null != mOnPlayToneCallback)
//                        		mOnPlayToneCallback.onCurFreqChanged(dFreq);
                        	
                        	if(null != mOnPlayToneCallback)
                        		mOnPlayToneCallback.onStopGen(itmCode.strEncode);
                    	}
                	}
                }
            });
        	mPlayCodeThread.start();
    	}
    }
   
    synchronized public void stopPlay2(){
    	if(null !=mToneAudioTrack && mToneAudioTrack.getPlayState() != AudioTrack.PLAYSTATE_STOPPED){
    		mToneAudioTrack.stop();
    		mToneAudioTrack = null;
    	}
    	mbStopPlayCodeThread = true;
    	mPlayCodeThread = null;
    }
    
    //self testing
    synchronized public void playCode3(String strCodeInput, final boolean bNeedEncode){
    	//strCodeInput = "0011111011010010";
    	String strEncode = "";
    	//try {
    		strEncode = bNeedEncode?encode(strCodeInput):strCodeInput;
//		} catch (WriterException e) {
//			e.printStackTrace();
//		}
    	
    	final String strECCode = strEncode.substring(strCodeInput.length());
    	
    	String strEncodeMark = encodeConsecutiveDigits(strEncode);
    	//String strEncodeMark = encodeConsecutiveDigits(strCodeInput)/*+DIVIDER*/+encodeConsecutiveDigits(strECCode);
    	
    	String strCode = bNeedEncode?("X"+PREFIX_DECODE+(PRE_EMPTY?"X":"")+strEncodeMark+""+POSTFIX_DECODE+sCodeTable.get(sCodeTable.size()-4))+"XXXXXX":strEncode;
    	
    	synchronized(mlstEncodeList){
    		mlstEncodeList.add(new EncodeItm(strCodeInput, strECCode, strEncodeMark, strCode));
    		mlstEncodeList.notify();
    	}
    	
    	if(null == mPlayCodeThread){
    		mPlayCodeThread = new Thread(new Runnable() {
                public void run() {
                	int iShiftIdx = 0;
                	mbStopPlayCodeThread = false;
                	final int iMinBufSize = TONE_SIZE_REC*2;/*AudioTrack.getMinBufferSize(miSampleRate, 
					    								                AudioFormat.CHANNEL_CONFIGURATION_MONO,
					    								                AudioFormat.ENCODING_PCM_16BIT);*/
                	
                	Log.i(TAG, "run(), iMinBufSize "+iMinBufSize );
                	
                	double[] sample = new double[iMinBufSize/2];
                	
                	while(!mbStopPlayCodeThread){
                		synchronized(mlstEncodeList){
                			iShiftIdx++;
                    		if(0 == mlstEncodeList.size()){
                    			try {
                    				Log.e(TAG, "run(), mPlayCodeThread begin to wait" );
    								mlstEncodeList.wait();
    								Log.e(TAG, "run(), mPlayCodeThread exit to wait" );
    							} catch (InterruptedException e) {
    								e.printStackTrace();
    							}
                    		}
                    		
                    		EncodeItm itmCode = mlstEncodeList.remove(0);
                    		
                    		if(null != mOnPlayToneCallback)
                        		mOnPlayToneCallback.onStartGen(itmCode.strCodeInput);
                    		
                    		if(null != mOnPlayToneCallback)
                        		mOnPlayToneCallback.onErrCorrectionCode(itmCode.strCodeInput, itmCode.strECCode, itmCode.strEncodeMark);
                        	
                        	int iCodeLen = itmCode.strEncode.length();
                        	float duration = TONE_DURATION;
                        	int numSamples = TONE_SIZE_REC;//(int) ((duration * SAMPLE_RATE_REC)*SILENCE_RATIO);
                        	int realNumSample = TONE_SIZE_REC;//(int) ((duration * SAMPLE_RATE_REC));
                        	int iTotalSamples = numSamples*iCodeLen;
                        	int iBufLen = sample.length;
                        	
                        	int i, iCurCodeIndex = 0;
                        	final int iDelta = (int)(((iShiftIdx%10)/10.0f)*FRAME_SIZE_REC);
                        	Log.e(TAG, "playCode3(), iDelta:"+iDelta );
                        	String iIndex = itmCode.strEncode.substring(iCurCodeIndex, iCurCodeIndex+1);
                    		Double dValue = sAlphabetTable.get(iIndex);
                    		double dFreq = (null == dValue)?0.0:dValue;
                        	//double dFreq = sAlphabetTable.get(itmCode.strEncode.substring(iCurCodeIndex, iCurCodeIndex+1));
                        	//int iEmptyCount = 0;
                        	
                        	int iDeltaRange = (int) (0.1*realNumSample);
                        	int iDoubleDeltaRange = iDeltaRange*4;
                        	float fBaseAmpRatio = 1.0f;
                        	
                            for (i = 0; i < iTotalSamples+iDelta; ++i) {
                            	if(0 < i && 0 == i % iBufLen){
                            		writeToneToBuf(sample, iBufLen);
                            		java.util.Arrays.fill(sample,(short) 0);
                            		//java.util.Arrays.fill(generatedSnd,(byte) 0);
                            	}
                            	
                            	int iCurPos = ((i-iDelta) % numSamples);
                 
                            	if(0 == iCurPos){
                            		iCurCodeIndex = Math.min(itmCode.strEncode.length()-1, (i-iDelta)/numSamples);
                            		//iEmptyCount = (int) (((iCurCodeIndex%10)/10.0f)*FRAME_SIZE_REC);
                            		
                            		iIndex = itmCode.strEncode.substring(iCurCodeIndex, iCurCodeIndex+1);
                            		dValue = sAlphabetTable.get(iIndex);
                            		dFreq = (null == dValue)?0.0:dValue;
                            		//dFreq = sAlphabetTable.get(itmCode.strEncode.substring(iCurCodeIndex, iCurCodeIndex+1));
                            		Log.i(TAG, "run(), strCode["+iCurCodeIndex+"] = "+itmCode.strEncode.substring(iCurCodeIndex, iCurCodeIndex+1)+", freq:"+dFreq);
                            		//Log.i(TAG, "run(), freq:"+dFreq );
                            		
                            		if(AMP_TUNE){
                            			fBaseAmpRatio = 1.0f;
                            			int idxFound = sCodeTable.indexOf(iIndex);
                            			if(0 <= idxFound && idxFound < AMP_BASE_RATIO.length){
                            				fBaseAmpRatio = AMP_BASE_RATIO[idxFound];
                            			}
                            		}
                            	}
                            	
                            	if(i < iDelta){
                            		sample[i% iBufLen] = 0.000000000000000f;
                            	}else if(realNumSample < iCurPos){
                            		sample[i% iBufLen] = 0.000000000000000f;
                            	}else{
//                            		if(iEmptyCount >= (i % FRAME_SIZE_REC))
//                            			sample[i% iBufLen] = 0;//Math.sin(TWO_PI * (i) * 1230.0 / SAMPLE_RATE_REC);
//                            		else
                            			//sample[i% iBufLen] = Math.sin(TWO_PI * (i-iDelta) * dFreq / SAMPLE_RATE_REC);
                            			
                        			float fRatio = 1.0f;
                            		if((realNumSample - iDoubleDeltaRange) <= iCurPos)
                            			fRatio = (float) (0.10f + 0.90f * ( 1 - Math.sin(HALF_PI * (((float) (iCurPos - (realNumSample - iDoubleDeltaRange) )) /(iDoubleDeltaRange)))));
                            	
                            		sample[i% iBufLen] = fBaseAmpRatio * fRatio * Math.sin(TWO_PI * (i-iDelta) * dFreq / SAMPLE_RATE_REC);
                            	}
                                //Log.i(TAG, "run(), sample["+i+"] = "+sample[i] );
                            }
                            
                            if(0 < (iTotalSamples+iDelta)%iBufLen){
                            	writeToneToBuf(sample, iTotalSamples%iBufLen);
                            	java.util.Arrays.fill(sample,(short) 0);
                        		//java.util.Arrays.fill(generatedSnd,(byte) 0);
                            }
                            
//                        	if(null != mOnPlayToneCallback)
//                        		mOnPlayToneCallback.onCurFreqChanged(dFreq);
                        	
                        	if(null != mOnPlayToneCallback)
                        		mOnPlayToneCallback.onStopGen(itmCode.strEncode);
                    	}
                	}
                }
            });
        	mPlayCodeThread.start();
    	}
    }
    
    private void writeTone(double[] sample, byte[] generatedSnd, int iLen){
    	if(null != mToneAudioTrack){
    		// convert to 16 bit pcm sound array
            // assumes the sample buffer is normalised.
            int idx = 0;
            for (int i =0; i< iLen;i++) {
                // scale to maximum amplitude
                final short val = (short) ((sample[i] * 32767));
                // in 16 bit wav PCM, first byte is the low order byte
                generatedSnd[idx++] = (byte) (val & 0x00ff);
                generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);

            }
    		mToneAudioTrack.write(generatedSnd, 0, iLen*2);
    	}
    }
    
    static long sTs = 0;
    static public Object sSyncObj = new Object();
    private void writeToneToBuf(double[] sample, int iLen){
    	// convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
    	short[] shortsRec = null;
    	while(null == (shortsRec = AudioBufferMgr.getInstance().getAvailableBuf())){
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	int iLenRec = shortsRec.length;
        int idx = 0, i =0;
        for (i =0; i< iLen;i++) {
        	if(null == shortsRec){
        		while(null == (shortsRec = AudioBufferMgr.getInstance().getAvailableBuf())){
        			try {
        				Thread.sleep(10);
        			} catch (InterruptedException e) {
        				// TODO Auto-generated catch block
        				e.printStackTrace();
        			}
            	}
        	}
        	
            // scale to maximum amplitude
        	shortsRec[i%iLenRec] = (short) ((sample[i] * 32767));

        	if(0 == (i+1)%iLenRec){
        		sTs+=FRAME_TS;
                AudioBufferMgr.getInstance().addToDataBuf(sTs, shortsRec, iLenRec);
                synchronized(sSyncObj){
                	try {
						sSyncObj.wait(20);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                }
                shortsRec = null;
        	}
        }
        
        if(null != shortsRec){
        	sTs+=FRAME_TS;
            AudioBufferMgr.getInstance().addToDataBuf(sTs, shortsRec, i%iLenRec);
            synchronized(sSyncObj){
            	try {
					sSyncObj.wait(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
        }        
    }
    
    static final String DEFAULT_BYTE_MODE_ENCODING = "ISO-8859-1";
    
    public static int getNumOfEcBytes(int numDataBytes){
    	return 2*((int) Math.ceil(EC_RATIO*numDataBytes));//6;
    }
    
    public static String encode(String content) {   
    	Log.e(TAG, "encode(), content= ["+content+"]" );
    	if(null == content || 0 == content.length()){
    		Log.i(TAG, "encode(), content is empty" );
    		return "";
    	}

    	int iMultiply = AubioTestConfig.getMultiplyByFFTYPE();
    	int iPower = AubioTestConfig.getPowerByFFTYPE();
    	    	
        int numDataBytes = content.length();
        int numEcBytesInBlock = getNumOfEcBytes(numDataBytes/iMultiply)*iMultiply;
        
        int[] toEncode = null;
        if(1 == AubioTestConfig.getMultiplyByFFTYPE()){
        	toEncode = new int[numDataBytes + numEcBytesInBlock];
        	int iLen = content.length();
        	for(int i =0;i < iLen;i++){
        		toEncode[i] = sCodeTable.indexOf(content.substring(i, i+1));
        	}
        }else{
        	toEncode = new int[(numDataBytes + numEcBytesInBlock)/iMultiply];
        	int iLen = content.length()/iMultiply;
        	for(int i =0;i < iLen;i++){
        		toEncode[i] = 0;
        		for(int j = 0;j < iMultiply;j++){
        			toEncode[i] <<= iPower;
        			toEncode[i]+= sCodeTable.indexOf(content.substring(i*iMultiply+j, i*iMultiply+j+1));
        		}
        		Log.d(TAG, "encode(), toEncode["+i+"]= "+toEncode[i] );
        	}
        }
   
        //new ReedSolomonEncoder(AubioTestConfig.CUR_GF).encode(toEncode, numEcBytesInBlock/iMultiply);
        AubioTestActivity.encodeRS(toEncode, toEncode.length, numEcBytesInBlock/iMultiply);
        
        int iLen = toEncode.length;
    	for(int i =0;i < iLen;i++){
    		Log.d(TAG, "encode(), after encode,  toEncode["+i+"]= "+toEncode[i] );
    	}

        StringBuilder ret = new StringBuilder();
        for(int idx = 0; idx < toEncode.length; idx++){
        	if(1 == AubioTestConfig.getMultiplyByFFTYPE()){
        		ret.append(sCodeTable.get(toEncode[idx]));
        	}else{
        		for(int j = iMultiply-1 ;j >=0 ;j--){
        			ret.insert(idx*iMultiply, sCodeTable.get(((toEncode[idx]) & ((0x1<<iPower) -1))));
        			toEncode[idx] >>= iPower;
        		}
        	}
        }
        
        Log.e(TAG, "encode(), ret=     ["+ret.toString()+"]");
        
        return ret.toString();   
    }
    
//    public static String encode2(String content)  {   	
//    	Log.e(TAG, "encode2(), content= ["+content+"]" );
//    	if(null == content || 0 == content.length()){
//    		Log.i(TAG, "encode2(), content is empty" );
//    		return "";
//    	}
//    	
//        int numDataBytes = content.length();
//        int numEcBytesInBlock = getNumOfEcBytes(numDataBytes);
//        
//        int[] toEncode = new int[numDataBytes + numEcBytesInBlock];
//    	int iLen = content.length();
//    	for(int i =0;i < iLen;i++){
//    		toEncode[i] = sCodeTable.indexOf(content.substring(i, i+1));
//    	}
//   
//       // new ReedSolomonEncoder(AubioTestConfig.CUR_GF).encode(toEncode, numEcBytesInBlock);
//        
//        int iLenEncode = toEncode.length;
//        StringBuilder ret = new StringBuilder();        
//        for(int idx = 0; idx < iLenEncode; idx++){
//        	ret.append(sCodeTable.get(toEncode[idx]));
//        	Log.i(TAG, "encode2(), after encode,  toEncode["+idx+"]= "+toEncode[idx] );
//        }
//        
//        Log.e(TAG, "encode2(), ret= ["+ret.toString()+"]");
//        
//        return ret.toString();   
//    }
    
//    //Utility for testing
//    static public List<String> getTestList(int iNumDigital){
//    	List<String> lstRet = new ArrayList<String>();
//    	genTestData(lstRet, "", iNumDigital);
//    	//Log.i(TAG, "getTestList(), ret= "+lstRet.toString());
//    	return lstRet;
//    }
    
    static private void genTestData(List<String> lstRet, String strPre, int iRestDigital){
    	if(0 < iRestDigital){
    		for(int i = 0; i < sCodeTable.size()-2;i++){
    			String strPreNew = strPre + sCodeTable.get(i);
    			genTestData(lstRet, strPreNew, iRestDigital - 1);
    		}
    	}else{
    		lstRet.add(strPre);
    	}
    }
    
    static public String genNextTestData(String strCurCode, int iNumDigits){
    	String strRet = "";
    	int iCodeLen = (null != strCurCode)?strCurCode.length():0;
    	if(0 == iCodeLen){
    		strRet = getNDigits(sCodeTable.get(0), iNumDigits*AubioTestConfig.getMultiplyByFFTYPE());
    	}else{
    		for(int iIndex = iCodeLen - 1; iIndex >=0; iIndex--){
    			if(AubioTestConfig.CUR_FF_TYPE.equals(AubioTestConfig.FINITE_FIELD_TYPE.FFT_O2) || 
    			   AubioTestConfig.CUR_FF_TYPE.equals(AubioTestConfig.FINITE_FIELD_TYPE.FFT_Q4) || 
    			   AubioTestConfig.CUR_FF_TYPE.equals(AubioTestConfig.FINITE_FIELD_TYPE.FFT_D16)){
        			int iDivision = AubioTestConfig.getDivisionByFFTYPE();
        			strRet+=(sCodeTable.get(seed.nextInt(iDivision)));
        		}else{
        			String strNextCode = getNextLegalCode(strCurCode.substring(iIndex, iIndex+1));
            		if(null != strNextCode){
            			if(iIndex == iCodeLen - 1){
            				strRet = strCurCode.substring(0, iCodeLen-1)+strNextCode;
            			}else if(iIndex == 0){
            				strRet = strNextCode + getNDigits(sCodeTable.get(0), (iCodeLen - 1));
            			}else{
            				strRet = strCurCode.substring(0, iIndex)+strNextCode+ getNDigits(sCodeTable.get(0), (iCodeLen - iIndex -1));
            			}
            			break;
            		}
        		}
        	}
    	}
    	
    	//Log.e(TAG, "genNextTestData(), strCurCode= "+strCurCode+", strRet="+strRet );
    	return strRet;
    }
    
    static private Random seed = new Random(System.currentTimeMillis());
    static private Random seedNumDigit = new Random(System.currentTimeMillis());
    
    static public String genNextRandomData(int iMinDigit){
    	StringBuilder strRet = new StringBuilder();
    	int iDivision = AubioTestConfig.getDivisionByFFTYPE();
    	
    	int iMaxDigit = Math.min(MAX_ENCODE_DATA_LEN*AubioTestConfig.getMultiplyByFFTYPE(), (int) ((Math.pow(2, AubioTestConfig.getPowerByFFTYPE()*AubioTestConfig.getMultiplyByFFTYPE()) -1 )* 0.6666666666666f));
    	
    	int iLen = getRandomNumDigit(iMinDigit, iMaxDigit)*AubioTestConfig.getMultiplyByFFTYPE();
    	
    	//Log.e(TAG, "genNextRandomData(), iMaxDigit= "+iMaxDigit+", iLen="+iLen );
    	
    	for(int i =0;i<iLen;i++){
    		strRet.append(sCodeTable.get(seed.nextInt(iDivision)));
    	}
    	return strRet.toString();
    }
    
    static private int getRandomNumDigit(int iMin, int iMax){
    	return iMin + seedNumDigit.nextInt(iMax - iMin);
    }
    
    static private String getNextLegalCode(String strCode){
    	
    	String strRet = null;
    	int iIndex = sCodeTable.indexOf(strCode);
    	if((AubioTestConfig.getDivisionByFFTYPE() -1) > iIndex){
    		strRet = sCodeTable.get(iIndex+1);
    	}
    	//Log.e(TAG, "getNextLegalCode(), strCode= "+strCode+", strRet="+strRet );
    	return strRet;
    }
}
