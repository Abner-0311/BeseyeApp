package com.app.beseye;

import static com.app.beseye.util.BeseyeConfig.*;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

public abstract class BeseyeBaseActivity extends ActionBarActivity implements OnClickListener{
	protected boolean mbFirstResume = true;
	protected boolean mActivityDestroy = false;
	protected boolean mActivityResume = false;
	
	private Handler mHandler = new Handler();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(getLayoutId());
	}
	
	@Override
	protected void onStart() {
		super.onStart();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mActivityResume = true;
		checkForCrashes();
	    checkForUpdates();
	}
	
	@Override
	protected void onPause() {
		mActivityResume = false;
		super.onPause();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mActivityDestroy = true;
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
	}

	@Override
	protected void onActivityResult(int arg0, int arg1, Intent arg2) {
		super.onActivityResult(arg0, arg1, arg2);
	}
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}
	
	private void checkForCrashes() {
	    CrashManager.register(this, HOCKEY_APP_ID);
	}
	
	private void checkForUpdates() {
	    // Remove this for store builds!
		if(DEBUG)
			UpdateManager.register(this, HOCKEY_APP_ID);
	}
	
	static public final String KEY_WARNING_TEXT = "KEY_WARNING_TEXT";
	
	static public final int DIALOG_ID_LOADING = 1; 
	static public final int DIALOG_ID_WARNING = 2; 
	
	static public final int DIALOG_ID_WIFI_BASE = 0x1000; 
	static public final int DIALOG_ID_TURN_ON_WIFI = DIALOG_ID_WIFI_BASE+1; 
	static public final int DIALOG_ID_WIFI_SCANNING = DIALOG_ID_WIFI_BASE+2; 
	static public final int DIALOG_ID_WIFI_SETTING = DIALOG_ID_WIFI_BASE+3; 
	static public final int DIALOG_ID_WIFI_TURN_ON_FAILED = DIALOG_ID_WIFI_BASE+4; 
	static public final int DIALOG_ID_WIFI_SCAN_FAILED = DIALOG_ID_WIFI_BASE+5; 
	static public final int DIALOG_ID_WIFI_AP_INFO = DIALOG_ID_WIFI_BASE+6; 
	static public final int DIALOG_ID_WIFI_AP_INCORRECT_PW = DIALOG_ID_WIFI_BASE+7; 
	static public final int DIALOG_ID_WIFI_AP_KEYINDEX= DIALOG_ID_WIFI_BASE+8; 
	static public final int DIALOG_ID_CAM_INFO= DIALOG_ID_WIFI_BASE+9; 
	
	@Override
	protected Dialog onCreateDialog(int id, Bundle bundle) {
		Dialog dialog;
		switch(id){
			case DIALOG_ID_WARNING:{
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
            	builder.setTitle(R.string.dialog_title_warning);
            	builder.setMessage(bundle.getString(KEY_WARNING_TEXT));
				builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog, int item) {
				    	removeMyDialog(DIALOG_ID_WARNING);
				    }
				});
				builder.setOnCancelListener(new OnCancelListener(){
					@Override
					public void onCancel(DialogInterface dialog) {
						removeMyDialog(DIALOG_ID_WARNING);
					}});
				
				dialog = builder.create();
				if(null != dialog){
					dialog.setCanceledOnTouchOutside(true);
				}
				break;
			}
			default:
				dialog = super.onCreateDialog(id, bundle);
		}
		return dialog;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		switch(id){
			case DIALOG_ID_LOADING:{
				dialog = ProgressDialog.show(this, "", getString(R.string.dialog_loading), true, true);
				dialog.setCancelable(false);
				//TODO: avoid this dialog infinite showing
				break;
			}
			case DIALOG_ID_TURN_ON_WIFI:{
				dialog = ProgressDialog.show(this, "", getString(R.string.dialog_wifi_turning_on), true, true);
				dialog.setCancelable(false);
				//TODO: avoid this dialog infinite showing
				break;
			}
			case DIALOG_ID_WIFI_SCANNING:{
				dialog = ProgressDialog.show(this, "", getString(R.string.dialog_wifi_scanning), true, true);
				dialog.setCancelable(false);
				//TODO: avoid this dialog infinite showing
				break;
			}
			case DIALOG_ID_WIFI_SETTING:{
				dialog = ProgressDialog.show(this, "", getString(R.string.dialog_wifi_setting), true, true);
				dialog.setCancelable(false);
				//TODO: avoid this dialog infinite showing
				break;
			}
			default:
				dialog = super.onCreateDialog(id);
		}
		return dialog;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
		// TODO Auto-generated method stub
		super.onPrepareDialog(id, dialog, args);
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		// TODO Auto-generated method stub
		super.onPrepareDialog(id, dialog);
	}
	
	public boolean showMyDialog(int iDialogId){
		return showMyDialog(iDialogId, null);
	}
	
	/*Workaround: Avoid the dialog isn't showed if we invoke it continuously. */
	private RemoveDialogRunnable mRemoveDialogRunnable = null;
	static private final long TIME_TO_REMOVE_DIALOG = 500L;
	
	private class RemoveDialogRunnable implements Runnable{
		public int miLastDialog = -1;
		public boolean mbCompleted = false;
		
		public RemoveDialogRunnable(int iLastDialog){
			miLastDialog = iLastDialog;
		}
		
		@Override
		public void run() {
			if(false == mActivityDestroy && 0 <= miLastDialog){
				Log.d(TAG, "removeDialog(), iDialogId="+miLastDialog);
				removeDialog(miLastDialog);
			}
			mbCompleted = true;
		}
	}
	
	public boolean showMyDialog(int iDialogId, Bundle bundle){
		if(false == mActivityDestroy && 0 <= iDialogId){
			if(null != mRemoveDialogRunnable && false == mRemoveDialogRunnable.mbCompleted && mRemoveDialogRunnable.miLastDialog == iDialogId){
				Log.d(TAG, "showMyDialog(), remove mRemoveDialogRunnable, iDialogId="+iDialogId);
				mHandler.removeCallbacks(mRemoveDialogRunnable);
				return true;
			}
			Log.d(TAG, "showMyDialog(), iDialogId="+iDialogId);
			return showDialog(iDialogId, bundle);
		}
		return false;
	}
	
	/*
	 *we will check if activity is destroyed before handle the dialog 
	 */
	public void dismissMyDialog(int iDialogId){
		if(false == mActivityDestroy && 0 <= iDialogId){
			mRemoveDialogRunnable = new RemoveDialogRunnable(iDialogId);
			mHandler.postDelayed(mRemoveDialogRunnable, TIME_TO_REMOVE_DIALOG);
		}
	}
	
	public void removeMyDialog(int iDialogId){
		if(false == mActivityDestroy && 0 <= iDialogId){
			mRemoveDialogRunnable = new RemoveDialogRunnable(iDialogId);
			mHandler.postDelayed(mRemoveDialogRunnable, TIME_TO_REMOVE_DIALOG);
		}
	}

	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		
	}

	protected abstract int getLayoutId();
}
