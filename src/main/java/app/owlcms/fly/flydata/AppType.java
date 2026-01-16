package app.owlcms.fly.flydata;

import java.util.HashMap;
import java.util.Map;

public enum AppType {
    OWLCMS("owlcms/owlcms", "scripts/createOwlcms.sh", "https://api.github.com/repos/owlcms/owlcms4/releases"),
    PUBLICRESULTS("owlcms/publicresults", "scripts/createPublicResults.sh",
            "https://api.github.com/repos/owlcms/owlcms4/releases"),
    TRACKER("owlcms/tracker", "scripts/createTracker.sh", "https://api.github.com/repos/owlcms/owlcms-tracker/releases"),
    DB("flyio/postgres-flex", null, null);

    public final String image;
    public final String create;
    public final String releaseApiUrl;

    private static final Map<String, AppType> BY_IMAGE = new HashMap<>();
    static {
        for (AppType e : values()) {
            BY_IMAGE.put(e.image, e);
        }
    }

    private AppType(String image, String create, String releaseApiUrl) {
        this.image = image;
        this.create = create;
        this.releaseApiUrl = releaseApiUrl;
    }

    public static AppType byImage(String image) {
        return BY_IMAGE.get(image);
    }
}
