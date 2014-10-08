package com.app.beseye.googlemap;
import static com.app.beseye.util.BeseyeConfig.TAG;
import static com.app.beseye.util.BeseyeJSONUtil.ACC_DATA;
import static com.app.beseye.util.BeseyeJSONUtil.LOCATION_LAT;
import static com.app.beseye.util.BeseyeJSONUtil.LOCATION_LONG;
import static com.app.beseye.util.BeseyeJSONUtil.LOCATION_OBJ;
import static com.app.beseye.util.BeseyeJSONUtil.OBJ_TIMESTAMP;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.app.beseye.BeseyeBaseActivity;
import com.app.beseye.CameraListActivity;
import com.app.beseye.R;
import com.app.beseye.httptask.BeseyeCamBEHttpTask;
import com.app.beseye.util.BeseyeCamInfoSyncMgr;
import com.app.beseye.util.BeseyeJSONUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class LocateOnGoogleMap extends BeseyeBaseActivity implements OnMarkerClickListener,
																	 OnMarkerDragListener{
   
	/* layout */
	private EditText SearchText;
	private ActionBar.LayoutParams mNavBarLayoutParams;
	private View mVwNavBar;
	
	/* map */
	private GoogleMap map;
    private Marker markerMe;
    private Marker markersearch;
    
    /* GPS */
    private LocationManager locationMgr;
    private String provider;
    
    /* Location */
    private double SE_LAT, SE_LNG;
    private double GET_LAT, GET_LNG;
    private boolean isSelect = false;
    private boolean isGetlocale = false;
    public static final String KEY_LOCALE_OBJ = "KEY_LOCALE_OBJ";
    public static final String KEY_LOCALE_TS = "KEY_LOCALE_TS";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        
    	super.onCreate(savedInstanceState);
    	
        getSupportActionBar().setDisplayOptions(0);
		getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
		
		mVwNavBar = getLayoutInflater().inflate(R.layout.layout_cam_list_nav, null);
		if(null != mVwNavBar){
			ImageView mIvBack = (ImageView)mVwNavBar.findViewById(R.id.iv_nav_menu_btn);
			if(null != mIvBack){
				mIvBack.setOnClickListener(this);
				mIvBack.setImageResource(R.drawable.sl_event_list_cancel);
			}
			
			ImageView mIvOK = (ImageView)mVwNavBar.findViewById(R.id.iv_nav_add_cam_btn);
			if(null != mIvOK){
				mIvOK.setOnClickListener(this);
				mIvOK.setImageResource(R.drawable.sl_nav_ok_btn);
			}
			
			TextView txtTitle = (TextView)mVwNavBar.findViewById(R.id.txt_nav_title);
			if(null != txtTitle){
				txtTitle.setText(R.string.cam_setting_title_setlocation);
			}
			
			mNavBarLayoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT);
			mNavBarLayoutParams.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
	        getSupportActionBar().setCustomView(mVwNavBar, mNavBarLayoutParams);
		}
		
		try {
			mCam_obj = new JSONObject(getIntent().getStringExtra(CameraListActivity.KEY_VCAM_OBJ));
			if(null != mCam_obj){
				mStrVCamID = BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_ID);
			}
		} catch (JSONException e1) {
			Log.e(TAG, "PowerScheduleActivity::updateAttrByIntent(), failed to parse, e1:"+e1.toString());
		}
		
		//map setting
        map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        map.setOnMarkerClickListener(this);
        map.setOnMarkerDragListener(this);
        
        //Get user locale info
        JSONObject dataObj = BeseyeJSONUtil.getJSONObject(mCam_obj, ACC_DATA);
		if(null != dataObj){
			
			JSONObject localeObj = BeseyeJSONUtil.getJSONObject(dataObj, LOCATION_OBJ);
			
			if(null != localeObj){
				
				GET_LAT = BeseyeJSONUtil.getJSONDouble(localeObj,BeseyeJSONUtil.LOCATION_LAT);
				GET_LNG = BeseyeJSONUtil.getJSONDouble(localeObj,BeseyeJSONUtil.LOCATION_LONG);
				
				if(GET_LAT != 0.0 || GET_LNG != 0.0){
					isGetlocale = true;
				}
				else{
					isGetlocale = false;
				}
				
				Log.e("LOCALE DEBUG", "Get locale info ... lat,lng = " + GET_LAT + "," + GET_LNG);
				
			}else{
				
				isGetlocale = false;
			}
			
		}else{
			Log.e("LOCALE DEBUG", "never get locale data");
		}
        
		
		//initial user location
        if(isGetlocale){
        	try{
        	
        	Geocoder gc = new Geocoder(this, Locale.TRADITIONAL_CHINESE); 	//地區:台灣
        	List<Address> lstAddress = gc.getFromLocation(GET_LAT, GET_LNG, 1);
	    	
			String returnAddress = lstAddress.get(0).getAddressLine(0);
        	
        	Log.e("MAPDEBUG", "isGetlocale start focus");
        	MarkerOptions markerOpt_home = new MarkerOptions();
        	markerOpt_home.position(new LatLng(GET_LAT,GET_LNG));
        	markerOpt_home.title("last setting");
        	markerOpt_home.snippet(returnAddress);
        	markerOpt_home.draggable(true);
        	markersearch = map.addMarker(markerOpt_home);
        	initLocationProvider();
        	
        	cameraFocusOnMe(GET_LAT, GET_LNG);
        	}catch(IOException e){
        		Log.e(TAG, "last location setting" + e.toString());
        	}
        	
        }else{
	        if (initLocationProvider()) {
	        	whereAmI();
	        }else{
	        	Toast.makeText(this, "please open network", Toast.LENGTH_SHORT).show();
	        }
        }
    }
  
    
    public void onSearchClick(View view){
    	
    	SearchText = (EditText) findViewById(R.id.locate_search_text);
    	
    	String LocationName = SearchText.getText().toString().trim();
    	if(LocationName.length()>0){
    		locationNameToMarker(LocationName);
    	}else{
    		Toast.makeText(this, "Query empty", Toast.LENGTH_SHORT).show();
    	}
    	
    }
    
    private void locationNameToMarker(String LocationName){
    	
    	if(markersearch != null){
    		markersearch.remove();
    	}
    	
    	Geocoder geocoder = new Geocoder(getBaseContext());
    	List<Address> addressList = null;
    	int maxResults = 1;
    	try{
    		
    		addressList = geocoder.getFromLocationName(LocationName, maxResults);
    		
    	}catch(IOException e){
    		
    		Log.e("TAG", "locationNameToMarker() geocoder error ... " + e.toString());
    		
    	}
    	
    	if(addressList == null || addressList.isEmpty()){
    		
    		Toast.makeText(this, "Address not found", Toast.LENGTH_SHORT).show();
    		
    	}else{
    		
    		Address address = addressList.get(0);
    		MarkerOptions markerOpt_home = new MarkerOptions();
        	markerOpt_home.position(new LatLng(address.getLatitude(),address.getLongitude()));
        	markerOpt_home.title(LocationName);
        	markerOpt_home.snippet(address.getAddressLine(0));
        	markerOpt_home.draggable(true);
        	markersearch = map.addMarker(markerOpt_home);
        	cameraFocusOnMe(address.getLatitude(), address.getLongitude());
        	
    	}
    }
 
    private boolean initLocationProvider(){
    	 locationMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    	 if (locationMgr.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
    		 provider = LocationManager.GPS_PROVIDER;
    		 return true;
    	 }else{
    		 Toast.makeText(this, "please open gps to improve location accuracy.", Toast.LENGTH_SHORT).show();
    		 Criteria criteria = new Criteria();
        	 criteria.setAccuracy(Criteria.ACCURACY_FINE);
        	 criteria.setAltitudeRequired(false);
        	 criteria.setBearingRequired(false);
        	 criteria.setCostAllowed(true);
        	 criteria.setPowerRequirement(Criteria.POWER_LOW);
        	 provider = locationMgr.getBestProvider(criteria, true);
        	 if (provider != null) {
        		 return true;
        	 }
    	 }
    	return false;
    }
    
    private void whereAmI(){
    	
    	Location location = locationMgr.getLastKnownLocation(provider);
    	updateWithNewLocation(location);

		//long minTime = 2000;//ms
		//float minDist = 1.0f;//meter
		//locationMgr.requestLocationUpdates(provider, minTime, minDist, locationListener);
		
    }
    
    private void updateWithNewLocation(Location location){
    	
    	if(location != null){
    		double lng = location.getLongitude();
    		double lat = location.getLatitude();
    		showMarkerMe(lat,lng);
    		cameraFocusOnMe(lat, lng);
    	}else{
    		Toast.makeText(this, "Can't detect location.", Toast.LENGTH_SHORT).show();
    	}
    	
    }
    
    
    private void showMarkerMe(double lat, double lng) {
    
    	try{
	    	if(markerMe != null){
	    		markerMe.remove();
	    	}
	    	
	    	Geocoder gc = new Geocoder(this, Locale.TRADITIONAL_CHINESE); 	//地區:台灣
			List<Address> lstAddress = gc.getFromLocation(lat, lng, 1);
	    	
			String returnAddress = lstAddress.get(0).getAddressLine(0);
	    	
	    	MarkerOptions markerOpt = new MarkerOptions();
	    	markerOpt.position(new LatLng(lat, lng));
	    	markerOpt.title("現在位置");
	    	markerOpt.snippet(returnAddress);
	    	markerOpt.draggable(true);
	    	markerMe = map.addMarker(markerOpt);
    	}
    	catch(Exception e){
    		Log.e("TAG", "showMarkerMe() geocoder error ... " + e.toString());
    	}
    }
    
    private void cameraFocusOnMe(double lat, double lng){
    	CameraPosition camposition = new CameraPosition.Builder()
    		.target(new LatLng(lat, lng))
    		.zoom(16)
    		.build();
    	map.animateCamera(CameraUpdateFactory.newCameraPosition(camposition));
    }
    
    
    GpsStatus.Listener gpsListener = new GpsStatus.Listener() {
		@Override
		public void onGpsStatusChanged(int event) {
			switch (event) {
	        case GpsStatus.GPS_EVENT_STARTED:
	        	//Log.d(TAG, "GPS_EVENT_STARTED");
	        	//Toast.makeText(LocateOnGoogleMap.this, "GPS_EVENT_STARTED", Toast.LENGTH_SHORT).show();
	            break;

	        case GpsStatus.GPS_EVENT_STOPPED:
	        	//Log.d(TAG, "GPS_EVENT_STOPPED");
	        	//Toast.makeText(LocateOnGoogleMap.this, "GPS_EVENT_STOPPED", Toast.LENGTH_SHORT).show();
	            break;

	        case GpsStatus.GPS_EVENT_FIRST_FIX:
	        	//Log.d(TAG, "GPS_EVENT_FIRST_FIX");
	        	//Toast.makeText(LocateOnGoogleMap.this, "GPS_EVENT_FIRST_FIX", Toast.LENGTH_SHORT).show();
	            break;

	        case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
	        	//Log.d(TAG, "GPS_EVENT_SATELLITE_STATUS");
	            break;
			}
		}
	};
    
	LocationListener locationListener = new LocationListener(){

		@Override
		public void onLocationChanged(Location location) {
			updateWithNewLocation(location);
			//Toast.makeText(LocateOnGoogleMap.this, "get new location", Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onProviderDisabled(String provider) {
			updateWithNewLocation(null);
		}

		@Override
		public void onProviderEnabled(String provider) {
			
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			switch (status) {
		    case LocationProvider.OUT_OF_SERVICE:
		        //Log.v(TAG, "Status Changed: Out of Service");
		        Toast.makeText(LocateOnGoogleMap.this, "Status Changed: Out of Service",
		                Toast.LENGTH_SHORT).show();
		        break;
		    case LocationProvider.TEMPORARILY_UNAVAILABLE:
		        //Log.v(TAG, "Status Changed: Temporarily Unavailable");
		        Toast.makeText(LocateOnGoogleMap.this, "Status Changed: Temporarily Unavailable",
		                Toast.LENGTH_SHORT).show();
		        break;
		    case LocationProvider.AVAILABLE:
		        //Log.v(TAG, "Status Changed: Available");
		        Toast.makeText(LocateOnGoogleMap.this, "Status Changed: Available",
		                Toast.LENGTH_SHORT).show();
		        break;
		    }
		}
		
	};

	@Override
	protected int getLayoutId() {
		return R.layout.layout_locate_on_google;
		
	}


	@Override
	public boolean onMarkerClick(Marker marker) {
		
		
		if (marker.equals(markerMe)){
			Toast.makeText(LocateOnGoogleMap.this, "選擇地點：" + marker.getSnippet() ,Toast.LENGTH_SHORT).show();
			
			SE_LAT = marker.getPosition().latitude;
			SE_LNG = marker.getPosition().longitude;
			
			isSelect = true;
			
		}else{
			Toast.makeText(LocateOnGoogleMap.this, "選擇地點：" + marker.getSnippet() ,Toast.LENGTH_SHORT).show();
			
			SE_LAT = marker.getPosition().latitude;
			SE_LNG = marker.getPosition().longitude;
			
			isSelect = true;
			
		}
		return false;
	}
	
	public void DonLocationChanged(Double LAT, Double LNG){
		
		monitorAsyncTask(new BeseyeCamBEHttpTask.SetCamLocaleTask(this), true, mStrVCamID, LAT.toString(), LNG.toString());
		Log.e("SETLOCALE", "Donlocationchange monitor task end");
		Log.e("SETLOCALE", "lat,lng ="+ LAT + "," + LNG);
		Log.e("SETLOCALE", "lat,lng to string ="+ LAT.toString() + "," + LNG.toString());
	}
	
	@Override
	public void onPostExecute(AsyncTask task, List<JSONObject> result, int iRetCode) {
		if(!task.isCancelled()){
			if(task instanceof BeseyeCamBEHttpTask.SetCamLocaleTask){
				if(0 == iRetCode){
					Log.e("SETLOCALE", "onpostexecute setcamlocaletask");
					JSONObject dataObj = BeseyeJSONUtil.getJSONObject(mCam_obj, ACC_DATA);
					if(null != dataObj){
						Log.e("SETLOCALE", "data object is not null");
						JSONObject localeObj = BeseyeJSONUtil.getJSONObject(dataObj, LOCATION_OBJ);
						if(null != localeObj){
							Log.e("SETLOCALE", "locale object is not null");
							BeseyeJSONUtil.setJSONDouble(localeObj, LOCATION_LAT, BeseyeJSONUtil.getJSONDouble(result.get(0), LOCATION_LAT));
							BeseyeJSONUtil.setJSONDouble(localeObj, LOCATION_LONG, BeseyeJSONUtil.getJSONDouble(result.get(0), LOCATION_LONG));
							BeseyeJSONUtil.setJSONLong(mCam_obj, OBJ_TIMESTAMP, BeseyeJSONUtil.getJSONLong(result.get(0), OBJ_TIMESTAMP));
							Log.e("SETLOCALE", "lat,lng = " + BeseyeJSONUtil.getJSONDouble(result.get(0), LOCATION_LAT) + "," +BeseyeJSONUtil.getJSONDouble(result.get(0), LOCATION_LONG));
							BeseyeCamInfoSyncMgr.getInstance().updateCamInfo(mStrVCamID, mCam_obj);
						}
					}
					
					JSONObject toSave;
					try {
						toSave = new JSONObject();
						toSave.put(LOCATION_LAT, BeseyeJSONUtil.getJSONDouble(result.get(0), LOCATION_LAT));
						toSave.put(LOCATION_LONG, BeseyeJSONUtil.getJSONDouble(result.get(0), LOCATION_LONG));
						
						Intent intent = new Intent();
						intent.putExtra(KEY_LOCALE_OBJ, toSave.toString());
						intent.putExtra(KEY_LOCALE_TS, BeseyeJSONUtil.getJSONLong(result.get(0), BeseyeJSONUtil.OBJ_TIMESTAMP));
						setResult(RESULT_OK, intent);
						finish();
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					
					
				}
				else{
					
					Log.e("SETLOCALE", "Cant set Cam Locale, iRetCode != 0");
					
				}
			}else{
				super.onPostExecute(task, result, iRetCode);
			}
		}
	}
	
	
	@Override
	public void onClick(View view) {
		switch(view.getId()){
			
			case R.id.iv_nav_add_cam_btn:{
				if(isSelect){
					DonLocationChanged(SE_LAT, SE_LNG);
					//finish();
				}
				else{
					Toast.makeText(this, "please select a location", Toast.LENGTH_SHORT).show();
				}
				break;
			}
			case R.id.iv_nav_menu_btn:{
				finish();
				break;
			}
			default:{
				super.onClick(view);
			}
		}
	}


	@Override
	public void onMarkerDrag(Marker marker) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void onMarkerDragEnd(Marker marker) {
		// TODO Auto-generated method stub
		
		Geocoder gc = new Geocoder(this, Locale.TRADITIONAL_CHINESE); 	//地區:台灣
		//自經緯度取得地址
		
		double lat,lng;
		
		lat = marker.getPosition().latitude;
		lng = marker.getPosition().longitude;
		
		try{
		
			List<Address> lstAddress = gc.getFromLocation(lat, lng, 1);
	    	
			String returnAddress = lstAddress.get(0).getAddressLine(0);
	
	    	marker.setTitle("坐標位置");
	    	marker.setSnippet(returnAddress);
 
		}catch(IOException e){
    		
    		//Toast.makeText(this, "Geocoder error!!", Toast.LENGTH_SHORT).show();
    		
    	}
		
	}


	@Override
	public void onMarkerDragStart(Marker marker) {
		// TODO Auto-generated method stub
		
	}
	
	public void checkislocationed(){
		
		monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamLocaleTask(this), true, mStrVCamID);
		Log.e("MAPDEBUG", "checkislocationed monitorasynctask end");
	}
	
}