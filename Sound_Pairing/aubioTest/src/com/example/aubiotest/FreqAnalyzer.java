package com.example.aubiotest;

import static com.example.aubiotest.AubioTestConfig.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.util.Log;

import com.example.aubiotest.AubioTestActivity.MatchRetSet;

public class FreqAnalyzer {	
	final static String TAG = AubioTestActivity.TAG;
	static private final String MISSING_CHAR = "%";

	final private List<FreqRecord> mFreqRecordList = new ArrayList<FreqRecord>();
	final private List<CodeRecord> mCodeRecordList = new ArrayList<CodeRecord>();
	
	private String mLastAbandant = "0";
	private StringBuilder msbDecode = new StringBuilder();
	
	private boolean mbStartAppend = false;
//	private boolean sbNeedToRecFirstTs = false;
	private boolean mbInSenderMode = false;
	
	private short[] bufSegment = new short[FRAME_SIZE_REC];
	private int mSessionOffset = 0;
	private long mSessionBeginTs = -1;
	private int mSessionBeginBufIdx = -1;
	
	//the last tone recognized
	private long mLastCheckToneTs = 0;
	
	final private boolean mbNeedToAutoCorrection;
	private FreqAnalyzer selfFreqAnalyzer;
	private MatchRetSet mprevMatchRet;
	
	private FreqAnalyzer(boolean bNeedToAutoCorrection){
		mbNeedToAutoCorrection = bNeedToAutoCorrection;
	}
		
	private static FreqAnalyzer sFreqAnalyzer;
	
	static public FreqAnalyzer getInstance(){
		if(sFreqAnalyzer == null)
			sFreqAnalyzer = new FreqAnalyzer(true);
		
		return sFreqAnalyzer;
	}
	
	static interface IFreqAnalyzeResultCB{
		public void onDetectStart();
		public void onAppendResult(String strCode);
		public void onSetResult(String strCode, String strDecodeMark, String strDecodeUnmark, boolean bFromAutoCorrection, MatchRetSet prevMatchRet);
		public void onTimeout(FreqAnalyzer freqAnalyzer, boolean bFromAutoCorrection, MatchRetSet prevMatchRet);
		
		public float onBufCheck(short[] buf, long lBufTs, boolean bResetFFT, int[] iFFTValues);
		
		public void decodeRSCode(int[] data, int iCount, int iNumErr);
	}
	
	private IFreqAnalyzeResultCB mIFreqAnalyzeResultCBListener = null;
	
	public void setIFreqAnalyzeResultCB(IFreqAnalyzeResultCB listener){
		mIFreqAnalyzeResultCBListener =listener;
	}
    
    // For listview display
    static public class FreqData{
    	int iType;
    	long mTime;
    	double mdFreq;
		public FreqData(int iType, long mTime, double mdFreq) {
			super();
			this.iType = iType;
			this.mTime = mTime;
			this.mdFreq = mdFreq;
		}
    }
    
    static public final class FreqRangeData{
    	double mdValue;
		double mdLowerBound;
		double mdUpperBound;
		
		public FreqRangeData(double mdValue, double dDelta) {
			this(mdValue, mdValue - Math.abs(dDelta), mdValue + Math.abs(dDelta));
		}
		
    	public FreqRangeData(double mdValue, double mdLowerBound, double mdUpperBound) {
			super();
			this.mdValue = mdValue;
			this.mdUpperBound = mdUpperBound;
			this.mdLowerBound = mdLowerBound;
		}

		@Override
		public String toString() {
			return "FreqRangeData [mdValue=" + mdValue + ", mdLowerBound="
					+ mdLowerBound + ", mdUpperBound=" + mdUpperBound + "]\n";
		}
		
		boolean withinFreqRange(double dFreq){
			return !Double.isInfinite(dFreq) && !Double.isNaN(dFreq) && Double.compare(dFreq, mdLowerBound) >=0 && Double.compare(dFreq, mdUpperBound) <=0;
		}
    }
	
    //For Freq-Code mapping
	static public final class FreqRange{
		@Override
		public String toString() {
			return (null != mlstFreqRangeData)?mlstFreqRangeData.toString():null;
		}
		static final double MIN_FREQ = 200.0f; 
		static final double MAX_FREQ = 8000.0f; 
//		double mdValue;
//		double mdUpperBound;
//		double mdLowerBound;
		List<FreqRangeData> mlstFreqRangeData;
		String mstrCode;
		
		FreqRange(double dFreq, String strCode){
			this(dFreq, 50.0, strCode);
		}
		
		FreqRange(double dFreq, double dDelta, String strCode){
			mlstFreqRangeData = new ArrayList<FreqRangeData>();
			mlstFreqRangeData.add(new FreqRangeData(dFreq, dDelta));
//			if(AUBIO_FFT){
//				int iDx = 1;
//				while((dFreq/++iDx) > MIN_FREQ){
//					mlstFreqRangeData.add(new FreqRangeData(dFreq/iDx, dDelta/iDx));
//				}
//			}else{
//				int iDx = 1;
//				while((++iDx*dFreq) < MAX_FREQ){
//					mlstFreqRangeData.add(new FreqRangeData(dFreq*iDx, dDelta*iDx));
//				}
//			}

//			mdValue = dFreq;
//			mdUpperBound = mdValue + Math.abs(dDelta);
//			mdLowerBound = mdValue - Math.abs(dDelta);
			mstrCode = strCode;
		}
		
		FreqRange(double dFreq, double dUpperBound, double dLowerBound, String strCode){
			mlstFreqRangeData = new ArrayList<FreqRangeData>();
			mlstFreqRangeData.add(new FreqRangeData(dFreq, dLowerBound, dUpperBound));
//			mdValue = dFreq;
//			mdUpperBound = dUpperBound;
//			mdLowerBound = dLowerBound;
			mstrCode = strCode;
		}
		
		boolean withinFreqRange(double dFreq){
			boolean bRet = false;
			if(!Double.isInfinite(dFreq) && !Double.isNaN(dFreq)){
				for(FreqRangeData frd :mlstFreqRangeData){
					if(frd.withinFreqRange(dFreq)){
						bRet = true;
						break;
					}
				}
			}
			return bRet;
		}
		
		boolean isOverlap(FreqRangeData frdChk){
			boolean bRet = false;
			if(!Double.isInfinite(frdChk.mdLowerBound) && !Double.isNaN(frdChk.mdLowerBound) && !Double.isInfinite(frdChk.mdUpperBound) && !Double.isNaN(frdChk.mdUpperBound)){
				for(FreqRangeData frd :mlstFreqRangeData){
					if(!(Double.compare(frd.mdLowerBound, frdChk.mdUpperBound) > 0 || Double.compare(frd.mdUpperBound, frdChk.mdLowerBound) < 0 )){
						bRet = true;
						break;
					}
				}
			}
			return bRet;
		}
	}
	
	//
    static final class FreqRecord{
    	int miBufIndex;
		long mlTs;
		double mdFreq;
		String mstrCode;
		int[] miFFTValues;
		
		FreqRecord(long lTs, double dFreq, String strCode, int iBufIndex, int[] iFFTValues){
			mlTs = lTs;
			mdFreq = dFreq;
			mstrCode = strCode;
			miBufIndex = iBufIndex;
			
			if(null != iFFTValues)
				miFFTValues = Arrays.copyOf(iFFTValues, iFFTValues.length);
		}
		
		FreqRecord(FreqRecord copy){
			if(null == copy)
				return;
			
			mlTs = copy.mlTs;
			mdFreq = copy.mdFreq;
			mstrCode = copy.mstrCode;
			miBufIndex = copy.miBufIndex;
			
			if(null != copy.miFFTValues)
				miFFTValues = Arrays.copyOf(copy.miFFTValues, copy.miFFTValues.length);
		}
		
		FreqRecord recheckToneCode(int iLastTone){
			FreqRecord frRet = this;
			if(0 < iLastTone && null != miFFTValues && 0 < miFFTValues.length){
				if(miFFTValues[0] > 0 && miFFTValues[1] > 0 && (miFFTValues[0] - iLastTone <= 1 && miFFTValues[0] - iLastTone >= -1) && (miFFTValues[0] - miFFTValues[1] >= 2 || miFFTValues[0] - miFFTValues[1] <= -2)){
					double newFreq = miFFTValues[1]*BIN_SIZE;
					if(0 != Double.compare(mdFreq, newFreq)){
						String strCode = findToneCodeByFreq(newFreq);
						if(!strCode.equals("")){
							frRet = new FreqRecord(this);
							Log.e(TAG, "recheckToneCode()---------------------------------->, change from "+mstrCode+" to "+ strCode);
							
							frRet.mdFreq = newFreq;
							frRet.mstrCode = strCode;
						}
					}
				}
			}
			return frRet;
		}
		
		@Override
		public String toString() {
			return "FreqRecord [miBufIndex=" + miBufIndex + ", mlTs=" + mlTs
					+ ", mdFreq=" + mdFreq + ", mstrCode=" + mstrCode
					+ ", miFFTValues=" + Arrays.toString(miFFTValues) + "]\n";
		}
	}
    
    static final class CodeRecord{
    	long lStartTs;
    	long lEndTs;
    	String strCdoe;
    	String strReplaced;
    	List<FreqRecord> mlstFreqRec;
    	
    	public CodeRecord() {
			super();
		}
    	
    	CodeRecord(List<FreqRecord> lstFreqRec, String strReplaced){
			this();
			this.strReplaced = strReplaced;
			mlstFreqRec = lstFreqRec;
			inferFreq();
		}
    	
		public CodeRecord(long lStartTs, long lEndTs, String strCdoe) {
			super();
			setParameters(lStartTs, lEndTs, strCdoe);
		}
		
		CodeRecord(long lStartTs, long lEndTs, String strCode, List<FreqRecord> lstFreqRec){
			this(lStartTs, lEndTs, strCode);
			mlstFreqRec = lstFreqRec;
		}
		
		private void setParameters(long lStartTs, long lEndTs, String strCdoe){
			this.lStartTs = lStartTs;
			this.lEndTs = lEndTs;
			this.strCdoe = strCdoe;
		}
		
		private void inferFreq(){
			if(null == mlstFreqRec || 0 == mlstFreqRec.size()){
				Log.e(TAG, "inferFreq()**, null mlstFreqRec");
				return;
			}
			int iSize = mlstFreqRec.size();
			FreqRecord frFirst = mlstFreqRec.get(0);
			FreqRecord frLast = mlstFreqRec.get(iSize-1);
			
			//infer the freq
			boolean bDiff = false;
			String strLastCheck = null;
			String curCode = null;
			for(int iIdx = 0; iIdx < iSize; iIdx++){
				curCode = mlstFreqRec.get(iIdx).mstrCode;
				if(null == strLastCheck){
					strLastCheck = curCode;
					continue;
				}
				
				if(!curCode.equals(strLastCheck)){
					bDiff = true;
				}
			
				strLastCheck = curCode;
			}
			
			if(false == bDiff){
				if(!MISSING_CHAR.equals(curCode)){
					Log.i(TAG, "inferFreq()**, all the same, code = ["+curCode+"]\n "+mlstFreqRec);
					setParameters(frFirst.mlTs, frLast.mlTs, curCode);
				}else{
					Log.i(TAG, "inferFreq()**, all the same, code = ["+curCode+"]\n "+mlstFreqRec+", replace it");
					setParameters(frFirst.mlTs, frLast.mlTs, strReplaced);
				}
			}else{	
				int iMedIdx = iSize/2;
				FreqRecord frMiddle = mlstFreqRec.get(iMedIdx);
				if(!MISSING_CHAR.equals(frMiddle.mstrCode)){
					//Check back-half items first
					bDiff = false;
					curCode = strLastCheck = frMiddle.mstrCode;
					
					for(int idx = iMedIdx +1; idx < iSize; idx++){
						curCode = mlstFreqRec.get(idx).mstrCode;
						if(!curCode.equals(strLastCheck)){
							bDiff = true;
						}
						strLastCheck = curCode;
					}
					
					if(false == bDiff && !MISSING_CHAR.equals(curCode)){
						Log.i(TAG, "inferFreq()**, all the same of back-half, code = ["+curCode+"]\n "+mlstFreqRec);
						setParameters(frFirst.mlTs, frLast.mlTs, curCode);
					}else{
						//Check front-half items
						bDiff = false;
						curCode = strLastCheck = frMiddle.mstrCode;
					
						for(int idx = iMedIdx -1; idx >= 0; idx--){
							curCode = mlstFreqRec.get(idx).mstrCode;
							if(!curCode.equals(strLastCheck)){
								bDiff = true;
							}
							strLastCheck = curCode;
						}
						
						if(false == bDiff && !MISSING_CHAR.equals(curCode)){
							Log.i(TAG, "inferFreq()**, all the same of first-half, code = ["+curCode+"]\n "+mlstFreqRec);
							setParameters(frFirst.mlTs, frLast.mlTs, curCode);
						}else{
							if(mlstFreqRec.get(0).mstrCode.equals(mlstFreqRec.get(iSize-1).mstrCode) && !mlstFreqRec.get(0).mstrCode.equals(MISSING_CHAR)){
								Log.i(TAG, "inferFreq()**, pick side items, code = ["+mlstFreqRec.get(0).mstrCode+"]\n "+mlstFreqRec);
								setParameters(frFirst.mlTs, frLast.mlTs, mlstFreqRec.get(0).mstrCode);
							}else{
								Log.i(TAG, "inferFreq()**, pick middle items, code = ["+frMiddle.mstrCode+"]\n "+mlstFreqRec);
								setParameters(frFirst.mlTs, frLast.mlTs, frMiddle.mstrCode);
							}
						}
					}
				}else{
					//if middle is missing char
					String strCodeInfer = null;
					for(int idx = iSize - 1; idx >= 0; idx--){
						curCode = mlstFreqRec.get(idx).mstrCode;
						if(!curCode.equals(MISSING_CHAR)){
							strCodeInfer = curCode;
							break;
						}
					}
					Log.i(TAG, "inferFreq()**, pick non MISSING_CHAR one, code = ["+strCodeInfer+"]\n "+mlstFreqRec);
					setParameters(frFirst.mlTs, frLast.mlTs, (null == strCodeInfer)?strReplaced:strCodeInfer);
				}
			}
		}
		
		@Override
		public String toString() {
			return "CodeRecord [lStartTs=" + lStartTs + ", lEndTs=" + lEndTs
					+ ", strCdoe=" + strCdoe + ", mlstFreqRec\n=" + mlstFreqRec + "]\n";
		}
		
		public boolean isSameCode(){
			boolean bRet = true;
			int iSize = (null != mlstFreqRec)?mlstFreqRec.size():0;
			for(int idx = 0; idx+1 < iSize; idx++){
				if(mlstFreqRec.get(idx).mstrCode.equals(MISSING_CHAR) || !mlstFreqRec.get(idx).mstrCode.equals(mlstFreqRec.get(idx+1).mstrCode)){
					bRet = false;
				}
			}
			return bRet;
		}
		
		static CodeRecord combineNewCodeRecord(CodeRecord cr1, CodeRecord cr2, int iOffset, int iLastTone){
			CodeRecord crRet = null;
			
			List<FreqRecord> lstFreqRec = new ArrayList<FreqRecord>();
			if(0 < iOffset){
				if(null != cr1 && null != cr1.mlstFreqRec){
					for(int i =  iOffset; i < cr1.mlstFreqRec.size();i++){
						lstFreqRec.add(cr1.mlstFreqRec.get(i).recheckToneCode(iLastTone));
					}
				}
				
				if(null != cr2 && null != cr2.mlstFreqRec){
					for(int i =  0; i < iOffset && i < cr2.mlstFreqRec.size();i++){
						lstFreqRec.add(cr2.mlstFreqRec.get(i).recheckToneCode(iLastTone));
					}
				}
				
				crRet = new CodeRecord(lstFreqRec, cr1.strReplaced);
			}else if(0 > iOffset){
				if(null != cr1 && null != cr1.mlstFreqRec){
					for(int i =  cr1.mlstFreqRec.size() + iOffset; 0 <= i && i < cr1.mlstFreqRec.size();i++){
						lstFreqRec.add(cr1.mlstFreqRec.get(i).recheckToneCode(iLastTone));
					}
				}
				
				if(null != cr2 && null != cr2.mlstFreqRec){
					for(int i =  0; i < (cr2.mlstFreqRec.size()+iOffset) && i < cr2.mlstFreqRec.size();i++){
						lstFreqRec.add(cr2.mlstFreqRec.get(i).recheckToneCode(iLastTone));
					}
				}
				crRet = new CodeRecord(lstFreqRec, cr2.strReplaced);
			}else{
				lstFreqRec = cr1.mlstFreqRec;
				crRet = cr1;
			}
			
			//Fill the missing items
			if(lstFreqRec.size()  < TONE_FRAME_COUNT){
				int iSize = lstFreqRec.size();
				if(0 < iSize){
					if(null == cr1 || null == cr1.mlstFreqRec){
						long lSesBeginTs = lstFreqRec.get(0).mlTs;
						for(int idx = 0; idx < TONE_FRAME_COUNT-iSize; idx++){
							long lTsByIdx = lSesBeginTs - (idx+1)*FRAME_TS;
							lstFreqRec.add(0, new FreqRecord(lTsByIdx, -1.0f, MISSING_CHAR, -1, null));
							Log.i(TAG, "combineNewCodeRecord()**, ====>>>> add missing char to lstFreqRec to head at "+(lTsByIdx));
						}
					}else if(null == cr2 || null == cr2.mlstFreqRec){
						long lSesTailTs = lstFreqRec.get(iSize-1).mlTs;
						for(int idx = 0; idx < TONE_FRAME_COUNT-iSize; idx++){
							long lTsByIdx = lSesTailTs + (idx+1)*FRAME_TS;
							lstFreqRec.add(lstFreqRec.size(), new FreqRecord(lTsByIdx, -1.0f, MISSING_CHAR, -1, null));
							Log.i(TAG, "combineNewCodeRecord()**, ====>>>> add missing char to lstFreqRec to tail at "+(lTsByIdx));
						}
					}		
				}
			}
			
			//Log.i(TAG, "combineNewCodeRecord()**, crRet:\n"+crRet);
			
			return crRet;
		}
    }
    
    synchronized public void reset(){
    	mLastCheckToneTs = -1;
		mbStartAppend = false;
		mFreqRecordList.clear();
		mSessionBeginTs = -1;
		mSessionOffset = 0;
		mLastAbandant = "0";
		mstrCodeTrace = null;
//		mlTraceTs = 0;
//		mlMaxWaitingTime = 0;
    }
    
    public void setSenderMode(boolean bIsSenderMode){
    	mbInSenderMode = bIsSenderMode;
    }
    
    private String mstrCodeTrace = null;
    private long mlTraceTs = 0;
    private long mlMaxWaitingTime = 0;
    
    public void beginToTrace(String strCode){
    	mstrCodeTrace = strCode;
    	mlTraceTs = System.currentTimeMillis();
    	mlMaxWaitingTime = strCode.length()*200+4000;
    	Log.i(TAG, "beginToTrace(), mlTraceTs:"+mlTraceTs+", mlMaxWaitingTime:"+mlMaxWaitingTime);
    }
    
    public void endToTrace(){
		mlTraceTs = 0;
		mlMaxWaitingTime = 0;
    }
    
    public int getLastDetectedToneIdx(long lCurTs){
    	return getLastDetectedToneIdxOnCodeList(mCodeRecordList, lCurTs);
    }
    
    private static int getLastDetectedToneIdxOnCodeList(List<CodeRecord> lstCodeRecord, long lCurTs){
    	int iRet = 0;
    	if(0 < lCurTs){
    		int iSize = lstCodeRecord.size();
        	if(0 < iSize){
        		CodeRecord lastRec = lstCodeRecord.get(iSize-1);
        		if(null != lastRec){
        			String strCode = null;
        			long lDelta = lCurTs - lastRec.lEndTs;
        			if(lDelta > 0 && lDelta <= 3* FRAME_TS){
        				strCode = lastRec.strCdoe;
        			}
        			
        			iRet = getToneIdxByCode(strCode);
        		}
        	}
    	}
    	return iRet;
    }
    
    private static int getToneIdxByCode(String strCode){
    	int iRet = -1;
    	if(null != strCode){
			Double dFreq = sAlphabetTable.get(strCode);
			if(null != dFreq){
				iRet = (int) (dFreq/BIN_SIZE);
			}
		}
    	return iRet;
    }
    
    private void checkTimeout(long lTs){
    	if(0 < mlTraceTs && 0 < mlMaxWaitingTime){
    		long lDelta = System.currentTimeMillis() - mlTraceTs;
        	int iSize = mFreqRecordList.size();
    		Log.d(TAG, "checkTimeout(), lDelta:"+lDelta+", lTs:"+lTs);
        	
        	if(false == mbStartAppend){
        		if(lDelta > mlMaxWaitingTime){
        			Log.e(TAG, "checkTimeout(), lDelta > mlMaxWaitingTime----------");
        			triggerTimeout();
        		}
        	}else if((0 < iSize && (lTs - mFreqRecordList.get(iSize-1).mlTs) >= 15 * TONE_PERIOD || getInvalidFreqCount() >= 15)){
        		Log.e(TAG, "checkTimeout(), cannot get ending char");
        		triggerTimeout();
        	}
    	}
    }
    
    private void triggerTimeout(){
    	if(null != mIFreqAnalyzeResultCBListener){
			mIFreqAnalyzeResultCBListener.onTimeout(this, !mbNeedToAutoCorrection, mprevMatchRet);
		}
    }
    
    static private String findToneCodeByFreq(double dFreq){
    	String strCode = "";
    	for(FreqRange fr : sFreqRangeTable){
			if(fr.withinFreqRange(dFreq)){
				strCode = fr.mstrCode;
				break;
			}
		}
    	return strCode;
    }
	
	synchronized public void analyze(long lTs, double dFreq, int iBufIndex, int[] iFFTValues){
		if(PRE_EMPTY && true == mbStartAppend){
			CodeRecord lastRec = mCodeRecordList.get(1);
			long lEndTs = lastRec.lEndTs;
			if((lTs - lEndTs) <= TONE_PERIOD){
				Log.w(TAG, "analyze(), ("+lTs+" - "+lEndTs+") <= TONE_PERIOD:"+TONE_PERIOD+", return");
				return;
			}
		}
		
		String strCode = findToneCodeByFreq(dFreq);
		
		if(true == "".equals(strCode)){
			if(dFreq > 0.0 || mbStartAppend){
				Log.w(TAG, "analyze(), cannot find code for freq:"+dFreq+" at "+lTs);
				if(mbStartAppend)
					mFreqRecordList.add(new FreqRecord(lTs, dFreq, MISSING_CHAR, iBufIndex, iFFTValues));
			}
			
			int iSize = mFreqRecordList.size();
			if(false == mbStartAppend && mSessionBeginTs > 0 && (0 < iSize && (lTs - mFreqRecordList.get(iSize-1).mlTs) >= 3 * TONE_PERIOD || getInvalidFreqCount() >= 3)){
				Log.e(TAG, "analyze(), remove bias");
				reset();
			}
			
			checkTimeout(lTs);
	
		}else{
			mFreqRecordList.add(new FreqRecord(lTs, dFreq, strCode, iBufIndex, iFFTValues));
			final int iCheckIndex = exceedToneCheckPeriod();
			if(-1 < iCheckIndex){
				if(SEGMENT_FEATURE && -1 < mSessionBeginTs){
					pickWithSeesion();
				}else{
					pickWithoutSession(iCheckIndex);
				}
			}
		}
	}
	
	private int getInvalidFreqCount(){
		int iRet = 0;
		int iSize = (null != mFreqRecordList)?mFreqRecordList.size():0;
		for(int idx = iSize -1; idx >= 0; idx--){
			FreqRecord fr = mFreqRecordList.get(idx);
			if(null == fr || null == fr.mstrCode || fr.mstrCode.equals(MISSING_CHAR) || fr.mstrCode.equals("")){
				iRet++;
			}else{
				break;
			}
		}
		return iRet;
	}
	
	private void fillEmptyCodeRecord(long lCurSesBeginTs){
		int iSize = mCodeRecordList.size();
		if(0 < iSize){
			CodeRecord lastCodeRec = mCodeRecordList.get(iSize -1);
			if(null != lastCodeRec){
				long lBeginTs = lastCodeRec.lStartTs;
				long lDelta = lCurSesBeginTs - lBeginTs;
				while(TONE_PERIOD < lDelta){
					if(PRE_EMPTY && (lBeginTs-mSessionBeginTs) == TONE_PERIOD){
						Log.d(TAG, "fillEmptyCodeRecord()**, ====>>>> no need to add CodeRecord at "+(lBeginTs+TONE_PERIOD)+", due to PRE_EMPTY");
					}else{
						Log.i(TAG, "fillEmptyCodeRecord()**, ====>>>> add empty CodeRecord at "+(lBeginTs+TONE_PERIOD));
						mCodeRecordList.add(new CodeRecord(lBeginTs+TONE_PERIOD, lBeginTs+TONE_PERIOD*2-FRAME_TS, MISSING_CHAR));
					}
					lBeginTs += TONE_PERIOD;
					lDelta = lCurSesBeginTs - lBeginTs;
				}
			}
		}
	}
	
	private void pickWithSeesion(){
		long lSesIdx = (mFreqRecordList.get(0).mlTs - mSessionBeginTs)/TONE_PERIOD;
		long lSesBeginTs = mSessionBeginTs+TONE_PERIOD*lSesIdx;
		long lSesEndTs = mSessionBeginTs+TONE_PERIOD*(lSesIdx+1);
		
		Log.i(TAG, "pickWithSeesion()**, ====>>>> (lSesBeginTs, lSesEndTs): ("+lSesBeginTs+", "+lSesEndTs+")");
		fillEmptyCodeRecord(lSesBeginTs);
		
		//Find out all session items
		List<FreqRecord> sesFreqList = new ArrayList<FreqRecord>();
		while(0 < mFreqRecordList.size()){
			FreqRecord fr = mFreqRecordList.get(0);
			if(fr.mlTs < lSesEndTs){
				mFreqRecordList.remove(0);
				sesFreqList.add(fr);
			}else{
				break;
			}
		}
		//Log.i(TAG, "analyze(), ====>>>> sesFreqList:"+sesFreqList.toString());
		
		//Fill the missing items
		if(sesFreqList.size() == 0){
			Log.i(TAG, "pickWithSeesion()**, ====>>>> no items in sesFreqList");
			for(int idx = 0; idx < TONE_FRAME_COUNT; idx++){
				sesFreqList.add(0, new FreqRecord(lSesBeginTs + idx*FRAME_TS, -1.0f, MISSING_CHAR, -1, null));
			}
		}else{
			for(int idx = 0; idx < TONE_FRAME_COUNT; idx++){
				long lTsByIdx = lSesBeginTs + idx*FRAME_TS;
				FreqRecord fr = sesFreqList.size() > idx ? sesFreqList.get(idx):null;
				if(null == fr || fr.mlTs != lTsByIdx){
					sesFreqList.add(idx, new FreqRecord(lTsByIdx, -1.0f, MISSING_CHAR, -1, null));
					Log.i(TAG, "pickWithSeesion()**, ====>>>> add missing char to sesFreqList at "+(lTsByIdx));
				}
			}
		}
		
		//Log.i(TAG, "analyze()**, ====>>>> sesFreqList:"+sesFreqList.toString());
		
		CodeRecord rec = new CodeRecord(sesFreqList, mLastAbandant);
		mLastCheckToneTs = rec.lEndTs;
		appendRet(rec);
	}
	
	private void pickWithoutSession(int iCheckIndex){
		//Log.i(TAG, "analyze(), codeList = "+codeList.toString());
		List<FreqRecord> codeList = new ArrayList<FreqRecord>();
		for(int i =0; i <= iCheckIndex;i++){
			FreqRecord fr = mFreqRecordList.get(i);
			codeList.add(fr);
			Log.i(TAG, "pickWithoutSession(), ====>>>> sFreqRecordList["+i+"]: "+fr);
		}
		
		//Criteria 1: all the same code
		String strLastCheck = null;
		String strFirstCode = null;
		long lFirstCodeTs = -1;
		int iNumSameFst = 1;
		int iLastIdxSameFst = 0;
		
		boolean bDiff = false;
		for(int iIdx = 0; iIdx < codeList.size(); iIdx++){
			if(null == strLastCheck){
				strFirstCode = strLastCheck = codeList.get(iIdx).mstrCode;
				lFirstCodeTs = codeList.get(iIdx).mlTs;
				continue;
			}
			
			if(!codeList.get(iIdx).mstrCode.equals(strLastCheck) || TONE_PERIOD <= (codeList.get(iIdx).mlTs - lFirstCodeTs)){
				bDiff = true;
			}
			
			if(codeList.get(iIdx).mstrCode.equals(strFirstCode) && TONE_PERIOD > (codeList.get(iIdx).mlTs - lFirstCodeTs) && iIdx == iNumSameFst){
				iNumSameFst++;
				iLastIdxSameFst = iIdx;
			}
			
			strLastCheck = codeList.get(iIdx).mstrCode;
		}
		
		List<FreqRecord> sesFreqList = new ArrayList<FreqRecord>();
		if(false == bDiff){
			checkEmptySlot();
			
			Log.i(TAG, "pickWithoutSession(), all the same, code = ["+mFreqRecordList.get(0).mstrCode+"]");
		
			long lFirstTs = mFreqRecordList.get(0).mlTs;;//(-1 == sLastCheckToneTs)?sFreqRecordList.get(0).mlTs:sLastCheckToneTs;
			
			long lStartTs = mFreqRecordList.get(0).mlTs;
			
			for(int i = 0 ; i <= iCheckIndex; i++){
				if(TONE_PERIOD > (mFreqRecordList.get(0).mlTs - lFirstTs)){
					mLastCheckToneTs = mFreqRecordList.get(0).mlTs;
					sesFreqList.add(mFreqRecordList.remove(0));
				}else{
					Log.w(TAG, "pickWithoutSession(), break for itm > TONE_PERIOD");
					break;
				}
			}
			
//			for(int i = 0 ; i <= iCheckIndex; i++){
//				sFreqRecordList.remove(0);
//			}
			appendRet(new CodeRecord(lStartTs, mLastCheckToneTs, strFirstCode, sesFreqList));
		}//else if((iNumSameFst) >= ((iCheckIndex/2)+1)){
		else if((iNumSameFst) >= 2){
			
			checkEmptySlot();
			
			Log.i(TAG, "pickWithoutSession(), first itm happens over 50 %, code = ["+mFreqRecordList.get(0).mstrCode+"]");
			long lFirstTs = mFreqRecordList.get(0).mlTs;
			
			long lStartTs = mFreqRecordList.get(0).mlTs;
			for(int i = 0 ; i <= iLastIdxSameFst; i++){
				if(TONE_PERIOD > (mFreqRecordList.get(0).mlTs - lFirstTs)){
					mLastCheckToneTs = mFreqRecordList.get(0).mlTs;
					sesFreqList.add(mFreqRecordList.remove(0));
				}else{
					Log.w(TAG, "pickWithoutSession(), break for itm > TONE_PERIOD");
					break;
				}
			}
			appendRet(new CodeRecord(lStartTs, mLastCheckToneTs, strFirstCode, sesFreqList));
		}else if(0 == iNumSameFst){
			Log.i(TAG, "pickWithoutSession(), remove first noise = ["+strFirstCode+"], sLastCheckToneTs="+mLastCheckToneTs);
			if(true == mbStartAppend){
				FreqRecord rec = mFreqRecordList.get(0);
				if((TONE_PERIOD*RANDUDANT_RATIO) <= (mFreqRecordList.get(1).mlTs - mLastCheckToneTs)){
					Log.w(TAG, "analyze(), -> Add first noise = ["+strFirstCode+"]");
					mLastCheckToneTs = mFreqRecordList.get(0).mlTs+TONE_PERIOD/2;
					appendRet(new CodeRecord( mLastCheckToneTs-TONE_PERIOD, 
											  mLastCheckToneTs, 
											  mFreqRecordList.get(0).mstrCode, 
											  null));
				}
				
				mFreqRecordList.remove(rec);
				mLastAbandant = rec.mstrCode;
				
			}else 
				mFreqRecordList.remove(0);
		}else if(2 <= iCheckIndex){
			Log.i(TAG, "pickWithoutSession(), else case -> remove first noise = ["+strFirstCode+"], sLastCheckToneTs="+mLastCheckToneTs);
			if(true == mbStartAppend){
				FreqRecord rec = mFreqRecordList.get(0);
				if((TONE_PERIOD*RANDUDANT_RATIO) <= (mFreqRecordList.get(1).mlTs - mLastCheckToneTs)){
					Log.i(TAG, "analyze(), else case -> Add first noise = ["+strFirstCode+"]");
					
					mLastCheckToneTs = rec.mlTs+TONE_PERIOD/2;
					appendRet(new CodeRecord( mLastCheckToneTs-TONE_PERIOD, 
											  mLastCheckToneTs, 
											  rec.mstrCode, 
											  null));
				}
				mFreqRecordList.remove(rec);
				mLastAbandant = rec.mstrCode;
			}else 
				mFreqRecordList.remove(0);
		}
	}
	
	private void checkEmptySlot(){
		if(true == mbStartAppend){
			if(-1 != mLastCheckToneTs){
//				long lTimeFromLastOne = sFreqRecordList.get(0).mlTs - sLastCheckToneTs;
//				
//				if(TONE_PERIOD < lTimeFromLastOne){
//					int iNumRedundant = (int) (lTimeFromLastOne/TONE_PERIOD);
//					for(int i = 0; i < iNumRedundant; i++){
//						Log.w(TAG, "checkEmptySlot(), add redundant one, sLastCheckToneTs = "+sLastCheckToneTs+", sLastAbandant="+sLastAbandant);
//						//if(sLastAbandant.equals("W") || sLastAbandant.equals("X") || sLastAbandant.equals("Y") || sLastAbandant.equals("Z"))
//						if(sLastAbandant.equals(sFreqRangeTable.get(sFreqRangeTable.size()-2).mstrCode) || sLastAbandant.equals(sFreqRangeTable.get(sFreqRangeTable.size()-1).mstrCode))
//							sLastAbandant = "0";
//						
//						sLastCheckToneTs +=TONE_PERIOD;
//						appendRet((sLastCheckToneTs-TONE_PERIOD/2), 
//								  (sLastCheckToneTs+TONE_PERIOD/2),  
//								  sLastAbandant);
//					}
//				}
			}
		}
	}
	
	private void appendRet(CodeRecord rec){
		msbDecode.append(rec.strCdoe);
		
		if(true == mbStartAppend){
			CodeRecord lastItm = (0 < mCodeRecordList.size())?mCodeRecordList.get(mCodeRecordList.size()-1):null;
			if(null != lastItm && lastItm.strCdoe.equals(rec.strCdoe) && (TONE_PERIOD >= (rec.lEndTs - lastItm.lStartTs))){
				lastItm.lEndTs = rec.lEndTs;
			}else{
				mCodeRecordList.add(rec);
			}
		}else if(false == mbStartAppend){
			if(-1 < msbDecode.lastIndexOf(PREFIX_DECODE)){
				mCodeRecordList.add(rec);
			}else if(SEGMENT_FEATURE && (msbDecode.length()-1) == msbDecode.lastIndexOf(PREFIX_DECODE.substring(0,1))){
				Log.e(TAG, "appendRet(), detect FIRST CHAR ------------------------------------------------------");
				
				//if First Char is detected already
				if(-1 < mSessionBeginTs){
					if(rec.lStartTs - mSessionBeginTs > TONE_PERIOD && null != rec.mlstFreqRec){
						Log.e(TAG, "appendRet(), lStartTs - sSessionBeginTs > TONE_PERIOD ==> recover and recheck");
						for(int i = rec.mlstFreqRec.size() -1; i >= 0;i--){
							FreqRecord fr = rec.mlstFreqRec.get(i);
							if(!fr.mstrCode.equals(MISSING_CHAR)){
								mFreqRecordList.add(0, fr);
							}
						}
						mSessionBeginTs= -1;
						mSessionBeginBufIdx = -1;
						int iSize = mCodeRecordList.size();
						if(0 < iSize && mCodeRecordList.get(iSize-1).strCdoe.equals(rec.strCdoe)){
							mCodeRecordList.remove(iSize - 1);
						}
						
						pickWithoutSession(exceedToneCheckPeriod());
						return;
					}else{
						Log.e(TAG, "appendRet(), lStartTs - sSessionBeginTs == TONE_PERIOD ==> no need to move");
						checkFirstCharOfPrefix(rec);
					}
				}else{
					checkFirstCharOfPrefix(rec);
				}
			}
		}
		appendRet(rec.strCdoe);
	}
	
	private void appendRet(String strCode){
		if(false == mbInSenderMode){
			if(false == mbStartAppend){
				if(-1 < msbDecode.lastIndexOf(PREFIX_DECODE)){
					Log.e(TAG, "appendRet(), detect PREFIX_DECODE ------------------------------------------------------");
					mbStartAppend = true;
					mLastAbandant = "0";
					msbDecode = new StringBuilder();
					mIFreqAnalyzeResultCBListener.onDetectStart();
					mIFreqAnalyzeResultCBListener.onSetResult("", "", "", !mbNeedToAutoCorrection, mprevMatchRet);
						
					CodeRecord lastTwoRec = mCodeRecordList.get(mCodeRecordList.size()-2);
					CodeRecord lastRec = mCodeRecordList.get(mCodeRecordList.size()-1);
					mCodeRecordList.clear();
					mCodeRecordList.add(lastTwoRec);
					mCodeRecordList.add(lastRec);
					
					mSessionOffset = segmentCheck(false);
					
					if(PRE_EMPTY){
						for(int i =0; i < mFreqRecordList.size();){
							if(mFreqRecordList.get(0).mlTs - lastRec.lEndTs <= TONE_PERIOD){
								Log.w(TAG, "appendRet(), ("+mFreqRecordList.get(0).mlTs +" - "+lastRec.lEndTs +") <= TONE_PERIOD:"+TONE_PERIOD+", remove");
								mFreqRecordList.remove(0);
							}else{
								break;
							}
						}
					}					
				}
			}else{
				int iIndex = -1;
				if(-1 < (iIndex = checkPostfix())){
					mbStartAppend = false;
					int iShift = checkFrameBySessionAndAutoCorrection();
					int iNewIndex = checkPostfix();
					Log.e(TAG, "appendRet(), redetect index, iShift = "+iShift+", iNewIndex="+iNewIndex);
					
					int iDxFstC = msbDecode.indexOf(POSTFIX_DECODE_C1);
					int iDxSndC = msbDecode.indexOf(POSTFIX_DECODE_C2);
					
					if(0 <= iNewIndex){
						if(0 < iDxFstC && iDxFstC < iNewIndex){//special case 1: ...H...HI
							Log.e(TAG, "appendRet(), special case 1, iDxFstC="+iDxFstC);
							iNewIndex = iDxFstC;
						}else if(0 < iDxSndC && iDxSndC < iNewIndex+1){//special case 2: ...I...HI
							Log.e(TAG, "appendRet(), special case 2, iDxSndC="+iDxSndC);
							iNewIndex = iDxSndC - 1;
						}
					}else{
						Log.e(TAG, "appendRet(), can not find postfix, redetect index at first char");
						if(-1 == iDxFstC){
							Log.e(TAG, "appendRet(), can not find first car of postfix, redetect index at second char");
							if(-1 == iDxSndC){
								Log.e(TAG, "appendRet(), can not find any char of postfix, redetect index by shift one");
								//iNewIndex = (iIndex-iShift);
							}else{
								iNewIndex = iDxSndC - 1;
							}
						}else{
							iNewIndex = iDxFstC;
						}
					}
					
					if(-1 < iNewIndex && iNewIndex != iIndex){
						Log.e(TAG, "appendRet(), change index from "+iIndex+" to "+iNewIndex);
						iIndex = iNewIndex;
					}
					
					checkResult(optimizeDecodeString(iIndex));
					mFreqRecordList.clear();
				}else{
					mIFreqAnalyzeResultCBListener.onAppendResult(strCode);
					//msbDecode.append(strCode);
				}
			}
		}else{
			if(-1 < msbDecode.lastIndexOf(PEER_SIGNAL)){
				msbDecode.delete(0, msbDecode.length());
				mIFreqAnalyzeResultCBListener.onSetResult(PEER_SIGNAL, "", "", !mbNeedToAutoCorrection, mprevMatchRet);
			}
		}
	}
	
	private String optimizeDecodeString(int iIndex){
		//String strDecode = msbDecode.substring(0, msbDecode.length()-((-1 < msbDecode.lastIndexOf(POSTFIX_DECODE) )?POSTFIX_DECODE.length():1));
		String strDecode = msbDecode.substring(0, iIndex);
		int iLen = strDecode.length();
		if(AubioTestConfig.getMultiplyByFFTYPE() > 1 && 0 != iLen%AubioTestConfig.getMultiplyByFFTYPE()){
			if(0 < getMsgLength(iLen+1)){
				strDecode = sCodeTable.get(0)+strDecode;
				Log.e(TAG, "optimizeDecodeString(), add dummy char at head because index is "+iIndex);
			}else if(0 < getMsgLength(iLen-1)){
				strDecode = strDecode.substring(0, iLen -1);
				Log.e(TAG, "optimizeDecodeString(), remove dummy char at tail because index is "+iIndex);
			}else{
				Log.e(TAG, "optimizeDecodeString(), can not optimize, index is "+iIndex);
			}
		}
		return strDecode;
	}
	
	private int checkPostfix(){
		int iRet = msbDecode.lastIndexOf(POSTFIX_DECODE);
		if(-1 >= iRet){
//			String strFstPostfix = POSTFIX_DECODE.substring(0,1);
//			String strSndPostfix = POSTFIX_DECODE.substring(1,2);
//			iRet = msbDecode.lastIndexOf(strSndPostfix+strSndPostfix);
//			if(0 < iRet){
//				Log.e(TAG, "checkPostfix(), detect "+strSndPostfix+strSndPostfix+" +++++++++++++++++++++++++++++++++++++++++++++++++++++");
//			}else{
//				iRet = msbDecode.lastIndexOf(strFstPostfix+strFstPostfix);
//				if(0 < iRet){
//					Log.e(TAG, "checkPostfix(), detect "+strFstPostfix+strFstPostfix+" +++++++++++++++++++++++++++++++++++++++++++++++++++++");
//				}
//			}
		}else{
			Log.e(TAG, "checkPostfix(), detect POSTFIX_DECODE +++++++++++++++++++++++++++++++++++++++++++++++++++++");
		}
		return iRet;
	}
	
	private void checkFirstCharOfPrefix(CodeRecord rec){
		mSessionBeginTs = rec.lStartTs;
		mSessionBeginBufIdx = rec.mlstFreqRec.get(0).miBufIndex;
		//mSessionOffset = segmentCheckOnFirst(false);
		
		Log.e(TAG, "checkFirstCharOfPrefix(), sSessionBeginTs:"+mSessionBeginTs+", sSessionBeginBufIdx:"+mSessionBeginBufIdx+",\n sesFreqList:"+rec.mlstFreqRec);
		//check next char
		if(null != rec.mlstFreqRec && null != mFreqRecordList && 0 < mFreqRecordList.size()){
			FreqRecord fr = mFreqRecordList.get(0);
			//FreqRecord frBeginOfFirstChar = sesFreqList.get(0);
			FreqRecord frEndOfFirstChar = rec.mlstFreqRec.get(rec.mlstFreqRec.size()-1);
			
			//if only 2 consecutive digits and next is second tone
			if((TONE_FRAME_COUNT-1) == rec.mlstFreqRec.size() && (frEndOfFirstChar.mlTs + FRAME_TS) == fr.mlTs){
				if(PREFIX_DECODE.substring(1,2).equals(fr.mstrCode)){
					Log.e(TAG, "checkFirstCharOfPrefix(), need to shift one digit backward");
					mSessionBeginTs -= FRAME_TS;
					rec.mlstFreqRec.add(0, new FreqRecord(mSessionBeginTs, -1.0f, MISSING_CHAR, -1, null));
				}else{
					Log.e(TAG, "checkFirstCharOfPrefix(), need to fill CodeRecord");
					rec.mlstFreqRec.add(mFreqRecordList.remove(0));
					rec.lEndTs += FRAME_TS;
				}
			}
			else if((frEndOfFirstChar.mlTs + FRAME_TS) == fr.mlTs && PREFIX_DECODE.substring(0,1).equals(fr.mstrCode)){
				Log.e(TAG, "checkFirstCharOfPrefix(), shift one frame_ts");
				if(rec.mlstFreqRec.size() == TONE_FRAME_COUNT){
					rec.mlstFreqRec.remove(0);
				}
				
				rec.mlstFreqRec.add(mFreqRecordList.remove(0));
				mSessionBeginTs = rec.lStartTs += FRAME_TS;
				rec.lEndTs += FRAME_TS;
				mSessionBeginBufIdx = (++mSessionBeginBufIdx)%AudioBufferMgr.MAX_QUEUE_SIZE;
			} 
			
			mCodeRecordList.add(rec);
		}
	}
	
	public void setSessionOffset(int iOffset){
		mSessionOffset = iOffset;
	}
	
	public int getSessionOffset(){
		return mSessionOffset;
	}
	
	private void amplitudeTest(int iBufIndex){
		Log.e(TAG, "amplitudeTest(), on iBufIndex:"+(iBufIndex-1)+"-------------------------------------");
		AudioBufferMgr.getInstance().getBufByIndex(iBufIndex-1, 0, bufSegment);
		for(int idx =0; idx< bufSegment.length;idx++)
			Log.e(TAG, "amplitudeTest(), bufSegment[:"+idx+"]="+Math.abs(bufSegment[idx]/32767.0f));
		
		Log.e(TAG, "amplitudeTest(), on iBufIndex:"+iBufIndex+"-------------------------------------");
		AudioBufferMgr.getInstance().getBufByIndex(iBufIndex, 0, bufSegment);
		for(int idx =0; idx< bufSegment.length;idx++)
			Log.e(TAG, "amplitudeTest(), bufSegment[:"+idx+"]="+Math.abs(bufSegment[idx]/32767.0f));
		
		Log.e(TAG, "amplitudeTest(), on iBufIndex:"+(iBufIndex+1)+"-------------------------------------");
		AudioBufferMgr.getInstance().getBufByIndex(iBufIndex+1, 0, bufSegment);
		for(int idx =0; idx< bufSegment.length;idx++)
			Log.e(TAG, "amplitudeTest(), bufSegment[:"+idx+"]="+Math.abs(bufSegment[idx]/32767.0f));
	}
	
	private int segmentCheckOnFirst(boolean bForcePerform){
		int iRet = 0;
		if((SEGMENT_OFFSET_FEATURE && mbNeedToAutoCorrection) || bForcePerform){
			
			long lTsBegin = System.currentTimeMillis();
			FreqRange fr = sFreqRangeTable.get(sCodeTable.indexOf(PREFIX_DECODE.substring(0,1)));
			int iBufIdxTOCheck = mSessionBeginBufIdx;
			
//			amplitudeTest(iBufIdxTOCheck);
//		
//			int iPart = FRAME_SIZE_REC/10;
//			for(int i = 0; i <= 10; i++){
//				int iOffset = - Math.min((int) (i*iPart),FRAME_SIZE_REC-1);
//				
//				AudioBufferMgr.getInstance().getBufByIndex(iBufIdxTOCheck, iOffset, bufSegment);
//				//Log.e(TAG, "segmentCheck(), bufSegment[iOffset]:"+bufSegment[Math.abs(iOffset)]);
//				float freq = mIFreqAnalyzeResultCBListener.onBufCheck(bufSegment, 0 == i);
//				//Log.e(TAG, "segmentCheck(), iBufIdxTOCheck:"+iBufIdxTOCheck+", freq:"+freq+", iOffset:"+iOffset+", sSessionOffset:"+sSessionOffset);
//				if(0.0 >= freq || false == fr.withinFreqRange(freq)){
//					iRet= Math.min(iOffset, 0);// + FRAME_SIZE_REC;
//					Log.e(TAG, "segmentCheckOnFirst(), iBufIdxTOCheck:"+iBufIdxTOCheck+", freq:"+freq+", iOffset:"+iOffset+", iRet:"+iRet);
//					break;
//				}
//			}
			
			final int MAX_SILENCE_SMAPLE = 13;
			final short sSilence = (short) (32767.0f * SILENCE_CRITERIA);
			boolean bCapture = false;
			int idxFirstDetect = -1, idxLastDetect = -1, iSilenceSample = 0;
			
			Log.e(TAG, "segmentCheckOnFirst(), on iBufIndex:"+iBufIdxTOCheck+"-------------------------------------");
			AudioBufferMgr.getInstance().getBufByIndex(iBufIdxTOCheck, 0, bufSegment);
			for(int idx =0; idx< bufSegment.length;idx++){
				if(sSilence < Math.abs(bufSegment[idx])){
					if(-1 == idxFirstDetect){
						idxFirstDetect = idx;
						Log.e(TAG, "amplitudeTest(), idxFirstDetect:"+idxFirstDetect);
					}else{
						idxLastDetect = idx;
						if((idxLastDetect - idxFirstDetect) > SILENCE_DETECTION_SAMPLE){
							Log.e(TAG, "amplitudeTest(), idxLastDetect:"+idxLastDetect);
							break;
						}
					}
					iSilenceSample = 0;
				}else{
					iSilenceSample++;
					if(MAX_SILENCE_SMAPLE < iSilenceSample && -1 < idxFirstDetect && -1 == idxLastDetect){
						Log.e(TAG, "amplitudeTest(), reset idxFirstDetect");
						idxFirstDetect = -1;
						iSilenceSample = 0;
					}
				}
				//Log.i(TAG, "amplitudeTest(), bufSegment[:"+idx+"]="+Math.abs(bufSegment[idx]/32767.0f));
			}
			
			if(-1 < idxFirstDetect && (FRAME_SIZE_REC - idxFirstDetect) < SILENCE_DETECTION_SAMPLE){
				Log.e(TAG, "iBufIdxTOCheck(), continue to check iBufIndex:"+(iBufIdxTOCheck+1)+"-------------------------------------");
				AudioBufferMgr.getInstance().getBufByIndex(iBufIdxTOCheck+1, 0, bufSegment);
				
				for(int idx = 0; idx < bufSegment.length; idx++){
					if(sSilence < Math.abs(bufSegment[idx])){
						if(-1 == idxFirstDetect){
							idxFirstDetect = idx + FRAME_SIZE_REC;
							Log.e(TAG, "amplitudeTest(), idxFirstDetect:"+idxFirstDetect);
						}else{
							idxLastDetect = idx + FRAME_SIZE_REC;
							if((idxLastDetect - idxFirstDetect) > SILENCE_DETECTION_SAMPLE){
								Log.e(TAG, "amplitudeTest(), idxLastDetect:"+idxLastDetect);
								break;
							}
						}
						iSilenceSample = 0;
					}else{
						iSilenceSample++;
						if(MAX_SILENCE_SMAPLE < iSilenceSample && -1 < idxFirstDetect && -1 == idxLastDetect){
							Log.e(TAG, "amplitudeTest(), reset idxFirstDetect");
							idxFirstDetect = -1;
							iSilenceSample = 0;
						}
					}
					
					//Log.i(TAG, "amplitudeTest(), bufSegment[:"+idx+"]="+Math.abs(bufSegment[idx]/32767.0f));
				}
			}
			
			if((idxLastDetect - idxFirstDetect) > SILENCE_DETECTION_SAMPLE){
				bCapture = true;
				iRet = idxFirstDetect;
				
				AudioBufferMgr.getInstance().getBufByIndex(iBufIdxTOCheck, idxFirstDetect, bufSegment);
				mIFreqAnalyzeResultCBListener.onBufCheck(bufSegment, 0, false, null);
				float freq = mIFreqAnalyzeResultCBListener.onBufCheck(bufSegment, 0, false, null);
				Log.e(TAG, "segmentCheckOnFirst(), iBufIdxTOCheck:"+iBufIdxTOCheck+", freq:"+freq+", iRet:"+iRet);
			}
			
			if(bCapture == false){
				Log.e(TAG, "iBufIdxTOCheck(), on iBufIndex:"+(iBufIdxTOCheck+1)+"-------------------------------------");
				AudioBufferMgr.getInstance().getBufByIndex(iBufIdxTOCheck+1, 0, bufSegment);
				idxFirstDetect = -1;
				idxLastDetect = -1;
				iSilenceSample = 0;
				
				for(int idx =0; idx< bufSegment.length;idx++){
					if(sSilence < Math.abs(bufSegment[idx])){
						if(-1 == idxFirstDetect){
							idxFirstDetect = idx;
							Log.e(TAG, "amplitudeTest(), idxFirstDetect:"+idxFirstDetect);
						}else{
							idxLastDetect = idx;
							if((idxLastDetect - idxFirstDetect) > SILENCE_DETECTION_SAMPLE){
								Log.e(TAG, "amplitudeTest(), idxLastDetect:"+idxLastDetect);
								break;
							}
						}
						iSilenceSample = 0;
					}else{
						iSilenceSample++;
						if(MAX_SILENCE_SMAPLE < iSilenceSample && -1 < idxFirstDetect && -1 == idxLastDetect){
							Log.e(TAG, "amplitudeTest(), reset idxFirstDetect");
							idxFirstDetect = -1;
							iSilenceSample = 0;
						}
					}
					
					//Log.i(TAG, "amplitudeTest(), bufSegment[:"+idx+"]="+Math.abs(bufSegment[idx]/32767.0f));
				}
				
				if((idxLastDetect - idxFirstDetect) > SILENCE_DETECTION_SAMPLE){
					bCapture = true;
					iRet = idxFirstDetect;
				}
			}
			
			Log.e(TAG, "segmentCheckOnFirst(), iBufIdxTOCheck:"+iBufIdxTOCheck+", iRet:"+iRet+", bCapture:"+bCapture);
//			if(iRet > (FRAME_SIZE_REC - SILENCE_DETECTION_SAMPLE)){
//				Log.e(TAG, "segmentCheckOnFirst(), iBufIdxTOCheck:"+iBufIdxTOCheck+", iRet:"+iRet+", bCapture:"+bCapture);
//			}
			
			Log.d(TAG, "segmentCheckOnFirst(), takes "+(System.currentTimeMillis() - lTsBegin)+" ms at "+mSessionBeginBufIdx);
		}
		return iRet;
	}
	
	private int segmentCheck(boolean bForcePerform){
		int iRet = 0;
		if(SEGMENT_OFFSET_FEATURE || bForcePerform){
			long lTsBegin = System.currentTimeMillis();
			FreqRange firstFR = sFreqRangeTable.get(sCodeTable.indexOf(PREFIX_DECODE.substring(0,1)));
			if(0 < mCodeRecordList.size()){
				List<FreqRecord> fRecLst = mCodeRecordList.get(0).mlstFreqRec;
				int iBufIdxTOCheck = mSessionBeginBufIdx;
				int iLen = (fRecLst != null)?fRecLst.size():-1;
				for(int i = 1; i < iLen; i++){
					if(fRecLst.get(i).mstrCode.equals(firstFR.mstrCode)){
						iBufIdxTOCheck++;
					}
				}
				
				boolean bWithinSecondRange = false;
				//Check if there is first code within second range
				List<FreqRecord> fRecLst2 = mCodeRecordList.get(1).mlstFreqRec;
				int iLen2 = (fRecLst2 != null)?fRecLst2.size():-1;
				for(int i = 0; i < iLen2; i++){
					if(fRecLst2.get(i).mstrCode.equals(firstFR.mstrCode)){
						Log.i(TAG, "segmentCheck(), there is first code within second range, fRecLst2:"+fRecLst2);
						iBufIdxTOCheck++;
						bWithinSecondRange =true;
						break;
					}
				}
			
				int iPart = FRAME_SIZE_REC/10;
				for(int i = 0; i <= 10; i++){
					int iOffset = Math.min((int) (i*iPart),FRAME_SIZE_REC-1);
					
					AudioBufferMgr.getInstance().getBufByIndex(iBufIdxTOCheck, iOffset, bufSegment);
					//Log.e(TAG, "segmentCheck(), bufSegment[iOffset]:"+bufSegment[Math.abs(iOffset)]);
					float freq = mIFreqAnalyzeResultCBListener.onBufCheck(bufSegment, 0, 0 == i, null);
					//Log.e(TAG, "segmentCheck(), iBufIdxTOCheck:"+iBufIdxTOCheck+", freq:"+freq+", iOffset:"+iOffset);
					if(0.0 >= freq || false == firstFR.withinFreqRange(freq)){
						iRet= iOffset - SEG_SES_OFFSET*iPart;//Math.max(iOffset - 3*iPart, 0)+(bWithinSecondRange?FRAME_SIZE_REC:0);//Math.min(iOffset + iPart, 0);
						Log.e(TAG, "segmentCheck(), iBufIdxTOCheck:"+iBufIdxTOCheck+", freq:"+freq+", iOffset:"+iOffset+", iRet:["+iRet+"]");
						break;
					}
				}
				Log.d(TAG, "segmentCheck(), takes "+(System.currentTimeMillis() - lTsBegin)+" ms at "+mSessionBeginBufIdx);
			}else{
				Log.e(TAG, "segmentCheck(), empty mCodeRecordList");
			}	
		}
		return iRet;
	}
	
//	private String RemoveUnusedChar(){
//		StringBuilder strRet = new StringBuilder();
//		int iLen = msbDecode.length();
//		int iMaxIndex = AubioTestConfig.getDivisionByFFTYPE();
//		for(int i = 0; i< iLen;i++){
//			String strCode = msbDecode.substring(i, i+1);
//			if(iMaxIndex > sCodeTable.indexOf(strCode)){
//				strRet.append(strCode);
//			}
//		}
//		Log.e(TAG, "RemoveUnusedChar(), ssbDecode = "+msbDecode+", strRet = "+strRet);
//		return strRet.toString();
//	}
	
	private String replaceInvalidChar(String strDecode){
		final int iLenPrefix = PREFIX_DECODE.length();
		StringBuilder strRet = new StringBuilder();
		int iLen = strDecode.length();
		int iMaxIndex = AubioTestConfig.getDivisionByFFTYPE();
		final String strDefReplaced = sCodeTable.get(0);
		
		//there is redundant digit check it from end
		if(0 != strDecode.length()%AubioTestConfig.getMultiplyByFFTYPE()){
			for(int i = iLen -1; i >=0; i--){
				String strCode = strDecode.substring(i, i+1);
				if(iMaxIndex <= sCodeTable.indexOf(strCode)){
					strDecode = ((i > 0)?strDecode.substring(0, i):"")+((i+1 < iLen)?strDecode.substring(i+1):"");
					if(i+iLenPrefix < mCodeRecordList.size()){
						mCodeRecordList.remove(i+iLenPrefix);
					}
					
					Log.e(TAG, "replaceInvalidChar(), remove last illegal char "+strCode+" at "+i+", strDecode = "+strDecode);
				}
			}
		}
		
		iLen = strDecode.length();
		
		for(int i = 0; i< iLen;i++){
			String strCode = strDecode.substring(i, i+1);
			if(iMaxIndex > sCodeTable.indexOf(strCode)){
				strRet.append(strCode);
			}else{
				String strReplace = strDefReplaced;
				if(i+iLenPrefix < mCodeRecordList.size()){
					CodeRecord cr = mCodeRecordList.get(i+iLenPrefix);
					if(null != cr){
						List<FreqRecord> lst = cr.mlstFreqRec;
						if(null != lst){
							int iLenLst = lst.size();
							for(int idx = iLenLst - 1; idx >=0; idx--){
								if(iMaxIndex > sCodeTable.indexOf(lst.get(idx).mstrCode)){
									strReplace = lst.get(idx).mstrCode;
								}
							}
						}
					}
					Log.i(TAG, "replaceInvalidChar(), replace ["+i+"] char  "+strCode+" to "+strReplace);
					mCodeRecordList.get(i+iLenPrefix).strCdoe = strReplace;
				}
				strRet.append(strReplace);
			}
		}
		Log.e(TAG, "replaceInvalidChar(), strDecode = "+strDecode+", \n" +
				   "                         strRet = "+strRet);
		return strRet.toString();
	}
	
	public boolean checkEndPoint(){
		//detect first char first
		int iIndex = msbDecode.indexOf(POSTFIX_DECODE_C1);
		if(0 > iIndex){
			Log.e(TAG, "checkEndPoint(), can not detect first char of POSTFIX_DECODE");
			//detect second char first
			iIndex = msbDecode.indexOf(POSTFIX_DECODE_C2, 1);
			if(0 <= iIndex){
				iIndex -=1;
				Log.e(TAG, "checkEndPoint(), detect second char of POSTFIX_DECODE, iIndex:"+iIndex+", +++++++++++++++++++++++++++++++++++++++++++++++++++++");
			}
		}else{
			Log.e(TAG, "checkEndPoint(), detect first char of POSTFIX_DECODE, iIndex:"+iIndex+", +++++++++++++++++++++++++++++++++++++++++++++++++++++");
			int iDxSndC = msbDecode.indexOf(POSTFIX_DECODE_C2, 1);
			if(0 <= iDxSndC && iDxSndC < iIndex){
				iIndex = iDxSndC - 1;
				Log.e(TAG, "checkEndPoint(), detect second char is prior to first char, iDxSndC:"+iDxSndC+", +++++++++++++++++++++++++++++++++++++++++++++++++++++");
			}
		}
		
		if(-1 < iIndex){
			mbStartAppend = false;
			int iShift = checkFrameBySessionAndAutoCorrection();
			
			if(0 != iShift){
				Log.e(TAG, "checkEndPoint(), redetect index, iShift = "+iShift);
				//We may get postfix after shift
				int iNewIndex = msbDecode.indexOf(POSTFIX_DECODE);
				
				Log.e(TAG, "checkEndPoint(), redetect index, iShift = "+iShift+", iNewIndex="+iNewIndex);
				
				int iDxFstC = msbDecode.indexOf(POSTFIX_DECODE_C1);
				int iDxSndC = msbDecode.indexOf(POSTFIX_DECODE_C2);
				
				if(0 <= iNewIndex){
					if(0 < iDxFstC && iDxFstC < iNewIndex){//special case 1: ...H...HI
						Log.e(TAG, "checkEndPoint(), special case 1, iDxFstC="+iDxFstC);
						iNewIndex = iDxFstC;
					}else if(0 < iDxSndC && iDxSndC < iNewIndex+1){//special case 2: ...I...HI
						Log.e(TAG, "checkEndPoint(), special case 2, iDxSndC="+iDxSndC);
						iNewIndex = iDxSndC - 1;
					}
				}else{
					if(-1 == iDxFstC){
						Log.e(TAG, "checkEndPoint(), can't detect first char of POSTFIX_DECODE, redetect index by second char ");
						if(-1 == iDxSndC){
							Log.e(TAG, "checkEndPoint(), can't detect second char of POSTFIX_DECODE, redetect index by shift");
							//iNewIndex = (iIndex-iShift);
						}else{
							iNewIndex = iDxSndC - 1;
						}
					}else{
						iNewIndex = iDxFstC;
					}
				}
				
				if(-1 < iNewIndex && iNewIndex != iIndex){
					Log.e(TAG, "checkEndPoint(), change index from "+iIndex+" to "+iNewIndex);
					iIndex = iNewIndex;
				}
			}
			
			checkResult(optimizeDecodeString(iIndex));
			return true;
		}else{
			Log.e(TAG, "checkEndPoint(), can not detect first char of POSTFIX_DECODE +++++++++++++++++++++++++++++++++++++++++++++++++++++");
		}
		return false;
	}
	
	private void checkResult(String strDecode){
//		strDecode = replaceInvalidChar(strDecode);
//		String strDecodeUnmark = removeDividerAndUnmark(strDecode);
		
		//sIFreqAnalyzeResultCBListener.onSetResult();

		
		String strDecodeUnmark = replaceInvalidChar(strDecode);
		msbDecode = new StringBuilder();
		if(mCodeRecordList.size() > 0){
			//sCodeRecordList.remove(sCodeRecordList.size()-1);
//			if(-1 < ssbDecode.lastIndexOf(POSTFIX_DECODE)){
//				sCodeRecordList.remove(sCodeRecordList.size()-1);
//			}
			
			for(CodeRecord rec : mCodeRecordList){
				Log.i(TAG, "checkResult(),===========>>>>>>>>>>>>>> rec= "+rec.toString() );
			}
		}
		
		String strRet = decodeRSEC(strDecodeUnmark);
		
		//int iLenByTime = getMsgLengthByTime();
//		String strRet = decodeRSEC((AubioTestConfig.getMultiplyByFFTYPE() == 1)?strDecodeUnmark
//				                                                               :((strDecodeUnmark.length()%AubioTestConfig.getMultiplyByFFTYPE() == 0)?strDecodeUnmark
//				                                                            		                                                                  :correctErrByDelta(1)));//Need to modify!!!!
//		if(strRet.startsWith("error")){
//			int iTrial = 0;
//			while(++iTrial <= 3 && strRet.startsWith("error")){
//				strRet = decodeRSEC(correctErrByDelta(iTrial));
//			}
//			
//			iTrial = 0;
//			while(++iTrial <= 3 && strRet.startsWith("error")){
//				strRet = decodeRSEC(correctErrByDelta(-iTrial));
//			}
//		}
		
		/*if(strRet.startsWith("error") && canPerformAutoCorrection()){
			Log.i(TAG, "checkResult(), strRet:"+strRet+", mstrCodeTrace:"+mstrCodeTrace);
			performAutoCorrection();
		}else*/{
			mIFreqAnalyzeResultCBListener.onSetResult(strRet, strDecode, strDecodeUnmark, !mbNeedToAutoCorrection, mprevMatchRet);
			if(false == canPerformAutoCorrection()){
				//set mIFreqAnalyzeResultCBListener as null, if it's one self analyzer
				mIFreqAnalyzeResultCBListener = null;
			}
		}
	}
	
	private int checkFrameBySessionAndAutoCorrection(){
		int iRet = 0;
		int iNumOfBias = getNumOfBias(mCodeRecordList);
		if(0 == iNumOfBias){
			Log.i(TAG, "checkFrameBySessionAndAutoCorrection(), iNumOfBias = 0, no need to adjust");
		}else{
			List<CodeRecord> lstCodeRecordBackward = getLstCodeRecordByOffset(mCodeRecordList, 1);
			int iNumOfBiasShiftBackward = getNumOfBias(lstCodeRecordBackward);
			
			List<CodeRecord> lstCodeRecordForward = getLstCodeRecordByOffset(mCodeRecordList, -1);
			int iNumOfBiasShiftForward = getNumOfBias(lstCodeRecordForward);
			
			Log.e(TAG, "checkFrameBySessionAndAutoCorrection(), iNumOfBias:"+iNumOfBias+", iNumOfBiasShiftBackward:"+iNumOfBiasShiftBackward+", iNumOfBiasShiftForward:"+iNumOfBiasShiftForward);
			
			if(iNumOfBiasShiftBackward <= iNumOfBiasShiftForward){
				if(iNumOfBiasShiftBackward< iNumOfBias){
					Log.e(TAG, "checkFrameBySessionAndAutoCorrection(), pick lstCodeRecordBackward **********************************************");
					mCodeRecordList.clear();
					mCodeRecordList.addAll(lstCodeRecordBackward);
					regenDecode();
					iRet = 1;
				}else{
					Log.i(TAG, "checkFrameBySessionAndAutoCorrection(), keep origianl");
				}
			}else{
				if(iNumOfBiasShiftForward< iNumOfBias){
					Log.e(TAG, "checkFrameBySessionAndAutoCorrection(), pick lstCodeRecordForward ***************************************************");
					mCodeRecordList.clear();
					mCodeRecordList.addAll(lstCodeRecordForward);
					regenDecode();
					iRet = -1;
				}else{
					Log.i(TAG, "checkFrameBySessionAndAutoCorrection(), keep origianl");
				}
			}
		}
		return iRet;
	}
	
	private void regenDecode(){
		msbDecode = new StringBuilder();
		int iSize = (null != mCodeRecordList)?mCodeRecordList.size():0;
		for(int idx = PREFIX_DECODE.length(); idx < iSize;idx++){
			msbDecode.append(mCodeRecordList.get(idx).strCdoe);
		}
	}
	
	static List<CodeRecord> getLstCodeRecordByOffset(List<CodeRecord> lstCodeRecord, int iOffset){
		Log.i(TAG, "getLstCodeRecordByOffset(), iOffset:"+iOffset+"   ###################################################################################################");
		List<CodeRecord> retLst = new ArrayList<CodeRecord>();
		
		int iSize = (null != lstCodeRecord)?lstCodeRecord.size():0;
		for(int idx = 0; idx + 1 < iSize;idx++){
			int iCurSize = retLst.size();
			if(0 < iOffset){
				retLst.add(CodeRecord.combineNewCodeRecord(lstCodeRecord.get(idx), lstCodeRecord.get(idx+1), iOffset, getToneIdxByCode((0 < iCurSize)?retLst.get(iCurSize-1).strCdoe:null)));
			}else if(0 > iOffset){
				retLst.add(CodeRecord.combineNewCodeRecord(lstCodeRecord.get(idx), lstCodeRecord.get(idx+1), iOffset, getToneIdxByCode((0 < iCurSize)?retLst.get(iCurSize-1).strCdoe:null)));
			}
		}
		
		if(0 < iOffset){
			retLst.add(CodeRecord.combineNewCodeRecord(lstCodeRecord.get(iSize-1), null, iOffset, -1));
		}else if(0 > iOffset){
			retLst.add(0, CodeRecord.combineNewCodeRecord(null, lstCodeRecord.get(0), iOffset, -1));
		}
		
		Log.d(TAG, "getLstCodeRecordByOffset(), iOffset:"+iOffset+", retLst:\n"+retLst);
		
		return retLst;
	}
	
	static private int getNumOfBias(List<CodeRecord> lstCodeRecord){
		int iRet = 0;
		int iSize = (null != lstCodeRecord)?lstCodeRecord.size():0;
		for(int idx = PREFIX_DECODE.length(); idx < iSize;idx++){
			if(false == lstCodeRecord.get(idx).isSameCode()){
				iRet++;
			}
		}
		
		return iRet;
	}
	
	public boolean canPerformAutoCorrection(){
		return mbNeedToAutoCorrection;
	}
	
	public boolean performAutoCorrection(MatchRetSet prevMatchRet){
		if(mbNeedToAutoCorrection){
			autoCorrection(prevMatchRet);
			return true;
		}
		return false;
	}
	
	private void autoCorrection(MatchRetSet prevMatchRet){
		Log.e(TAG, "autoCorrection(),++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		if(null != mCodeRecordList){
			if(null == selfFreqAnalyzer)
				selfFreqAnalyzer = new FreqAnalyzer(false);
			
			selfFreqAnalyzer.mprevMatchRet = prevMatchRet;
			selfFreqAnalyzer.setIFreqAnalyzeResultCB(mIFreqAnalyzeResultCBListener);
			int iOffset = segmentCheckOnFirst(true);//segmentCheck(true);
			selfFreqAnalyzer.setSessionOffset(iOffset);
			
			boolean bFirstTime = true;
			for(CodeRecord rec : mCodeRecordList){
				if(null != rec){
					List<FreqRecord> frRecLst = rec.mlstFreqRec;
					if(null != frRecLst){
						for(FreqRecord fr : frRecLst){
							if(fr.miBufIndex >=0){
								AudioBufferMgr.getInstance().getBufByIndex(fr.miBufIndex, iOffset, bufSegment);
								if(bFirstTime){
									//Workaround
									mIFreqAnalyzeResultCBListener.onBufCheck(bufSegment, 0, true, null);
									bFirstTime = false;
								}
								
								float freq = mIFreqAnalyzeResultCBListener.onBufCheck(bufSegment, fr.mlTs, false, fr.miFFTValues);
								selfFreqAnalyzer.analyze(fr.mlTs, freq, fr.miBufIndex, fr.miFFTValues);
							}else{
								selfFreqAnalyzer.analyze(fr.mlTs, 0.0, -1, fr.miFFTValues);
							}
						}
					}else{
						Log.e(TAG, "autoCorrection(), frRecLst is null for "+rec.strCdoe+", at "+rec.lStartTs);
					}
				}
			}
			
			//to simulate timeout case
			if(selfFreqAnalyzer.mbStartAppend){
				//if not detect postfix code, force to complete it
				if(false == selfFreqAnalyzer.checkEndPoint()){
					selfFreqAnalyzer.triggerTimeout();
				}
			}else{
				//if not get one result, force to timeout
				if(null != selfFreqAnalyzer.mIFreqAnalyzeResultCBListener){
					Log.e(TAG, "autoCorrection(), selfFreqAnalyzer.mbStartAppend = false, trigger timeout" );
					selfFreqAnalyzer.triggerTimeout();
				}
			}
			
			selfFreqAnalyzer.setIFreqAnalyzeResultCB(null);
			selfFreqAnalyzer.reset();
			selfFreqAnalyzer.endToTrace();
		}
		Log.e(TAG, "autoCorrection(),------------------------------------------------------------------------------------------------------");
	}
	
	static private String removeDividerAndUnmark(String strDecode){
//		String[] strSplit = strDecode.split(DIVIDER);
		String strDecodeUnmark = null;
//		if(null != strSplit && strSplit.length == 2){
//			Log.e(TAG, "removeDivider(), strSplit[0] = ["+strSplit[0]+"], strSplit[1] = ["+strSplit[1]+"]");
//			strDecodeUnmark = decodeConsecutiveDigits(strSplit[0])+decodeConsecutiveDigits(strSplit[1]);
//		}else{
//			Log.e(TAG, "removeDivider(), can not find DIVIDER");
			strDecodeUnmark = decodeConsecutiveDigits(strDecode);
//		}
		return strDecodeUnmark;
	}
	
	private String correctErrByDelta(int iDelta){
		Log.i(TAG, "correctErrByDelta(),===========>>>>>>>>>>>>>>iDelta = "+iDelta);
		StringBuilder strRet = new StringBuilder();
		if(0 < iDelta){
			int iTime = iDelta;
			while(iTime > 0){
				CodeRecord[] pairRec = findMaxDelta();
				Log.i(TAG, "correctErrByDelta(),===========>>>>>>>>>>>>>>findMaxDelta,  pairRec[0]= "+pairRec[0]+", pairRec[1]="+pairRec[1]);
				if(null != pairRec[0] && null != pairRec[1])
					mCodeRecordList.add(mCodeRecordList.indexOf(pairRec[1]), new CodeRecord((pairRec[1].lStartTs + pairRec[0].lStartTs)/2, (pairRec[1].lEndTs + pairRec[0].lEndTs)/2, "0"));
				iTime--;
			}
		}else if(0 > iDelta){
			int iTime = Math.abs(iDelta);
			while(iTime > 0){
				CodeRecord removeRec = findMinDelta();
				Log.i(TAG, "correctErrByDelta(),===========>>>>>>>>>>>>>>findMinDelta, removeRec= "+removeRec);
				if(null == removeRec)
					break;
				mCodeRecordList.remove(removeRec);
				iTime--;
			}
		}
		
		for(int i = 1; i < mCodeRecordList.size()-1;i++){
			strRet.append(mCodeRecordList.get(i).strCdoe);
		}
		Log.i(TAG, "correctErrByDelta(),===========>>>>>>>>>>>>>> try to fix result to strRet= "+strRet.toString());
		
		return removeDividerAndUnmark(strRet.toString());
	}
	
	private String analyzeCodeRecordList(){
		StringBuilder strRet = new StringBuilder();
		if(mCodeRecordList.size() > 0){
			CodeRecord prevRec = null;
			CodeRecord[] pairRec = new CodeRecord[2];
			
			long lStartTs = 0, lEndTs, lMaxDelta = -1;;
			for(CodeRecord rec : mCodeRecordList){
				if(null == prevRec){
					lStartTs = rec.lEndTs;
				}else{
					if(-1 == lMaxDelta){
						lMaxDelta = rec.lEndTs -prevRec.lEndTs;
						pairRec[0] = prevRec;
						pairRec[1] = rec;
					}else{
						if(lMaxDelta < rec.lEndTs -prevRec.lEndTs){
							lMaxDelta = rec.lEndTs -prevRec.lEndTs;
							pairRec[0] = prevRec;
							pairRec[1] = rec;
						}
					}
				}
				
				prevRec = rec;
			}
			lEndTs = prevRec.lStartTs;
			
			long lTotalPeriod = lEndTs - lStartTs;
			
			Log.i(TAG, "analyzeCodeRecordList(),===========>>>>>>>>>>>>>> lTotalPeriod= "+lTotalPeriod+", lMaxDelta="+lMaxDelta);
			Log.i(TAG, "analyzeCodeRecordList(),===========>>>>>>>>>>>>>> pairRec[0]= "+pairRec[0]+", pairRec[1]="+pairRec[1]);
			
			mCodeRecordList.add(mCodeRecordList.indexOf(pairRec[1]), new CodeRecord((pairRec[1].lStartTs + pairRec[0].lStartTs)/2, (pairRec[1].lEndTs + pairRec[0].lEndTs)/2, "0"));
			for(int i = 1; i < mCodeRecordList.size()-1;i++){
				strRet.append(mCodeRecordList.get(i).strCdoe);
			}
			Log.i(TAG, "analyzeCodeRecordList(),===========>>>>>>>>>>>>>> try to fix result to strRet= "+strRet.toString());
		}
		
		return strRet.toString();
	}
	
	private CodeRecord[] findMaxDelta(){
		CodeRecord[] pairRec = new CodeRecord[2];
		if(mCodeRecordList.size() > 0){
			CodeRecord prevRec = null;
			
			long lMaxDelta = -1;;
			for(CodeRecord rec : mCodeRecordList){
				if(null != prevRec){
					long lDleta=((rec.lEndTs -prevRec.lEndTs)+(rec.lStartTs -prevRec.lStartTs))/2;
					if(-1 == lMaxDelta){
						lMaxDelta = lDleta;
						pairRec[0] = prevRec;
						pairRec[1] = rec;
					}else{
						if(lMaxDelta < lDleta){
							lMaxDelta = lDleta;
							pairRec[0] = prevRec;
							pairRec[1] = rec;
						}
					}
				}
				
				prevRec = rec;
			}
		}
		return pairRec;
	}
	
	private CodeRecord findMinDelta(){
		int[] pairRec = new int[2];
		if(mCodeRecordList.size() > 0){
			int prevRec = -1;
			
			long lMinDelta = Long.MAX_VALUE;
			for(int i = 0; i< mCodeRecordList.size(); i++){
				if(-1 != prevRec){
					long lDleta=((mCodeRecordList.get(i).lEndTs -mCodeRecordList.get(prevRec).lEndTs)+(mCodeRecordList.get(i).lEndTs -mCodeRecordList.get(prevRec).lEndTs))/2;
					if(Long.MAX_VALUE == lMinDelta){
						lMinDelta = lDleta;
						pairRec[0] = 0;
						pairRec[1] = i;
					}else{
						if(lMinDelta > lDleta){
							lMinDelta = lDleta;
							pairRec[0] = prevRec;
							pairRec[1] = i;
						}
					}
				}
				prevRec = i;
			}
		}
		
		if(0 == pairRec[0] && mCodeRecordList.size() > 1){
			return mCodeRecordList.get(pairRec[1]);
		}else if((mCodeRecordList.size()-1) == pairRec[1]){
			return mCodeRecordList.get(pairRec[0]);
		}else{
			if(mCodeRecordList.size() > (pairRec[1]+1)){
				long lDeltaPrev = ((mCodeRecordList.get(pairRec[0]).lEndTs - mCodeRecordList.get(pairRec[0] -1 ).lEndTs) + (mCodeRecordList.get(pairRec[0]).lStartTs - mCodeRecordList.get(pairRec[0] -1 ).lStartTs))/2;
				long lDeltaNext = ((mCodeRecordList.get(pairRec[1]+1).lEndTs - mCodeRecordList.get(pairRec[1]).lEndTs) + (mCodeRecordList.get(pairRec[1]+1).lStartTs - mCodeRecordList.get(pairRec[1] ).lStartTs))/2;
				if(lDeltaPrev < lDeltaNext){
					return mCodeRecordList.get(pairRec[0]); 
				}else{
					return mCodeRecordList.get(pairRec[1]);
				}
			}
			
		}
		return null;
	}
//	
//	static private int getMsgLengthByTime(){
//		int iRet = -1;
//		if(sCodeRecordList.size() > 1){
//			long lDelta = (sCodeRecordList.get(sCodeRecordList.size()-1).lStartTs +sCodeRecordList.get(sCodeRecordList.size()-2).lEndTs)/2 - (sCodeRecordList.get(1).lStartTs+sCodeRecordList.get(0).lEndTs)/2;
//			iRet = (int) (lDelta/TONE_PERIOD+((lDelta%TONE_PERIOD > TONE_PERIOD/2)?1:0));
//		}
//		return iRet;
//	}
	
	private int exceedToneCheckPeriod(){
		int iRet = -1;
		if(2 <= mFreqRecordList.size()){
			FreqRecord firstRec = mFreqRecordList.get(0);
			FreqRecord lastRec = mFreqRecordList.get(mFreqRecordList.size()-1);
			if((SEGMENT_FEATURE && -1 < mSessionBeginTs && 2*FRAME_TS <= (lastRec.mlTs - firstRec.mlTs)) || 
			   (TONE_PERIOD <= (lastRec.mlTs - firstRec.mlTs))
			  ){
				iRet = mFreqRecordList.size()-1;
			}
		}
		return iRet;
	}

	static private int getMsgLength(int iDataLength){
		int iMultiply = AubioTestConfig.getMultiplyByFFTYPE();
		
		int iRealLen = (iMultiply > 1)?((iDataLength+1)/iMultiply):iDataLength;
		int iRet = -1;
		for(int i = 1;;i++){
			int iSum = 2*((int) Math.ceil(EC_RATIO*i))+i;
			if(iRealLen == iSum){
				iRet = i;
				break;
			}else if(iDataLength < iSum){
				break;
			}
		}
		
		return iRet*iMultiply;
	}
	
	private int getMeaningfulMsgLength(int iDataLength, boolean bAbove){
		final int MAX_TRIAL = 10;
		int iTrial = 0;
		int iNumTrial = iDataLength;
		int iRet = -1;
		while(iTrial++ < MAX_TRIAL){
			iNumTrial += (bAbove?1:-1);
			if(-1 < (iRet = getMsgLength(iNumTrial))){
				break;
			}
		}
		return iRet;
	}
	
	static private String decodeRSEC(String content){
		if(null == content || 0 == content.length()){
			Log.i(TAG, "decodeRSEC(), content is null" );
			return "";
		}
		
		//String content = decodeConsecutiveDigits(strDecode);
		
		int iMultiply = AubioTestConfig.getMultiplyByFFTYPE();
		int iPower = AubioTestConfig.getPowerByFFTYPE();
		
		StringBuilder ret = new StringBuilder();
		int numDataBytes = content.length();
		Log.i(TAG, "decodeRSEC(),===========>>>>>>>>>>>>>> content= "+content+", numDataBytes="+numDataBytes );
		
		int numMsgBytes= getMsgLength(numDataBytes);//(int) Math.ceil((2*numDataBytes-3)/3.0f);
		if(0 > numMsgBytes){
			return "error1";
		}
		
        int numEcBytesInBlock = (1 == iMultiply)?(numDataBytes - numMsgBytes):((numDataBytes - numMsgBytes + 1)/iMultiply)*iMultiply;
        
        Log.i(TAG, "decodeRSEC(), ===========>>>>>>>>>>>>>> numMsgBytes="+numMsgBytes+" , numEcBytesInBlock= "+numEcBytesInBlock );
        
        int[] toDecode = null;
        if(1 == AubioTestConfig.getMultiplyByFFTYPE()){
        	toDecode = new int[numDataBytes];
        	int iLen = content.length();
        	for(int i =0;i < iLen;i++){
        		String strCode = content.substring(i, i+1);
        		for(int iIndex = 0; iIndex < sFreqRangeTable.size(); iIndex++){
        			if(strCode.equals(sFreqRangeTable.get(iIndex).mstrCode)){
        				toDecode[i] = iIndex;
        				break;
        			}
        		}
        	}
        }else{ 
        	toDecode = new int[numDataBytes/iMultiply];
        	int iLen = content.length()/iMultiply;
        	for(int i =0;i < iLen;i++){
        		for(int j = 0;j < iMultiply;j++){
    				String strCode = content.substring(i*iMultiply+j, i*iMultiply+j+1);
    				for(int iIndex = 0; iIndex < sFreqRangeTable.size(); iIndex++){
        				if(strCode.equals(sFreqRangeTable.get(iIndex).mstrCode)){
        					toDecode[i] <<= iPower;
            				toDecode[i] += iIndex;
            				break;
            			}
    				}
        		}
        		//Log.i(TAG, "decodeRSEC(), toDecode["+i+"] = "+toDecode[i]);
        	}
        }
        
		try {
			if(numMsgBytes < 0){
	        	return "error5_numMsgBytes<0";
			}
        	//new ReedSolomonDecoder(AubioTestConfig.CUR_GF).decode(toDecode, numEcBytesInBlock/iMultiply);
			AubioTestActivity.decodeRS(toDecode, toDecode.length, numEcBytesInBlock/iMultiply);
			
            for(int idx = 0; idx < toDecode.length; idx++){
            	if(1 == AubioTestConfig.getMultiplyByFFTYPE()){
            		ret.append(sFreqRangeTable.get(toDecode[idx]).mstrCode);
            	}else{
            		for(int j = 0;j < iMultiply;j++){
            			ret.insert(idx*iMultiply, sFreqRangeTable.get(toDecode[idx] & ((0x1<<iPower) -1)).mstrCode);
            			toDecode[idx] >>= iPower;
            		}
            	}
            }
            Log.i(TAG, "decodeRSEC(), ret= "+ret.toString() );
          } /*catch (ReedSolomonException ignored) {
            try {
				throw ChecksumException.getChecksumInstance();
			} catch (ChecksumException e) {
				e.printStackTrace();
				return "error2_ChecksumException";
			}
          }*/ catch(ArrayIndexOutOfBoundsException e){
        	  Log.i(TAG, "decodeRSEC(), r= "+e.toString() );
        	  return "error3_ArrayIndexOutOfBoundsException";
          } catch(IllegalStateException e4){
        	  Log.i(TAG, "decodeRSEC(), r= "+e4.toString() );
        	  return "error4_IllegalStateException";
          }
		
		return (ret.length() >= numEcBytesInBlock)?ret.substring(0, ret.length()-(numEcBytesInBlock)):"";
	}
}
