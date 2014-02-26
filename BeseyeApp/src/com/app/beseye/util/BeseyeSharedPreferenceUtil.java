package com.app.beseye.util;

import static com.app.beseye.util.BeseyeConfig.*;
import java.util.Map;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;

public class BeseyeSharedPreferenceUtil {
	
	static public SharedPreferences getSharedPreferences(Context context, String strName){
		if(null != context){
			int sdkLevel = Build.VERSION.SDK_INT;
			return context.getSharedPreferences(strName, (sdkLevel > Build.VERSION_CODES.FROYO) ? 4 : Context.MODE_PRIVATE);
		}
		return null;
	}
	
	static public SharedPreferences getSecuredSharedPreferences(Context context, String strName){
		if(null != context){
			int sdkLevel = Build.VERSION.SDK_INT;
			return new ObscuredSharedPreferences(context, context.getSharedPreferences(strName, (sdkLevel > Build.VERSION_CODES.FROYO) ? 4 : Context.MODE_PRIVATE));
		}
		return null;
	}
	
	static public void clearSharedPreferences(SharedPreferences pref){
		if(null != pref){
			Editor editor = pref.edit();
			if(null != editor){
				editor.clear();
				editor.apply();
			}
		}
	}
	
	static public String getPrefStringValue(SharedPreferences pref, String strKey){
		return getPrefStringValue(pref, strKey, "");
	}
	
	static public String getPrefStringValue(SharedPreferences pref, String strKey, String defValue){
		String strRet = defValue;
		if(null != pref){
			strRet = pref.getString(strKey, defValue);
//			if(Configuration.DEBUG)
//				Log.d(TAG,"getPrefStringValue(), strKey = "+strKey+", strRet="+strRet+", thread="+Thread.currentThread());
		}
		return strRet;
	}
	
	static public void setPrefStringValue(SharedPreferences pref, String strKey, String strValue){
		if(null == strKey || 0 == strKey.length()){
			Log.e(TAG,"setPrefValue(), invalid strKey");
			return;
		}
		
		if(null == strValue){
			Log.e(TAG,"setPrefValue(), invalid strValue, strKey="+strKey);
			return;
		}
			
		if(null != pref){
			Editor editor = pref.edit();
			if(null != editor){
				editor.putString(strKey, strValue);
				editor.apply();
			}
//			if(Configuration.DEBUG)
//				Log.d(TAG,"setPrefStringValue(), strKey = "+strKey+", strValue="+strValue+", thread="+Thread.currentThread());
		}
	}
	
	static public int getPrefIntValue(SharedPreferences pref, String strKey){
		return getPrefIntValue(pref, strKey, -1);
	}
	
	static public int getPrefIntValue(SharedPreferences pref, String strKey, int iDefault){
		int iRet = iDefault;
		if(null != pref){
			iRet = pref.getInt(strKey, iDefault);
//			if(Configuration.DEBUG)
//				Log.d(TAG,"getPrefIntValue(), strKey = "+strKey+", iRet="+iRet+", thread="+Thread.currentThread());
		}
		return iRet;
	}
	
	static public void setPrefBooleanValue(SharedPreferences pref, String strKey, boolean iValue){
		if(null == strKey || 0 == strKey.length()){
			Log.e(TAG,"setPrefValue(), invalid strKey");
			return;
		}
			
		if(null != pref){
			Editor editor = pref.edit();
			if(null != editor){
				editor.putBoolean(strKey, iValue);
				editor.apply();
			}
//			if(Configuration.DEBUG)
//				Log.d(TAG,"setPrefIntValue(), strKey = "+strKey+", iValue="+iValue+", thread="+Thread.currentThread());
		}
	}
	
	static public boolean getPrefBooleanValue(SharedPreferences pref, String strKey, boolean iDefault){
		boolean iRet = iDefault;
		if(null != pref){
			iRet = pref.getBoolean(strKey, iDefault);
//			if(Configuration.DEBUG)
//				Log.d(TAG,"getPrefIntValue(), strKey = "+strKey+", iRet="+iRet+", thread="+Thread.currentThread());
		}
		return iRet;
	}
	
	static public void setPrefIntValue(SharedPreferences pref, String strKey, int iValue){
		if(null == strKey || 0 == strKey.length()){
			Log.e(TAG,"setPrefValue(), invalid strKey");
			return;
		}
			
		if(null != pref){
			Editor editor = pref.edit();
			if(null != editor){
				editor.putInt(strKey, iValue);
				editor.apply();
			}
//			if(Configuration.DEBUG)
//				Log.d(TAG,"setPrefIntValue(), strKey = "+strKey+", iValue="+iValue+", thread="+Thread.currentThread());
		}
	}
	
	
	static public long getPrefLongValue(SharedPreferences pref, String strKey){
		return getPrefLongValue(pref, strKey, -1L);
	}
	
	static public long getPrefLongValue(SharedPreferences pref, String strKey, long iDefault){
		long iRet = iDefault;
		if(null != pref){
			iRet = pref.getLong(strKey, iDefault);
//			if(Configuration.DEBUG)
//				Log.d(TAG,"getPrefIntValue(), strKey = "+strKey+", iRet="+iRet+", thread="+Thread.currentThread());
		}
		return iRet;
	}
	
	static public void setPrefLongValue(SharedPreferences pref, String strKey, long iValue){
		if(null == strKey || 0 == strKey.length()){
			Log.e(TAG,"setPrefValue(), invalid strKey");
			return;
		}
			
		if(null != pref){
			Editor editor = pref.edit();
			if(null != editor){
				editor.putLong(strKey, iValue);
				editor.apply();
			}
//			if(Configuration.DEBUG)
//				Log.d(TAG,"setPrefIntValue(), strKey = "+strKey+", iValue="+iValue+", thread="+Thread.currentThread());
		}
	}
	
	/**
	 * Warning, this gives a false sense of security.  If an attacker has enough access to
	 * acquire your password store, then he almost certainly has enough access to acquire your
	 * source binary and figure out your encryption key.  However, it will prevent casual
	 * investigators from acquiring passwords, and thereby may prevent undesired negative
	 * publicity.
	 */
	static public class ObscuredSharedPreferences implements SharedPreferences {
	    protected static final String UTF8 = "utf-8";
	    private static final char[] SEKRIT = {'i','k','a','l','a','I','s','B','E','s','T'} ; // INSERT A RANDOM PASSWORD HERE.
	                                               // Don't use anything you wouldn't want to
	                                               // get out there if someone decompiled
	                                               // your app.


	    protected SharedPreferences delegate;
	    protected Context context;

	    public ObscuredSharedPreferences(Context context, SharedPreferences delegate) {
	        this.delegate = delegate;
	        this.context = context;
	    }

	    public class Editor implements SharedPreferences.Editor {
	        protected SharedPreferences.Editor delegate;

	        public Editor() {
	            this.delegate = ObscuredSharedPreferences.this.delegate.edit();                    
	        }

	        @Override
	        public Editor putBoolean(String key, boolean value) {
	            delegate.putString(key, encrypt(Boolean.toString(value)));
	            return this;
	        }

	        @Override
	        public Editor putFloat(String key, float value) {
	            delegate.putString(key, encrypt(Float.toString(value)));
	            return this;
	        }

	        @Override
	        public Editor putInt(String key, int value) {
	            delegate.putString(key, encrypt(Integer.toString(value)));
	            return this;
	        }

	        @Override
	        public Editor putLong(String key, long value) {
	            delegate.putString(key, encrypt(Long.toString(value)));
	            return this;
	        }

	        @Override
	        public Editor putString(String key, String value) {
	            delegate.putString(key, encrypt(value));
	            return this;
	        }

	        @Override
	        public void apply() {
	            delegate.apply();
	        }

	        @Override
	        public Editor clear() {
	            delegate.clear();
	            return this;
	        }

	        @Override
	        public boolean commit() {
	            return delegate.commit();
	        }

	        @Override
	        public Editor remove(String s) {
	            delegate.remove(s);
	            return this;
	        }

			@Override
			public android.content.SharedPreferences.Editor putStringSet(
					String arg0, Set<String> arg1) {
				// TODO Auto-generated method stub
				return null;
			}
	    }

	    public Editor edit() {
	        return new Editor();
	    }


	    @Override
	    public Map<String, ?> getAll() {
	        throw new UnsupportedOperationException(); // left as an exercise to the reader
	    }

	    @Override
	    public boolean getBoolean(String key, boolean defValue) {
	        final String v = delegate.getString(key, null);
	        return v!=null ? Boolean.parseBoolean(decrypt(v)) : defValue;
	    }

	    @Override
	    public float getFloat(String key, float defValue) {
	        final String v = delegate.getString(key, null);
	        return v!=null ? Float.parseFloat(decrypt(v)) : defValue;
	    }

	    @Override
	    public int getInt(String key, int defValue) {
	        final String v = delegate.getString(key, null);
	        return v!=null ? Integer.parseInt(decrypt(v)) : defValue;
	    }

	    @Override
	    public long getLong(String key, long defValue) {
	        final String v = delegate.getString(key, null);
	        return v!=null ? Long.parseLong(decrypt(v)) : defValue;
	    }

	    @Override
	    public String getString(String key, String defValue) {
	        final String v = delegate.getString(key, null);
	        return v != null ? decrypt(v) : defValue;
	    }

	    @Override
	    public boolean contains(String s) {
	        return delegate.contains(s);
	    }

	    @Override
	    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener onSharedPreferenceChangeListener) {
	        delegate.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
	    }

	    @Override
	    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener onSharedPreferenceChangeListener) {
	        delegate.unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
	    }

	    protected String encrypt( String value ) {

	        try {
	            final byte[] bytes = value!=null ? value.getBytes(UTF8) : new byte[0];
	            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
	            SecretKey key = keyFactory.generateSecret(new PBEKeySpec(SEKRIT));
	            Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
	            pbeCipher.init(Cipher.ENCRYPT_MODE, key, new PBEParameterSpec(Settings.Secure.getString(context.getContentResolver(),Settings.System.ANDROID_ID).getBytes(UTF8), 20));
	            return new String(Base64.encode(pbeCipher.doFinal(bytes), Base64.NO_WRAP),UTF8);

	        } catch( Exception e ) {
	            throw new RuntimeException(e);
	        }

	    }

	    protected String decrypt(String value){
	        try {
	            final byte[] bytes = value!=null ? Base64.decode(value,Base64.DEFAULT) : new byte[0];
	            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
	            SecretKey key = keyFactory.generateSecret(new PBEKeySpec(SEKRIT));
	            Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
	            pbeCipher.init(Cipher.DECRYPT_MODE, key, new PBEParameterSpec(Settings.Secure.getString(context.getContentResolver(),Settings.System.ANDROID_ID).getBytes(UTF8), 20));
	            return new String(pbeCipher.doFinal(bytes),UTF8);

	        } catch( Exception e) {
	            throw new RuntimeException(e);
	        }
	    }


		@Override
		public Set<String> getStringSet(String arg0, Set<String> arg1) {
			// TODO Auto-generated method stub
			return null;
		}

	}
}
