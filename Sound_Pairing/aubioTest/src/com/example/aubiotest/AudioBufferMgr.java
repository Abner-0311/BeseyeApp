package com.example.aubiotest;

import static com.example.aubiotest.AubioTestConfig.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.util.Log;

public class AudioBufferMgr {
	final static String TAG = AubioTestActivity.TAG;
	
	static private AudioBufferMgr sAudioBufferMgr; 
	static public final int MAX_QUEUE_SIZE = (int) (MAX_RECORDING_TIME*SAMPLE_RATE_REC/FRAME_SIZE_REC);//30;
	
	public static AudioBufferMgr getInstance(){
		if(null == sAudioBufferMgr){
			sAudioBufferMgr = new AudioBufferMgr();
		}
		return sAudioBufferMgr;
	}
	
	 static final class BufRecord{
		int miIndex = 0;
		long mlTs;
		short[] mbBuf;
		int miSampleRead;
		int[] miFFTValues = new int[FFT_ANALYSIS_COUNT];
			
		public BufRecord(long mlTs, short[] mbBuf, int miSampleRead) {
			super();
			this.mlTs = mlTs;
			this.mbBuf = mbBuf;
			this.miSampleRead = miSampleRead;
			miIndex = getInstance().getBufIndex(mbBuf);
		}

		@Override
		public String toString() {
			return "BufRecord [mlTs=" + mlTs + ", mbBuf="
					+ Arrays.toString(mbBuf) + ", miSampleRead=" + miSampleRead
					+ "]";
		}
	}
	
	//private int miLenBufRecording = (int) (MAX_RECORDING_TIME*SAMPLE_RATE_REC/FRAME_SIZE_REC);
	private List<short[]> mAvailalbeBufList = new ArrayList<short[]>();
	private List<BufRecord> mDataBufList = new ArrayList<BufRecord>();
	private int miBufSize = 0;
	private Object mSyncObj = new Object();
	private int miPivotRecording = 0;
	private int miPivotAnalysis = 0;
	
//	private Object mSyncBufRecording = new Object();
//	private short[] mBufRecording;
//	private int miLenBufRecording = (int) (MAX_RECORDING_TIME*SAMPLE_RATE_REC);
	
	
	private AudioBufferMgr(){
		for(int i =0; i < MAX_QUEUE_SIZE ;i++){
			mAvailalbeBufList.add(new short[FRAME_SIZE_REC]);
		}
		//mBufRecording = new short[miLenBufRecording];
		miPivotRecording = 0;
		miPivotAnalysis = 0;
	}
	
//	private void addDataToRecordingBuf(short[] data){
//		if(null != data){
//			int iLenData = data.length;
//			if(miPivotRecording + iLenData < miLenBufRecording){
//				synchronized(mSyncBufRecording){
//					System.arraycopy(mBufRecording, miPivotRecording, data, 0, iLenData);
////					for(int idx = 0 ; idx < iLenData; idx++){
////						mBufRecording[miPivotRecording+idx] = data[idx];
////					}
//					miPivotRecording+=iLenData;
//				}
//			}else{
//				Log.e(TAG, "addDataToRecordingBuf(), out of recording buffer, miPivotRecording = "+miPivotRecording);
//			}
//		}
//	}
//	
//	public void trimBufToLastBufSession(int iSessionLeft){
//		synchronized(mSyncBufRecording){
//			int iLenToLeft = iSessionLeft * FRAME_SIZE_REC;
//			int IdxBeginToCopy = (miPivotRecording) - iLenToLeft;
//			if(0 > IdxBeginToCopy){
//				Log.e(TAG, "trimBufToLastBufSession(), no enough buffer to left, iLenToLeft = "+iLenToLeft+", miPivotRecording = "+miPivotRecording);
//			}else{
////				for(int idx = 0 ; idx < iLenToLeft; idx++){
////					mBufRecording[idx] = mBufRecording[IdxBeginToCopy+idx];
////				}
//				System.arraycopy(mBufRecording, miPivotRecording, mBufRecording, 0, iLenToLeft);
//				miPivotRecording = iLenToLeft;
//			}
//		}
//	}
//	
	public void cleanRecordingBuf(){
		synchronized(mSyncObj){
			miPivotRecording = 0;
			miPivotAnalysis = 0;
		}
	}
	
	public int getBufIndex(short[] buf){
		int iIndex = -1;
		synchronized(mSyncObj){
			iIndex = mAvailalbeBufList.indexOf(buf);
		}
		return iIndex;
	}
	
	public void setBufferSize(int iSize){
		miBufSize = iSize;
	}
	
	public Object getSyncObject(){
		return mSyncObj;
	}
	
	public synchronized short[] getBufByIndex(int iBufIndexInput, int iOffset, short[] bufReturn){
		//Log.d(TAG, "getBufByIndex(), iBufIndexInput:"+iBufIndexInput+", iOffset:"+iOffset);
		
		int iBufIndex = iBufIndexInput;
		while(iOffset > FRAME_SIZE_REC){
			iOffset -= FRAME_SIZE_REC;
			iBufIndex = (iBufIndex+1)%MAX_QUEUE_SIZE;
			Log.e(TAG, "getBufByIndex(), shift iBufIndex because iOffset > FRAME_SIZE_REC");
		}
		
		int iLenBuf = (null == bufReturn)?0:bufReturn.length;
		short[] curBuf = (0 <= iBufIndex && iBufIndex < MAX_QUEUE_SIZE)?mAvailalbeBufList.get(iBufIndex):null;
		if(null == curBuf){
			Log.e(TAG, "getBufByIndex(), curBuf is null");
		}else if(FRAME_SIZE_REC != iLenBuf){
			Log.e(TAG, "getBufByIndex(), length doesn't match iLenBuf:"+iLenBuf);
		}else{
			if(0 == iOffset){
				System.arraycopy(mAvailalbeBufList.get(iBufIndex), 0, bufReturn, 0, iLenBuf);
			}else{
				int iDelta = Math.abs(iOffset);
				if(iDelta > FRAME_SIZE_REC){
					Log.e(TAG, "getBufByIndex(), too large iOffset:"+iOffset);
				}else if(0 < iOffset){
					short[] nextBuf = mAvailalbeBufList.get((iBufIndex+1)%MAX_QUEUE_SIZE);
					if(null != nextBuf){
						java.util.Arrays.fill(bufReturn,(short) 0);
						System.arraycopy(curBuf , iDelta , bufReturn, 0               , iLenBuf - iDelta);
						System.arraycopy(nextBuf, 0      , bufReturn, iLenBuf - iDelta, iDelta);
					}else{
						Log.e(TAG, "getBufByIndex(), nextBuf is null, nextBuf:"+nextBuf);
					}
				}else{
					int idxPrev = (0 == iBufIndex)?(MAX_QUEUE_SIZE-1):(iBufIndex-1);
					short[] prevBuf = mAvailalbeBufList.get(idxPrev);
					//Log.e(TAG, "getBufByIndex(), iBufIndex:"+iBufIndex+", idxPrev:"+idxPrev+", iOffset:"+iOffset+", iLenBuf:"+iLenBuf);
					if(null != prevBuf){
						java.util.Arrays.fill(bufReturn,(short) 0);
						//Log.e(TAG, "getBufByIndex(), prevBuf[iLenBuf - iDelta]:"+prevBuf[iLenBuf - iDelta]+", bufReturn[0]:"+bufReturn[0]);
						System.arraycopy(prevBuf, iLenBuf - iDelta, bufReturn, 0     , iDelta);
						//Log.e(TAG, "getBufByIndex(), bufReturn[0]:"+bufReturn[0]+"curBuf[0]:"+curBuf[0]+", bufReturn[iDelta]:"+bufReturn[iDelta]);
						System.arraycopy(curBuf , 0               , bufReturn, iDelta, iLenBuf - iDelta);
						//Log.e(TAG, "getBufByIndex(), bufReturn[iDelta]:"+bufReturn[iDelta]);
					}else{
						Log.e(TAG, "getBufByIndex(), prevBuf is null, nextBuf:"+prevBuf);
					}
				}
			}
		}
		//Log.i(TAG, "getBufByIndex(), bufReturn[0]:"+bufReturn[0]);
		return bufReturn;
	}
	
	public synchronized short[] getAvailableBuf(){
//		int iSize = mAvailalbeBufList.size();
//		if(0 == iSize){
//			if(MAX_QUEUE_SIZE > mDataBufList.size()){
//				Log.w(TAG, "getAvailableBuf(), size="+iSize+", need to create new buffer+++++++++++++++++++++++++");
//				mAvailalbeBufList.add(new short[miBufSize]);
//			}else{
//				Log.w(TAG, "getAvailableBuf(), size="+iSize+", need to reuse old buffer+++++++++++++++++++++++++&&&&");
//				mAvailalbeBufList.add(getDataBuf().mbBuf);
//			}
//		}
//		return mAvailalbeBufList.remove(0);
		
		short[] buf = null;
		synchronized(mSyncObj){
			if(-1 == miPivotRecording){
				Log.e(TAG, "getAvailableBuf(), buffer is out, need to wait for reset");
			}else{
				buf = mAvailalbeBufList.get(miPivotRecording);
				//Log.i(TAG, "getAvailableBuf(), get buffer at pos "+miPivotRecording);
				if(miPivotRecording == MAX_QUEUE_SIZE -1){
					Log.e(TAG, "getAvailableBuf(), buffer is out");
					miPivotRecording = -1;
				}else
					miPivotRecording = (++miPivotRecording)%MAX_QUEUE_SIZE;
				
				if(miPivotRecording == miPivotAnalysis){
					//Log.w(TAG, "getAvailableBuf(), meet non-analyzed buf, push it");
					miPivotAnalysis = (++miPivotAnalysis)%MAX_QUEUE_SIZE;
				}
			}
		}
		return buf;
	}
	
	public synchronized void addToDataBuf(long lTs, short[] buf, int iSampleRead){
		//Log.i(TAG, "addToDataBuf(), lTs:"+lTs+", iSampleRead:"+iSampleRead+" at buf "+getBufIndex(buf));
		synchronized(mSyncObj){
			mDataBufList.add(new BufRecord(lTs, buf,iSampleRead) );
		}
		synchronized(mSyncObj){
			mSyncObj.notifyAll();
		}
	}
	
	public synchronized BufRecord getDataBuf(){
		return getDataBuf(0);
	}
	
	public synchronized BufRecord getDataBuf(int iNumToRest){
		BufRecord br = null;
		synchronized(mSyncObj){
			int iSize = mDataBufList.size();
			if((iNumToRest + 1) <= iSize){
				br = mDataBufList.remove(0);
			}
		}
		return br;
	}
	
	public synchronized void addToAvailableBuf(BufRecord buf){
		//Log.w(TAG, "addToAvailableBuf(), return old buffer------------------------");
		//java.util.Arrays.fill(buf.mbBuf,(short) 0);
		//mAvailalbeBufList.add(buf.mbBuf);
	}
	
	public synchronized void recycleAllBuffer(){
		synchronized(mSyncObj){
			Log.w(TAG, "recycleAllBuffer(), %%%%%%%%%%%%%%%%%%%%%%%%");
			cleanRecordingBuf();
			BufRecord rec = null;
			while(null != (rec = getDataBuf())){
				addToAvailableBuf(rec);
			}
		}
	}
}
