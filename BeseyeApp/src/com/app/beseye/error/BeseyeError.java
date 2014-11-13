package com.app.beseye.error;

public class BeseyeError {
	static public final int E_BE_ACC_GENERAL_ERR 						   = 0x00010000;	
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
	
	
	static public final int E_BE_ACC_SESSION_NOT_FOUND                    = 0x00102008;   
	
	//# session sub-module
	static public final int E_BE_ACC_SESSION_CLIENT_DEV_UDID_INVALID     = 0x00012000;
	static public final int E_BE_ACC_SESSION_CLIENT_USR_AGENT_INVALID    = 0x00012001;
	static public final int E_BE_ACC_SESSION_CLIENT_LOCATION_INVALID     = 0x00012002;
	static public final int E_BE_ACC_SESSION_CLIENT_IP_INVALID           = 0x00012003;
	static public final int E_BE_ACC_SESSION_CREATE_FAILED               = 0x00012004;   
	static public final int E_BE_ACC_SESSION_DESTROY_FAILED              = 0x00012005;   
	static public final int E_BE_ACC_SESSION_SAVE_FAILED                 = 0x00012006;
	static public final int E_BE_ACC_SESSION_NOT_EXIST                   = 0x00012007;   
	static public final int E_BE_ACC_SESSION_EXPIRED                     = 0x00012008;   
	static public final int E_BE_ACC_SESSION_CLIENT_DEV_UDID_NOT_MATCH   = 0x00012009;   
	static public final int E_BE_ACC_SESSION_CLIENT_USR_AGENT_NOT_MATCH  = 0x0001200a;
}
