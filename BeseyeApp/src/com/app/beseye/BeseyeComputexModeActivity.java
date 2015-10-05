package com.app.beseye;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;

import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.app.beseye.httptask.SessionMgr;
import com.app.beseye.httptask.SessionMgr.SERVER_MODE;
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
	private CheckBox mCbCamSWUpdateSuspended, mCbCamShowNotificationToast, mCbShowHumanDetectOneTime, mCbDetachHWIDs[];
	private EditText mEtDefEmail = null;
	private static String[] hwids = new String[]{"00409O92TX91", "00409T95HZSR"};//new String[]{"0050C101A639", "00409CR26Q1M"};//new String[]{"00409NDO3R15", "00409XONGY7H"}
	private static int[] ctrlhwids = new int[]{R.id.ck_hw_id_1, R.id.ck_hw_id_2};
	
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
		
		mCbShowHumanDetectOneTime = (CheckBox)findViewById(R.id.ck_enable_human_detect_intro_show_once);
		if(null != mCbShowHumanDetectOneTime){
			mCbShowHumanDetectOneTime.setChecked(SessionMgr.getInstance().getHumanDetectIntroShowOnce());
		}
		
		mSpServerType = (Spinner)findViewById(R.id.sp_server_type);
		if(null != mSpServerType){
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,new String[]{"Develop Server","Develop 2 Server(deprecated)","Production Server", "Staging Server (Computex)", "China p2-Stage Server"});
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			mSpServerType.setAdapter(adapter);
		}
		
		mCbDetachHWIDs = new CheckBox[hwids.length];
		String[] strDeatchHWID = SessionMgr.getInstance().getDetachHWID().split(",");
		for(int idx = 0; idx < hwids.length; idx ++){
			mCbDetachHWIDs[idx] = (CheckBox)findViewById(ctrlhwids[idx]);
			if(null != mCbDetachHWIDs[idx]){
				mCbDetachHWIDs[idx].setText(hwids[idx]);
				for(int idxChk = 0; idxChk < strDeatchHWID.length;idxChk++){
					if(hwids[idx].equals(strDeatchHWID[idxChk])){
						mCbDetachHWIDs[idx].setChecked(true);
						break;
					}
				}
			}
		}
		
//		AsyncTask task = new AsyncTask(){
//
//			@Override
//			protected Object doInBackground(Object... params) {
//
//				
//				String path = "https://www.dropbox.com/s/7dslg5u6dxbkema/hwids.txt?dl=0";//"https://drive.google.com/file/d/0B--ub8Utz0EXWDVXdEYtREpHZmc/view?usp=sharing";
//				InputStream inputStream = null;
//				try {
//					AndroidHttpClient client = AndroidHttpClient.newInstance("Android");
//					HttpGet getRequest = new HttpGet(path);
//					try{
//						if(null != getRequest){
//					      	HttpResponse response = client.execute(getRequest);
//							//Log.w(TAG, "end to download, uri:" + uri);
//					      	final int statusCode = response.getStatusLine().getStatusCode();
//					      	if(statusCode == HttpStatus.SC_OK){
//					      		final HttpEntity entity = response.getEntity();
//					      		if(entity != null){
//					      			inputStream = AndroidHttpClient.getUngzippedContent(entity);
//					      			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
//									try {
//										String strPeriod = (null != reader)?reader.readLine():null;
//										if(null != strPeriod && 0 < strPeriod.length())
//											Log.e(BeseyeConfig.TAG, strPeriod);
//											//iPeriod = Integer.parseInt(strPeriod);
//									} catch (IOException e) {
//										e.printStackTrace();
//									}
//					      			entity.consumeContent();
//					      		}
//					      	}
//						    
//						}
//					}catch(Exception e){
//					      // Could provide a more explicit error message for IOException or
//					      // IllegalStateException
//						Log.w(TAG, "Http Get image fail: " + e);
//					    if(null != getRequest){
//					    	getRequest.abort();
//					    }
//					}finally{
//					    if(client != null){
//					    	client.close();
//					    }
//					}
//				} catch (Exception e) {
//					Log.w(TAG, "Http Get image fail: " + e);
//				} finally {
//					if(null != inputStream){
//						try {
//							inputStream.close();
//						} catch (IOException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
//					}
//				}
//				return null;
//			}};
//		task.execute();
		
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
	}
	
	private void applyMode(){
		SERVER_MODE mode = com.app.beseye.util.BeseyeConfig.DEFAULT_SERVER_MODE;
		if(null != mSpServerType){
			mode = SERVER_MODE.translateToMode(mSpServerType.getSelectedItemPosition());
			SessionMgr.getInstance().setServerMode(mode);
			SessionMgr.getInstance().setBEHostUrl(mode);
			SessionMgr.getInstance().setIsCamSWUpdateSuspended(mCbCamSWUpdateSuspended.isChecked());
			SessionMgr.getInstance().setIsShowNotificationFromToast(mCbCamShowNotificationToast.isChecked());
			SessionMgr.getInstance().setHumanDetectIntroShowOnce(mCbShowHumanDetectOneTime.isChecked());
			
//			if(null != mSpDetachHWID){
//				SessionMgr.getInstance().setDetachHWID(hwids[mSpDetachHWID.getSelectedItemPosition()]);
//			}
			
			String strHWIds = "";
			for(int idx = 0; idx < hwids.length; idx ++){
				if(mCbDetachHWIDs[idx].isChecked()){
					if(strHWIds.equals("")){
						strHWIds = hwids[idx];
					}else{
						strHWIds += (","+hwids[idx]);
					}
				}
			}
			
			SessionMgr.getInstance().setDetachHWID(strHWIds);
			
			if(null != mEtDefEmail){
				SessionMgr.getInstance().setSignupEmail(mEtDefEmail.getText().toString());
			}
			//Toast.makeText(this, "Server mode is "+mode, Toast.LENGTH_LONG).show();
			Toast.makeText(this, "Server mode is "+mode+"\nCam SW update is"+(SessionMgr.getInstance().getIsCamSWUpdateSuspended()?"":" not")+
					" suspended.\nDetach HW ID:"+strHWIds+"\n Email:"+SessionMgr.getInstance().getSignupEmail(), Toast.LENGTH_LONG).show();
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
