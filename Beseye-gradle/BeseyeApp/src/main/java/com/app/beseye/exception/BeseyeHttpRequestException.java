package com.app.beseye.exception;

public class BeseyeHttpRequestException extends Exception {
	private int miHttpStatusCode;
	
	public BeseyeHttpRequestException(int iHttpStatusCode) {
		miHttpStatusCode = iHttpStatusCode;
	}

	public int getHttpStatusCode(){
		return miHttpStatusCode;
	}
}
