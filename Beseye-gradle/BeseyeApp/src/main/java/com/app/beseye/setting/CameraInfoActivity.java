package com.app.beseye.setting;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;
import static com.app.beseye.util.BeseyeJSONUtil.ACC_DATA;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.app.beseye.BeseyeBaseActivity;
import com.app.beseye.CameraListActivity;
import com.app.beseye.R;
import com.app.beseye.httptask.BeseyeAccountTask;
import com.app.beseye.httptask.BeseyeCamBEHttpTask;
import com.app.beseye.httptask.SessionMgr;
import com.app.beseye.util.BeseyeCamInfoSyncMgr;
import com.app.beseye.util.BeseyeConfig;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;

public class CameraInfoActivity extends BeseyeBaseActivity{

	private TextView mTxtCamName, mTxtSwVersion, mTxtSerialNum, mTxtMacAddr, mTxtVcamId, mTxtIpAddr;
	private String mStrNameCandidate, mStrVCamSN = null;
	private String mStrVCamMacAddr = null;
	private String mStrSwVer = null;
	
	private View mVwNavBar;
	private ViewGroup mVgSWVer, mVgVCamId, mVgIpAddr;
	private ActionBar.LayoutParams mNavBarLayoutParams;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if(DEBUG)
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
				txtTitle.setOnClickListener(this);
			}
			
			mNavBarLayoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
	        getSupportActionBar().setCustomView(mVwNavBar, mNavBarLayoutParams);
			BeseyeUtils.setToolbarPadding(mVwNavBar, 0);
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
		
		mVgSWVer = (ViewGroup)findViewById(R.id.vg_sw_ver_holder);
		if(null != mVgSWVer){
			if(BeseyeConfig.PRODUCTION_VER){
				mVgSWVer.setVisibility(View.GONE);
			}
		}
		
		mTxtSwVersion = (TextView)findViewById(R.id.txt_sw_ver);
		mTxtSerialNum = (TextView)findViewById(R.id.txt_sn);
		if(null != mTxtSerialNum){
			mTxtSerialNum.setText(BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_VCAM_HW_ID));
		}

		
		mTxtMacAddr = (TextView)findViewById(R.id.txt_mac_addr);
		if(null != mTxtMacAddr){
			mTxtMacAddr.setText(BeseyeJSONUtil.getJSONString(BeseyeJSONUtil.getJSONObject(mCam_obj, BeseyeJSONUtil.ACC_DATA), BeseyeJSONUtil.MAC_ADDR));
		}
		
		mVgVCamId = (ViewGroup)findViewById(R.id.vg_vcam_id_holder);
		if(null != mVgVCamId){
			if(BeseyeConfig.PRODUCTION_VER){
				mVgVCamId.setVisibility(View.GONE);
			}
			mVgVCamId.setOnClickListener(this);
			mTxtVcamId = (TextView)findViewById(R.id.txt_vcam_id);
			if(null != mTxtVcamId){
				mTxtVcamId.setText(BeseyeConfig.PRODUCTION_VER?(mStrVCamID.substring(0, 6)+"..."):mStrVCamID);
			}
		}
		
		mVgIpAddr = (ViewGroup)findViewById(R.id.vg_ip_addr_holder);
		if(null != mVgIpAddr){
			if(BeseyeConfig.PRODUCTION_VER){
				mVgIpAddr.setVisibility(View.GONE);
			}
			mTxtIpAddr = (TextView)findViewById(R.id.txt_ip_addr);
		}
	}
	
	protected void onSessionComplete(){
		super.onSessionComplete();
		monitorAsyncTask(new BeseyeCamBEHttpTask.GetSWVersionTask(this), true, mStrVCamID);
		if(BeseyeConfig.DEBUG)
			monitorAsyncTask(new BeseyeCamBEHttpTask.GetSystemInfoTask(this).setDialogId(-1), true, mStrVCamID);

	}
	
	@Override
	protected void onResume() {
		if(DEBUG)
			Log.i(TAG, "CameraSettingActivity::onResume()");
		
		super.onResume();
		if(!mbFirstResume){
			monitorAsyncTask(new BeseyeAccountTask.GetCamInfoTask(this).setDialogId(-1), true, mStrVCamID);
			
			if(null == BeseyeJSONUtil.getJSONObject(mCam_obj, BeseyeJSONUtil.ACC_DATA) && null != mStrVCamID)
				monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamSetupTask(this).setDialogId(-1), true, mStrVCamID);
		}
	}
	
	@Override
	protected int getLayoutId() {
		return R.layout.layout_cam_info;
	}

	private int miHitCount = 0;
	private int miHitCount2 = 0;
	
	@Override
	public void onClick(View view) {
		if(R.id.txt_cam_name == view.getId()){
			showMyDialog(DIALOG_ID_CAM_INFO);
		}else if(R.id.btn_ok == view.getId()){
			removeMyDialog(DIALOG_ID_CAM_INFO);
		}else if(R.id.txt_nav_title == view.getId()){
			if(++miHitCount == 3){
				BeseyeUtils.setVisibility(mVgSWVer, View.VISIBLE);
			}else if(++miHitCount == 5){
				BeseyeUtils.setVisibility(mVgVCamId, View.VISIBLE);
			}
		}else if(R.id.vg_vcam_id_holder == view.getId()){
			if(!BeseyeConfig.PRODUCTION_VER && SessionMgr.getInstance().getShowPirvateCam() && ++miHitCount2 == 3 ){
				BeseyeUtils.launchEmail(this, null, "[Info] "+mStrVCamName+"'s VCam id", "VCam id is "+mStrVCamID, null);
			}
		}else{
			super.onClick(view);
		}
	}
	
	@Override
	public void onErrorReport(AsyncTask<String, Double, List<JSONObject>> task, final int iErrType, String strTitle,
			String strMsg) {
		if(task instanceof BeseyeCamBEHttpTask.GetSWVersionTask && null != mVgSWVer && View.VISIBLE == mVgSWVer.getVisibility()){
			BeseyeUtils.postRunnable(new Runnable(){
				@Override
				public void run() {
					Bundle b = new Bundle();
					b.putString(KEY_WARNING_TEXT, BeseyeUtils.appendErrorCode(CameraInfoActivity.this, R.string.cam_setting_fail_to_get_cam_info, iErrType));
					//b.putBoolean(KEY_WARNING_CLOSE, true);
					showMyDialog(DIALOG_ID_WARNING, b);
				}}, 0);
			
		}else if(task instanceof BeseyeCamBEHttpTask.GetSystemInfoTask){
			//do nothing due to debug usage
		}else
			super.onErrorReport(task, iErrType, strTitle, strMsg);
	}

	@Override
	public void onPostExecute(AsyncTask<String, Double, List<JSONObject>> task, List<JSONObject> result, int iRetCode) {
		if(!task.isCancelled()){
			if(task instanceof BeseyeCamBEHttpTask.GetSWVersionTask){
				if(0 == iRetCode){
					JSONObject obj = result.get(0);
					if(null != obj){
						if(DEBUG)
							Log.i(TAG, "onPostExecute(), "+obj.toString());
						
						Log.i(TAG, "onPostExecute(), Kelly "+obj.toString());
						mStrSwVer = BeseyeJSONUtil.getJSONString(obj, BeseyeJSONUtil.CAM_SW_VERSION);
						if(null != mTxtSwVersion)
							mTxtSwVersion.setText(mStrSwVer);
					}
				}
			}else if(task instanceof BeseyeCamBEHttpTask.GetSystemInfoTask){
				if(0 == iRetCode){
					JSONObject obj = result.get(0);
					if(null != obj){
						if(DEBUG)
							Log.i(TAG, "onPostExecute(), "+obj.toString());
						JSONObject objData = BeseyeJSONUtil.getJSONObject(obj, BeseyeJSONUtil.ACC_DATA);
						if(null != objData){
							if(null != mTxtIpAddr){
								mTxtIpAddr.setText(BeseyeJSONUtil.getJSONString(objData, BeseyeJSONUtil.CAM_IP_ADDR));
							}
							if(null != mTxtSerialNum){
								mTxtSerialNum.setText(BeseyeJSONUtil.getJSONString(objData, BeseyeJSONUtil.CAM_SN));
							}
						}
					}
				}
			}else if(task instanceof BeseyeAccountTask.SetCamAttrTask){
				if(0 == iRetCode){
					if(DEBUG){
						Log.i(TAG, "onPostExecute(), "+result.toString());
						onToastShow(task, getString(R.string.cam_name_update_success));
					}
					
					mStrVCamName = mStrNameCandidate;
					if(null != mTxtCamName){
						mTxtCamName.setText(mStrVCamName);
					}
					
					BeseyeJSONUtil.setJSONString(mCam_obj, BeseyeJSONUtil.ACC_NAME, mStrNameCandidate);
					BeseyeJSONUtil.setJSONLong(mCam_obj, BeseyeJSONUtil.OBJ_TIMESTAMP, System.currentTimeMillis());
					BeseyeCamInfoSyncMgr.getInstance().updateCamInfo(mStrVCamID, mCam_obj);
					setActivityResultWithCamObj();
				}
			}else{
				super.onPostExecute(task, result, iRetCode);
			}
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		if(DEBUG)
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
								if(DEBUG)
									Log.i(TAG, "onDismiss(), cam name is "+etCamName.getText().toString());
								mStrNameCandidate = etCamName.getText().toString();
								if(null != mStrNameCandidate && 0 < mStrNameCandidate.length()){
									/*if(ASSIGN_ST_PATH){
										STREAM_PATH_LIST.set(0, mStrNameCandidate);
									}else */
									if(null == mStrVCamName || !mStrVCamName.equals(mStrNameCandidate)){
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
					etCamName.setText(mStrVCamName);
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
