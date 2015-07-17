package com.app.beseye.util;

import static com.app.beseye.util.BeseyeConfig.TAG;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.json.JSONObject;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.app.beseye.R;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.share.model.ShareContent;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.model.ShareVideo;
import com.facebook.share.model.ShareVideoContent;
import com.facebook.share.widget.MessageDialog;
import com.facebook.share.widget.ShareDialog;

/*
 * How to share:
 * 		public int BeseyeShare(Activity activity, int TYPE, String content);
 * 
 * Parameters:
 * 		activity	just a activity
 * 		TYPE		(int) LINK, IMAGE, VIDEO
 * 		content		(String) link, image file path, video file path
 * 
 * Return:
 * 		error code (0 is no error)	
 * 
 * Sample:
 * 		ShareMgr.BeseyeShare(this, ShareMgr.TYPE.IMAGE, "/storage/sdcard1/DCIM/100ANDRO/DSC_3431.JPG");
 * 		ShareMgr.BeseyeShare(this, ShareMgr.TYPE.LINK, "http://beseye.com");
 *		ShareMgr.BeseyeShare(this, ShareMgr.TYPE.VIDEO, "/storage/sdcard1/DCIM/100ANDRO/MOV_3455.mp4");
 * 
 * NOTE: 	
 * 		when you call ShareMgr.BeseyeShare, please add 
 * 		1. "ShareMgr.setShareOnActivityResult(requestCode, resultCode, intent);"
 * 			in onActivityResult.
 * 		2.  Because FB share photo will take some time to start a FB activity, add
 * 			onShowDialog(null, BeseyeBaseActivity.DIALOG_ID_LOADING, 0, 0);
 * 			in onPause.
 * 			onDismissDialog(null, BeseyeBaseActivity.DIALOG_ID_LOADING);
 * 			in onResume		  
 * 
 */

public class ShareMgr {
	public static final int ERR_TYPE_NO_ERR 				= 0;
	public static final int ERR_TYPE_INVALID_TYPE			= 1;
	public static final int ERR_TYPE_INVALID_LINK			= 2;
	public static final int ERR_TYPE_INVALID_FILE			= 3;
	public static final int ERR_TYPE_NULL					= 4;
	public static final int ERR_TYPE_INVALID_INTENT			= 5;
	public static final int ERR_TYPE_INVALID_PACKAGE		= 6;
	public static final int ERR_TYPE_INVALID_FB_CONTENT		= 7;
	public static final int ERR_TYPE_NO_INTENT_FOUND		= 8;
	public static final int ERR_TYPE_FBLOGININIT_ERR		= 9;
	
	public enum TYPE {
	    LINK, IMAGE, VIDEO
	};
	
	static Boolean sbIsFBLogin = false; 
	private static String PHOTO_SHARE_HASH; 
	private static CallbackManager sCallbackManager = CallbackManager.Factory.create(); 
	private static LoginManager sLoginManager;
	
	public static int BeseyeShare(final Activity activity, final TYPE type, final String content){
		int iErrType = ERR_TYPE_NO_ERR; 
		
		FacebookSdk.sdkInitialize(activity.getApplicationContext());
//		FacebookSdk.setIsDebugEnabled(true);
		
		if(null != AccessToken.getCurrentAccessToken()){
    		sbIsFBLogin = true;
    	}	
		iErrType = fbLoginInit(activity, type, content);
		
		PHOTO_SHARE_HASH = activity.getResources().getString(R.string.share_hash);
		
		//Validate type and content
		if(ERR_TYPE_NO_ERR == iErrType) {	
			iErrType = isValidInput(activity, type, content);
		}
		if(ERR_TYPE_NO_ERR == iErrType) {		
			//get intent activities and link to adapter
			final Intent shareIntent = getShareIntent(type, content);
			final PackageManager packageManager = activity.getPackageManager();
			
			if(null == shareIntent){
				iErrType = ERR_TYPE_INVALID_INTENT;
			} else if(null == packageManager) {
				iErrType = ERR_TYPE_INVALID_PACKAGE;
			} else {
				List<ResolveInfo> listActivities = packageManager.queryIntentActivities(shareIntent, PackageManager.MATCH_DEFAULT_ONLY);
				if(listActivities.isEmpty()){
					iErrType = ERR_TYPE_NO_INTENT_FOUND;
				} else {
					//set adapter and create Dialog
					final ShareAdapter adapter = new ShareAdapter(activity, listActivities.toArray());
					iErrType = buildAlertDialog(adapter, activity, type, content, shareIntent);
				}
			}
		}
		return iErrType;
	}
	
	
	private static int fbLoginInit(final Activity activity, final TYPE type, final String content) {	
		int iErrType = ERR_TYPE_NO_ERR;
		sLoginManager = LoginManager.getInstance();
		
		if(null != sLoginManager){
		    sLoginManager.registerCallback(sCallbackManager,
		            new FacebookCallback<LoginResult>() {
		                @Override
		                public void onSuccess(LoginResult loginResult) {	      
		                	if (loginResult.getAccessToken() != null) {
		                		//if user deny permission, ask again
		                        Set<String> deniedPermissions = loginResult.getRecentlyDeniedPermissions();
		                        if (deniedPermissions.contains("user_friends")) {
		                            LoginManager.getInstance().logInWithReadPermissions(activity, Arrays.asList("user_friends"));
		                        } else {         
				                	GraphRequest request = GraphRequest.newMeRequest(
				                            loginResult.getAccessToken(),
				                            new GraphRequest.GraphJSONObjectCallback() {
				                                @Override
				                                public void onCompleted(JSONObject object, GraphResponse response) {
				                                	
				                                }
				                            });
				                    request.executeAsync();
				                    //share
				                    fbShareAction(activity, type, content);
				                }
		                	}
		                }
		                @Override
		                public void onCancel() {
		                	Log.e(TAG, "FBLoginCancel");
		                }
	
		                @Override
		                public void onError(FacebookException exception) {
		                    Log.e(TAG, "FBLoginError");
		                }
		            });
		} else {
			iErrType = ERR_TYPE_FBLOGININIT_ERR;
		}
		return iErrType;
	}
	
	private static void fbShareAction(final Activity activity, final TYPE type, final String content) {
		@SuppressWarnings("rawtypes")
		ShareContent shareContent = getShareContent(type, content);
		if(null == shareContent) {
			Log.e(TAG, "invalid fb content");
		} else {
    		ShareDialog shareDialog = new ShareDialog(activity);
			shareDialog.show(shareContent);
		}
	}
	
	public static void setShareOnActivityResult(int requestCode, int resultCode, Intent data) {                                                                                                                                                            
		sCallbackManager.onActivityResult(requestCode, resultCode, data);
	}
	
	private static int buildAlertDialog(final ShareAdapter adapter, final Activity activity, final TYPE type, final String content, final Intent shareIntent){
		int iErrType = ERR_TYPE_NO_ERR;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                ResolveInfo info = (ResolveInfo) adapter.getItem(which);
                if(null == info) {
                	Log.e(TAG, "no info be resolved");
                } else {               
	                if(info.activityInfo.packageName.contains("facebook.katana")) {
	                	//if user have already login, share directly
	                	if(true == sbIsFBLogin) {
	                		fbShareAction(activity, type, content);
	                	} else{
	                		Collection<String> permissions = (Collection<String>) Arrays.asList("public_profile", "user_friends");
	                		if(null != sLoginManager){
	                			sLoginManager.logInWithReadPermissions(activity, permissions);
	                		}
	                		/* TODO: where can I save data?
	                		for (Object o : permissions)
	                			Log.v(TAG, "Permission "+o);
	                		*/
	                	}
	                } else if(info.activityInfo.packageName.contains("facebook.orca")) { 
						@SuppressWarnings("rawtypes")
						ShareContent shareContent = getShareContent(type, content);
						MessageDialog messageDialog = new MessageDialog(activity);
						messageDialog.show(shareContent);
	                } else { 
	                	//WeChat is special case, only image no text
	                	if(!info.activityInfo.packageName.contains("tencent")) {
	                		shareIntent.putExtra(Intent.EXTRA_TEXT, PHOTO_SHARE_HASH);
	                	}
	                    shareIntent.setClassName(info.activityInfo.packageName, info.activityInfo.name);
	                    activity.startActivity(shareIntent);
	                }
                }
            }
        }).setCancelable(true); 
		
        TextView title = new TextView(activity);
        title.setText(activity.getResources().getString(R.string.share_way));
        title.setPadding(activity.getResources().getDimensionPixelSize(R.dimen.alertdialog_padding_left),
        		activity.getResources().getDimensionPixelSize(R.dimen.alertdialog_padding_top),
        		activity.getResources().getDimensionPixelSize(R.dimen.alertdialog_padding_top),
        		activity.getResources().getDimensionPixelSize(R.dimen.alertdialog_padding_top));
        
        title.setTextColor(activity.getResources().getColor(R.color.wifi_info_dialog_title_font_color));
        float scaledDensity = activity.getResources().getDisplayMetrics().scaledDensity;
        title.setTextSize(activity.getResources().getDimension(R.dimen.wifi_ap_info_dialog_title_font_size)/scaledDensity);        
        builder.setCustomTitle(title);
        
        //It is a hack!
        //http://stackoverflow.com/questions/14439538/how-can-i-change-the-color-of-alertdialog-title-and-the-color-of-the-line-under
        Dialog d = builder.show();
        int dividerId = d.getContext().getResources().getIdentifier("android:id/titleDivider", null, null);
        View divider = d.findViewById(dividerId);
        divider.setBackgroundColor(activity.getResources().getColor(R.color.wifi_info_dialog_title_font_color));
        
        d.getWindow().setFlags(
        	    WindowManager.LayoutParams.FLAG_FULLSCREEN, 
        	    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
		return iErrType;
	}
	
	private static Intent getShareIntent(TYPE type, String content){
		Intent intent = new Intent(android.content.Intent.ACTION_SEND);
		
		switch(type){
			case LINK:
				intent.setType("text/plain");
		        intent.putExtra(Intent.EXTRA_TEXT, PHOTO_SHARE_HASH + "  " + content);
				break;
			case IMAGE:
				Uri imageUri = null;
				try {
					imageUri = Uri.fromFile(new File(content));
				} catch (Exception e) {
					Log.e(TAG, "image file error");
				}
		        intent.setType("image/jpeg");
		        intent.putExtra(Intent.EXTRA_STREAM, imageUri);
		        //intent.putExtra(Intent.EXTRA_TEXT, PHOTO_SHARE_HASH);
				break;
			case VIDEO:				
				//NOTE: not available for youtube now (need more setting)
				Uri videoUri = null;
				try {
					Uri.fromFile(new File(content));
				} catch (Exception e) {
					Log.e(TAG, "video file error");
				}
				intent.setType("video/mp4");
		        intent.putExtra(Intent.EXTRA_STREAM, videoUri);
		        intent.putExtra(Intent.EXTRA_TEXT, PHOTO_SHARE_HASH);
				break;
			default:
				Log.e(TAG, "invalid type");
		}	
        return intent;
	}
	
	@SuppressWarnings("rawtypes")
	private static ShareContent getShareContent(TYPE type, String content){
		ShareContent shareContent = null;
		switch(type){
			case LINK:
				shareContent = getLinkContent(content); 
				break;
			case IMAGE:
				shareContent = getImageContent(content);
				break;
			case VIDEO:
				shareContent = getVideoContent(content);
				break;
			default:
				Log.e(TAG, "invalid type");
		}	
    	return shareContent;
	}
	
	private static ShareLinkContent getLinkContent(String address){
		ShareLinkContent linkContent = null;
		//Facebook SDK
		linkContent  = new ShareLinkContent.Builder()
        	.setContentUrl(Uri.parse(address))
        	.build();
		return linkContent;
	}
	
	private static SharePhotoContent getImageContent(String filename){
		SharePhotoContent imageContent = null;
		
		Bitmap bMap = BitmapFactory.decodeFile(filename);
		if(null == bMap){
			Log.e(TAG, "image file cannot be decoded");
		} else {
		
			ExifInterface exif = null;
			try {
				exif = new ExifInterface(filename);
			} catch (IOException e) {
				Log.e(TAG, "cannot find Exif Info of image file");
			}
			int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
			
			Matrix matrix = new Matrix();
			
			switch (orientation) {
				case ExifInterface.ORIENTATION_NORMAL:
					matrix.postRotate(0);
					break;
				case ExifInterface.ORIENTATION_ROTATE_90:
					matrix.postRotate(90);
					break;
				case ExifInterface.ORIENTATION_ROTATE_180:
					matrix.postRotate(180);
					break;
				case ExifInterface.ORIENTATION_ROTATE_270:
					matrix.postRotate(270);
					break;
				default:
					matrix.postRotate(0);
					Log.v(TAG, "strange image orientation");
			}
			
			Bitmap rotatedBitmap = null;
			try {
				rotatedBitmap = Bitmap.createBitmap(bMap , 0, 0, bMap.getWidth(), bMap.getHeight(), matrix, true);
			} catch (Exception e){
				Log.e(TAG, "rotateBitmap error");
			}
			
			//Facebook SDK
	    	SharePhoto photo = new SharePhoto.Builder().setBitmap(rotatedBitmap).build();
	    	if(null == photo){
				Log.e(TAG, "SharePhoto photo error");
			} else{
				imageContent = new SharePhotoContent.Builder()
					.addPhoto(photo)
					.build();
				if(null == imageContent){
					Log.e(TAG, "SharePhoto imageContent error");
				}
			}
		}
		return imageContent;
	}
	
	private static ShareVideoContent getVideoContent(String filename){
		ShareVideoContent videoContent = null;
		
		Uri videoFileUri = null;
		try {
			videoFileUri = Uri.fromFile(new File(filename));
		} catch(Exception e){
			Log.e(TAG, "video file error");
		}
		
		//Facebook SDK
		ShareVideo video = new ShareVideo.Builder()
		        .setLocalUrl(videoFileUri)
		        .build();
		if(null == video){
			Log.e(TAG, "ShareVideoContent video error");
		} else{
			videoContent = new ShareVideoContent.Builder()
			        .setVideo(video)
			        .build();
			if(null == videoContent){
				Log.e(TAG, "ShareVideoContent videoContent error");
			}
		}
		return videoContent;
	}
	
	private static int isValidInput(Activity activity, TYPE type, String content){
		int iErrType = ERR_TYPE_NO_ERR;
		
		if(null == activity || null == content){
			iErrType = ERR_TYPE_NULL;
		} else {
			switch(type){
				case LINK:
					try{
						new URL(content);
					} catch(Exception e){	
						iErrType = ERR_TYPE_INVALID_LINK;
					}
					break;
				case IMAGE:
				case VIDEO:
					if(true != new File(content).exists()){
						iErrType = ERR_TYPE_INVALID_FILE;	
					} 
					break;
				default:
					iErrType = ERR_TYPE_INVALID_TYPE;
			}
		}
		return iErrType;
	}
}
