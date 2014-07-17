package com.app.beseye.setting;

import static com.app.beseye.util.BeseyeConfig.*;
import static com.app.beseye.util.BeseyeJSONUtil.*;
import static com.app.beseye.websockets.BeseyeWebsocketsUtil.WS_ATTR_CAM_UID;
import static com.app.beseye.websockets.BeseyeWebsocketsUtil.WS_ATTR_TS;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.app.beseye.BeseyeBaseActivity;
import com.app.beseye.CameraListActivity;
import com.app.beseye.OpeningPage;
import com.app.beseye.WifiSetupGuideActivity;

import com.app.beseye.R;
import com.app.beseye.httptask.BeseyeAccountTask;
import com.app.beseye.httptask.BeseyeCamBEHttpTask;
import com.app.beseye.setting.CamSettingMgr.CAM_CONN_STATUS;
import com.app.beseye.util.BeseyeConfig;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.widget.BeseyeSwitchBtn.SwitchState;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class CameraInfoActivity extends BeseyeBaseActivity{

	private TextView mTxtCamName, mTxtSwVersion, mTxtSerialNum, mTxtMacAddr;
	private String mStrNameCandidate, mStrVCamSN = null;
	private String mStrVCamMacAddr = null;
	private String mStrSwVer = null;
	
	private View mVwNavBar;
	private ActionBar.LayoutParams mNavBarLayoutParams;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "CameraInfoActivity::onCreate()");
		super.onCreate(savedInstanceState);

		getSupportActionBar().setDisplayOptions(0);
		getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
		
		mVwNavBar = getLayoutInflater().inflate(R.layout.layout_base_nav, null);
		if(null != mVwNavBar){
			ImageView mIvBack = (ImageView)mVwNavBar.findViewById(R.id.iv_nav_left_btn);
			if(null != mIvBack){
				mIvBack.setOnClickListener(this);
			}
						
			TextView txtTitle = (TextView)mVwNavBar.findViewById(R.id.txt_nav_title);
			if(null != txtTitle){
				txtTitle.setText(R.string.cam_setting_title_cam_info);
			}
			
			mNavBarLayoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.FILL_PARENT, ActionBar.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
	        getSupportActionBar().setCustomView(mVwNavBar, mNavBarLayoutParams);
		}
		
		try {
			mCam_obj = new JSONObject(getIntent().getStringExtra(CameraListActivity.KEY_VCAM_OBJ));
			
			if(null != mCam_obj){
				mStrVCamID = BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_ID);
				mStrVCamName = BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_NAME);
			}
		} catch (JSONException e1) {
			Log.e(TAG, "CameraInfoActivity::updateAttrByIntent(), failed to parse, e1:"+e1.toString());
		}
		
		mTxtCamName = (TextView)findViewById(R.id.txt_cam_name);
		if(null != mTxtCamName){
			mTxtCamName.setText(mStrVCamName);
			mTxtCamName.setOnClickListener(this);
		}
		
		mTxtSwVersion = (TextView)findViewById(R.id.txt_sw_ver);
		mTxtSerialNum = (TextView)findViewById(R.id.txt_sn);
		mTxtMacAddr = (TextView)findViewById(R.id.txt_mac_addr);

	}
	
	protected void onSessionComplete(){
		super.onSessionComplete();
		monitorAsyncTask(new BeseyeCamBEHttpTask.GetSystemInfoTask(this), true, mStrVCamID);
	}
	
	@Override
	protected int getLayoutId() {
		return R.layout.layout_cam_info;
	}

	@Override
	public void onClick(View view) {
		if(R.id.txt_cam_name == view.getId()){
			showDialog(DIALOG_ID_CAM_INFO);
		}else if(R.id.btn_ok == view.getId()){
			removeDialog(DIALOG_ID_CAM_INFO);
		}else{
			super.onClick(view);
		}
	}
	
	@Override
	public void onErrorReport(AsyncTask task, int iErrType, String strTitle,
			String strMsg) {
		if(task instanceof BeseyeCamBEHttpTask.GetSystemInfoTask){
			BeseyeUtils.postRunnable(new Runnable(){
				@Override
				public void run() {
					Bundle b = new Bundle();
					b.putString(KEY_WARNING_TEXT, getResources().getString(R.string.cam_setting_fail_to_get_cam_info));
					b.putBoolean(KEY_WARNING_CLOSE, true);
					showMyDialog(DIALOG_ID_WARNING, b);
				}}, 0);
			
		}else
			super.onErrorReport(task, iErrType, strTitle, strMsg);
	}

	@Override
	public void onPostExecute(AsyncTask task, List<JSONObject> result, int iRetCode) {
		if(!task.isCancelled()){
			if(task instanceof BeseyeCamBEHttpTask.GetSystemInfoTask){
				if(0 == iRetCode){
					JSONObject obj = result.get(0);
					if(null != obj){
						Log.i(TAG, "onPostExecute(), "+obj.toString());
						JSONObject dataObj = BeseyeJSONUtil.getJSONObject(obj, ACC_DATA);
						if(null != dataObj){
							mStrVCamSN = BeseyeJSONUtil.getJSONString(dataObj, BeseyeJSONUtil.CAM_SN);
							mStrVCamMacAddr = BeseyeJSONUtil.getJSONString(dataObj, BeseyeJSONUtil.CAM_MAC_ADDR);
							mStrSwVer = BeseyeJSONUtil.getJSONString(dataObj, BeseyeJSONUtil.CAM_SOFTWARE);
							
							if(null != mTxtSwVersion)
								mTxtSwVersion.setText(mStrSwVer);
							
							if(null != mTxtSerialNum)
								mTxtSerialNum.setText(mStrVCamSN);
							
							if(null != mTxtMacAddr)
								mTxtMacAddr.setText(mStrVCamMacAddr);
	
						}
					}
				}
			}else if(task instanceof BeseyeAccountTask.SetCamAttrTask){
				if(0 == iRetCode){
					Log.i(TAG, "onPostExecute(), "+result.toString());
					onToastShow(task, "Change cam name Successfully.");
					
					mStrVCamName = mStrNameCandidate;
					if(null != mTxtCamName){
						mTxtCamName.setText(mStrVCamName);
					}
					
					BeseyeJSONUtil.setJSONString(mCam_obj, BeseyeJSONUtil.ACC_NAME, mStrNameCandidate);
					Intent resultIntent = new Intent();
					resultIntent.putExtra(CameraListActivity.KEY_VCAM_OBJ, mCam_obj.toString());
					setResult(RESULT_OK, resultIntent);
				}
			}else{
				super.onPostExecute(task, result, iRetCode);
			}
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		Log.d(TAG, "CameraSettingActivity::onCreateDialog()");
		Dialog dialog;
		switch(id){
			case DIALOG_ID_CAM_INFO:{
				dialog = new Dialog(this);
				dialog.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
				dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
				dialog.setContentView(createCamInfoDialog());
				
				if(null != dialog){
					dialog.setCanceledOnTouchOutside(true);
					dialog.setOnCancelListener(new OnCancelListener(){
						@Override
						public void onCancel(DialogInterface arg0) {
							//removeMyDialog(DIALOG_ID_CAM_INFO);
						}});
					dialog.setOnDismissListener(new OnDismissListener(){
						@Override
						public void onDismiss(DialogInterface dialog) {
							EditText etCamName = (EditText)((Dialog)dialog).findViewById(R.id.et_cam_name);
							if(null != etCamName){
								Log.i(TAG, "onDismiss(), cam name is "+etCamName.getText().toString());
								mStrNameCandidate = etCamName.getText().toString();
								if(null != mStrNameCandidate && 0 < mStrNameCandidate.length()){
									if(ASSIGN_ST_PATH){
										STREAM_PATH_LIST.set(0, mStrNameCandidate);
									}else if(null == mStrVCamName || !mStrVCamName.equals(mStrNameCandidate)){
										monitorAsyncTask(new BeseyeAccountTask.SetCamAttrTask(CameraInfoActivity.this), true, mStrVCamID, etCamName.getText().toString());
									}
								}else{
									Bundle b = new Bundle();
									b.putString(KEY_WARNING_TEXT, getResources().getString(R.string.cam_setting_empty_name_warning));
									showMyDialog(DIALOG_ID_WARNING, b);
								}
								//CamSettingMgr.getInstance().setCamName(TMP_CAM_ID, etCamName.getText().toString());
							}
						}});
				}
            	break;
			}
			default:
				dialog = super.onCreateDialog(id);
		}
		return dialog;
	}
	
	private View createCamInfoDialog(){
		View viewRet = null;
		LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if(null != inflater){
			viewRet = (View)inflater.inflate(R.layout.cam_name_edit_dialog, null);
			if(null != viewRet){
				EditText etCamName = (EditText)viewRet.findViewById(R.id.et_cam_name);
				if(null != etCamName){
					etCamName.setText(null == mStrVCamName?CamSettingMgr.getInstance().getCamName(TMP_CAM_ID):mStrVCamName);
//					if(!ASSIGN_ST_PATH)
//						etCamName.setFocusable(false);
				}
				
				Button btnOK = (Button)viewRet.findViewById(R.id.btn_ok);
				if(null != btnOK){
					btnOK.setOnClickListener(this);
				}
			}
		}
		return viewRet;
	}
}
