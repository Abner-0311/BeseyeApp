package com.app.beseye.util;

import static com.app.beseye.util.BeseyeConfig.TAG;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
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

import com.facebook.CallbackManager;
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
 * 
 * NOTE:
 * 		when you call ShareMgr.BeseyeShare, please add 
 * 		"ShareMgr.setShareOnActivityResult(requestCode, resultCode, intent);"
 * 		in onActivityResult.
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
	
	public enum TYPE {
	    LINK, IMAGE, VIDEO
	}
	 
	private static final String PHOTO_SHARE_HASH = " #Beseye";
	private static CallbackManager callbackManager = CallbackManager.Factory.create();

	
	public static int BeseyeShare(final Activity activity, final TYPE type, final String content){
		
		//Validate type and content
		int miErrType = isValidInput(activity, type, content);
		if(ERR_TYPE_NO_ERR == miErrType) {		
			//get intent activities and link to adapter
			final Intent shareIntent = getShareIntent(type, content);
			final PackageManager packageManager = activity.getPackageManager();
			
			if(null == shareIntent){
				miErrType = ERR_TYPE_INVALID_INTENT;
			} else if(null == packageManager) {
				miErrType = ERR_TYPE_INVALID_PACKAGE;
			} else {
				List<ResolveInfo> listActivities = packageManager.queryIntentActivities(shareIntent, PackageManager.MATCH_DEFAULT_ONLY);
				if(listActivities.isEmpty()){
					miErrType = ERR_TYPE_NO_INTENT_FOUND;
				} else {
					//set adapter and create Dialog
					final ShareAdapter adapter = new ShareAdapter(activity, listActivities.toArray());
					miErrType = buildAlertDialog(adapter, activity, type, content, shareIntent);
				}
			}
		}
		return miErrType;
	}
	
	public static void setShareOnActivityResult(int requestCode, int resultCode, Intent data) {
	    callbackManager.onActivityResult(requestCode, resultCode, data);
	}
	
	private static int buildAlertDialog(final ShareAdapter adapter, final Activity activity, final TYPE type, final String content, final Intent shareIntent){
		int miErrType = ERR_TYPE_NO_ERR;
		
		new AlertDialog.Builder(activity)
        .setAdapter(adapter, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                ResolveInfo info = (ResolveInfo) adapter.getItem(which);
                if(null == info) {
                	Log.e(TAG, "no info be resolved");
                } else {               
	                if(info.activityInfo.packageName.contains("facebook")) { 
	            		@SuppressWarnings("rawtypes")
						ShareContent shareContent = getShareContent(type, content);
	            		if(null == shareContent) {
	            			Log.e(TAG, "invalid fb content");
	            		} else {
		                	if(info.activityInfo.packageName.contains("katana")) {
		                		ShareDialog shareDialog = new ShareDialog(activity);	
		            			shareDialog.show(shareContent);
		                	} else if(info.activityInfo.packageName.contains("orca")){
		                		MessageDialog messageDialog = new MessageDialog(activity);
		                		messageDialog.show(shareContent);
		                	}
	            		}
	                } else {
	                    shareIntent.setClassName(info.activityInfo.packageName, info.activityInfo.name);
	                    activity.startActivity(shareIntent);
	                }
                }
            }
        }).setNegativeButton("Cancel", null)
        .setTitle("Choose one")
        .setCancelable(true) 
        .show(); 
		
		return miErrType;
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
		        intent.putExtra(Intent.EXTRA_TEXT, PHOTO_SHARE_HASH);
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
		int miErrType = ERR_TYPE_NO_ERR;
		
		if(null == activity || null == content){
			miErrType = ERR_TYPE_NULL;
		} else {
			switch(type){
				case LINK:
					try{
						new URL(content);
					} catch(Exception e){	
						miErrType = ERR_TYPE_INVALID_LINK;
					}
					break;
				case IMAGE:
				case VIDEO:
					if(true != new File(content).exists()){
						miErrType = ERR_TYPE_INVALID_FILE;	
					} 
					break;
				default:
					miErrType = ERR_TYPE_INVALID_TYPE;
			}
		}
		return miErrType;
	}
}
