package com.app.beseye.util;

import static com.app.beseye.util.BeseyeConfig.*;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.app.beseye.httptask.SessionMgr;

import android.text.Html;
import android.util.Log;

public class BeseyeJSONUtil {
	//Http & Session
	public static final String HOST 					= "Host"; 
	public static final String SESSION_MDID 			= "Mdid"; 
	
	//Generic
	public static final String RET_CODE 				= "ReturnCode"; 
	public static final String OBJ_CNT 					= "ObjCount";
	public static final String OBJ_LST 					= "Objs";
	public static final String OBJ_LK_KEY 				= "LinkKey";
	public static final String OBJ_NXT_PAGE 			= "NextPage";
	public static final String OBJ_UNREAD 				= "UnreadObjCount";
	public static final String OBJ_UNREAD_SOCIAL 		= "UnreadObjCountSocial";
	public static final String OBJ_UNREAD_CHORUS 		= "UnreadObjCountChorus";
	public static final String OBJ_UNREAD_MESSAGE 		= "UnreadObjCountMessage";
	public static final String OBJ_UNREAD_NOTIFY 		= "UnreadObjCountNotification";
	public static final String OBJ_RECORDS 				= "Records";
	public static final String OBJ_START_TIME 			= "StartTime";
	public static final String OBJ_END_TIME 			= "EndTime";
	
	public static final String HITS_NUM 				= "Hits";
	public static final String LIKES_NUM 				= "Likes";
	public static final String FANS_NUM 				= "Fans";
	public static final String WORKS_NUM 				= "Works";
	
	public static final String FROM 					= "From";
	public static final String USER_ID 					= "UserID";
	public static final String USER_NAME 				= "UserName";
	public static final String USER_ICON 				= "UserIcon";
	
	public static final String CREATE_TIME 				= "CreateTime";
	public static final String UPDATE_TIME 				= "UpdateTime";
	public static final String INDEX 					= "Index";
	public static final String UNREAD 					= "Unread";
	
	public final static String REF_LABEL 				= "REF_LABEL";
	public final static String REF_INTENT 				= "REF_INTENT";
	
	//For Signup
	public static final String AGREEMENT 				= "Agreement"; 
	
	//For User Info
	public static final String USER_MDID 				= "mdid"; 
	public static final String USER_ACCOUNT 			= "Account"; 
	public static final String USER_VIP_LVL 			= "VipState"; 
	public static final String USER_IS_VIP  			= "IsVip"; 
	public static final String USER_EXPIRE  			= "ExpireDate"; 
	public static final String USER_CHANNEL 			= "Channel";
	public static final String USER_OPENACCOUNT  		= "OpenAccount";
	public static final String USER_OPEN_ID 		    = "OpenID";
	public static final String USER_AGREE  	 			= "ReadAgreement";
	public static final String USER_S_TOT   			= "SpaceTotal";
	public static final String USER_S_REC   			= "SpaceRecord";
	public static final String USER_S_ALBM  			= "SpaceAlbum";
	public static final String USER_REGISTER_DATE  		= "CreateDate";
	
	//For Payment
	public static final String PAY_RECORDS 				= "Records"; 
	public static final String PAY_DATA 				= "Data"; 
	public static final String PAY_SN 					= "SerialNumber"; 
	public static final String PAY_TYPE 				= "Type"; 
	public static final String PAY_VIP 					= "Vip"; 
	public static final String PAY_AVAILABLE 			= "Available"; 
	public static final String PAY_IS_UPGRADE 			= "Upgrade"; 
	public static final String PAY_KCOIN 				= "Kcoin"; 
	public static final String PAY_PLAN 				= "Plan"; 
	public static final String PAY_ORDER_DATE 			= "OrderDate"; 
	public static final String PAY_EXPIRE_DATE 			= "ExpireDate"; 
	public static final String PAY_AUTO_MONTH 			= "AutoMonth"; 
	public static final String PAY_PRICE 				= "Price"; 
	public static final String PAY_PRICE_SYS 			= "PriceSystem"; 
	public static final String PAY_BONUS 				= "Bonus"; 
	public static final String PAY_BONUS_TYPE 			= "Type"; 
	public static final String PAY_BONUS_NUM 			= "Number"; 
	public static final String PAY_CONFIRM 				= "Confirm"; 
	public static final String PAY_STATE 				= "State"; 
	public static final String PAY_VIP_CATEGORY 		= "VipCategory"; 
	public static final String PAY_RESULT_SERIAL		= "Serial"; 
	
	//For TelePay
	public static final String TELEPAY_RET 				= "Result"; 
	public static final String TELEPAY_RET_CONTENT 		= "Ret_content"; 

	//For Video
	public final static String VIDEO_RANK 				= "Rank";
	
	//For Channel
	public static final String CHANNEL_NAME 			= "ChannelName";
	public static final String CHANNEL_INFO 			= "ChannelInfo";
	public static final String CHANNEL_MEMO 			= "ChannelMemo";
	public static final String CHANNEL_PHOTO			= "ChannelPhoto";
	public static final String CHANNEL_SOCIAL			= "SocialStatus";
	public static final String CHANNEL_SEEN				= "CountSeen";
	public static final String CHANNEL_RECOM			= "CountRecommend";
	public static final String CHANNEL_RECOMABLE		= "Recommendable";
	
	public static final String CHANNEL_WORK_THUMB		= "WorkIconURL";
	public static final String CHANNEL_ALBUM_THUMB		= "PhotoAlbumIconURL";
	public static final String CHANNEL_SOCIAL_THUMB		= "SocialIconURL";
	
	//For AboutMe
	public static final String ABOUT_R_NAME 			= "Realname";
	public static final String ABOUT_N_NAME	 			= "NickName";
	public static final String ABOUT_PIC 				= "MyPicture";
	public static final String ABOUT_GENDER 			= "Gender";
	public static final String ABOUT_BDAY 				= "Birthday";
	public static final String ABOUT_AGE 				= "Age";
	
	public static final String ABOUT_PHONE 				= "Phone";
	public static final String ABOUT_PHONE_HOME 		= "Phone";
	public static final String ABOUT_PHONE_MOB 			= "MobilePhone";
	
	public static final String ABOUT_LOC 				= "Location";
	public static final String ABOUT_ADDR 				= "Address";
	
	public static final String ABOUT_STAR 				= "StarSign";
	public static final String ABOUT_EMAIL 				= "ContactEmail";
	
	public static final String ABOUT_FAV_MUSIC 			= "FavoriteMusicTypes";
	public static final String ABOUT_FAV_INSTR 			= "MusicalInstrument";
	public static final String ABOUT_FAV_ARTIST 		= "FavoriteArtists";
	public static final String ABOUT_FAV_SONG 			= "FavoriteSongs";
	
	public static final String ABOUT_EDU 				= "Education";
	public static final String ABOUT_EDU_INFO 			= "EducationInfo";
	public static final String ABOUT_EDU_STAGE 			= "Stage";
	public static final String ABOUT_EDU_SCHL 			= "SchoolName";
	
	public static final String ABOUT_COMPANY 			= "CompanyName";
	public static final String ABOUT_ME_INFO 			= "AboutMe";
	public static final String ABOUT_ME_INFO_DESC 		= "Description";
	
	public static final String ABOUT_KSONG 				= "MyKSongs";
	public static final String ABOUT_S_ALBUM 			= "MySongAlbums";
	public static final String ABOUT_P_ALBUM 			= "MyPhotoAlbums";
	public static final String ABOUT_FANS 				= "MyFans";
	public static final String ABOUT_FRIENDS 			= "MyFriends";
	public static final String ABOUT_IDOLS 				= "MyIdols";
	public static final String ABOUT_BROWSED 			= "Browsed";
	
	//For Albums
	public static final String ALBUM_ID 				= "AlbumID";
	public static final String ALBUM_NAME				= "AlbumName";
	public static final String ALBUM_COV 				= "CoverIcon";
	public static final String ALBUM_PIC_CNT 			= "TotalPhotos";
	
	//For Photo list
	public static final String PHOTO_ID 				= "PhotoID";
	public static final String PHOTO_ICON 				= "PhotoIcon";
	public static final String PHOTO_URL 				= "PhotoURL";
	public static final String PHOTO_INFO 				= "PhotoInfo";
	public static final String PHOTO_W 					= "w";
	public static final String PHOTO_H 					= "h";
	public static final String PHOTO_CAP 				= "Caption";
	
	//For Song
	public static final String SONG_ARY 				= "Songs";
	public static final String SONG_ID 					= "SongID";
	public static final String SONG_NAME 				= "SongName";
	public static final String SONG_ICON_URL 			= "SongIconURL";
	public static final String SONG_VOCAL 				= "VocalFlag";
	
	//For Artist
	public static final String ARTIST_ARY 				= "Artists";
	public static final String ARTIST_ID 				= "ArtistID";
	public static final String ARTIST_NAME 				= "ArtistName";
	public static final String ARTIST_ICON_URL 			= "ArtistIcon";
	
	//For Work
	public static final String WORK_ARY    				= "Works";
	public static final String WORK_ID 					= "WorkID";
	public static final String WORK_TITLE 				= "WorkTitle";
	public static final String WORK_ICON_URL			= "VideoIcon";
	public static final String WORK_TYPE 				= "WorkType";
	public static final String WORK_DATA 				= "WorkData";
	public static final String WORK_NAME    			= "WorkName";
	public static final String WORK_ICON 				= "WorkIcon";
	public static final String WORK_INFO 				= "WorkInfo";
	public static final String WORK_DURATION			= "Duration";
	public static final String WORK_STATUS 				= "WorkStatus";
	public static final String WORK_PUBLISH 			= "PublishTime";
	public static final String WORK_CMT 				= "Comment";
	public static final String WORK_CMT_CNT 			= "Comments";
	public static final String WORK_RESERVE 			= "WorkReserve";
	public static final String WORK_RESERVE_ATT_URL 	= "AttachedURL";
	public static final String WORK_RESERVE_PHOTO_URL 	= "PhotoAttachedURL";
	public static final String WORK_RESERVE_ATT_TITLE 	= "TitleAttachedURL";
	public static final String WORK_RESERVE_ATT_MEMO 	= "MemoAttachedURL";
	public static final String WORK_POSTEE_NAME 		= "PosteeName";
	public static final String WORK_POSTEE_ID 			= "PosteeID";
	public static final String COMMENT_ID 				= "CommentID";
	public static final String WORK_AUDIO_TYPE    		= "AUDIO";
	
	
	public static final String WORK_PERMISSION 			= "Permission";
	public static final String WORK_PERM_ALL 			= "All";
	public static final String WORK_PERM_ONLYSELF 		= "OnlySelf";
	public static final String WORK_PERM_FRIEND 		= "GroupFriends";
	
	
	public static final String HEAD_SHOT_OFFSET_X 		= "Offset_X";
	public static final String HEAD_SHOT_OFFSET_Y 		= "Offset_Y";
	public static final String HEAD_SHOT_OFFSET_X_E 	= "Offset_X_E";
	public static final String HEAD_SHOT_OFFSET_Y_E 	= "Offset_Y_E";
	
	//For Work type
	public static final String WORK_RECORD				= "record";
	public static final String WORK_RECORD2				= "Record";
	public static final String WORK_KSONG 				= "KSong";
	public static final String WORK_VIDEO 				= "IVideo";
	public static final String WORK_SONGALBUM 			= "SongAlbum";
	public static final String WORK_PHOTOALBUM 			= "PhotoAlbum";
	public static final String WORK_WALL 				= "Wall";
	public static final String WORK_PHOTO 				= "Photo";
	public static final String WORK_CONVERSATION		= "Conversation";
	
	public static final String WORK_SOCIAL 				= "Social";
	public static final String WORK_CHANNEL 			= "Channel";
	
	public static final String WORK_CATEGORY 			= "Category"; 
	
	public static final String COLLA_ARY 				= "Collaborators";
	
	//for Member
	public static final String MEMBER_ARY 				= "Members";
	public static final String RELATION_ARY 			= "Relations";
	public static final String RELATION_FAN 			= "Fan";
	public static final String RELATION_FRIEND 			= "Friend";
	public static final String RELATION_FRIEND_REQ_ED 	= "FriendRequested";
	public static final String RELATION_FRIEND_REQ_ING  = "FriendRequesting";
	public static final String RELATION_IDOL 			= "Idol";
	
	//For Search functions
	public static final String SEARCH_TOTAL_SONG 		= "TotalSongNum";
	
	
	//For KSong state
	public static final String KSONG_ID 				= "ID";
	public static final String KSONG_DATA 				= "StatusData";
	public static final String KSONG_TYPE 				= "StatusType";
	public static final String KSONG_INFO 				= "StatusInfo";
	public static final String KSONG_COMT 				= "Comment";
	public static final String KSONG_ICON 				= "KSongIcon";
	public static final String KSONG_COMT_NUM 			= "CommentNum";
	public static final String KSONG_REPLY_NUM 			= "ReplyNum";
	public static final String KSONG_REPLY_DATA 			= "ReplyData";
	
	//For KSong StatusType
	public static final String KSONG_T_CREATE 			= "Create";
	public static final String KSONG_T_LIKE 			= "Like";
	public static final String KSONG_T_SHARE 			= "Share";
	public static final String KSONG_T_SINGING 			= "Singing";
	public static final String KSONG_T_COMMENT 			= "Comment";
	public static final String KSONG_T_COMMENT_REPLY 	= "CommentReply";
	public static final String KSONG_T_TALK 			= "Talk";
	//public static final String KSONG_T_COLLECT 		= "Collect";
	public static final String KSONG_T_BEFAN 			= "BeFan";
	public static final String KSONG_T_BEFRIEND 		= "BeFriend";
	public static final String KSONG_T_BEIDOL 			= "BeIdol";
	public static final String KSONG_T_REGISTER 		= "Register";
	
	//For Notifications
	public static final String NOTIFY_ID   				= "NotifyID";
	public static final String NOTIFY_DATA 				= "NotifyData";
	public static final String NOTIFY_TYPE 				= "NotifyType";
	public static final String NOTIFY_INFO 				= "NotifyInfo";
	public static final String NOTIFY_URL  				= "NotifyURL";
	
	
	//For Notifications StatusType
	public static final String NOTIFY_T_CREATE 			= KSONG_T_CREATE;
	public static final String NOTIFY_T_DELETE 			= "Delete";
	public static final String NOTIFY_T_ANNOUNCE 		= "Announce";
	public static final String NOTIFY_T_SPONSOR 		= "Sponsor";
	public static final String NOTIFY_T_COMMENT 		= KSONG_T_COMMENT;
	public static final String NOTIFY_T_COMMENT_REPLY 	= KSONG_T_COMMENT_REPLY;
	public static final String NOTIFY_T_FRIEND_INVITE 	= "FriendInvite";
	public static final String NOTIFY_T_BEFAN 			= KSONG_T_BEFAN;
	public static final String NOTIFY_T_BEFRIEND 		= KSONG_T_BEFRIEND;
	public static final String NOTIFY_T_BEIDOL 			= KSONG_T_BEIDOL;
	public static final String NOTIFY_T_CHO_INVITE 		= "ChorusInvite";
	public static final String NOTIFY_T_CHO_COMMIT 		= "ChorusCommit";
	public static final String NOTIFY_T_CHO_PUBLISH 	= "ChorusToPublish";
	//public static final String NOTIFY_T_CHO_DELETE 	= "ChrousInviteDelete";
	public static final String NOTIFY_T_MSG 			= "Message";
	
	//For Message 
	public static final String MSG_CONV_ID 				= "ConvID";
	public static final String MSG_CONV_INFO 			= "ConversationInfo";
	public static final String MSG_CONV_INDEX 			= "ConvIndex";
	public static final String MSG_CONV_RECIPIENT 		= "Recipients";
	
	
	//For Social
	public static final String SOCIAL_REQ_TYPE 			= "RequestType";
	public static final String SOCIAL_CLIENT_TYPE 		= "ClientType";
	public static final String SOCIAL_CLIENT_TYPE_AND 	= "mobile_android";
	public static final String SOCIAL_FB_TITLE 			= "Title";
	public static final String SOCIAL_FB_URL 			= "WorkURL";
	public static final String SOCIAL_FB_CAPTION 		= "Caption";
	public static final String SOCIAL_FB_DESC 			= "Description";
	
	//For web addr
	public static final String WEB_ADDR_URL 			= "Url";
	
	//For Push service
	public static final String PS_PORJ_ID 				= "GCMProjectID";
	public static final String PS_REG_ID 				= "RegisterID";
	public static final String PS_REG_IDS 				= "RegIDs";
	public static final String PS_REG_ID_OLD 			= "OrigRegisterID";
	public static final String PS_REG_ID_NEW 			= "NewRegisterID";
	public static final String PS_NOTIFY_TYPE 			= "notify_type";
	public static final String PS_WORK_TYPE 			= "work_type";
	public static final String PS_NOTIFY_INFO 			= "Info";
	
	//Helper functions
	static public JSONObject newJSONObject(String json){
		JSONObject objRet;
		try{
			objRet = new JSONObject(json);
		} catch (JSONException e) {
			Log.w(TAG, "newJSONObject(), can't new JSONObject by "+json);
			objRet = null;
		}
		return objRet;
	}
	
	static public JSONObject getJSONObject(JSONObject obj, String strKey){
		JSONObject objRet = null;
		if(null != obj){
			try {
				objRet = obj.getJSONObject(strKey);
			} catch (JSONException e) {
				Log.w(TAG, "getJSONObject(), can't get value by "+strKey);
				objRet = null;
			}
		}
		return objRet;
	}
	
	static public JSONArray getJSONArray(JSONObject obj, String strKey){
		JSONArray objRet = null;
		if(null != obj){
			try {
				objRet = obj.getJSONArray(strKey);
			} catch (JSONException e) {
				Log.w(TAG, "getJSONArray(), can't get value by "+strKey);
				objRet = null;
			}
		}
		return objRet;
	}
	
	static public String getJSONString(JSONObject obj, String strKey){
		return getJSONString(obj, strKey, "");
	}
	
	static public String getJSONString(JSONObject obj, String strKey, String strDefault){
		String strRet = null;
		if(null != obj){
			try {
				strRet = obj.getString(strKey);
				if(null != strRet && 0 < strRet.length()){
					strRet = Html.fromHtml(strRet).toString();
					strRet = strRet.replaceAll("<br/>", "\n");
				}else{
					strRet = strDefault;
				}
				
				//workaround for backend
				if("null".equalsIgnoreCase(strRet)){
					strRet = strDefault;
				}
			} catch (JSONException e) {
				Log.w(TAG, "getJSONString(), can't get value by "+strKey);
				strRet = strDefault;
			}
		}
		return strRet;
	}
	
	static public int getJSONInt(JSONObject obj, String strKey){
		return getJSONInt(obj, strKey, 0);
	}
	
	static public int getJSONInt(JSONObject obj, String strKey, int iDefaultValue){
		int iRet = iDefaultValue;
		if(null != obj){
			try {
				iRet = obj.getInt(strKey);
			} catch (JSONException e) {
				Log.w(TAG, "getJSONInt(), can't get value by "+strKey);
			}
		}
		return iRet;
	}
	
	static public long getJSONLong(JSONObject obj, String strKey){
		long lRet = 0;
		if(null != obj){
			try {
				lRet = obj.getLong(strKey);
			} catch (JSONException e) {
				Log.w(TAG, "getJSONLong(), can't get value by "+strKey);
				lRet = -1;
			}
		}
		return lRet;
	}
	
	static public double getJSONDouble(JSONObject obj, String strKey){
		double dRet = 0.0d;
		if(null != obj){
			try {
				dRet = obj.getDouble(strKey);
			} catch (JSONException e) {
				Log.w(TAG, "getJSONDouble(), can't get value by "+strKey);
				dRet = Double.NaN;
			}
		}
		return dRet;
	}
	
	static public boolean getJSONBoolean(JSONObject obj, String strKey){
		return getJSONBoolean(obj, strKey, false);
	}
	
	static public boolean getJSONBoolean(JSONObject obj, String strKey, boolean bDefault){
		boolean bRet = bDefault;
		if(null != obj){
			try {
				bRet = obj.getBoolean(strKey);
			} catch (JSONException e) {
				Log.w(TAG, "getJSONBoolean(), can't get value by "+strKey);
			}
		}
		return bRet;
	}
	
	static public List<String> getListFromJSONArray(JSONArray arr){
		List<String> arrRet = new ArrayList<String>();
		if(null != arr){
			int iLen = arr.length();
			for(int iIndex = 0; iIndex < iLen; iIndex++){
				try {
					String value = arr.getString(iIndex);
					if(null != value && 0 < value.trim().length())
						arrRet.add(value);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return arrRet;
	}
	
	static public JSONObject appendMoreObj(JSONObject src, JSONObject dest){
		if(null != src){
			if(null != dest){
				try {
					dest.put(OBJ_NXT_PAGE, BeseyeJSONUtil.getJSONBoolean(src, OBJ_NXT_PAGE));
					dest.put(OBJ_CNT, BeseyeJSONUtil.getJSONInt(src, OBJ_CNT)+BeseyeJSONUtil.getJSONInt(dest, OBJ_CNT));
					JSONArray arrDest = BeseyeJSONUtil.getJSONArray(dest, OBJ_LST);
					JSONArray arrSrc = BeseyeJSONUtil.getJSONArray(src, OBJ_LST);
					if(null != arrSrc && null != arrDest){
						int iCount = arrSrc.length();
						for(int i = 0; i < iCount;i++){
							arrDest.put(arrSrc.get(i));
						}
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}else
				dest = src;
		}
		
		return dest;
	}
	
	static public String getLastLinkKey(JSONObject obj){
		String strRet = null;
		if(null != obj){
			JSONArray arr = BeseyeJSONUtil.getJSONArray(obj, OBJ_LST);
			if(null != arr && 0 < arr.length()){
				try {
					strRet = BeseyeJSONUtil.getJSONString(arr.getJSONObject(arr.length()-1), OBJ_LK_KEY);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
		return strRet;
	}
	
	static public boolean appendObjToArrayBegin(JSONObject dest, JSONObject obj){
		boolean bRet = false;
		if(null != dest && null != obj){
			JSONArray arr = BeseyeJSONUtil.getJSONArray(dest, OBJ_LST);
			if(null != arr){
				try {
					JSONArray copy = new JSONArray(arr.toString());
					if(null != copy){
						arr.put(0, obj);
						int iCount = copy.length();
						for(int i = 0 ;i< iCount;i++){
							arr.put(i+1, copy.getJSONObject(i));
						}
						dest.put(OBJ_CNT, BeseyeJSONUtil.getJSONInt(dest, OBJ_CNT)+1);
						bRet = true;
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
		return bRet;
	}
	
	static public boolean appendObjToArrayEnd(JSONObject dest, JSONObject obj){
		boolean bRet = false;
		if(null != dest && null != obj){
			JSONArray arr = BeseyeJSONUtil.getJSONArray(dest, OBJ_LST);
			if(null != arr){
				arr.put(obj);
				try {
					dest.put(OBJ_CNT, BeseyeJSONUtil.getJSONInt(dest, OBJ_CNT)+1);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				bRet = true;
			}
		}
		return bRet;
	}
	
	static public boolean removeObjFromArrayBegin(JSONObject dest){
		boolean bRet = false;
		if(null != dest ){
			JSONArray arr = BeseyeJSONUtil.getJSONArray(dest, OBJ_LST);
			if(null != arr){
				try {
					JSONArray copy = new JSONArray();
					if(null != copy){
						int iCount = arr.length();
						if(0 < iCount){
							for(int i = 1 ;i< iCount;i++){
								copy.put(i-1, arr.getJSONObject(i));
							}
							dest.put(OBJ_CNT, BeseyeJSONUtil.getJSONInt(dest, OBJ_CNT)-1);
							dest.put(OBJ_LST, copy);
						}
						
						bRet = true;
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
				bRet = true;
			}
		}
		return bRet;
	}
	
	static public boolean removeObjFromArrayEnd(JSONObject dest){
		boolean bRet = false;
		if(null != dest ){
			JSONArray arr = BeseyeJSONUtil.getJSONArray(dest, OBJ_LST);
			if(null != arr){
				try {
					JSONArray copy = new JSONArray();
					if(null != copy){
						int iCount = arr.length();
						if(0 < iCount){
							for(int i = 0 ;i< iCount-1;i++){
								copy.put(i, arr.getJSONObject(i));
							}
							dest.put(OBJ_CNT, BeseyeJSONUtil.getJSONInt(dest, OBJ_CNT)-1);
							dest.put(OBJ_LST, copy);
						}
						bRet = true;
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
				bRet = true;
			}
		}
		return bRet;
	}
	
	static public JSONObject getMsgReceiver(JSONArray members){
		JSONObject rec = null;
		if(null != members){
			String ownerId = SessionMgr.getInstance().getMdid();
			int iCount = members.length();
			for(int iIndex = 0; iIndex < iCount;iIndex++){
				JSONObject member = members.optJSONObject(iIndex);
				if(null != member && !ownerId.equals(BeseyeJSONUtil.getJSONString(member, USER_ID))){
					rec = member;
					break;
				}
			}
		}
		return rec;
	}
	
	static public String parseOwnerInfo(JSONObject obj){
		JSONObject ret = new JSONObject();
		if(null != obj){
			try {
				ret.put(USER_MDID, getJSONString(obj, USER_MDID));
				ret.put(USER_ACCOUNT, getJSONString(obj, USER_ACCOUNT));
				ret.put(USER_VIP_LVL, getJSONString(obj, USER_VIP_LVL));
				ret.put(USER_IS_VIP, getJSONString(obj, USER_IS_VIP));
				ret.put(USER_EXPIRE, getJSONString(obj, USER_EXPIRE));
				ret.put(USER_OPENACCOUNT, getJSONString(obj, USER_OPENACCOUNT));
				ret.put(USER_AGREE, getJSONString(obj, USER_AGREE));
				ret.put(USER_OPEN_ID, getJSONString(obj, USER_OPEN_ID));
				ret.put(USER_S_TOT, getJSONString(obj, USER_S_TOT));
				ret.put(USER_S_REC, getJSONString(obj, USER_S_REC));
				ret.put(USER_S_ALBM, getJSONString(obj, USER_S_ALBM));
				ret.put(PAY_AUTO_MONTH, getJSONString(obj, PAY_AUTO_MONTH));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return ret.toString();
	}
}
