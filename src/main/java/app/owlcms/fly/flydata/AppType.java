package app.owlcms.fly.flydata;

import java.util.HashMap;
import java.util.Map;

public enum AppType {
    OWLCMS("owlcms/owlcms", "scripts/createOwlcms.sh", "https://api.github.com/repos/owlcms/owlcms4/releases", "owlcms.toml"),
    PUBLICRESULTS("owlcms/publicresults", "scripts/createPublicResults.sh",
            "https://api.github.com/repos/owlcms/owlcms4/releases", "publicresults.toml"),
    TRACKER("owlcms/tracker", "scripts/createTracker.sh", "https://api.github.com/repos/owlcms/owlcms-tracker/releases", "tracker.toml"),
    DB("flyio/postgres-flex", null, null, null);

    public final String image;
    public final String create;
    public final String releaseApiUrl;
    public final String configFile;

    private static final Map<String, AppType> BY_IMAGE = new HashMap<>();
    static {
        for (AppType e : values()) {
            BY_IMAGE.put(e.image, e);
        }
    }

    private AppType(String image, String create, String releaseApiUrl, String configFile) {
        this.image = image;
        this.create = create;
        this.releaseApiUrl = releaseApiUrl;
        this.configFile = configFile;
    }

    public String getConfigFile() {
        // toml files are at /app in the Docker container, or user.dir locally
        String projectRoot = System.getProperty("user.dir", "/app");
        if (!projectRoot.startsWith("/app") && new java.io.File("/app/" + configFile).exists()) {
            projectRoot = "/app";
        }
        return projectRoot + "/" + configFile;
    }

    public static AppType byImage(String image) {
        if (image == null) {
            return null;
        }
        AppType exact = BY_IMAGE.get(image);
        if (exact != null) {
            return exact;
        }
        String normalized = image.toLowerCase();
        // Check for tracker/fhq first (more specific)
        if (normalized.contains("tracker") || normalized.contains("fhq")) {
            return TRACKER;
        }
        // Check for publicresults before owlcms (publicresults contains "owlcms" prefix)
        if (normalized.contains("publicresults")) {
            return PUBLICRESULTS;
        }
        // Check for owlcms (most common)
        if (normalized.contains("owlcms")) {
            return OWLCMS;
        }
        // Check for postgres
        if (normalized.contains("postgres")) {
            return DB;
        }
        return null;
    }
}
