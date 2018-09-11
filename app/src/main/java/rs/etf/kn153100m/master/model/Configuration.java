package rs.etf.kn153100m.master.model;

import java.util.ArrayList;
import java.util.List;

public class Configuration {
    private List<Camera> cameras = new ArrayList<>();
    private List<Street> streets = new ArrayList<>();
    private List<Mark> marks = new ArrayList<>();
    private List<MapView> mapViews = new ArrayList<>();

    public static final String SERVER_ADDRESS = "http://10.0.2.2:8080/Master/";
//    public static final String SERVER_ADDRESS = "http://192.168.43.155:8080/Master/";
//    public static final String SERVER_ADDRESS = "http://trafficmaster.ddns.net:9999/Master/";

    private static Configuration config = new Configuration();

    public static Configuration getConfig() {
        return config;
    }

    private Configuration() {
        config = this;
    }

    public List<Camera> getCameras() {
        return cameras;
    }

    public List<Street> getStreets() {
        return streets;
    }

    public List<Mark> getMarks() {
        return marks;
    }

    public List<MapView> getMapViews() {
        return mapViews;
    }


    public class IntensityColorMap {
        public int lowIntensityColor;
        public int lowIntensityLevel;
        public int midIntensityColor;
        public int midIntensityLevel;
        public int highIntensityColor;

        private IntensityColorMap() {
            lowIntensityColor = 0xFFF23C32;
            lowIntensityLevel = 5;
            midIntensityColor = 0xFFFF974D;
            midIntensityLevel = 10;
            highIntensityColor = 0XFF63D668;
        }
    }

    public IntensityColorMap intensityColorMap = new IntensityColorMap();
}
