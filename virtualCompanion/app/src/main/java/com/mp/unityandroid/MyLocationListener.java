package com.mp.unityandroid;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by garyNoh on 2017. 5. 29..
 */

//위치정보 리스너
    /*---------- Listener class to get coordinates ------------- */
public class MyLocationListener implements LocationListener {


    Context context;
    static double latitude = 0.0;
    static double longtitude = 0.0;

    public MyLocationListener(Context context){
        Log.e("gary", "create object ");
        this.context = context;
    }


    @Override
    public void onLocationChanged(Location loc) {
        Toast.makeText(context, "LocationChanged", Toast.LENGTH_SHORT).show();
        Log.e("gary", "onloactionchanged");
        //context 가 없으면 실행시키지 않는다
        if (context == null) return;

        //위도와 경도를 가져온다
        double longitude = loc.getLongitude();
        //Log.e("gary", longitude);
        double latitude = loc.getLatitude();
        //Log.e("gary", latitude);

        /*------- To get city name from coordinates -------- */
//        String cityName = null;
//        Geocoder gcd = new Geocoder(context, Locale.getDefault());
//        List<Address> addresses;
//        try {
//            addresses = gcd.getFromLocation(loc.getLatitude(),
//                    loc.getLongitude(), 1);
//            if (addresses.size() > 0) {
//                cityName = addresses.get(0).getLocality();
//            }
//        }
//        catch (IOException e) {
//            e.printStackTrace();
//        }
//        String s = longitude + "\n" + latitude + "\n\nMy Current City is: "
//                + cityName;
//        Log.e("gary", s);


        //            //도시 알아오기
        List<Address> addresses;
        String cityName = null;
        Geocoder gcd = new Geocoder(context, Locale.getDefault());
        try {
            addresses = gcd.getFromLocation(latitude,
                    longtitude, 1);
            if (addresses.size() > 0) {
                cityName = addresses.get(0).getLocality();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        String s = longitude + "\n" + latitude + "\n\nMy Current City is: "
                + cityName;
        Log.e("gary", s);
        Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onProviderDisabled(String provider) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}


    public static double getLatitude() {
        return latitude;
    }

    public static double getLongtitude() {
        return longtitude;
    }
}