package rs.etf.kn153100m.master.model;

public class Street {
    private Location[] path;
    private String owner;
    private String id;
    private String infoText;
    private String validFrom;
    private String validTo;
    private int intensity;

    public Street(String id, Location[] path, String owner, String infoText, String validFrom, String validTo, int intensity) {
        this.id = id;
        this.path = path;
        this.owner = owner;
        this.infoText = infoText;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.intensity = intensity;
    }

    public Street(Location[] path) {
        this.path = path;
    }

    public Location[] getPath() {
        return path;
    }

    public void setPath(Location[] path) {
        this.path = path;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getInfoText() {
        return infoText;
    }

    public void setInfoText(String infoText) {
        this.infoText = infoText;
    }

    public String getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(String validFrom) {
        this.validFrom = validFrom;
    }

    public String getValidTo() {
        return validTo;
    }

    public void setValidTo(String validTo) {
        this.validTo = validTo;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getIntensity() {
        return intensity;
    }

    public void setIntensity(int intensity) {
        this.intensity = intensity;
    }
}
