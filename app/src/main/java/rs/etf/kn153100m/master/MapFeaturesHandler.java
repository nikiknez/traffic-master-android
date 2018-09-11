package rs.etf.kn153100m.master;

import android.app.Activity;
import android.graphics.Color;
import android.support.v7.widget.AppCompatSpinner;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;
import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.RoadsApi;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.SnappedPoint;

import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import rs.etf.kn153100m.master.model.Camera;
import rs.etf.kn153100m.master.model.Configuration;
import rs.etf.kn153100m.master.model.Location;
import rs.etf.kn153100m.master.model.MapView;
import rs.etf.kn153100m.master.model.Mark;
import rs.etf.kn153100m.master.model.Street;

public class MapFeaturesHandler implements
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnPolylineClickListener,
        GoogleMap.OnInfoWindowCloseListener,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnInfoWindowClickListener,
        PendingResult.Callback<DirectionsResult> {

    private Activity context;
    private GoogleMap mMap;
    private GeoApiContext geoApiContext;
    private ImageView camImageView;

    private Timer updateCamImageTimer;

    //                  id, polyLine
    private HashMap<String, Polyline> streets;

    private HashMap<Polyline, Marker> polylineMarkerMap;

    //                  id, mark
    private HashMap<String, Marker> marks;

    //                  id, cam
    private HashMap<String, Marker> cameras;

    private Marker navigationMarker;
    private Polyline navigationPolyline;

    private static final int GOOGLE_SOURCE = 0x1;
    private static final int CAM_SOURCE = 0x2;
    private static final int MOBILE_SOURCE = 0x4;
    private static final int USER_SOURCE = 0x8;
    private static final int ALL_SOURCES = 0xF;

    private int selectedSource = ALL_SOURCES;

    private static final List<PatternItem> INFO_STREET_PATTERN =
            Arrays.asList(new Dash(20), new Gap(20));

    public MapFeaturesHandler(GoogleMap map, Activity context) {
        this.mMap = map;
        this.context = context;
        streets = new HashMap<>();
        marks = new HashMap<>();
        cameras = new HashMap<>();
        polylineMarkerMap = new HashMap<>();
        mMap.setOnMarkerClickListener(this);
        mMap.setOnPolylineClickListener(this);
        mMap.setOnMapClickListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.setOnInfoWindowCloseListener(this);
        mMap.setOnInfoWindowClickListener(this);

        mMap.getUiSettings().setMapToolbarEnabled(false);

        String apiKey = context.getResources().getString(R.string.google_maps_key);
        geoApiContext = new GeoApiContext.Builder().apiKey(apiKey).build();

        final View infoWindow = context.getLayoutInflater().inflate(R.layout.info_window, null);
        map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                Log.i("MapFeaturesHandler", "getInfoContents: " + marker.getTitle());
                TextView titleUi = ((TextView) infoWindow.findViewById(R.id.camName));
                titleUi.setText(marker.getTitle());
                return infoWindow;
            }
        });
        camImageView = (ImageView) infoWindow.findViewById(R.id.camImageView);
        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
            }

            @Override
            public void onMarkerDrag(Marker marker) {
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
            }
        });

        navigationMarker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(0, 0)).visible(false).title("Putanja dovde").draggable(true));
        navigationPolyline = mMap.addPolyline(new PolylineOptions().color(Color.BLUE));
        updateCamImageTimer = new Timer();
        initDataSourceSelect();

//        loadDefaultConfig();
    }

    public void loadDefaultConfig() {
        InputStreamReader isr = new InputStreamReader(context.getResources().openRawResource(R.raw.config));
        Configuration c = new Gson().fromJson(isr, Configuration.class);
        updateConfig(c);
    }

    public void updateConfig(Configuration c) {
        loadStreets(c.getStreets());
        loadMarks(c.getMarks());
        loadCameras(c.getCameras());
        loadMapViews(c.getMapViews());
    }

    private void loadMarks(List<Mark> newMarks) {
        HashSet<String> newMarksSet = new HashSet<>();
        for (Mark m : newMarks) {
            newMarksSet.add(m.getId());
            if (marks.get(m.getId()) == null) {
                marks.put(m.getId(), mMap.addMarker(new MarkerOptions()
                        .title(m.getInfo())
                        .position(new LatLng(m.getLocation().getLat(), m.getLocation().getLng()))
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.warning))));
                marks.get(m.getId()).setTag(m.getOwner());
            }
        }
        for (String m : marks.keySet()) {
            if (!newMarksSet.contains(m)) {
                marks.remove(m).remove();
            }
        }
    }

    private void loadStreets(List<Street> newStreets) {
        for (Street s : newStreets) {
            if (streets.get(s.getId()) == null) {
                PolylineOptions po = new PolylineOptions();
                for (Location l : s.getPath()) {
                    po.add(new LatLng(l.getLat(), l.getLng()));
                }
                po.width(5);
                po.zIndex(1);
                po.visible(shouldBeVisible(s.getOwner()));

                Polyline polyline = mMap.addPolyline(po);

                if (s.getInfoText() != null) {
                    createPolylineMarker(polyline, s.getInfoText());
                    polyline.setClickable(true);
                    polyline.setPattern(INFO_STREET_PATTERN);
                    polyline.setColor(Color.RED);
                }
                polyline.setTag(s);
                streets.put(s.getId(), polyline);
            } else {
                Marker m = polylineMarkerMap.get(streets.get(s.getId()));
                if (m != null) {
                    m.setTitle(s.getInfoText());
                }
            }
        }
    }

    private void loadCameras(List<Camera> newCameras) {
        for (Camera c : newCameras) {
            if (cameras.get(c.getId()) == null) {
                Location l = c.getLocation();
                MarkerOptions mo = new MarkerOptions();
                mo.position(new LatLng(l.getLat(), l.getLng()));
                mo.icon(BitmapDescriptorFactory.fromResource(R.drawable.camera));
                mo.title(c.getName());
                mo.zIndex(2);
                Marker marker = mMap.addMarker(mo);
                marker.setTag(c);
                cameras.put(c.getId(), marker);
            }
        }
    }

    public void onNewTrafficData(List<Street> streets) {
        for (Street s : streets) {
            Polyline streetPolyline = this.streets.get(s.getId());
            if (streetPolyline != null) {
                streetPolyline.setColor(intensityToColorMap(s.getIntensity()));
                if (s.getPath() != null) {
                    streetPolyline.setPoints(toGms(s.getPath()));
                }
            } else if (s.getPath() != null) {
                PolylineOptions po = new PolylineOptions();
                po.addAll(toGms(s.getPath()));
                po.width(3);
                po.zIndex(1);
                po.visible(shouldBeVisible(s.getOwner()));
                po.color(intensityToColorMap(s.getIntensity()));
                Polyline polyline = mMap.addPolyline(po);
                polyline.setTag(s);
                this.streets.put(s.getId(), polyline);
            }
        }
    }

    private static int intensityToColorMap(int intensity) {
        if (intensity < Configuration.getConfig().intensityColorMap.lowIntensityLevel) {
            return Configuration.getConfig().intensityColorMap.lowIntensityColor;
        }
        if (intensity < Configuration.getConfig().intensityColorMap.midIntensityLevel) {
            return Configuration.getConfig().intensityColorMap.midIntensityColor;
        }
        return Configuration.getConfig().intensityColorMap.highIntensityColor;
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        Log.i("MapFeaturesHandler", "onMarkerClick: ");
        updateCamImageTimer.cancel();
        camImageView.setVisibility(View.GONE);
        if (!(marker.getTag() instanceof Camera)) {
            return false;
        }
        Camera c = (Camera) marker.getTag();
        if (c != null) {
            startUpdatingCamImage(c, marker);
        }
        return false;
    }

    @Override
    public void onInfoWindowClose(Marker marker) {
        Log.i("MapFeaturesHandler", "onInfoWindowClose: ");
        updateCamImageTimer.cancel();
        if (navigationMarker.getTitle().equals("Putanja dovde")) {
            navigationMarker.setVisible(false);
        }
    }

    @Override
    public void onPolylineClick(Polyline polyline) {
        Log.i("MapFeaturesHandler", "onPolylineClick: ");
        Marker marker = polylineMarkerMap.get(polyline);
        if (marker == null) {
            return;
        }
        camImageView.setVisibility(View.GONE);
        marker.showInfoWindow();
        mMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
    }

    private void createPolylineMarker(Polyline polyline, String text) {
        MarkerOptions mo = new MarkerOptions();
        mo.title(text);
        LatLng p = getCenterPoint(polyline);
        mo.position(p);
        mo.icon(BitmapDescriptorFactory.fromResource(R.drawable.transparent));
        mo.anchor(0.5f, 0.5f);
        Marker marker = mMap.addMarker(mo);
        polylineMarkerMap.put(polyline, marker);
    }

    private LatLng getCenterPoint(Polyline polyline) {
        List<LatLng> points = polyline.getPoints();
        int n = points.size();
        LatLng p;
        if (n % 2 == 0) {
            int i1 = n / 2 - 1;
            int i2 = n / 2;
            LatLng p1 = points.get(i1);
            LatLng p2 = points.get(i2);
            p = new LatLng((p1.latitude + p2.latitude) / 2, (p1.longitude + p2.longitude) / 2);
        } else {
            int i = n / 2;
            p = points.get(i);
        }
        return p;
    }

    public void stop() {
        updateCamImageTimer.cancel();
    }

    private void loadMapViews(final List<MapView> mapViews) {
        AppCompatSpinner viewsSpinner = (AppCompatSpinner) context.findViewById(R.id.viewSpinner);
        ArrayAdapter<String> viewsAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item);
        viewsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        viewsAdapter.add("Centriraj mapu");
        for (MapView mapView : mapViews) {
            viewsAdapter.add(mapView.getName());
        }
        viewsSpinner.setAdapter(viewsAdapter);

        viewsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position <= mapViews.size() && position > 0) {
                    MapView mapView = mapViews.get(position - 1);
                    LatLng p = new LatLng(mapView.getLat(), mapView.getLng());
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(p, (float) mapView.getZoom()));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void initDataSourceSelect() {
        AppCompatSpinner dataSourceSpinner = (AppCompatSpinner) context.findViewById(R.id.dataSourceSpinner);
        ArrayAdapter<CharSequence> sourcesAdapter = ArrayAdapter.createFromResource(context,
                R.array.data_sources_array, android.R.layout.simple_spinner_dropdown_item);
        sourcesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        dataSourceSpinner.setAdapter(sourcesAdapter);
        dataSourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    selectedSource = ALL_SOURCES;
                } else {
                    selectedSource = 1 << (position - 1);
                }
                Log.i("MapFeaturesHandler", "onItemSelected: position = " + position);
                changeStreetsVisibility();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void changeStreetsVisibility() {
        if (selectedSource == GOOGLE_SOURCE) {
            for (Polyline p : streets.values()) {
                p.setVisible(false);
            }
            mMap.setTrafficEnabled(true);
        } else {
            mMap.setTrafficEnabled(false);
            for (Polyline p : streets.values()) {
                Street s = (Street) p.getTag();
                p.setVisible(shouldBeVisible(s.getOwner()));
            }
            for (String id : marks.keySet()) {
                String owner = (String) marks.get(id).getTag();
                marks.get(id).setVisible(shouldBeVisible(owner));
            }
        }
    }

    private boolean shouldBeVisible(String owner) {
        if (selectedSource == ALL_SOURCES) {
            return true;
        }
        if ("admin".equals(owner)) {
            return (selectedSource & CAM_SOURCE) > 0;
        }
        if ("mobile".equals(owner)) {
            return (selectedSource & MOBILE_SOURCE) > 0;
        }
        return (selectedSource & USER_SOURCE) > 0;
    }

    @Override
    public void onMapClick(LatLng latLng) {
        Log.i("MapFeaturesHandler", "onMapClick: ");
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        camImageView.setVisibility(View.GONE);
        navigationMarker.hideInfoWindow();
        navigationMarker.setPosition(latLng);
        navigationMarker.setTitle("Putanja dovde");
        navigationMarker.setVisible(true);
        navigationMarker.showInfoWindow();
        navigationMarker.setDraggable(true);

        navigationPolyline.setVisible(false);
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        if (marker.getId().equals(navigationMarker.getId()) && "Putanja dovde".equals(marker.getTitle())) {
            Log.i("MapFeaturesHandler", "onInfoWindowClick: in if");
            com.google.maps.model.LatLng destination = new com.google.maps.model.LatLng(
                    navigationMarker.getPosition().latitude, navigationMarker.getPosition().longitude);
            com.google.maps.model.LatLng origin = new com.google.maps.model.LatLng(
                    mMap.getMyLocation().getLatitude(), mMap.getMyLocation().getLongitude());
            DirectionsApiRequest request = DirectionsApi.newRequest(geoApiContext).origin(origin).destination(destination);
            request.setCallback(this);

            marker.hideInfoWindow();
        }
    }

    @Override
    public void onResult(final DirectionsResult results) {
        Log.i("MapFeaturesHandler", "onResult: summary: " + getEndLocationTitle(results));
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                navigationMarker.setTitle(getEndLocationTitle(results));
                navigationMarker.setPosition(toGms(results.routes[0].legs[0].endLocation));
                navigationMarker.setVisible(true);
                navigationMarker.showInfoWindow();
                navigationMarker.setDraggable(false);

                navigationPolyline.setPoints(toGms(results.routes[0].overviewPolyline.decodePath()));
                navigationPolyline.setVisible(true);

                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(new LatLngBounds(
                        toGms(results.routes[0].bounds.southwest), toGms(results.routes[0].bounds.northeast)), 10));
            }
        });

        final List<String> streetIds = getStreetIds(results.routes[0].overviewPolyline.decodePath());
        boolean noDataForPath = Collections.disjoint(streetIds, streets.keySet());
        Log.i("MapFeaturesHandler", "onResult: has data for path = " + !noDataForPath);

        streetIds.retainAll(streets.keySet());


        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int slowStreetsLength = 0;
                for (String streetId : streetIds) {
                    Polyline pLine = streets.get(streetId);
                    Street s = (Street) pLine.getTag();
                    if (s != null && s.getIntensity() < 5) {
                        slowStreetsLength += getPathLength(s.getPath());
                    }
                }
                if (slowStreetsLength > 5) {
                    navigationMarker.hideInfoWindow();
                    navigationMarker.setTitle(navigationMarker.getTitle() +
                            "\nRuta prolazi kroz " + slowStreetsLength + " metara usporenog saobracaja");
                    navigationMarker.showInfoWindow();
                }
            }
        });
    }

    private int getPathLength(Location[] points) {
        if (points == null || points.length < 2) {
            return 0;
        }
        float[] r = new float[1];
        android.location.Location.distanceBetween(points[0].getLat(), points[0].getLng(),
                points[points.length - 1].getLat(), points[points.length - 1].getLng(), r);
        return (int) r[0];
    }

    private List<String> getStreetIds(List<com.google.maps.model.LatLng> locations) {
        if (locations.size() > 100) {
            locations = locations.subList(0, 100);
        }

        SnappedPoint[] snappedPoints = RoadsApi.snapToRoads(geoApiContext, true,
                locations.toArray(new com.google.maps.model.LatLng[0])).awaitIgnoreError();
        List<String> ids = new LinkedList<>();
        String lastPlaceId = "";
        if (snappedPoints != null) {
            for (SnappedPoint sp : snappedPoints) {
                if (!lastPlaceId.equals(sp.placeId)) {
                    ids.add(lastPlaceId);
                    lastPlaceId = sp.placeId;
                }
            }
        }
        return ids;
    }

    private List<LatLng> toGms(List<com.google.maps.model.LatLng> locations) {
        List<LatLng> list = new LinkedList<>();
        for (com.google.maps.model.LatLng l : locations) {
            list.add(new LatLng(l.lat, l.lng));
        }
        return list;
    }

    private List<LatLng> toGms(Location[] locations) {
        List<LatLng> list = new LinkedList<>();
        for (Location l : locations) {
            list.add(new LatLng(l.getLat(), l.getLng()));
        }
        return list;
    }

    private LatLng toGms(com.google.maps.model.LatLng l) {
        return new LatLng(l.lat, l.lng);
    }

    private String getEndLocationTitle(DirectionsResult results) {
        return "Vreme: " + results.routes[0].legs[0].duration.humanReadable +
                " Razdaljina: " + results.routes[0].legs[0].distance.humanReadable;
    }

    @Override
    public void onFailure(Throwable e) {
        Log.i("MapFeaturesHandler", "onFailure: " + e.getMessage());
    }

    private void startUpdatingCamImage(Camera c, final Marker marker) {
        final String url;
        if (c.getType().equals("file")) {
            url = Configuration.SERVER_ADDRESS + "GetVideoFrameServlet?video=" + c.getVideoFileName();
            new UpdateCamImageTask(camImageView, marker).execute(url);
        } else {
            url = c.getIpAddress();
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    new UpdateCamImageTask(camImageView, marker).execute(url);
                }
            };
            updateCamImageTimer = new Timer();
            updateCamImageTimer.schedule(task, 0, 500);
        }
        camImageView.setVisibility(View.VISIBLE);
    }
}
