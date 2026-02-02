package app.owlcms.fly.flydata;

public class App implements Comparable<App> {

    // private static final Logger logger=LoggerFactory.getLogger(App.class);
    public AppType appType;
    public String name;
    public boolean created;
    public String regionCode;
    private VersionInfo versionInfo;
    public boolean stopped;
    public String machine;

    public App(String s, AppType appType, String region, String version, String machine, String status) {
        this.name = s;
        this.appType = appType;
        this.regionCode = region;
        if (appType != null && appType.releaseApiUrl != null && !appType.releaseApiUrl.isBlank()) {
            this.versionInfo = new VersionInfo(version, appType.releaseApiUrl);
        } else {
            this.versionInfo = new VersionInfo(version);
        }
        this.machine = machine;
        if (status == null) {
            this.stopped = true;
        } else {
            String normalizedStatus = status.trim().toLowerCase();
            this.stopped = normalizedStatus.equals("stopped") || normalizedStatus.equals("suspended");
        }
    }

    @Override
    public int compareTo(App o) {
        return this.appType.compareTo(o.appType);
    }

    @Override
    public String toString() {
        return "App [appType=" + appType + ", name=" + name + ", regionCode=" + regionCode + ", versionInfo="
                + versionInfo.getCurrentVersionString()+"/"+versionInfo.getReferenceVersionString() + ", stopped=" + stopped + ", machine=" + machine + "]";
    }

    public String getCurrentVersion() {
        return versionInfo.getCurrentVersionString();
    }

    public String getReferenceVersion() {
        return versionInfo.getReferenceVersionString();
    }

    public boolean isUpdateRequired() {
        return versionInfo == null || versionInfo.getComparison() < 0;
    }

    public VersionInfo getVersionInfo() {
        return versionInfo;
    }

    public void setVersionInfo(VersionInfo versionInfo) {
        this.versionInfo = versionInfo;
    }

}
