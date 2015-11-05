package com.app.beseye.error;

public class BeseyeError {
	static public final int   E_BE_ACC_GENERAL_ERR 						   = 0x00010000;	
	//user
	static public final int   E_BE_ACC_USER_CLIENT_IP_BLANK                = 0x00101000;//freeze,
	static public final int   E_BE_ACC_USER_CLIENT_IP_FORMAT_INVALID       = 0x00101001;//freeze,
	static public final int   E_BE_ACC_USER_LOCATION_BLANK                 = 0x00101002;//freeze,
	static public final int   E_BE_ACC_USER_UID_NOT_UNIQUE                 = 0x00101003;//freeze,
	static public final int   E_BE_ACC_USER_EMAIL_NOT_UNIQUE               = 0x00101004;//Email is not unquie!" }.freeze,
	static public final int   E_BE_ACC_USER_EMAIL_FORMAT_INVALID           = 0x00101005;//Email format is invalid!" }.freeze,
	static public final int   E_BE_ACC_USER_EMAIL_BLANK                    = 0x00101006;//Email is empty or blank!" }.freeze,
	static public final int   E_BE_ACC_USER_NAME_BLANK                     = 0x00101007;//User name is empty or blank!" }.freeze,
	static public final int   E_BE_ACC_USER_PASSWORD_BLANK                 = 0x00101008;//Password is empty or blank!" }.freeze,
	static public final int   E_BE_ACC_USER_PASSWORD_INCORRET              = 0x00101009;//freeze,
	static public final int   E_BE_ACC_USER_NOT_FOUND_BY_UID               = 0x0010100a;//freeze,
	static public final int   E_BE_ACC_USER_NOT_FOUND_BY_ID                = 0x0010100b;//freeze,
	static public final int   E_BE_ACC_USER_NOT_FOUND_BY_EMAIL             = 0x0010100c;//freeze,
	static public final int   E_BE_ACC_USER_ALREADY_EXIST                  = 0x0010100d;//freeze,
	static public final int   E_BE_ACC_USER_BOTH_USER_UID_N_VCAM_UID_BLANK = 0x0010100e;//freeze,
	static public final int   E_BE_ACC_USER_NEW_PASSWORD_BLANK             = 0x0010100f;//New password is empty or blank!" }.freeze,
	static public final int   E_BE_ACC_USER_NEW_PASSWORD_STRENGTH_TOO_LOW  = 0x00101010;//New password's strength is too low!" }.freeze,
		
	static public final int   E_BE_ACC_VCAM_CAM_HW_UID_ALREADY_USED_BY_OTHER_VCAM = 0x00104001;
	static public final int   E_BE_ACC_PS_KEEP_POLLING 							  = 0x00104500;
	static public final int   E_BE_ACC_PS_ALL_DEV_HW_FAILED						  = 0x00104501;
	static public final int   E_BE_ACC_PS_SOME_DEV_HW_FAILED					  = 0x00104502;
	static public final int   E_BE_ACC_PS_VCAM_WAIT_FOR_ATTACH_CONFIRM			  = 0x00104505;
	
	//# session sub-module
	static public final int   E_BE_ACC_USER_SESSION_CLIENT_DEV_UDID_NOT_UNIQUE    = 0x00102000;
	static public final int   E_BE_ACC_USER_SESSION_CLIENT_DEV_UDID_BLANK    	  = 0x00102001;
	static public final int   E_BE_ACC_USER_SESSION_CLIENT_DEV_UDID_NOT_MATCH     = 0x00102002;
	static public final int   E_BE_ACC_USER_SESSION_CLIENT_USR_AGENT_BLANK        = 0x00102003;
	static public final int   E_BE_ACC_USER_SESSION_CLIENT_USR_AGENT_NOT_MATCH    = 0x00102004;   
	static public final int   E_BE_ACC_USER_SESSION_CLIENT_IS_NOT_TRUSTED         = 0x00102005;   
	static public final int   E_BE_ACC_USER_SESSION_CLIENT_IP_FORMAT_INVALID      = 0x00102006;
	static public final int   E_BE_ACC_USER_SESSION_CLIENT_LOCATION_BLANK         = 0x00102007;   
	static public final int   E_BE_ACC_USER_SESSION_NOT_FOUND_BY_TOKEN            = 0x00102008;   
	static public final int   E_BE_ACC_USER_SESSION_NOT_FOUND_BY_CLIENT_DEV_UDID  = 0x00102009;   
	static public final int   E_BE_ACC_USER_SESSION_TOKEN_NOT_UNIQUE  			  = 0x0010200a;
	static public final int   E_BE_ACC_USER_SESSION_TOKEN_BLANK  			  	  = 0x0010200b;
	static public final int   E_BE_ACC_USER_SESSION_EXPIRED  			  		  = 0x0010200c;
	
	static public final int   E_BE_ACC_TT_PINCODE_VERIFY_FAILED  			  	  = 0x00102302;
	static public final int   E_BE_ACC_TT_PINCODE_VERIFY_FAILED_TOO_MANY_TIMES    = 0x00102303;
	static public final int   E_BE_ACC_TT_PINCODE_NOT_FOUND    					  = 0x00102304;
	static public final int   E_BE_ACC_TT_PINCODE_IS_EXPIRED				      = 0x00102305;
	
	
	//# OTA related
	static public final int   E_OTA_SW_ALRADY_LATEST  					   = 0x00500202;
	static public final int   E_OTA_SW_UPDATING  					   	   = 0x00500205;
	static public final int   E_WEBSOCKET_OPERATION_FAIL                   = 0x00500400;
	static public final int   E_WEBSOCKET_CONN_NOT_EXIST                   = 0x00500401;
	static public final int   E_WEBSOCKET_AUDIO_CONN_OCCUPIED              = 0x00500600;
	static public final int   E_WEBSOCKET_AUDIO_CONN_FAILED                = 0x00500603;
	
	//# Cam related
	static public final int   E_CAM_INVALID_WIFI_PW_LENGTH			   	   = 0x00200013;
	static public final int   E_CAM_INVALID_WIFI_SEC_TYPE 			       = 0x00200014;
	static public final int   E_CAM_INVALID_WIFI_INFO 				       = 0x00200015;
	
	//# Push related
	static public final int	  E_BE_PUSH_ALREADY_REGISTER				   = 0x00500002;	
	static public final int	  E_BE_CAM_INFO_NOT_EXIST				   	   = 0x0050000f;	
	
	//Android Error
	static public final int	  E_FE_AND_SERVER_ERROR				   		   = 0x00400001;	//server_error
	static public final int	  E_FE_AND_INVALID_DVR_PATH				   	   = 0x00400002;	//streaming_invalid_dvr
	static public final int	  E_FE_AND_PLAYER_ERROR				   		   = 0x00400003;	//streaming_playing_error
	static public final int	  E_FE_AND_PLAYER_UNKNOWN_ERR				   = 0x00400004;	//streaming_error_unknown
	static public final int	  E_FE_AND_PLAYER_LOW_MEM				   	   = 0x00400005;	//streaming_error_low_mem
	static public final int	  E_FE_AND_OTA_TIMEOUT				   		   = 0x00400006;	//cam_update_timeout
	static public final int	  E_FE_AND_OTA_UPDATE_FAILED				   = 0x00400007;	//cam_update_failed
	static public final int	  E_FE_AND_HTTP_UNKNOWN_ERR				   	   = 0x00400008;
	static public final int	  E_FE_AND_HTTP_INVALID_DATA				   = 0x00400009;
	static public final int	  E_FE_AND_HTTP_NO_CONN				   		   = 0x0040000A;
	static public final int	  E_FE_AND_HTTP_CONN_TIMEOUT				   = 0x0040000B;
	
}
