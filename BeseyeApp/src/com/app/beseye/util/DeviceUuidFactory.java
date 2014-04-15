package com.app.beseye.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

public class DeviceUuidFactory {
	protected static final String PREFS_FILE = "device_id.xml";
	protected static final String PREFS_DEVICE_ID = "device_id";

	protected static UUID uuid;
	private static DeviceUuidFactory sDeviceUuidFactory;
	
	static public DeviceUuidFactory getInstance(Context coNtext){
		if(null == sDeviceUuidFactory){
			sDeviceUuidFactory = new DeviceUuidFactory(coNtext);
		}
		return sDeviceUuidFactory;
	}

	private DeviceUuidFactory(Context coNtext) {
		if( uuid ==null ) {
			synchronized (DeviceUuidFactory.class) {
			if( uuid == null) {
				final SharedPreferences prefs = coNtext.getSharedPreferences( PREFS_FILE, 0);
				final String id = prefs.getString(PREFS_DEVICE_ID, null );
			
				if (id != null) {
					// Use the ids previously computed and stored in the prefs file
					uuid = UUID.fromString(id);
					
					} else {
					
					final String androidId = Secure.getString(coNtext.getContentResolver(), Secure.ANDROID_ID);
					
					// Use the Android ID unless it's broken, in which case fallback on deviceId,
					// unless it's not available, then fallback on a random number which we store
					// to a prefs file
					try {
						if (!"9774d56d682e549c".equals(androidId)) {
							uuid = UUID.nameUUIDFromBytes(androidId.getBytes("utf8"));
						} else {
							final String deviceId = ((TelephonyManager) coNtext.getSystemService( Context.TELEPHONY_SERVICE )).getDeviceId();
							uuid = deviceId!=null ? UUID.nameUUIDFromBytes(deviceId.getBytes("utf8")) : UUID.randomUUID();
						}
					}catch (UnsupportedEncodingException e) {
						throw new RuntimeException(e);
					}
					
					// Write the value out to the prefs file
					prefs.edit().putString(PREFS_DEVICE_ID, uuid.toString() ).commit();
					
					}
				}
			}
		}
	}

	static public UUID getDeviceUuid() {
		return uuid;
	}
}