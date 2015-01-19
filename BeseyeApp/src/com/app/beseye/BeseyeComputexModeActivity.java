package com.app.beseye;


import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Toast;

import com.app.beseye.httptask.SessionMgr;
import com.app.beseye.httptask.SessionMgr.SERVER_MODE;

public class BeseyeComputexModeActivity extends BeseyeBaseActivity {
	private RadioButton mRbDemomode;
	private RadioButton mRbPairingmode;
	private RadioButton mRbP2Pmode;
	private EditText mTxtIP;
	private EditText mTxtCamName;
	private EditText mTxtPeriod;
	private Button mBtnApply;
	private Spinner mSpServerType;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mbIgnoreSessionCheck = true;
		mbIgnoreCamVerCheck = true;
		
		mRbDemomode = (RadioButton)findViewById(R.id.rbDemo);
		mRbPairingmode = (RadioButton)findViewById(R.id.rbPairing);
		mRbP2Pmode = (RadioButton)findViewById(R.id.rbP2P);
		
		mTxtIP = (EditText)findViewById(R.id.editText_ip);
		mTxtCamName = (EditText)findViewById(R.id.editText_cam_name);
		mTxtPeriod = (EditText)findViewById(R.id.editText_notify_period);
		
		mBtnApply = (Button)findViewById(R.id.button_confirm);
		if(null != mBtnApply){
			mBtnApply.setOnClickListener(this);
		}
		
		mSpServerType = (Spinner)findViewById(R.id.sp_server_type);
		if(null != mSpServerType){
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,new String[]{"Develop Server","Develop 2 Server","Staging Server", "Tokyo Staging Server(deprecated)"/*, "Production Server"*/});
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			mSpServerType.setAdapter(adapter);
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
		SERVER_MODE mode = SessionMgr.DEFAULT_SERVER_MODE;
		if(null != mSpServerType){
			mode = SERVER_MODE.translateToMode(mSpServerType.getSelectedItemPosition());
			SessionMgr.getInstance().setServerMode(mode);
			SessionMgr.getInstance().setBEHostUrl(mode);
			Toast.makeText(this, "Server mode is "+mode, Toast.LENGTH_LONG).show();
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
