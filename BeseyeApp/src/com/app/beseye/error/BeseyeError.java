package com.app.beseye.error;

public class BeseyeError {
	static public final int E_BE_ACC_GENERAL_ERR 						 = 0x00010000;
	
	static public final int E_BE_ACC_USER_CREATE_FAILED                  = 0x00011000; //"Failed to create user!" }.freeze,
	static public final int E_BE_ACC_USER_CLIENT_IP_IS_BLANK             = 0x00011001; //"" }.freeze,
	static public final int E_BE_ACC_USER_EMAIL_NOT_UNIQUE               = 0x00011002; //"Email is not unquie!" }.freeze,
	static public final int E_BE_ACC_USER_EMAIL_FORMAT_INVALID           = 0x00011003; //"Email format is invalid!" }.freeze,
	static public final int E_BE_ACC_USER_EMAIL_EMPTY                    = 0x00011004; //"Email is empty or blank!" }.freeze,
	static public final int E_BE_ACC_USER_NAME_EMPTY                     = 0x00011005; //"User name is empty or blank!" }.freeze,
	static public final int E_BE_ACC_USER_PASSWORD_EMPTY                 = 0x00011006; //"Password is empty or blank!" }.freeze,
	static public final int E_BE_ACC_USER_PASSWORD_STRENGTH_TOO_LOW      = 0x00011007; //"Password strength is not enough!" }.freeze,
	static public final int E_BE_ACC_USER_DESTROY_FAILED                 = 0x00011008; //"" }.freeze,
	static public final int E_BE_ACC_USER_PASSWORD_INCORRET              = 0x00011009; //"" }.freeze,
	static public final int E_BE_ACC_USER_SAVE_TO_DB_FAILED              = 0x0001100a; //"" }.freeze,
	static public final int E_BE_ACC_USER_NOT_EXIST                      = 0x0001100b; //"" }.freeze,
	static public final int E_BE_ACC_USER_ALREADY_EXIST                  = 0x0001100c; //"" }.freeze,
	static public final int E_BE_ACC_USER_PAIRING_NOT_ENABLED            = 0x0001100d; //"" }.freeze,
	
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
