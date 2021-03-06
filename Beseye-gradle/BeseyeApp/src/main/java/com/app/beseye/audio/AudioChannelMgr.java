package com.app.beseye.audio;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class AudioChannelMgr {
    private static AudioTrack sAudioTrack;
    private static int sSampleRate;
    private static int sChannelConfig;
    private static int sAudioFormat;
    private static boolean sbMute = false;
    
    //For Soundpairing wav output
    //private static FileOutputStream sFileOS = null;
    
    synchronized public static int audioInit(int sampleRate, boolean is16Bit, boolean isStereo, int desiredFrames) {
        int channelConfig = isStereo ? AudioFormat.CHANNEL_CONFIGURATION_STEREO : AudioFormat.CHANNEL_CONFIGURATION_MONO;
        int audioFormat = is16Bit ? AudioFormat.ENCODING_PCM_16BIT : AudioFormat.ENCODING_PCM_8BIT;
        int frameSize = (isStereo ? 2 : 1) * (is16Bit ? 2 : 1);
        if(DEBUG)
        	Log.i(TAG, "AudioChannelMgr::audioInit(), SDL audio: wanted " + (isStereo ? "stereo" : "mono") + " " + (is16Bit ? "16-bit" : "8-bit") + " " + (sampleRate / 1000f) + "kHz, " + desiredFrames + " frames buffer");
        
        // Let the user pick a larger buffer if they really want -- but ye
        // gods they probably shouldn't, the minimums are horrifyingly high
        // latency already
        desiredFrames = Math.max(desiredFrames, (AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat) + frameSize - 1) / frameSize);
        
        if(null != sAudioTrack && (sampleRate != sSampleRate || channelConfig != sChannelConfig || audioFormat != sAudioFormat)){
        	Log.w(TAG, "audioInit(), config mismatch, recreate it");
        	audioQuit();
        }
        
        if (sAudioTrack == null) {
            sAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                    channelConfig, audioFormat, desiredFrames * frameSize, AudioTrack.MODE_STREAM);
            
            // Instantiating AudioTrack can "succeed" without an exception and the track may still be invalid
            // Ref: https://android.googlesource.com/platform/frameworks/base/+/refs/heads/master/media/java/android/media/AudioTrack.java
            // Ref: http://developer.android.com/reference/android/media/AudioTrack.html#getState()
            
            if (sAudioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
                Log.e(TAG, "audioInit(), Failed during initialization of Audio Track");
                sAudioTrack = null;
                return -1;
            }
            
            sAudioTrack.play();
            sbMute = false;
        }
        
        sSampleRate = sampleRate;
        sChannelConfig = channelConfig;
        sAudioFormat = audioFormat;
       
//        if(null == sFileOS){
//        	try {
//        		sFileOS = new FileOutputStream(BeseyeStorageAgent.getFileInDownloadDir(BeseyeApplication.getApplication(), "BeseyeSoundpairing.pcm"));
//            } catch (FileNotFoundException e) {
//                Log.e(TAG, "audioInit(), Can't open BeseyeSoundpairing.pcm");
//
//                e.printStackTrace();
//            }
//
//        }
        if(DEBUG)
        	Log.v(TAG, "audioInit(), SDL audio: got " + ((sAudioTrack.getChannelCount() >= 2) ? "stereo" : "mono") + " " + ((sAudioTrack.getAudioFormat() == AudioFormat.ENCODING_PCM_16BIT) ? "16-bit" : "8-bit") + " " + (sAudioTrack.getSampleRate() / 1000f) + "kHz, " + desiredFrames + " frames buffer");
        
        return 0;
    }
    
    synchronized public static int getAudioBufSize(int iSampleRate){
    	int iMinBufSize = AudioTrack.getMinBufferSize(  iSampleRate,
														AudioFormat.CHANNEL_CONFIGURATION_MONO,
														AudioFormat.ENCODING_PCM_16BIT);
    	return iMinBufSize;
    }
    
    public static void setMute(boolean bMute){
    	sbMute = bMute;
    }
    
//    static private byte[] short2byte(short[] sData) {
//        int shortArrsize = sData.length/2;
//        byte[] bytes = new byte[shortArrsize * 2];
//        for (int i = 0; i < shortArrsize; i++) {
//            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
//            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
//            //sData[i] = 0;
//        }
//        return bytes;
//
//    }
    
    synchronized public static void audioWriteShortBuffer(short[] buffer, int iLen) {
    	if (sAudioTrack == null) {
    		Log.w(TAG, "AudioChannelMgr::audioWriteShortBuffer(), sAudioTrack is null");
    		return;
    	}
    	
		//Log.w(TAG, "AudioChannelMgr::audioWriteShortBuffer(), sbMute:"+sbMute);

    	if(sbMute){
    		return;
    	}
    	
//    	byte bData[] = short2byte(buffer);
//        try {
//			sFileOS.write(bData, 0, bData.length);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
        
    	for (int i = 0; i < iLen; ) {
             int result = sAudioTrack.write(buffer, i, iLen - i);
             
             if (result > 0) {
                 i += result;
             } else if (result == 0) {
                 try {
                     Thread.sleep(1);
                 } catch(InterruptedException e) {
                     // Nom nom
                 }
             } else {
                 Log.w(TAG, "AudioChannelMgr::SDL audio: error return from write(short)");
                 return;
             }
         }
    	

    }
    
    synchronized public static void audioWriteByteBuffer(byte[] buffer) {
    	if (sAudioTrack == null) {
    		Log.w(TAG, "AudioChannelMgr::audioWriteByteBuffer(), sAudioTrack is null");
    		return;
    	}
    	
    	if(sbMute){
    		return;
    	}
    	
        for (int i = 0; i < buffer.length; ) {
            int result = sAudioTrack.write(buffer, i, buffer.length - i);
            if (result > 0) {
                i += result;
            } else if (result == 0) {
                try {
                    Thread.sleep(1);
                } catch(InterruptedException e) {
                    // Nom nom
                }
            } else {
                Log.w(TAG, "AudioChannelMgr::SDL audio: error return from write(byte)");
                return;
            }
        }
    }

    synchronized public static void audioQuit() {
        if (sAudioTrack != null) {
            sAudioTrack.stop();
            sAudioTrack = null;
        }
        
//        if(null != sFileOS){
//        	try {
//				sFileOS.close();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//        }
    }
}
