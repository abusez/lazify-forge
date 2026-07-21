package com.lazify.api;

import java.io.InputStream;
import java.util.Properties;

public class ApiCredentials {

    private static String url = "";
    private static String key = "";

    static {
        try {
            InputStream is = ApiCredentials.class.getResourceAsStream("/api.properties");
            if (is != null) {
                Properties p = new Properties();
                p.load(is);
                is.close();
                url = p.getProperty("api.url", "");
                key = p.getProperty("api.key", "");
            }
        } catch (Exception ignored) {}
    }

    public static String getUrl() { return url; }
    public static String getKey() { return key; }
}
