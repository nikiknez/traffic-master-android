package rs.etf.kn153100m.master.model;

import java.util.ArrayList;
import java.util.List;

public class Camera {
    private String name;
    private String type;
    private String ipAddress;
    private String videoFileName;
    private String id;
    private List<String> streets;
    private Location location;

    public Camera(String id, String name, String type, String ipAddress, String videoFileName, Location location) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.ipAddress = ipAddress;
        this.videoFileName = videoFileName;
        this.location = location;
        this.streets = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getVideoFileName() {
        return videoFileName;
    }

    public void setVideoFileName(String videoFileName) {
        this.videoFileName = videoFileName;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getStreets() {
        return streets;
    }

    public void setStreets(List<String> streets) {
        this.streets = streets;
    }
    
    
    
}
