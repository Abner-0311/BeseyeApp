package com.app.beseye.error;

public class BeseyeError {
	static public final int   E_BE_OK 						   			   = 0x00000000;
	
	static public final int   E_BE_ERR 						   			   = 0x000000FF;
	
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
	static public final int   E_BE_ACC_USER_IS_INACTIVATED_THUS_SIGN_IN_FORBIDDEN  = 0x00101019;
	
		
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
	
	
	//UPDATE BE Error
	static public final int	  E_BE_UPDATE_SW_NOT_EXIST				   	   		= 0x00C00301;
	static public final int	  E_BE_UPDATE_OTHER_EXCEPTION				   	   	= 0x00C00500;
	static public final int	  E_BE_UPDATE_PARAM_INVALID				   	   		= 0x00C00501;
	static public final int	  E_BE_UPDATE_PARAM_BLANK				   	   		= 0x00C00502;
	
	
	// Service temporarily unavailable connection error (0x0000_0F00~0x0000_0FFF)
	static public final int E_CONN_SOCKET_ERR                                  = 0x00000F00;
	static public final int E_CONN_SOCKET_ERR_DNS_RESOLVE_FAILED               = 0x00000F01;
	static public final int E_CONN_ERRNO_ECONNREFUSED                          = 0x00000F02;
	static public final int E_CONN_ERRNO_ETIMEDOUT                             = 0x00000F03;
	static public final int E_CONN_REST_CLIENT_TIMEOUT                         = 0x00000F04;
	static public final int E_CONN_HTTP_404_RESOURCE_NOT_FOUND                 = 0x00000F05;
	static public final int E_CONN_HTTP_408_REQUEST_TIMEOUT                    = 0x00000F06;
	static public final int E_CONN_HTTP_429_TOO_MANY_REQUESTS                  = 0x00000F07;
	static public final int E_CONN_HTTP_500_INTERNAL_SERVER_ERROR              = 0x00000F08;
	static public final int E_CONN_HTTP_502_BAD_GATEWAY                        = 0x00000F09;
	static public final int E_CONN_HTTP_503_SERVICE_UNAVAILABLE                = 0x00000F0a;
	static public final int E_CONN_HTTP_504_GATEWAY_TIMEOUT                    = 0x00000F0b;
	static public final int E_CONN_SOCKET_ERR_END                              = 0x00000FFF;

	//Connection error (0x0000_0E00~0x0000_0EFF)
	static public final int E_CONN_EXCEPTION                                   = 0x00000E00;
	static public final int E_CONN_HTTP_1XX                                    = 0x00000E01;
	static public final int E_CONN_HTTP_INVALID_STATUS                         = 0x00000E02;
	static public final int E_CONN_HTTP_3XX                                    = 0x00000E03;
	static public final int E_CONN_HTTP_4XX                                    = 0x00000E04;
	static public final int E_CONN_HTTP_5XX                                    = 0x00000E05;
	static public final int E_CONN_REST_CLIENT_EXCEPTION                       = 0x00000E06;
	static public final int E_CONN_REST_CLIENT_SERVER_BROKE_CONNECTION         = 0x00000E07;
	static public final int E_CONN_HTTP_301_MOVED_PERMANENTLY                  = 0x00000E08;
	static public final int E_CONN_HTTP_304_NOT_MODIFIED                       = 0x00000E09;
	static public final int E_CONN_HTTP_403_FORBIDDEN                          = 0x00000E0a;
	static public final int E_CONN_EXCEPTION_END                               = 0x00000EFF;
	
	//OTA error 
	static public final int E_OTA_PREPARE_FAILED                               = 0x00D00011;//Found error before 20% in finalStatus
	static public final int E_REBOOT_DURING_PREPARING_STAGE                    = 0x00D00015;//Power off before 20% in finalStatus
	static public final int E_MISSING_V1_FILE                                  = 0x00D00024;//CAM SW mismatch
	static public final int E_V1_MISMATCH_HASH                                 = 0x00D00025;//..
	static public final int E_V1_MISMATCH_SIZE                                 = 0x00D00027;//..
	static public final int E_NOT_ENOUGH_SPACE                                 = 0x00D00037;//Out of space
	static public final int E_NOT_ENOUGH_SPACE_FOR_DL                          = 0x00D0004b;//..
	static public final int E_INVALID_SIGNATURE_LEN                            = 0x00D0002a;//No available OTA package
	static public final int E_SIGNATURE_VERIFICATION_FAILED                    = 0x00D0002b;//..
	
	static public final int E_CURL_NETWORK_ERROR                               = 0x00D000b0;//Trial down failed
	static public final int E_CURL_NETWORK_DEAD                                = 0x00D000b1;//..
	static public final int E_NETWORK_ERROR_WHEN_DOWNLOADING_FILE              = 0x00D00050;//..
	
	//WS/WSA errors 
	static public final int E_BE_WS_CAN_NOT_GET_SERVER_WS_ID 				   = 0x01100200;//Can't get server side id
	static public final int E_BE_WS_CAN_NOT_REGISTER_SERVER_WS_ID 			   = 0x01100202;//Can't register server side id
	
	static public boolean isNoError(final int iRetCode){
		return BeseyeError.E_BE_OK <= iRetCode && iRetCode < BeseyeError.E_BE_ERR ;
	}
	
	static public boolean isUserSessionInvalidError(final int iRetCode){
		return (E_BE_ACC_USER_SESSION_NOT_FOUND_BY_TOKEN == iRetCode) ||
			   (E_BE_ACC_USER_SESSION_EXPIRED == iRetCode) ;
	}
	
	static public boolean isWSServerUnavailableError(final int iRetCode){
		return (E_BE_WS_CAN_NOT_GET_SERVER_WS_ID == iRetCode) ||
			   (E_BE_WS_CAN_NOT_REGISTER_SERVER_WS_ID == iRetCode) ||
			   (E_CONN_SOCKET_ERR <= iRetCode && iRetCode <= E_CONN_SOCKET_ERR_END);
	}
}
