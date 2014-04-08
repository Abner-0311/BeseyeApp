package com.example.aubiotest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import android.util.Log;

import com.example.aubiotest.FreqAnalyzer.FreqRange;
import com.example.aubiotest.FreqAnalyzer.FreqRangeData;

public class AubioTestConfig {

	static public enum FINITE_FIELD_TYPE{
		FFT_O2,
		FFT_S4,
		FFT_D4,
		FFT_Q4,
		FFT_S8,
		FFT_D8,
		FFT_S16,
		FFT_D16,
		
		FFT_S32,
		FFT_S64,
		FFT_S128,
		FFT_S256;
	}
	
//	static private GenericGF getGenericGFByFFTYPE(){
//		switch(CUR_FF_TYPE){
//			case FFT_S4:
//				return null;
//			case FFT_D4:
//			case FFT_S16:
//				return null;
//			case FFT_S8:
//				return null;
//			case FFT_S64:
//			case FFT_D8:
//				return GenericGF.AUBIO_DATA_6;
//			
//			case FFT_O2:
//			case FFT_Q4:
//			case FFT_D16:
//			case FFT_S256:
//				return GenericGF.QR_CODE_FIELD_256;
//
//			case FFT_S32:
//				return GenericGF.AUBIO_DATA_5;
//			case FFT_S128:
//				return null;
//			default:
//				return null;
//		}
//	}	
	
	static public int getDivisionByFFTYPE(){
		switch(CUR_FF_TYPE){
			case FFT_O2:
				return 2;
			case FFT_S4:
			case FFT_D4:
			case FFT_Q4:
				return 4;
			case FFT_S8:
			case FFT_D8:
				return 8;
			case FFT_S16:
			case FFT_D16:
				return 16;
			case FFT_S32:
				return 32;
			case FFT_S64:
				return 64;
			case FFT_S128:
				return 128;
			case FFT_S256:
				return 256;
		}
		
		return 1;
	}
	
	static public int getMultiplyByFFTYPE(){
		switch(CUR_FF_TYPE){
			case FFT_O2:
				return 8;
			case FFT_Q4:
				return 4;
			case FFT_D4:
			case FFT_D8:
			case FFT_D16:
				return 2;
			default:
				return 1;
		}				
	}
	
	static public int getPowerByFFTYPE(){
		switch(CUR_FF_TYPE){
			case FFT_O2:
				return 1;
			case FFT_S4:
			case FFT_D4:
			case FFT_Q4:
				return 2;
			case FFT_S8:
			case FFT_D8:
				return 3;
			case FFT_S16:
			case FFT_D16:
				return 4;
			case FFT_S32:
				return 5;
			case FFT_S64:
				return 6;
			case FFT_S128:
				return 7;
			case FFT_S256:
				return 8;
			default:
				return 1;
		}				
	}
	
	static final public Map<String, Double> sAlphabetTable = new TreeMap<String, Double>();
	static final public List<FreqRange> sFreqRangeTable = new ArrayList<FreqRange>();
    static final public List<String> sCodeTable = new ArrayList<String>();
    
	final static String TAG = AubioTestActivity.TAG;
	
	static final public FINITE_FIELD_TYPE CUR_FF_TYPE = FINITE_FIELD_TYPE.FFT_D16;//.FFT_O2;//FFT_D16;//FFT_S_32;//FFT_D_16;//FFT_S_64;//FINITE_FIELD_TYPE.FFT_D_8;
	//static final public GenericGF CUR_GF= getGenericGFByFFTYPE();
	static final public boolean SELF_TEST = false;
	static final public boolean MARK_FEATURE = false;
	static final public boolean PRE_EMPTY = true;
	static final public boolean AMP_TUNE = true;
	static final public boolean NOISE_SUPPRESS = true;
	static final public boolean SEGMENT_FEATURE = true;
	static final public boolean SEGMENT_OFFSET_FEATURE = true;
	static final public boolean AUBIO_FFT = false;
	static final public boolean ENABLE_LV_DISPLAY = false;
	static final public int SEG_SES_OFFSET = 3;
	
	
	static final public String BT_MSG_ACK = "BT_MSG_ACK";
	static final public String BT_MSG_PURE = "?P?";
	static final public float SILENCE_RATIO = 1.0f;
	
	//for speex noise suppress
	static final public int NOISE_SUPPRESS_INDEX = 0;
	//for speex AGC
	static final public float AGC_LEVEL = 0.0f;
	//for de-reverberation
	static final public boolean ENABLE_DEVERB = true;
	static final public float DEVERB_DECAY = 0.3f;
	static final public float DEVERB_LEVEL = 0.4f;
	
	//for phase I segmentation solution
	static final public float SILENCE_CRITERIA = 0.002f;
	static final public int SILENCE_DETECTION_SAMPLE = 256;
	
	static final public int SAMPLE_RATE_PLAY = 44100;
	static final public int SAMPLE_RATE_REC  = 16000;
	static final public int FRAME_SIZE_REC   = 512; 
	static final public float BIN_SIZE       = (SAMPLE_RATE_REC)/(float)FRAME_SIZE_REC; 
	static final public int TONE_SIZE_REC    = 1536;
	static final public int TONE_FRAME_COUNT = TONE_SIZE_REC/FRAME_SIZE_REC;
	static final public long FRAME_TS        = (FRAME_SIZE_REC*1000)/SAMPLE_RATE_REC;
	static final public float TONE_DURATION  = TONE_SIZE_REC/16000.0f;//4096.0f/44100.0f;
	static final public long TONE_PERIOD     = (long)(TONE_DURATION*1000);//SELF_TEST?(long)((4096.0f/44100.0f)*1000):100L; 
	static final public float RANDUDANT_RATIO = 1.0f;
	static final public int FFT_ANALYSIS_COUNT= 5; 
	
	static final public String BT_BINDING_MAC = "D4:20:6D:EA:0F:3F";// XL  "BC:CF:CC:E2:C3:AC";// -> Butterfly  // 
	static final public String BT_BINDING_MAC_SENDER = "7C:61:93:BF:32:3D";// FLYER
	static final public double dStartValue = 2500.0;
	static final public double dEndValue = 4000.0;//10500.0;
	static final public int iDigitalToTest = 3;
	static final public int MAX_ENCODE_DATA_LEN = 16;//127;
	static final public double EC_RATIO = 0.25f;
	
	static public String PREFIX_DECODE = "w~";
	static public String POSTFIX_DECODE = "~w";
	static public String POSTFIX_DECODE_C1 = "~w";
	static public String POSTFIX_DECODE_C2 = "~w";
	static public String PEER_SIGNAL = "}#";
	static public String CONSECUTIVE_MARK = "";
	static public String DIVIDER = "";
	static public String BT_MSG_DIVIDER;
	static public String BT_MSG_FORMAT;
	static public String BT_MSG_SET_VOLUME = "BTVOL_";
	static public String BT_MSG_SET_VOLUME_END = "_VOLBT";
	
	//For recording buffer
	static final public long MAX_RECORDING_TIME = 60L; //60 seconds
	
	static public String configuration(){
		return "CUR_FF_TYPE:["+CUR_FF_TYPE+"], SELF_TEST:["+SELF_TEST+"], MARK_FEATURE:["+MARK_FEATURE+"], PRE_EMPTY:["+PRE_EMPTY+"], AMP_TUNE:["+AMP_TUNE+"],\n"+
			   "NOISE_SUPPRESS:["+NOISE_SUPPRESS+"], SEGMENT_FEATURE:["+SEGMENT_FEATURE+"], SEGMENT_OFFSET_FEATURE:["+SEGMENT_OFFSET_FEATURE+"], AUBIO_FFT:["+AUBIO_FFT+"], ENABLE_LV_DISPLAY:["+ENABLE_LV_DISPLAY+"],\n"+
			   "NOISE_SUPPRESS_INDEX:["+NOISE_SUPPRESS_INDEX+"], AGC_LEVEL:["+AGC_LEVEL+"], SAMPLE_RATE_PLAY:["+SAMPLE_RATE_PLAY+"], SAMPLE_RATE_REC:["+SAMPLE_RATE_REC+"], FRAME_SIZE_REC:["+FRAME_SIZE_REC+"]\n"+
			   "ENABLE_DEVERB:["+ENABLE_DEVERB+"], DEVERB_DECAY:["+DEVERB_DECAY+"], DEVERB_LEVEL:["+DEVERB_LEVEL+"]\n";
	}
	
	//make the waveform equal
//	static final public float[] AMP_BASE_RATIO = {1.00f, 1.00f, 1.10f, 1.10f, // 1093.75, 1187.50, 1812.50, 1968.75,
//		
//												  1.04f, 1.00f, 1.00f, 1.05f, // 1281.25, 1468.75, 1593.75, 1687.5,
//												  
//												  0.60f, 0.98f, 1.02f, 1.00f, // 2843.75, 2093.75, 2187.50, 2375.0,
//												  
//												  1.00f, 0.90f, 0.80f, 0.68f, // 2468.75, 2562.50, 2656.25, 2750.0,
//												  
//												  0.8f, 1.0f, 0.4f         }; // 1375.75, 2281.25, 3218.75,
	
	
//	static final public float[] AMP_BASE_RATIO = {0.94f, 1.10f, 1.15f, 1.00f, // 1718.75, 1843.75, 1968.75, 2093.75,
//		
//												  0.96f, 0.96f, 0.80f, 0.70f, // 2343.75, 2468.75, 2593.75, 2718.75,
//												  
//												  0.65f, 0.48f, 0.40f, 0.34f, // 2906.25, 3031.25, 3156.25, 3406.25,
//												  
//												  0.36f, 0.36f, 0.40f, 0.45f, // 3531.25, 3656.25, 3781.25, 3906.25,
//												  
//												  1.00f, 1.00f, 0.40f      }; // 1593.75, 2281.25, 3218.75,

	static final public float[] AMP_BASE_RATIO = {1.00f, 1.00f, 1.10f, 1.10f, // 1093.75, 1187.50, 1781.25, 1968.75,
												  1.04f, 1.00f, 1.00f, 1.05f, // 1281.25, 1468.75, 1593.75, 1687.5,
												  0.60f, 0.98f, 1.02f, 1.00f, // 2843.75, 2093.75, 2187.50, 2375.0,
												  1.00f, 0.90f, 0.80f, 0.68f, // 2468.75, 2562.50, 2656.25, 2750.0,		  
												  0.80f, 1.00f, 0.40f      }; // 1406.25, 2281.25, 3218.75,
	
	//static final public float[] AMP_BASE_RATIO = {0.81f, 0.87f, 0.90f, 1.0f, 0.6f, 1.0f, 0.4f}; // => Q4, 32 problem 
	//static final public float[] AMP_BASE_RATIO = {0.9f, 1.0f, 0.6f, 1.0f, 0.4f, 0.8f, 0.7f, 0.6f, 0.5f, 0.45f, 0.4f};// => O2	
	
    static {  
    	double dDelta = (dEndValue - dStartValue)/(AubioTestConfig.getDivisionByFFTYPE()+3);//Plus additional 5 char for special use
    	double dValue = dStartValue;
    	if(AubioTestConfig.CUR_FF_TYPE.equals(AubioTestConfig.FINITE_FIELD_TYPE.FFT_D16)){
    		double[] freqs = {
    				1093.75,
    				1187.50,
    				1781.25,
    				1968.75,//2843.75,//2000.0,//influence 1777

    				1281.25,
    				1468.75,
    				1593.75,
    				1687.5,
    				
    				2843.75,//1875.0,
    				2093.75,
    				2187.50,
    				2375.00,
    				
    				2468.75,
    				2562.50,
    				2656.25,
    				2750.00,
    				
    				1375.75,
    				2281.25,
    				3218.75,
			  };
//    		double[] freqs = {
//    				1718.75,//    				1093.75,
//    				1843.75,//    				1187.50,
//    				1968.75,//    				1812.50,
//    				2093.75,//    				1968.75,//2843.75,//2000.0,//influence 1777
//						//
//    				2343.75,//    				1281.25,
//    				2468.75,//    				1468.75,
//    				2593.75,//    				1593.75,
//    				2718.75,//    				1687.5,
//						//    				
//    				2906.25,//    				2843.75,//1875.0,
//    				3031.25,//    				2093.75,
//    				3156.25,//    				2187.50,
//    				3406.25,//    				2375.00,
//						//    				
//    				3531.25,//    				2468.75,
//    				3656.25,//    				2562.50,
//    				3781.25,//    				2656.25,
//    				3906.25,//    				2750.00,
//						//    				
//    				1593.75,//    				1375.00,
//    				2218.75,//    				2281.25,
//    				3281.25//    				3218.75,
//			  };
        	
    		int iDx = 0;
        	
        	//0~9
        	for(char i = 0x30; i<= 0x39 ; i++, dValue+=dDelta, iDx++){
        		sCodeTable.add(String.valueOf((char)i));
        		sAlphabetTable.put(String.valueOf((char)i), freqs[iDx]);
        		sFreqRangeTable.add(new FreqRange(freqs[iDx], (AUBIO_FFT?BIN_SIZE/*0.5*(freqs[iDx]/600.0f)*/:BIN_SIZE), String.valueOf((char)i)));
        	}
        	
        	//A~I
        	for(char i = 0x41; i<= 0x49 ; i++, dValue+=dDelta, iDx++){
        		sCodeTable.add(String.valueOf((char)i));
        		sAlphabetTable.put(String.valueOf((char)i), freqs[iDx]);
        		sFreqRangeTable.add(new FreqRange(freqs[iDx], (AUBIO_FFT?BIN_SIZE/*0.5*(freqs[iDx]/600.0f)*/:BIN_SIZE), String.valueOf((char)i)));
        	}
    	}else if(AubioTestConfig.CUR_FF_TYPE.equals(AubioTestConfig.FINITE_FIELD_TYPE.FFT_O2)){
    		char cStart = 0x30;
    		char cEnd   = 0x34;
    		
    		double[] freqs = {
    				842.1052632,
    				1230.769231,
    				1454.545455,
    				2285.714286,
    				3200.0,
////			  1568.0,// 44K->?, 16K->X
//			  //1661.2,// 44K->?, 16K->XX
//			  //1760.0,// 44K->O, 16K->X
//			  //1864.7,// 44K->O, 16K->XX
//			  1975.5,// 44K->O, 16K->X
//			  //2093.0,// 44K->O, 16K->XX
//			  //2217.5,// 44K->O, 16K->O
			  };
        	//0~5
    		int iDx = 0;
        	for(char i = cStart; i<= cEnd ; i++, dValue+=dDelta, iDx++){
        		sCodeTable.add(String.valueOf((char)i));
        		sAlphabetTable.put(String.valueOf((char)i), freqs[iDx]);
        		sFreqRangeTable.add(new FreqRange(freqs[iDx], (AUBIO_FFT?0.5*(freqs[iDx]/600.0f):(BIN_SIZE*2)), String.valueOf((char)i)));        		
        	}
    	}else if(AubioTestConfig.CUR_FF_TYPE.equals(AubioTestConfig.FINITE_FIELD_TYPE.FFT_Q4)){
    		double[] freqs = {
    				//533.3333333,//many bias
    				//551.7241379,//XX
    				//571.4285714,//conflict with 2285
    				//695.6521739,//XX
    				
    				//842.1052632,
    				//888.8888889,
    				//941.1764706,//XX
    				1066.666667,//XX
    				1230.769231,
    				//1333.333333,//XX many bias
    				
    				1777.777778,//X
    				2000.0,//conflict with 2285
    				
    				1454.545455,
    				
    				2285.714286,
    				3200.0,
    				//5333.333333,
    				//8000.0
    		  //523.25, 
			  //554.37, 
			  //587.33, 
			  //622.25, 
			  //659.26, 
			  //698.46, 
			  //739.99, 
			  //783.99, 
			  //830.61, 
			  //880.00, 
			  //932.33, 
			  //987.77,
			  //1046.5, 
			  //1108.7, 
			  //1174.7, 
			  //1244.5, 
			  //1318.5, 
			  //1396.9, 
			  //1480.0, 
			  
			  //1568.0, 
//			  1661.2,
//			  1760.0, 
//			  1864.7, 
//			  1975.5, 
//			  2093.0, 
//			  2217.5, 
//			  2349.3, 
			  //2489.0, 
			  //2637.0, 
			  //2793.8,
			  //2960.0,
			  //3136.0, 
			  //3322.4, 
			  //3520.0,
			  //3729.3,  
//			  3951.1,
//			  4186.0,
//			  4434.9, 
//			  4698.6,
//			  4978.0,
//			  5274.04,
//    		  5587.65,
    		  //5919.91,
    		  //6271.92,
//    		  6644.87,
//    		  7040.00,
//    		  7458.62,
//    		  7902.13
			  };
        	
    		int iDx = 0;
        	//0~6
        	for(char i = 0x30; i<= 0x36 ; i++, dValue+=dDelta, iDx++){
        		sCodeTable.add(String.valueOf((char)i));
        		sAlphabetTable.put(String.valueOf((char)i), freqs[iDx]);
        		sFreqRangeTable.add(new FreqRange(freqs[iDx], (AUBIO_FFT?BIN_SIZE/*0.5*(freqs[iDx]/600.0f)*/:BIN_SIZE*3), String.valueOf((char)i)));
        	}
    	}else if(AubioTestConfig.CUR_FF_TYPE.equals(AubioTestConfig.FINITE_FIELD_TYPE.FFT_D8)){
    		char cStart = 0x30;
    		char cEnd   = 0x3a;
    		
    		double[] freqs = {
    		  //523.25, 
			  //554.37, 
			  //587.33, 
			  //622.25, 
			  //659.26, 
			  //698.46, 
			  //739.99, 
			  //783.99, 
			  //830.61, 
			  //880.00, 
			  //932.33, 
			  //987.77,
			  //1046.5, 
			  //1108.7, 
			  //1174.7, 
			  //1244.5, 
			  //1318.5, 
			  //1396.9, 
			  //1480.0, 
			  //1568.0, 
			  //1661.2,
			  1760.0, 
			  1864.7, 
			  1975.5, 
			  2093.0, 
			  2217.5,/// 
			  2349.3, 
			  2489.0, 
			  2637.0, 
			  2793.8,
			  2960.0,
			  3136.0, 
			  //3322.4, 
			  //3520.0,
			  //3729.3,  
			  //3951.1,
			  //4186.0,
			  //4434.9, 
			  //4698.6
			  };
        	//0~5
    		int iDx = 0;
        	for(char i = cStart; i<= cEnd ; i++, dValue+=dDelta, iDx++){
        		sCodeTable.add(String.valueOf((char)i));
        		sAlphabetTable.put(String.valueOf((char)i), freqs[iDx]);

        		if(i == cStart){
        			double dRange = (freqs[iDx+1]-freqs[iDx])/2;
        			sFreqRangeTable.add(new FreqRange(freqs[iDx], dRange, String.valueOf((char)i)));
        		}else if(i == cEnd){
        			double dRange = (freqs[iDx]-freqs[iDx-1])/2;
        			sFreqRangeTable.add(new FreqRange(freqs[iDx], dRange, String.valueOf((char)i)));
        		}else{
        			sFreqRangeTable.add(new FreqRange(freqs[iDx], (freqs[iDx+1]+freqs[iDx])/2, (freqs[iDx]+freqs[iDx-1])/2, String.valueOf((char)i)));
        		}
        	}
//        	//0~9
//        	for(char i = 0x30; i<= 0x3b ; i++, dValue+=dDelta){
//        		sCodeTable.add(String.valueOf((char)i));
//        		sAlphabetTable.put(String.valueOf((char)i), dValue);
//        		sFreqRangeTable.add(new FreqRange(dValue, dDelta/2, String.valueOf((char)i)));
//        	}
        	
 		
    	}else if(AubioTestConfig.CUR_FF_TYPE.equals(AubioTestConfig.FINITE_FIELD_TYPE.FFT_S32)){

        	//0~9
        	for(char i = 0x30; i<= 0x39 ; i++, dValue+=dDelta){
        		sCodeTable.add(String.valueOf((char)i));
        		sAlphabetTable.put(String.valueOf((char)i), dValue);
        		sFreqRangeTable.add(new FreqRange(dValue, dDelta/2, String.valueOf((char)i)));
        	}
        	
        	//A~Z
        	for(char i = 0x41; i<= 0x5a ; i++, dValue+=dDelta){
        		sCodeTable.add(String.valueOf((char)i));
        		sAlphabetTable.put(String.valueOf((char)i), dValue);
        		sFreqRangeTable.add(new FreqRange(dValue, dDelta/2, String.valueOf((char)i)));
        	}
        	
    	}else if(AubioTestConfig.CUR_FF_TYPE.equals(AubioTestConfig.FINITE_FIELD_TYPE.FFT_S64)){

        	//0~9
        	for(char i = 0x30; i<= 0x39 ; i++, dValue+=dDelta){
        		sCodeTable.add(String.valueOf((char)i));
        		sAlphabetTable.put(String.valueOf((char)i), dValue);
        		sFreqRangeTable.add(new FreqRange(dValue, dDelta/2, String.valueOf((char)i)));
        	}
        	
        	//A~\
        	for(char i = 0x41; i<= 0x5c ; i++, dValue+=dDelta){
        		sCodeTable.add(String.valueOf((char)i));
        		sAlphabetTable.put(String.valueOf((char)i), dValue);
        		sFreqRangeTable.add(new FreqRange(dValue, dDelta/2, String.valueOf((char)i)));
        	}
        	
        	//a~~
        	for(char i = 0x61; i<= 0x7E ; i++, dValue+=dDelta){
        		sCodeTable.add(String.valueOf((char)i));
        		sAlphabetTable.put(String.valueOf((char)i), dValue);
        		sFreqRangeTable.add(new FreqRange(dValue, dDelta/2, String.valueOf((char)i)));
        	}
    	}
    	
    	AubioTestConfig.PREFIX_DECODE 	 = sCodeTable.get(sCodeTable.size()-3)+sCodeTable.get(sCodeTable.size()-1);//IK
    	
    	AubioTestConfig.POSTFIX_DECODE_C1= sCodeTable.get(sCodeTable.size()-2);
    	AubioTestConfig.POSTFIX_DECODE_C2= sCodeTable.get(sCodeTable.size()-1);
    	
    	AubioTestConfig.POSTFIX_DECODE 	 = POSTFIX_DECODE_C1 + POSTFIX_DECODE_C2;//KI
    	
    	//AubioTestConfig.PEER_SIGNAL 	 = sCodeTable.get(sCodeTable.size()-4)+sCodeTable.get(sCodeTable.size()-1);
    	AubioTestConfig.CONSECUTIVE_MARK = sCodeTable.get(sCodeTable.size()-3);
    	//AubioTestConfig.DIVIDER			 = sCodeTable.get(sCodeTable.size()-5);
    	
    	BT_MSG_DIVIDER = sCodeTable.get(sCodeTable.size()-2)+sCodeTable.get(sCodeTable.size()-1);
    	BT_MSG_FORMAT = "%s"+BT_MSG_DIVIDER+"%s";
    	
    	resolveFreqRangeConflict();
    	normalizeRatio();
    	
    	for(FreqRange fr : sFreqRangeTable){
    		Log.e(TAG, fr.toString());
    	}
    	
//    	unitestConsecutiveDigits("000111222");
//    	unitestConsecutiveDigits("aaaaaaaaaaaaaaaaaaaa");
//    	unitestConsecutiveDigits("aaaaaaaaaaaaaaaaaaaabbb");
//    	unitestConsecutiveDigits("cccaaaaaaaaaaaaaaaaaaaa");
//    	unitestConsecutiveDigits("0123456789abcdef");
//    	unitestConsecutiveDigits("01234567822224443333359abcdef");
    	
//    	//for peer signal ! #
//    	sFreqRangeTable.add(new FreqRange(1600.0, 50, String.valueOf((char)0x23)));

    	
//    	sFreqRangeTable.add(new FreqRange(1672.0, 40.0, "0"));
//    	
//    	sFreqRangeTable.add(new FreqRange(1764.0, 40.0, "1"));
//    	sFreqRangeTable.add(new FreqRange(1850.0, 40.0, "2"));
//    	sFreqRangeTable.add(new FreqRange(1926.0, 40.0, "3"));
//    	sFreqRangeTable.add(new FreqRange(2000.0, 40.0, "4"));
//    	sFreqRangeTable.add(new FreqRange(2080.0, 40.0, "5"));
//    	sFreqRangeTable.add(new FreqRange(2165.0, 40.0, "6"));
//    	sFreqRangeTable.add(new FreqRange(2240.0, 35.0, "7"));
//    	sFreqRangeTable.add(new FreqRange(2320.0, 40.0, "8"));
//    	sFreqRangeTable.add(new FreqRange(2400.0, 40.0, "9"));
//    	
//    	sFreqRangeTable.add(new FreqRange(2490.0, 40.0, "A"));
//    	sFreqRangeTable.add(new FreqRange(2570.0, 40.0, "B"));
//    	sFreqRangeTable.add(new FreqRange(2650.0, 40.0, "C"));
//    	sFreqRangeTable.add(new FreqRange(2730.0, 40.0, "D"));
//    	sFreqRangeTable.add(new FreqRange(2810.0, 40.0, "E"));
//    	sFreqRangeTable.add(new FreqRange(2890.0, 35.0, "F"));
//    	sFreqRangeTable.add(new FreqRange(2960.0, 35.0, "G"));
//    	sFreqRangeTable.add(new FreqRange(3030.0, 35.0, "H"));
//    	sFreqRangeTable.add(new FreqRange(3100.0, 35.0, "I"));
//    	sFreqRangeTable.add(new FreqRange(3170.0, 35.0, "J"));
//    	sFreqRangeTable.add(new FreqRange(3240.0, 35.0, "K"));
//    	sFreqRangeTable.add(new FreqRange(3320.0, 40.0, "L"));
//    	sFreqRangeTable.add(new FreqRange(3400.0, 40.0, "M"));
//    	sFreqRangeTable.add(new FreqRange(3480.0, 40.0, "N"));
//    	sFreqRangeTable.add(new FreqRange(3560.0, 40.0, "O"));
//    	sFreqRangeTable.add(new FreqRange(3640.0, 40.0, "P"));
//    	sFreqRangeTable.add(new FreqRange(3730.0, 40.0, "Q"));
//    	sFreqRangeTable.add(new FreqRange(3810.0, 40.0, "R"));
//    	sFreqRangeTable.add(new FreqRange(3890.0, 35.0, "S"));
//    	sFreqRangeTable.add(new FreqRange(3965.0, 40.0, "T"));
//    	sFreqRangeTable.add(new FreqRange(4045.0, 40.0, "U"));
//    	sFreqRangeTable.add(new FreqRange(4125.0, 40.0, "V"));
//    	
//    	sFreqRangeTable.add(new FreqRange(4205.0, 40.0, "W"));
//    	sFreqRangeTable.add(new FreqRange(4285.0, 35.0, "X"));
//    	sFreqRangeTable.add(new FreqRange(4350.0, 30.0, "Y"));
//    	//sFreqRangeTable.add(new FreqRange(4420.0, 40.0, "Z"));
//    	sFreqRangeTable.add(new FreqRange(4475.0, 60.0, "Z"));
    	
    	//prefix ($#) and postfix (#$)
    	//sFreqRangeTable.add(new FreqRange(1580.0, 40.0, "$"));
    	//sFreqRangeTable.add(new FreqRange(4500.0, 40.0, "#"));
    	
    	
//    	sFreqRangeTable.add(new FreqRange(1905.0, 50.0, "1"));
//    	sFreqRangeTable.add(new FreqRange(2150.0, 50.0, "2"));
//    	sFreqRangeTable.add(new FreqRange(2371.0, 50.0, "3"));
//    	sFreqRangeTable.add(new FreqRange(2601.0, 50.0, "4"));
//    	sFreqRangeTable.add(new FreqRange(2835.0, 50.0, "5"));
//    	sFreqRangeTable.add(new FreqRange(3065.0, 50.0, "6"));
//    	sFreqRangeTable.add(new FreqRange(3292.0, 50.0, "7"));
//    	sFreqRangeTable.add(new FreqRange(3520.0, 50.0, "8"));
//    	sFreqRangeTable.add(new FreqRange(3750.0, 50.0, "9"));
//    	
//    	sFreqRangeTable.add(new FreqRange(3980.0, 50.0, "A"));
//    	sFreqRangeTable.add(new FreqRange(4200.0, 50.0, "B"));
//    	sFreqRangeTable.add(new FreqRange(4407.0, 50.0, "C"));
//    	sFreqRangeTable.add(new FreqRange(4646.0, 50.0, "D"));
//    	sFreqRangeTable.add(new FreqRange(4796.0, 50.0, "E"));
//    	sFreqRangeTable.add(new FreqRange(5084.0, 50.0, "F"));
//    	sFreqRangeTable.add(new FreqRange(5295.0, 50.0, "G"));
//    	sFreqRangeTable.add(new FreqRange(5496.0, 50.0, "H"));
//    	sFreqRangeTable.add(new FreqRange(5791.0, 50.0, "I"));
//    	sFreqRangeTable.add(new FreqRange(5959.0, 50.0, "J"));
//    	sFreqRangeTable.add(new FreqRange(6220.0, 50.0, "K"));
//    	sFreqRangeTable.add(new FreqRange(6400.0, 50.0, "L"));
//    	sFreqRangeTable.add(new FreqRange(6636.0, 50.0, "M"));
//    	sFreqRangeTable.add(new FreqRange(6831.0, 50.0, "N"));
//    	sFreqRangeTable.add(new FreqRange(7011.0, 50.0, "O"));
//    	sFreqRangeTable.add(new FreqRange(7210.0, 50.0, "P"));
//    	sFreqRangeTable.add(new FreqRange(7461.0, 50.0, "Q"));
//    	sFreqRangeTable.add(new FreqRange(7826.0, 50.0, "R"));
//    	sFreqRangeTable.add(new FreqRange(7938.0, 50.0, "S"));
//    	sFreqRangeTable.add(new FreqRange(8110.0, 50.0, "T"));
//    	sFreqRangeTable.add(new FreqRange(8273.0, 50.0, "U"));
//    	sFreqRangeTable.add(new FreqRange(8400.0, 50.0, "V"));
//    	sFreqRangeTable.add(new FreqRange(8664.0, 50.0, "W"));
//    	sFreqRangeTable.add(new FreqRange(8850.0, 50.0, "X"));
//    	sFreqRangeTable.add(new FreqRange(9235.0, 50.0, "Y"));
//    	sFreqRangeTable.add(new FreqRange(9780.0, 50.0, "Z"));
    	
    	
//    	//for peer signal ! #
//		sCodeTable.add(String.valueOf((char)0x23));
//		sAlphabetTable.put(String.valueOf((char)0x23), 1600.0);
    	
//    	final double dStartValue = 1700.0;
//    	final double dEndValue = 4700.0;//10500.0;
//    	
//    	double dDelta = (dEndValue - dStartValue)/36;
//    	double dValue = dStartValue;
//    	//0~9
//    	for(char i = 0x30; i<= 0x39 ; i++, dValue+=dDelta){
//    		sCodeTable.add(String.valueOf((char)i));
//    		sAlphabetTable.put(String.valueOf((char)i), dValue);
//    	}
//    	
//    	//A~Z
//    	for(char i = 0x41; i<= 0x5a ; i++, dValue+=dDelta){
//    		sCodeTable.add(String.valueOf((char)i));
//    		sAlphabetTable.put(String.valueOf((char)i), dValue);
//    	}
//    	
//    	sAlphabetTable.put("Z", 4650.0);
    	
    	//prefix (BE) and postfix (EB)
    	//sAlphabetTable.put("$", 1600.0);
    	//sAlphabetTable.put("#", 4700.0);
    	
    	//special handling
//    	sAlphabetTable.put("E", 5050.0);
//    	sAlphabetTable.put("I", 6150.0);
//    	sAlphabetTable.put("K", 6700.0);
//    	sAlphabetTable.put("P", 7900.0);
//    	sAlphabetTable.put("R", 8425.0);
//    	sAlphabetTable.put("V", 9200.0);
//    	sAlphabetTable.put("W", 9650.0);
//    	sAlphabetTable.put("Z", 10700.0);
    }
    
    static private void unitestConsecutiveDigits(String strCode){
    	String strEncode = encodeConsecutiveDigits(strCode);
    	String strDecode = decodeConsecutiveDigits(strEncode);
    	Log.e(TAG, "unitestConsecutiveDigits(), \n" +
    			"strCode   = ["+strCode+"]\n" +
    			"strEncode = ["+strEncode+"]\n" +
    			"strDecode = ["+strDecode+"]\n");
    }
    
    static public String encodeConsecutiveDigits(String strCode){
 //   	
    	if(MARK_FEATURE){
	    	StringBuilder strRet = new StringBuilder(strCode);
	    	int iDivision = AubioTestConfig.getDivisionByFFTYPE()-1;
	    	int iCurIdx = 0;
	    	while(iCurIdx < strRet.length()){
	    		if(AubioTestConfig.CUR_FF_TYPE.equals(AubioTestConfig.FINITE_FIELD_TYPE.FFT_O2)){
	    			if((iCurIdx+1 < strRet.length()) && strRet.charAt(iCurIdx) == strRet.charAt(iCurIdx+1)){
	    				strRet.insert(iCurIdx+1, CONSECUTIVE_MARK);
	    				iCurIdx+=2;
	    			}else{
	    				iCurIdx++;
	    			}
	    		}else{
	    			char curDigit = strRet.charAt(iCurIdx);
	        		int iIdxEnd = iCurIdx+1;
	        		for(; iIdxEnd < strRet.length(); iIdxEnd++){
	        			if(curDigit != strRet.charAt(iIdxEnd) || (iIdxEnd - iCurIdx) == iDivision){
	        				break;
	        			}
	        		}
	        		
	        		//need to encode
	    			if(1 < (iIdxEnd - iCurIdx)){
	    				strRet.replace(iCurIdx, iIdxEnd, genConsecutiveMark(curDigit, (iIdxEnd - iCurIdx)));
	    				iCurIdx+=2;
	    			}else{
	    				iCurIdx++;
	    			}
	    		}
	    		
	    	}
	    	
	    	Log.e(TAG, "encodeConsecutiveDigits(), \n" +
	    			"strCode   = ["+strCode+"]\n" +
	    			"strRet    = ["+strRet.toString()+"]");
	    	
	    	return strRet.toString();
    	}else
    		return strCode;
    }
    
    static public String decodeConsecutiveDigits(String strCode){
    	if(MARK_FEATURE){
    		StringBuilder strRet = new StringBuilder(strCode);
        	int iIdx = -1;
        	if(AubioTestConfig.CUR_FF_TYPE.equals(AubioTestConfig.FINITE_FIELD_TYPE.FFT_O2)){
        		return strCode.replace(CONSECUTIVE_MARK, "");
    		}else{
    			while(-1 < (iIdx = strRet.lastIndexOf(CONSECUTIVE_MARK))){
    	    		if(0 <= (iIdx-1) && strRet.length() > (iIdx +1)){
    	    			String strDigit = strRet.substring(iIdx-1, iIdx);
    	    			String strNum = strRet.substring(iIdx+1, iIdx+2);
    	    			String strDecdoe = getNDigits(strDigit, sCodeTable.indexOf(strNum));
    	    			strRet.replace(iIdx-1, iIdx+2, strDecdoe);
    	    		}else{
    	    			Log.e(TAG, "unitestConsecutiveDigits(), invalid iIdx = "+iIdx+", len ="+strRet.length()+"\n" +
    	    	    			   "strCode   = ["+strCode+"]\n" +
    	    	    			   "strRet    = ["+strRet.toString()+"]\n");
    	    			break;
    	    		}
    	    	}
    		}
        	
        	
        	Log.e(TAG, "decodeConsecutiveDigits(), \n" +
        			"strCode   = ["+strCode+"]\n" +
        			"strRet    = ["+strRet.toString()+"]");
        	
        	return strRet.toString();
    	}else
    	  return strCode;
    }
    
    static private String genConsecutiveMark(char cDigit, int iNum){
    	return cDigit+CONSECUTIVE_MARK+sCodeTable.get(iNum);
    }
    
    static public String getNDigits(String strCode, int iNumDigits){
    	String strRet = "";
    	for(int i =0; i < iNumDigits; i++){
    		strRet+=strCode;
    	}
    	return strRet;
    }
    
    static private void resolveFreqRangeConflict(){
    	int iSize = sFreqRangeTable.size();
    	if(AUBIO_FFT){
    		for(int i =0; i < iSize - 1; i++){
        		FreqRange fr = sFreqRangeTable.get(i);
        		List<FreqRangeData> lstFRD = fr.mlstFreqRangeData;
        		int iSizeFRD = lstFRD.size();
        		for(int idx = iSizeFRD -1; idx > 0; idx--){
        			FreqRangeData frd = lstFRD.get(idx);
        			if(null != frd){
        				boolean bConflict = false;
        				for(int iChkIdx = i+1; iChkIdx < iSize - 1;iChkIdx++){
        					FreqRange frChk = sFreqRangeTable.get(iChkIdx);
        					//if(frChk.withinFreqRange(frd.mdLowerBound) || frChk.withinFreqRange(frd.mdUpperBound)){
        					if(frChk.isOverlap(frd)){
        						bConflict = true;
        						break;
        					}
        				}
        				if(bConflict){
        					Log.e(TAG, "resolveFreqRangeConflict(), remove frd = "+frd);
        					lstFRD.remove(idx);
        				}
        			}
        		}
        	}
    	}else{
    		for(int i =iSize - 1; i >= 0 ; i--){
        		FreqRange fr = sFreqRangeTable.get(i);
        		List<FreqRangeData> lstFRD = fr.mlstFreqRangeData;
        		int iSizeFRD = lstFRD.size();
        		for(int idx = iSizeFRD -1; idx > 0; idx--){
        			FreqRangeData frd = lstFRD.get(idx);
        			if(null != frd){
        				boolean bConflict = false;
        				if(i == 0){
        					for(int iChkIdx = 1; iChkIdx < iSize; iChkIdx++){
            					FreqRange frChk = sFreqRangeTable.get(iChkIdx);
            					//if(frChk.withinFreqRange(frd.mdLowerBound) || frChk.withinFreqRange(frd.mdUpperBound)){
            					if(frChk.isOverlap(frd)){
            						bConflict = true;
            						break;
            					}
            				}
        				}else{
        					for(int iChkIdx = i-1; iChkIdx >=0;iChkIdx--){
            					FreqRange frChk = sFreqRangeTable.get(iChkIdx);
            					//if(frChk.withinFreqRange(frd.mdLowerBound) || frChk.withinFreqRange(frd.mdUpperBound)){
            					if(frChk.isOverlap(frd)){
            						bConflict = true;
            						break;
            					}
            				}
        				}
        				
        				if(bConflict){
        					Log.e(TAG, "resolveFreqRangeConflict(), remove frd = "+frd);
        					lstFRD.remove(idx);
        				}
        			}
        		}
        	}
    	}	
    }
    
	static private void normalizeRatio(){
		int iLen = AMP_BASE_RATIO.length;
		float fMax = AMP_BASE_RATIO[0];
		for(int i =1; i < iLen; i++){
			if(fMax < AMP_BASE_RATIO[i]){
				fMax = AMP_BASE_RATIO[i];
			}
		}
		
		for(int i =0; i < iLen; i++){
			AMP_BASE_RATIO[i]/=fMax;
			//Log.e(TAG, "normalizeRatio(), AMP_BASE_RATIO["+i+"] = "+AMP_BASE_RATIO[i]);
		}
	}
}
