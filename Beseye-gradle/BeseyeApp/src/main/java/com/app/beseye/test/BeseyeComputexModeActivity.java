package com.app.beseye.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.app.beseye.BeseyeBaseActivity;
import com.app.beseye.BeseyeEntryActivity;
import com.app.beseye.R;
import com.app.beseye.R.id;
import com.app.beseye.R.layout;
import com.app.beseye.httptask.SessionMgr;
import com.app.beseye.httptask.SessionMgr.SERVER_MODE;
import com.app.beseye.util.BeseyeConfig;
import com.app.beseye.util.BeseyeStorageAgent;
import com.app.beseye.util.BeseyeUtils;

public class BeseyeComputexModeActivity extends BeseyeBaseActivity {
//	private RadioButton mRbDemomode;
//	private RadioButton mRbPairingmode;
//	private RadioButton mRbP2Pmode;
//	private EditText mTxtIP;
//	private EditText mTxtCamName;
//	private EditText mTxtPeriod;
	private Button mBtnApply, mBtnSendLog;
	private Spinner mSpServerType;//, mSpDetachHWID;
	private CheckBox mCbCamSWUpdateSuspended, mCbCamShowNotificationToast, mCbShowHumanDetectAlways, mCbEnableBeseyeAppVerChk, mCbDetachHWIDs[];
	private EditText mEtDefEmail = null, mEtStreamFile = null;
	private LinearLayout mVgHWIDs;
	private static String[] hwids_dev = new String[]{"00409O92TX91", "00409T95HZSR"};//new String[]{"0050C101A639", "00409CR26Q1M"};//new String[]{"00409NDO3R15", "00409XONGY7H"}
	//private static int[] ctrlhwids = new int[]{R.id.ck_hw_id_1, R.id.ck_hw_id_2};
	public static String[] hwids_prod = new String[]{"0090G101A232", "0090G101A235","0090G101A225"};
	private String[] hwids = null;
	private ArrayList<String> mArrHWIDs = null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mbIgnoreSessionCheck = true;
		mbIgnoreCamVerCheck = true;
		
//		mRbDemomode = (RadioButton)findViewById(R.id.rbDemo);
//		mRbPairingmode = (RadioButton)findViewById(R.id.rbPairing);
//		mRbP2Pmode = (RadioButton)findViewById(R.id.rbP2P);
//		
//		mTxtIP = (EditText)findViewById(R.id.editText_ip);
//		mTxtCamName = (EditText)findViewById(R.id.editText_cam_name);
//		mTxtPeriod = (EditText)findViewById(R.id.editText_notify_period);
		
		mBtnApply = (Button)findViewById(R.id.button_confirm);
		if(null != mBtnApply){
			mBtnApply.setOnClickListener(this);
		}
		
		mCbCamSWUpdateSuspended = (CheckBox)findViewById(R.id.ck_suspend_cam_sw_update);
		if(null != mCbCamSWUpdateSuspended){
			mCbCamSWUpdateSuspended.setChecked(SessionMgr.getInstance().getIsCamSWUpdateSuspended());
		}
		
		mCbCamShowNotificationToast = (CheckBox)findViewById(R.id.ck_enable_notify_show);
		if(null != mCbCamShowNotificationToast){
			mCbCamShowNotificationToast.setChecked(SessionMgr.getInstance().getIsShowNotificationFromToast());
		}
		
		mCbShowHumanDetectAlways = (CheckBox)findViewById(R.id.ck_enable_human_detect_intro_show_always);
		if(null != mCbShowHumanDetectAlways){
			mCbShowHumanDetectAlways.setChecked(SessionMgr.getInstance().getHumanDetectIntroShowAlways());
		}
		
		mCbEnableBeseyeAppVerChk = (CheckBox)findViewById(R.id.ck_enable_beseye_app_ver_check);
		if(null != mCbEnableBeseyeAppVerChk){
			mCbEnableBeseyeAppVerChk.setChecked(SessionMgr.getInstance().getEnableBeseyeAppVerControl());
		}
		
		mSpServerType = (Spinner)findViewById(R.id.sp_server_type);
		if(null != mSpServerType){
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,new String[]{"Develop Server","Develop 2 Server(deprecated)","Production Server", "Staging Server (Computex)", "China p2-Stage Server"});
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			mSpServerType.setAdapter(adapter);
//			mSpServerType.setOnItemSelectedListener(new OnItemSelectedListener(){
//				@Override
//				public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//					
//				}
//
//				@Override
//				public void onNothingSelected(AdapterView<?> parent) {
//				}});
		}
		
		hwids = hwids_prod;
		
		mVgHWIDs = (LinearLayout)findViewById(R.id.vg_detach_hw_ids);
		
//		File notifyFile = new File(path);
//		//int iPeriod = 5;
//		if(null != notifyFile && notifyFile.exists()){
//			try {
//				BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(notifyFile)));
//				try {
//					String strPeriod = (null != reader)?reader.readLine():null;
//					if(null != strPeriod && 0 < strPeriod.length())
//						Log.e(BeseyeConfig.TAG, strPeriod);
//						//iPeriod = Integer.parseInt(strPeriod);
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			} catch (FileNotFoundException e) {
//				e.printStackTrace();
//			}
//		}else{
//			Log.e(BeseyeConfig.TAG, "file not exist");
//		}
//		mSpDetachHWID = (Spinner)findViewById(R.id.sp_detach_hw_id);
//		if(null != mSpDetachHWID){
//			String strDeatchHWID = SessionMgr.getInstance().getDetachHWID();
//			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,hwids);
//			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//			mSpDetachHWID.setAdapter(adapter);
//			
//			if(null != strDeatchHWID){
//				mSpDetachHWID.setSelection(0);
//				if(0 == strDeatchHWID.length()){
//					SessionMgr.getInstance().setDetachHWID(hwids[0]);
//				}else{
//					for(int idx = 0;idx < hwids.length;idx++){
//						if(strDeatchHWID.equals(hwids[idx])){
//							mSpDetachHWID.setSelection(idx);
//						}
//					}
//				}
//			}
//		}
		
		mEtDefEmail = (EditText)findViewById(R.id.et_signup_email);
		if(null != mEtDefEmail){
			mEtDefEmail.setText(SessionMgr.getInstance().getSignupEmail());
		}
		
		mEtStreamFile = (EditText)findViewById(R.id.et_fake_path);
		if(null != mEtStreamFile){
			mEtStreamFile.setText(SessionMgr.getInstance().getDebugStreamPath());
		}
		
		mBtnSendLog = (Button)findViewById(R.id.btn_send_log);
		if(null != mBtnSendLog){
			mBtnSendLog.setOnClickListener(this);
		}
		checkMode();
	}

	@Override
	public void onClick(View view) {
		switch(view.getId()){
			case R.id.button_confirm:{
				applyMode();
				break;
			}
			case R.id.btn_send_log:{
				BeseyeUtils.saveLogToFile(this);
				break;
			}
			default:
				super.onClick(view);
		}
	}
	
	private void checkMode(){
//		File pairingFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Download/bes_pairing");
//		if((null != pairingFile)&&pairingFile.exists()){
//			mRbPairingmode.setChecked(true);
//		}else{
//			File p2pFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Download/bes_p2p");
//			if(null != p2pFile && p2pFile.exists()){
//				mRbP2Pmode.setChecked(true);
//			}else{
//				mRbDemomode.setChecked(true);
//			}
//		}
//		
//		File notifyFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Download/bes_notify");
//		int iPeriod = 5;
//		if(null != notifyFile && notifyFile.exists()){
//			try {
//				BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(notifyFile)));
//				try {
//					String strPeriod = (null != reader)?reader.readLine():null;
//					if(null != strPeriod && 0 < strPeriod.length())
//						iPeriod = Integer.parseInt(strPeriod);
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			} catch (FileNotFoundException e) {
//				e.printStackTrace();
//			}
//		}
//		mTxtPeriod.setText(iPeriod+"");
		
		SERVER_MODE mode = SessionMgr.getInstance().getServerMode();
		if(null != mSpServerType){
			mSpServerType.setSelection(mode.ordinal());
		}
		
		if(null != mVgHWIDs){
			mVgHWIDs.removeAllViews();
			
			mArrHWIDs = new ArrayList<String>();
			File fileHWIDs = BeseyeStorageAgent.getFileInDownloadDir(getApplicationContext(), "hwids.txt");
			if(null != fileHWIDs && fileHWIDs.isFile() && fileHWIDs.exists()){
				try {
					BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileHWIDs)));
					try {
						String strHWID = "";
						while(null != (strHWID = (null != reader)?reader.readLine():null)){
							if(null != strHWID && 0 < strHWID.length()){
								mArrHWIDs.add(strHWID);
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
			
			if(0 == mArrHWIDs.size()){
				Log.e(BeseyeConfig.TAG, "file is not exist");
				for(int idx =0; idx < hwids.length;idx++){
					mArrHWIDs.add(hwids[idx]);
				}
			}
			
			if(0 < mArrHWIDs.size()){
				mCbDetachHWIDs = new CheckBox[mArrHWIDs.size()];
				String[] strDeatchHWID = SessionMgr.getInstance().getDetachHWID().split(",");

				for(int idx2 = 0; idx2 < mArrHWIDs.size(); idx2++){
					mCbDetachHWIDs[idx2] = new CheckBox(this);
					if(null != mCbDetachHWIDs[idx2]){
						String strHWID = mArrHWIDs.get(idx2);
						mCbDetachHWIDs[idx2].setText(strHWID);
						for(int idxChk = 0; idxChk < strDeatchHWID.length;idxChk++){
							if(strHWID.equals(strDeatchHWID[idxChk])){
								mCbDetachHWIDs[idx2].setChecked(true);
								break;
							}
						}
						mVgHWIDs.addView(mCbDetachHWIDs[idx2]);
					}
				}
			}
		}	
	}
	
	private void applyMode(){
		SERVER_MODE mode = com.app.beseye.util.BeseyeConfig.DEFAULT_SERVER_MODE;
		if(null != mSpServerType){
			mode = SERVER_MODE.translateToMode(mSpServerType.getSelectedItemPosition());
			SessionMgr.getInstance().setServerMode(mode);
			SessionMgr.getInstance().setBEHostUrl(mode);
			SessionMgr.getInstance().setIsCamSWUpdateSuspended(mCbCamSWUpdateSuspended.isChecked());
			SessionMgr.getInstance().setIsShowNotificationFromToast(mCbCamShowNotificationToast.isChecked());
			SessionMgr.getInstance().setHumanDetectIntroShowAlways(mCbShowHumanDetectAlways.isChecked());
			SessionMgr.getInstance().setEnableBeseyeAppVerControl(mCbEnableBeseyeAppVerChk.isChecked());

			
//			if(null != mSpDetachHWID){
//				SessionMgr.getInstance().setDetachHWID(hwids[mSpDetachHWID.getSelectedItemPosition()]);
//			}
			
			String strHWIds = "";
			if(null != mArrHWIDs){
				for(int idx = 0; idx < mArrHWIDs.size(); idx ++){
					if(mCbDetachHWIDs[idx].isChecked()){
						if(strHWIds.equals("")){
							strHWIds = mArrHWIDs.get(idx);
						}else{
							strHWIds += (","+mArrHWIDs.get(idx));
						}
					}
				}
			}
			
			
			SessionMgr.getInstance().setDetachHWID(strHWIds);
			
			if(null != mEtDefEmail){
				SessionMgr.getInstance().setSignupEmail(mEtDefEmail.getText().toString());
			}
			
			if(null != mEtStreamFile){
				SessionMgr.getInstance().setDebugStreamPath(mEtStreamFile.getText().toString());
			}
			
			//Toast.makeText(this, "Server mode is "+mode, Toast.LENGTH_LONG).show();
			Toast.makeText(this, "Server mode is "+mode+"\nCam SW update is"+(SessionMgr.getInstance().getIsCamSWUpdateSuspended()?"":" not")+
					" suspended.\nDetach HW ID:"+strHWIds+"\n Enable beseye ver control:"+SessionMgr.getInstance().getEnableBeseyeAppVerControl()+"\n Email:"+SessionMgr.getInstance().getSignupEmail()+"\n stream path:"+SessionMgr.getInstance().getDebugStreamPath(), Toast.LENGTH_LONG).show();
		}
		
//		if(mRbDemomode.isChecked() && mode.ordinal() <= SERVER_MODE.MODE_COMPUTEX.ordinal()){
//			File pairingFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Download/bes_pairing");
//			if((null != pairingFile)&&pairingFile.exists()){
//				pairingFile.delete();
//			}
//			File p2pFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Download/bes_p2p");
//			if(null != p2pFile && p2pFile.exists()){
//				p2pFile.delete();
//			}
//			Toast.makeText(this, "Demo mode applied", Toast.LENGTH_LONG).show();
//		}else if(mRbPairingmode.isChecked() || mode.ordinal() > SERVER_MODE.MODE_COMPUTEX.ordinal()){
//			File pairingFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Download/bes_pairing");
//			if((null != pairingFile)){
//				Writer writer = null;
//				try {
//					writer = new BufferedWriter(new FileWriter(pairingFile));
//					if(null != writer){
//						writer.write("pairing");
//						writer.close();
//					}
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
//			
//			File p2pFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Download/bes_p2p");
//			if(null != p2pFile && p2pFile.exists()){
//				p2pFile.delete();
//			}
//			Toast.makeText(this, "Pairing mode applied", Toast.LENGTH_LONG).show();
//		}else{
//			File pairingFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Download/bes_pairing");
//			if((null != pairingFile)&&pairingFile.exists()){
//				pairingFile.delete();
//			}
//			
//			File p2pFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Download/bes_p2p");
//			if(null != p2pFile){
//				Writer writer = null;
//				try {
//					writer = new BufferedWriter(new FileWriter(p2pFile));
//					if(null != writer){
//						writer.write("rtmp://"+mTxtIP.getText().toString()+"/proxypublish/stream2 live=1"+"\n"+mTxtCamName.getText().toString());
//						writer.close();
//					}
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
//			
//			Toast.makeText(this, "P2P mode applied", Toast.LENGTH_LONG).show();
//		}
//		
//		if(null != mTxtPeriod){
//			int iPeriod = 5;
//			String strPeriod  = mTxtPeriod.getText().toString();
//			if(null != strPeriod && 0 < strPeriod.length()){
//				iPeriod = Integer.parseInt(strPeriod);
//			}
//			
//			if(0 > iPeriod){
//				iPeriod = 0;
//			}
//			
//			if(60*60*1000 < iPeriod){
//				iPeriod = 60*60;
//			}
//			
//			File notifyFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Download/bes_notify");
//			
//			if((null != notifyFile)&&notifyFile.exists()){
//				notifyFile.delete();
//			}
//			
//			Writer writer = null;
//			try {
//				writer = new BufferedWriter(new FileWriter(notifyFile));
//				if(null != writer){
//					writer.write(iPeriod+"");
//					writer.flush();
//					writer.close();
//				}
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			
//			Toast.makeText(this, "Notify period is "+iPeriod+" seconds", Toast.LENGTH_LONG).show();
//		}
		
		launchDelegateActivity(BeseyeEntryActivity.class.getName());
	}

	@Override
	protected int getLayoutId() {
		return R.layout.layout_computex_demo;
	}

}
