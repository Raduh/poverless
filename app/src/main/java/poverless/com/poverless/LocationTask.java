package poverless.com.poverless;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Created by andreibadoi on 17/10/15.
 */
public class LocationTask extends AsyncTask<Void, Void, Void> {
    private GoogleMap googleMap;
    private ProgressBar progressBar;
    private Context context;
    private Float latitude;
    private Float longitude;
    private PositionData positionData;

    LocationTask(GoogleMap googleMap, ProgressBar progressBar, Context context,
                 PositionData positionData) {
        this.googleMap = googleMap;
        this.progressBar = progressBar;
        this.context = context;
        this.positionData = positionData;
    }
    @Override
    public void onPreExecute() {
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    protected Void doInBackground(Void... voids) {
        LocationManager locationManager =
                (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        Location myLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (myLocation == null) {
            myLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
        if (myLocation == null) {
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                return null;
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0,
                    new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    latitude = (float) location.getLatitude();
                    longitude = (float) location.getLongitude();
                }

                @Override
                public void onStatusChanged(String s, int i, Bundle bundle) {
                }

                @Override
                public void onProviderEnabled(String s) {
                }

                @Override
                public void onProviderDisabled(String s) {
                }
            });
        } else {
            latitude = (float) myLocation.getLatitude();
            longitude = (float) myLocation.getLongitude();
        }
        return null;
    }

    @Override
    public void onPostExecute(Void result) {
        progressBar.setVisibility(View.GONE);
        if (latitude == null || longitude == null) {
            return;
        }

        final LatLng latLng = new LatLng(latitude, longitude);
        positionData.center = latLng;

        CameraUpdate center = CameraUpdateFactory.newLatLng(latLng);
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(Const.MAP_ZOOM);

        googleMap.moveCamera(center);
        googleMap.animateCamera(zoom);

        Util.showInterestArea(googleMap, positionData);
    }
}
