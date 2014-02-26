package com.example.aubiotest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import android.os.Environment;
import android.util.Log;

public class AutoTestResultSaver {
	final static String TAG = AubioTestActivity.TAG;
	private String strFileName;
	private File outputFile;
	private boolean mbRandomMode = false;
	private BufferedWriter writer;
	private int[][] mTypeCounter = new int[AUTO_CORRECTION_TYPE][];
	
	static public enum MATCH_RESULTS{
		DESC_MATCH,
		DESC_MATCH_EC,
		DESC_MATCH_MSG,
		DESC_MISMATCH,
		DESC_TIMEOUT,
		DESC_TIMEOUT_MSG,
		DESC_TIMEOUT_MSG_EC,
		DESC_RESULT_TYPE;
		
		public String toString(MATCH_RESULTS type){
			switch(type){
				case DESC_MATCH:
					return "Match_Before_EC";
				case DESC_MATCH_EC:
					return "Match_After_EC";
				case DESC_MATCH_MSG:
					return "Match_Msg_Only";
				case DESC_MISMATCH:
					return "Mismatch";
				case DESC_TIMEOUT:
					return "Timeout_Mismatch";
				case DESC_TIMEOUT_MSG:
					return "Timeout_Match_Msg_Only";
				case DESC_TIMEOUT_MSG_EC:
					return "Timeout_Match_Msg_EC";
				default:
					return "Invalid value";
			}
		}
		
		@Override
		public String toString(){
			return toString(this);
		}
	}
	
	static public final int MATCH_TYPE = MATCH_RESULTS.DESC_RESULT_TYPE.ordinal();
	static public final int AUTO_CORRECTION_TYPE = 2;
	
	static private final String REC_FORMAT 		  = "AT-%s-%s-%s-%s-D%s-%sM-V%s.txt"; //0: device name 1: Local/Remote 2: FL(Fixed length)/RL(Random length) 3: FFT Type 4: Digit length 5: Distance 6: volume (0~100)

	public AutoTestResultSaver(int iNumDigital, int iDistance, int iVolume, boolean bRandomMode, boolean bRemoteMode){
		mbRandomMode = bRandomMode;
		strFileName = genFileName(iNumDigital, iDistance, iVolume, bRandomMode, bRemoteMode);
		Log.i(TAG, "saveDataToFile(), file:"+strFileName);
	    File outDir = getFolder();
	    if(null != outDir)
	    	outputFile = new File(outDir, strFileName);
	    
	    mTypeCounter[0] = new int[MATCH_TYPE];
	    mTypeCounter[1] = new int[MATCH_TYPE];
	}
	
	private void addCountByType(MATCH_RESULTS type, boolean bFromAutoCorrection){
		final int iAC_TYPE = bFromAutoCorrection?1:0;
		if(null != type){
			mTypeCounter[iAC_TYPE][type.ordinal()]++;
		}
	}
	
	public String getTypeCountValue(){
		return String.format("[%d, %d, %d, %d, %d, %d, %d]\n[%d, %d, %d, %d, %d, %d, %d]",  mTypeCounter[0][0],  mTypeCounter[0][1],  mTypeCounter[0][2],  mTypeCounter[0][3],  mTypeCounter[0][4],  mTypeCounter[0][5],  mTypeCounter[0][6]
																				         ,  mTypeCounter[1][0],  mTypeCounter[1][1],  mTypeCounter[1][2],  mTypeCounter[1][3],  mTypeCounter[1][4],  mTypeCounter[1][5],  mTypeCounter[1][6]);
	}
	
	private static File getFolder(){
		File root = Environment.getExternalStorageDirectory();
	    File outDir = new File(root.getAbsolutePath() + File.separator + "AudioTest");
	    if (!outDir.isDirectory()) {
	      outDir.mkdir();
	    }
	    
	    try {
	      if (!outDir.isDirectory()) {
	        throw new IOException(
	            "Unable to create directory EZ_time_tracker. Maybe the SD card is mounted?");
	      }
	    } catch (IOException e) {
	    	Log.e(TAG, "addRecord(), e:"+e.toString());
	    }
	    
	    return outDir;
	}
	
	public String getFileName(){
		return strFileName;
	}
	
	public static String genFileName(int iNumDigital, int iDistance, int iVolume, boolean bRandomMode, boolean bRemoteMode){
	    return String.format(REC_FORMAT, android.os.Build.DEVICE.replaceAll(" ", "_"), (bRemoteMode?"Remote":"Local"), (bRandomMode?"RL":"FL"), AubioTestConfig.CUR_FF_TYPE.toString(), iNumDigital, iDistance, iVolume);
	}
	
	public static String deleteAutoTestRec(int iNumDigital, int iDistance, int iVolume, boolean bRandomMode, boolean bRemoteMode){
	    return deleteAutoTestRec(genFileName(iNumDigital, iDistance, iVolume, bRandomMode, bRemoteMode));
	}
	
	public static String deleteAutoTestRec(String file){
		File outDir = getFolder();
	    if(null != outDir){
	    	File delFile = new File(outDir, file);
	    	if(null != delFile && delFile.exists()){
	    		delFile.delete();
	    		return "Delete "+file+" successfully.";
	    	}else
	    		return file+" not exist.";
	    }
	    return outDir.getAbsolutePath()+" not exist.";
	}
	
	private int miTimer = 0;
	static private final String FORMAT_LINE 		= "%s\t%s\t%s\t%s\t%s\t%s\t%s\n";
	static private final String FORMAT_LINE_RANDOM  = "%s\t%s\t%s\t%s\t%s\t%s\t%s\n";
	
	public void addRecord(String strCode, String strECCode, String strEncodeMark, String strDecode, String strDecodeUnmark, String strDecodeMark, MATCH_RESULTS Desc, boolean bFromAutoCorrection){
		try {
			if(null == writer){
				writer = new BufferedWriter(new FileWriter(outputFile, true));
			}
			
			String strContent = null;
			if(MATCH_RESULTS.DESC_MATCH.equals(Desc)){
				strContent = String.format(mbRandomMode?FORMAT_LINE_RANDOM:FORMAT_LINE, strCode, strECCode, strEncodeMark, strDecode, strDecodeUnmark, strDecodeMark ,Desc+(bFromAutoCorrection?"_AC":""));
			}else if(MATCH_RESULTS.DESC_MATCH_EC.equals(Desc) || MATCH_RESULTS.DESC_MISMATCH.equals(Desc)){
				strDecodeUnmark = findDifference(strCode, strDecodeUnmark);
				strContent = String.format(mbRandomMode?FORMAT_LINE_RANDOM:FORMAT_LINE, strCode, strECCode, strEncodeMark, strDecode, strDecodeUnmark, strDecodeMark ,Desc+(bFromAutoCorrection?"_AC":""));
			}else if(MATCH_RESULTS.DESC_MATCH_MSG.equals(Desc) ){
				strDecodeUnmark = findDifferenceFromEnd(strECCode, strDecodeUnmark);
				strContent = String.format(mbRandomMode?FORMAT_LINE_RANDOM:FORMAT_LINE, strCode, strECCode, strEncodeMark, strDecode, strDecodeUnmark, strDecodeMark ,Desc+(bFromAutoCorrection?"_AC":""));
			}else if(MATCH_RESULTS.DESC_TIMEOUT.equals(Desc) || MATCH_RESULTS.DESC_TIMEOUT_MSG.equals(Desc) || MATCH_RESULTS.DESC_TIMEOUT_MSG_EC.equals(Desc)){
				strContent = String.format(mbRandomMode?FORMAT_LINE_RANDOM:FORMAT_LINE, strCode, strECCode, strEncodeMark, strDecode, strDecodeUnmark, strDecodeMark ,Desc+(bFromAutoCorrection?"_AC":""));
			}
			
			  //Log.e(TAG, "saveDataToFile(), strContent:"+strContent);
			writer.write(strContent);
			writer.flush();
			if(10 <=  ++miTimer){
				miTimer = 0;
				closeFile();
			}
		} catch (IOException e) {
			Log.e(TAG, "addRecord(), e:"+e.toString());
		}
		addCountByType(Desc, bFromAutoCorrection);
	}
	
	static private String findDifference(String strSrc, String strDecode){
		StringBuilder strRet = new StringBuilder(strDecode);
		int iLenSrc = (null != strSrc)?strSrc.length():0;
		for(int i =0; i < iLenSrc; i++){
			if(i >= strDecode.length())
				break;
			if(!strSrc.substring(i, i+1).equals(strDecode.substring(i, i+1))){
				strRet.insert(i, "#");
				break;
			}
		}
		return strRet.toString();
	}
	
	static private String findDifferenceFromEnd(String strSrc, String strDecode){
		StringBuilder strRet = new StringBuilder(strDecode);
		int iLenSrc = (null != strSrc)?strSrc.length():0;
		int iLenDecode = (null != strDecode)?strDecode.length():0;
		for(int i =0; i < iLenSrc; i++){
			if(i > strDecode.length())
				break;
			if(!strSrc.substring((iLenSrc - 1) - i, ((iLenSrc - 1) - i)+1).equals(strDecode.substring((iLenDecode - 1) - i, ((iLenDecode - 1) - i)+1))){
				strRet.insert(((iLenSrc - 1) - i), "$");
				break;
			}
		}
		return strRet.toString();
	}
	
	public void closeFile(){
		if(null != writer){
			try {
				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			writer = null;
		}
	}
	
	public int getRecordCount(){
		int iRet = 0;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(outputFile));
			if(null != reader){
				try {
					while(null != (reader.readLine())) 
						iRet++;
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			reader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return iRet;
	}
	
	public String getLastRecord(){
		String strRet = null;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(outputFile));
			if(null != reader){
				String strLastLine = null;
				try {
					String strTmp = null;
					while(null != (strTmp = reader.readLine())) 
						strLastLine = strTmp;
					
					//reader.reset();
					//strLastLine = reader.readLine();
					Log.i(TAG, "getLastRecord(), strLastLine:"+strLastLine);
					if(null != strLastLine){
						String[] split = strLastLine.split("\t");
						if(null != split && 0 < split.length){
							strRet = split[0];
						}
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			reader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return strRet;
	}
}
