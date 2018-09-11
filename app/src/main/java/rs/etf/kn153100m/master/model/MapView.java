package rs.etf.kn153100m.master.model;

public class MapView {
    private double lat;
    private double lng;
    private double zoom;
    private String name;

    public MapView(double lat, double lng, double zoom, String name) {
        this.lat = lat;
        this.lng = lng;
        this.zoom = zoom;
        this.name = name;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public double getZoom() {
        return zoom;
    }

    public void setZoom(double zoom) {
        this.zoom = zoom;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    
}
