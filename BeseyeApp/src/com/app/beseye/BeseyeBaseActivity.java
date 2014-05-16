package com.app.beseye;

import static com.app.beseye.util.BeseyeConfig.*;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import com.app.beseye.BeseyeApplication.BeseyeAppStateChangeListener;
import com.app.beseye.httptask.BeseyeAccountTask;
import com.app.beseye.httptask.BeseyeHttpTask.OnHttpTaskCallback;
import com.app.beseye.httptask.SessionMgr;
import com.app.beseye.httptask.SessionMgr.ISessionUpdateCallback;
import com.app.beseye.httptask.SessionMgr.SessionData;
import com.app.beseye.service.BeseyeNotificationService;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.ServiceConnection;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

public abstract class BeseyeBaseActivity extends ActionBarActivity implements OnClickListener, 
																			  OnHttpTaskCallback, 
																			  ISessionUpdateCallback,
																			  BeseyeAppStateChangeListener{
	static public final String KEY_FROM_ACTIVITY					= "KEY_FROM_ACTIVITY";
	
	protected boolean mbFirstResume = true;
	protected boolean mActivityDestroy = false;
	protected boolean mActivityResume = false;
	protected boolean mbIgnoreSessionCheck = false;
	
	private Handler mHandler = new Handler();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(getLayoutId());
		BeseyeApplication.registerAppStateChangeListener(this);
		SessionMgr.getInstance().registerSessionUpdateCallback(this);
		doBindService();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		checkForCrashes();
	    checkForUpdates();
	    BeseyeApplication.increVisibleCount(this);
	    
		//if(! mbIgnoreSessionCheck && checkSession())
	    if( mbIgnoreSessionCheck || checkSession())
			invokeSessionComplete();
		mActivityResume = true;
	}
	
	@Override
	protected void onPause() {
		BeseyeApplication.decreVisibleCount(this);
		mActivityResume = false;
		super.onPause();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}
	
	@Override
	protected void onDestroy() {
		clearLastAsyncTask();
		cancelRunningTasks();
		doUnbindService();
		BeseyeApplication.unregisterAppStateChangeListener(this);
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
	
	private boolean checkSession(){
		if(SessionMgr.getInstance().isTokenValid()){
			monitorAsyncTask(new BeseyeAccountTask.CheckAccountTask(this).setDialogId(mbFirstResume?DIALOG_ID_LOADING:-1), true, SessionMgr.getInstance().getAuthToken());
			//invokeSessionComplete();
			return false;
		}	
		else{
			Log.e(TAG, "checkSession(), need to get new session");
			onSessionInvalid();
			//monitorAsyncTask(new iKalaAddrTask.GetSessionTask(this), true);
		}
		return false;
	}
	
	private void invokeSessionComplete(){
		if(mbFirstResume)
			onSessionComplete();
		mbFirstResume = false;
	}
	
	protected void onSessionComplete(){
		 
	}
	
	private void checkForCrashes() {
	    CrashManager.register(this, HOCKEY_APP_ID);
	}
	
	private void checkForUpdates() {
	    // Remove this for store builds!
		if(DEBUG)
			UpdateManager.register(this, HOCKEY_APP_ID);
	}
	
	static public final String KEY_WARNING_TITLE = "KEY_WARNING_TITLE";
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
	static public final int DIALOG_ID_CAM_DETTACH_CONFIRM= DIALOG_ID_WIFI_BASE+10; 
	
	@Override
	protected Dialog onCreateDialog(int id, Bundle bundle) {
		Dialog dialog;
		switch(id){
			case DIALOG_ID_WARNING:{
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
            	builder.setTitle(bundle.getString(KEY_WARNING_TEXT, getString(R.string.dialog_title_warning)));
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
		switch (id) {
			case DIALOG_ID_LOADING:{
				String strMsgRes = "";
				if(null != args){
					strMsgRes = args.getString(KEY_WARNING_TEXT);
				}
				if(dialog instanceof AlertDialog){
					if(0 < strMsgRes.length())
						((AlertDialog) dialog).setMessage(strMsgRes);
				}
			}
	        case DIALOG_ID_WARNING:{
				String strTitleRes = "", strMsgRes = "";
				if(null != args){
					strTitleRes = args.getString(KEY_WARNING_TITLE);
					strMsgRes = args.getString(KEY_WARNING_TEXT);
				}
				if(dialog instanceof AlertDialog){
					((AlertDialog) dialog).setIcon(R.drawable.common_app_icon_shadow);
					((AlertDialog) dialog).setTitle((strTitleRes == null || 0 == strTitleRes.length())?getString(R.string.dialog_title_warning):strTitleRes);
					if(0 < strMsgRes.length())
						((AlertDialog) dialog).setMessage(strMsgRes);
				}
				
				break;
			}
	        default:
	        	super.onPrepareDialog(id, dialog, args);
	    }
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
	
	/*
	 * Well manage the async tasks
	 * Cancel it if we don't need to run it after finishing this page
	 */
    private Map<AsyncTask, AsyncTaskParams> mMapCurAsyncTasks;
    private AsyncTask mLastAsyncTask = null;
    private AsyncTaskParams mLastTaskParams = null;
    
    static class AsyncTaskParams{
    	AsyncTaskParams(boolean bCancelWhenDestroy, String... strArgs){
    		this.bCancelWhenDestroy = bCancelWhenDestroy;
    		this.strArgs = strArgs;
    	}
    	boolean bCancelWhenDestroy= false;
    	String[] strArgs;
    }
    
    public void monitorAsyncTask(AsyncTask task, boolean bCancelWhenDestroy, String... strArgs){
    	if(null != task){
    		if(null != mMapCurAsyncTasks){
        		mMapCurAsyncTasks.put(task, new AsyncTaskParams(bCancelWhenDestroy, strArgs));
        	}
    		task.execute(strArgs);
    	}
    }
    
    protected void cancelRunningTasks(){
    	if(null != mMapCurAsyncTasks){
    		for(AsyncTask task:mMapCurAsyncTasks.keySet()){
    			AsyncTaskParams params = mMapCurAsyncTasks.get(task);
    			if((null == params || true == params.bCancelWhenDestroy) && AsyncTask.Status.FINISHED != task.getStatus())
    				task.cancel(true);
    		}
    		mMapCurAsyncTasks.clear();
    	} 
    }
    
    protected void recordLastAsyncTask(AsyncTask task){
    	//the task is still executed after clone
//    	try {
//    		if(null != task){
//    			mLastAsyncTask = (AsyncTask) ((iKalaHttpTask)task).clone();
//    			mLastTaskParams = mMapCurAsyncTasks.get(task);
//    		}
//		} catch (CloneNotSupportedException e) {
//			e.printStackTrace();
//		}
    }
    
    protected void clearLastAsyncTask(){
    	mLastAsyncTask = null;
    	mLastTaskParams = null;
    }
    
    protected void onRetryHttpTask(){
    	if(null != mLastAsyncTask){
    		//TODO: how to clone a AsyncTask
//    		if(null != mLastTaskParams)
//    			monitorAsyncTask(mLastAsyncTask, mLastTaskParams.bCancelWhenDestroy, mLastTaskParams.strArgs);
//    		else
//    			monitorAsyncTask(mLastAsyncTask, true);
    	}else{
    		//The basic handle: simulate the scenario of page begin
//		if(checkHost())
//			onSessionComplete();
    	}
    }
    
    protected void onRetryLaterHttpTask(){
    	//The basic handle: close current page
    	finish();
    }
    
	public void onShowDialog(AsyncTask task, final int iDialogId, final int iTitleRes, final int iMsgRes){
		runOnUiThread(new Runnable(){
			@Override
			public void run() {
				Bundle b = null;
				if(0 < iTitleRes || 0 < iMsgRes){
					b = new Bundle();
					if(iTitleRes > 0)
						b.putString(KEY_WARNING_TITLE, getString(iTitleRes));
					if(iMsgRes > 0)
						b.putString(KEY_WARNING_TEXT, getString(iMsgRes));		
				}
				showMyDialog(iDialogId, b);
			}});
	}
	
	public void onShowDialog(AsyncTask task, final int iDialogId, final String strTitleRes, final String strMsgRes){
		runOnUiThread(new Runnable(){
			@Override
			public void run() {
				Bundle b = null;
				b = new Bundle();
				b.putString(KEY_WARNING_TITLE, strTitleRes);
				b.putString(KEY_WARNING_TEXT, strMsgRes);	
				showMyDialog(iDialogId, b);
			}});
	}

	@Override
	public void onDismissDialog(AsyncTask task, final int iDialogId) {
		runOnUiThread(new Runnable(){
			@Override
			public void run() {
				dismissMyDialog(iDialogId);
			}});
	}

	@Override
	public void onErrorReport(AsyncTask task, int iErrType, String strTitle, String strMsg) {
		if(task instanceof BeseyeAccountTask.CheckAccountTask){
			onSessionInvalid();
		}else if(task instanceof BeseyeAccountTask.LogoutHttpTask){
			SessionMgr.getInstance().cleanSession();
			onSessionInvalid();
		}
		if(DEBUG){
			onToastShow(task, strMsg);
			Log.e(TAG, "onErrorReport(), task:["+task.getClass().getSimpleName()+"], iErrType:"+iErrType+", strTitle:"+strTitle+", strMsg:"+strMsg);
		}
	}

	@Override
	public void onPostExecute(AsyncTask task, List<JSONObject> result, int iRetCode) {
		Log.e(TAG, "BeseyeBaseActivity::onPostExecute(), "+task.getClass().getSimpleName()+", iRetCode="+iRetCode);	
		if(!task.isCancelled()){
			if(task instanceof BeseyeAccountTask.CheckAccountTask){
				if(0 == iRetCode){
					invokeSessionComplete();
				}
			}else if(task instanceof BeseyeAccountTask.LogoutHttpTask){
				if(0 == iRetCode){
					//Log.i(TAG, "onPostExecute(), "+result.toString());
					onSessionInvalid();
				}
			}
		}
		
		if(null != mMapCurAsyncTasks){
			mMapCurAsyncTasks.remove(task);
		}
	}

	@Override
	public void onToastShow(AsyncTask task,final String strMsg) {
		runOnUiThread(new Runnable(){
			@Override
			public void run() {
					Toast.makeText(BeseyeBaseActivity.this, strMsg, Toast.LENGTH_LONG).show();
			}});
	}
	
	protected void invokeLogout(){
		monitorAsyncTask(new BeseyeAccountTask.LogoutHttpTask(this), true, SessionMgr.getInstance().getAuthToken());
	}

	@Override
	public void onSessionInvalid(AsyncTask task, int iInvalidReason) {
		onSessionInvalid();
	}
	
	protected void onSessionInvalid(){
		SessionMgr.getInstance().cleanSession();
		launchDelegateActivity(BeseyeEntryActivity.class.getName());
	}
	
	public void launchActivityByIntent(Intent intent){
		intent.putExtra(KEY_FROM_ACTIVITY, getClass().getSimpleName());
		startActivity(intent);
	}
    
    protected void launchActivityByClassName(String strClass){
    	launchActivityByClassName(strClass, new Bundle());
	}
    
    public void launchActivityByClassName(String strClass, Bundle bundle){
		Intent intent = new Intent();
		intent.setClassName(this, strClass);
		if(null != bundle)
			intent.putExtras(bundle);
		
		launchActivityByIntent(intent);
	}
    
    public void launchActivityForResultByClassName(String strClass, Bundle bundle, int iRequestCode){
		Intent intent = new Intent();
		intent.setClassName(this, strClass);
		intent.putExtra(KEY_FROM_ACTIVITY, getClass().getSimpleName());
		if(null != bundle)
			intent.putExtras(bundle);
		startActivityForResult(intent, iRequestCode);
	}
    
    protected void launchDelegateActivity(String strCls){
		launchDelegateActivity(strCls, null);
    }
	
	protected void launchDelegateActivity(String strCls, Bundle bundle){
    	Intent intent = new Intent();
		intent.putExtra("ClassName", strCls);
		intent.setClass(this, OpeningPage.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		if(null != bundle)
			intent.putExtras(bundle);
		startActivity(intent);
    }
	
	public void onAppEnterForeground(){}
	public void onAppEnterBackground(){}
	
	private boolean mbNeedToNotifyWhenServiceConnected = false;
	final public void notifyServiceAppForeground(){
		if(null != mNotifyService){
			try {
				mNotifyService.send(Message.obtain(null, BeseyeNotificationService.MSG_APP_TO_FOREGROUND));
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}else{
			mbNeedToNotifyWhenServiceConnected = true;
		}
	}
	
	final public void notifyServiceAppBackground(){
		if(null != mNotifyService){
			try {
				mNotifyService.send(Message.obtain(null, BeseyeNotificationService.MSG_APP_TO_BACKGROUND));
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}
	
	//TO notify the latest session data to service
	public void onSessionUpdate(SessionData data){
		if(null != mNotifyService){
			try {
				Message msg = Message.obtain(null, BeseyeNotificationService.MSG_UPDATE_SESSION_DATA);
				if(null != msg){
					msg.getData().putParcelable("SessionData", data);
					mNotifyService.send(msg);
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}
	
	private Messenger mMessenger = new Messenger(new NotificationHandler(this));
	protected Messenger mNotifyService = null;
	protected Messenger mUploadWorksService = null;
	 /** Flag indicating whether we have called bind on the service. */
	protected  boolean mIsBound;
	
	/**
     * Handler of incoming messages from service.
     */
    static class NotificationHandler extends Handler {
    	private final WeakReference<BeseyeBaseActivity> mActivity; 
    	
    	NotificationHandler(BeseyeBaseActivity act){
    		mActivity = new WeakReference<BeseyeBaseActivity>(act);
    	}
    	
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
//                case BeseyeNotificationService.MSG_SET_NOTIFY_NUM:{
//                	BeseyeBaseActivity act = mActivity.get();
//                	if(null != act)
//                		act.onUnReadNotificationCallback(msg.arg1);
//                    break;
//                }
//                case BeseyeNotificationService.MSG_SET_UNREAD_MSG_NUM:{
//                	BeseyeBaseActivity act = mActivity.get();
//                	if(null != act)
//                		act.onUnReadMsgCallback(msg.arg1);
//                    break;
//                }
                default:
                    super.handleMessage(msg);
            }
        }
    }
	
	/**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
        	mNotifyService = new Messenger(service);
          

            // We want to monitor the service for as long as we are
            // connected to it.
            try {
                Message msg = Message.obtain(null,
                		BeseyeNotificationService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mNotifyService.send(msg);
//                mNotifyService.send(Message.obtain(null, BeseyeNotificationService.MSG_QUERY_NOTIFY_NUM));
                if(mbNeedToNotifyWhenServiceConnected){
                	mbNeedToNotifyWhenServiceConnected = false;
                	mNotifyService.send(Message.obtain(null, BeseyeNotificationService.MSG_APP_TO_FOREGROUND));
                }
//                //To stop the pulling in case of app crash
//                mNotifyService.send(Message.obtain(null, BeseyeNotificationService.MSG_STOP_TO_PULL_MSG));
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            }
            
            // As part of the sample, tell the user what happened.
//            Toast.makeText(BeseyeBaseActivity.this, "onServiceConnected",
//                    Toast.LENGTH_SHORT).show();
            notifyServiceConnected();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
        	mNotifyService = null;

            // As part of the sample, tell the user what happened.
//            Toast.makeText(BeseyeBaseActivity.this, "onServiceDisconnected",
//                    Toast.LENGTH_SHORT).show();
        	notifyServiceDisconnected();
        }
    };
    
//    private ServiceConnection mUploadConnection = new ServiceConnection() {
//        public void onServiceConnected(ComponentName className,
//                IBinder service) {
//            // This is called when the connection with the service has been
//            // established, giving us the service object we can use to
//            // interact with the service.  We are communicating with our
//            // service through an IDL interface, so get a client-side
//            // representation of that from the raw service object.
//        	mUploadWorksService = new Messenger(service);
//          
//        }
//
//        public void onServiceDisconnected(ComponentName className) {
//            // This is called when the connection with the service has been
//            // unexpectedly disconnected -- that is, its process crashed.
//        	mUploadWorksService = null;
//            // As part of the sample, tell the user what happened.
//        }
//    };
    
    protected void notifyServiceConnected(){}
    protected void notifyServiceDisconnected(){}
    
    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        bindService(new Intent(BeseyeBaseActivity.this, 
        		BeseyeNotificationService.class), mConnection, Context.BIND_AUTO_CREATE);
//        bindService(new Intent(BeseyeBaseActivity.this, 
//        		iKalaUploadWorksService.class), mUploadConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }
    
    void doUnbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.
            if (mNotifyService != null) {
                try {
                    Message msg = Message.obtain(null,
                    		BeseyeNotificationService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mNotifyService.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                }
            }
            
            // Detach our existing connection.
            unbindService(mConnection);
            //unbindService(mUploadConnection);
            mIsBound = false;
        }
    }

	protected abstract int getLayoutId();
}
