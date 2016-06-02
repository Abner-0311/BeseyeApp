/**
 * Created by Abner on 16/5/27.
 */
package com.app.beseye.util;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.content.PermissionChecker;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import static com.app.beseye.util.BeseyeConfig.TAG;

public class BeseyeLocationMgr {
    private WeakReference<Context> mWrContext;
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private PendingResult<LocationSettingsResult> result;

    static private BeseyeLocationMgr sBeseyeLocationMgr;
    //static private Handler sHandler = new Handler();

    static public BeseyeLocationMgr createInstance(Context context){
        if(null == sBeseyeLocationMgr){
            sBeseyeLocationMgr = new BeseyeLocationMgr(context);
        }
        return sBeseyeLocationMgr;
    }

    static public BeseyeLocationMgr getInstance(){
        return sBeseyeLocationMgr;
    }

    private BeseyeLocationMgr(Context context){
        if(null != context){
            mWrContext = new WeakReference<Context>(context);
        }
    }

    public void turnGPSOn(){
        Context context = mWrContext.get();
        if(null != context){
            Intent intent = new Intent("android.location.GPS_ENABLED_CHANGE");
            intent.putExtra("enabled", true);
            context.sendBroadcast(intent);

            String provider = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

            if(!provider.contains("gps")){ //if gps is disabled
                final Intent poke = new Intent();
                poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
                poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
                poke.setData(Uri.parse("3"));
                context.sendBroadcast(poke);
            }
        }
    }

    public void turnGPSOff(){
        Context context = mWrContext.get();
        if(null != context){
            Intent intent = new Intent("android.location.GPS_ENABLED_CHANGE");
            intent.putExtra("enabled", false);
            context.sendBroadcast(intent);

            String provider = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            if(provider.contains("gps")){ //if gps is enabled
                final Intent poke = new Intent();
                poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
                poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
                poke.setData(Uri.parse("3"));
                context.sendBroadcast(poke);
            }
        }
    }

    public void requestGoogleApiClient(final Activity act, final int iRequestCode){
        Log.i(TAG, getClass().getSimpleName()+"::requestGoogleApiClient()+++ ");
        if ( android.os.Build.VERSION.SDK_INT >= 23) {

            if (null == mGoogleApiClient) {
                mGoogleApiClient = new GoogleApiClient.Builder(act)
                        .addApi(LocationServices.API)
                        .addConnectionCallbacks((GoogleApiClient.ConnectionCallbacks) act)
                        .addOnConnectionFailedListener((GoogleApiClient.OnConnectionFailedListener) act).build();
            }

            if (null != mGoogleApiClient) {
                if (false == mGoogleApiClient.isConnected() && false == mGoogleApiClient.isConnecting()) {
                    mGoogleApiClient.connect();
                } else if (mGoogleApiClient.isConnected()) {
                    checkLocationService(act, iRequestCode);
                }
            }
        }
    }


    public void checkLocationService(final Activity act, final int iRequestCode){
        if(null == mLocationRequest) {
            mLocationRequest = LocationRequest.create();
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            mLocationRequest.setInterval(30 * 1000);
            mLocationRequest.setFastestInterval(5 * 1000);
        }

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        builder.setAlwaysShow(true);

        result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                Log.i(TAG, "status.getStatusCode():"+status.getStatusCode());
                //final LocationSettingsStates state = result.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can initialize location
                        // requests here.
                        //...

                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(act, iRequestCode);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                        //...
                        break;
                }
            }
        });
    }

    public boolean isLocationPermissionGranted(){
        boolean bRet = true;
        if ( android.os.Build.VERSION.SDK_INT >= 23) {
            Context context = mWrContext.get();
            if(null != context) {
//                Log.i(TAG, getClass().getSimpleName() + "::isLocationPermissionGranted()" + PermissionChecker.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) +
//                        "," + PermissionChecker.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) +
//                        "," + PermissionChecker.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) +
//                        "," + PermissionChecker.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION));

                if (PermissionChecker.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED ||
                        PermissionChecker.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, getClass().getSimpleName() + "::isLocationPermissionGranted(), permission not granted");
                    bRet = false;
                }
            }
        }
        return bRet;
    }

    public void requestLocationPermission(final Activity act, final int iRequestCode){
        if(null != act) {
            android.support.v4.app.ActivityCompat.requestPermissions(act, new String[] {  android.Manifest.permission.ACCESS_COARSE_LOCATION , android.Manifest.permission.ACCESS_FINE_LOCATION}, iRequestCode);
        }
    }
}
