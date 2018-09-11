package rs.etf.kn153100m.master;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rs.etf.kn153100m.master.model.Configuration;

class LocationTracker implements LocationListener, Response.ErrorListener, Response.Listener<String> {

    private Context context;
    private GoogleMap map;
    private Polyline myTrajectory;
    private float trajectoryLength = 0;
    private ArrayDeque<Location> locationHistory;
    private RequestQueue queue;

    private long lastTimeMoved;
    private long lastTimeSent;
    private boolean sentStallData = false;

    LocationTracker(Context c) {
        context = c;
        map = null;
        locationHistory = new ArrayDeque<>(10);
        queue = Volley.newRequestQueue(context);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i("LocationTracker", "onLocationChanged: " + location.toString());
        if (location.getAccuracy() > 20) {
            return;
        }
        Location prevLocation = locationHistory.peekLast();
        if (prevLocation == null) {
            updateMyTrajectory(location);
            locationHistory.add(location);
            lastTimeMoved = location.getTime();
            lastTimeSent = lastTimeMoved;
            return;
        }

        float ds = location.distanceTo(prevLocation);
        long dt = location.getTime() - prevLocation.getTime();
        float v = ds * 1000 / dt;
        Log.i("LocationTracker", "calculateLocationSpeed: ds = " + ds + ", dt = " + dt + ", v = " + v);

        if (ds > 300) {
            if (trajectoryLength > 100) {
                sendDataToServer();
            }
            resetLocationHistory(location);
            sentStallData = false;
            return;
        }
        if (ds < 1) {
            long timeSinceMoved = location.getTime() - lastTimeMoved;
            long timeSinceSent = location.getTime() - lastTimeSent;
            Log.i("LocationTracker", "onLocationChanged: stopped for " + timeSinceMoved / 1000 + " seconds");
            if ((timeSinceMoved >= 60 * 1000 || timeSinceSent >= 60 * 1000) && !sentStallData) {
                location.setSpeed(-1000);
                locationHistory.add(location);
                sendDataToServer();
                resetLocationHistory(location);
                sentStallData = true;
                lastTimeMoved = location.getTime();
            }
            return;
        }
        lastTimeMoved = location.getTime();
        sentStallData = false;
        trajectoryLength += ds;
        Log.i("LocationTracker", "onLocationChanged: trajectoryLength = " + trajectoryLength);
        calculateLocationSpeed(location, prevLocation);
        Toast.makeText(context, "New location: speed = " + location.getSpeed(), Toast.LENGTH_SHORT).show();
        updateMyTrajectory(location);
        locationHistory.add(location);

        if (locationHistory.size() == 10) {
            sendDataToServer();
            resetLocationHistory(location);
        }
    }

    private void resetLocationHistory(Location l) {
        locationHistory = new ArrayDeque<>(10);
        locationHistory.add(l);
        l.removeSpeed();
        trajectoryLength = 0;
        lastTimeSent = l.getTime();

        LatLng p = new LatLng(l.getLatitude(), l.getLongitude());
        List<LatLng> points = myTrajectory.getPoints();
        points.clear();
        points.add(p);
        myTrajectory.setPoints(points);
    }

    private void calculateLocationSpeed(Location location, Location prevLocation) {
        float ds = location.distanceTo(prevLocation);
        float dt = location.getTime() - prevLocation.getTime();
        float v = ds * 1000 / dt;
        location.setSpeed(v);
        if (!prevLocation.hasSpeed()) {
            prevLocation.setSpeed(v);
        } else if (dt >= 15 * 1000) {
            location.setSpeed(prevLocation.getSpeed());
        }
    }

    private void updateMyTrajectory(Location l) {
        LatLng p = new LatLng(l.getLatitude(), l.getLongitude());
        List<LatLng> points = myTrajectory.getPoints();
        points.add(p);
        myTrajectory.setPoints(points);
//        map.addMarker(new MarkerOptions().position(p));
//        if (map.getProjection().getVisibleRegion().latLngBounds.contains(p)) {
//            map.animateCamera(CameraUpdateFactory.newLatLng(p));
//        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        Log.w("LocationTracker", "onErrorResponse: Got error " + error.getMessage());
    }

    @Override
    public void onResponse(String response) {
    }

    void setMap(GoogleMap map) {
        this.map = map;
        createMyTrajectory(map);
    }

    private void createMyTrajectory(GoogleMap map) {
        PolylineOptions o = new PolylineOptions();
        o.clickable(false);
        o.zIndex(1);
        o.width(3);
        o.color(Color.BLUE);
        myTrajectory = map.addPolyline(o);
    }

    private void sendDataToServer() {
        final double[][] trafficData = new double[locationHistory.size()][];
        int i = 0;
        for (Location l : locationHistory) {
            trafficData[i++] = new double[]{l.getLatitude(), l.getLongitude(), l.getSpeed()};
        }
        String url = Configuration.SERVER_ADDRESS + "UploadMobileDataServlet";
        StringRequest request = new StringRequest(Request.Method.POST, url, this, this) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("data", new Gson().toJson(trafficData));
                return params;
            }
        };
        Log.i("LocationTracker", "sendDataToServer: About to send: " + new Gson().toJson(trafficData));
        queue.add(request);
    }
}
