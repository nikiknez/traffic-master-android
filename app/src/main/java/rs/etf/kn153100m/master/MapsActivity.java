package rs.etf.kn153100m.master;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import java.util.Timer;
import java.util.TimerTask;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, DialogInterface.OnClickListener {

    private GoogleMap mMap;
    private LocationManager locationManager;
    private LocationTracker locationTracker;
    private FetchTrafficDataTask fetchTrafficDataTask = null;
    private FetchConfigTask fetchConfigTask = null;
    private MapFeaturesHandler mapFeaturesHandler = null;
    private RequestQueue requestQueue;
    private Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        locationTracker = new LocationTracker(this);

        requestQueue = Volley.newRequestQueue(this);
        timer = new Timer();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        loadCamPosition();
        mapFeaturesHandler = new MapFeaturesHandler(mMap, this);

        scheduleDataFetching();

        locationTracker.setMap(mMap);

        showStartupDialog();
    }

    private void requestLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, locationTracker);
            mMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, locationTracker);
            mMap.setMyLocationEnabled(true);
        }
    }

    private void loadCamPosition() {
        SharedPreferences sharedPref = getPreferences(MODE_PRIVATE);
        String camPosition = sharedPref.getString("camPosition", "44.808;20.44;13");
        String[] values = camPosition.split(";");
        double lat = Double.parseDouble(values[0]);
        double lng = Double.parseDouble(values[1]);
        float zoom = Float.parseFloat(values[2]);
        LatLng latLng = new LatLng(lat, lng);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }

    private void saveCamPosition() {
        double lat = mMap.getCameraPosition().target.latitude;
        double lng = mMap.getCameraPosition().target.longitude;
        float zoom = mMap.getCameraPosition().zoom;
        SharedPreferences sharedPref = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("camPosition", lat + ";" + lng + ";" + zoom);
        editor.apply();
    }

    @Override
    protected void onDestroy() {
        Log.i("MapsActivity", "onDestroy: invoked");
        timer.cancel();
        if (fetchTrafficDataTask != null) {
            fetchTrafficDataTask.cancel(true);
        }
        if (fetchConfigTask != null) {
            fetchConfigTask.cancel(true);
        }
        if (mapFeaturesHandler != null) {
            mapFeaturesHandler.stop();
        }
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        saveCamPosition();
        super.onStop();
    }

    private void scheduleDataFetching() {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                fetchConfigTask = new FetchConfigTask(requestQueue, mapFeaturesHandler);
                fetchConfigTask.execute();
                fetchTrafficDataTask = new FetchTrafficDataTask(requestQueue, mapFeaturesHandler);
                fetchTrafficDataTask.execute();
            }
        };
        timer.schedule(task, 100, 30000);
    }

    private void showStartupDialog() {
        if (getPreferences(MODE_PRIVATE).contains("asked")) {
            if (getPreferences(MODE_PRIVATE).getBoolean("asked", false)) {
                requestLocationUpdates();
                return;
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.startup_question);
        builder.setPositiveButton("Da", this);
        builder.setNegativeButton("Ne", this);
        builder.show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                getPreferences(MODE_PRIVATE).edit().putBoolean("asked", true).apply();
                requestLocationUpdates();
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                getPreferences(MODE_PRIVATE).edit().putBoolean("asked", false).apply();
                break;
        }
        dialog.dismiss();
    }
}
