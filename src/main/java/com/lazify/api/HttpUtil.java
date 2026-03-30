package com.lazify.api;

import com.lazify.LazifyMod;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class HttpUtil {

    /**
     * Performs an HTTP GET request and returns [JsonWrapper, httpCode].
     * On error returns [JsonWrapper(null), 500].
     */
    public static Object[] get(String urlStr, int timeout) {
        return get(urlStr, timeout, null);
    }

    public static Object[] get(String urlStr, int timeout, Map<String, String> extraHeaders) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(timeout);
            conn.setReadTimeout(timeout);
            conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            conn.setRequestProperty("Accept", "application/json");
            if (extraHeaders != null) {
                for (Map.Entry<String, String> entry : extraHeaders.entrySet()) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            int code = conn.getResponseCode();

            if (code >= 200 && code < 300) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                br.close();
                return new Object[]{JsonWrapper.parse(sb.toString()), code};
            } else {
                // Try to read error stream
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    br.close();
                    return new Object[]{JsonWrapper.parse(sb.toString()), code};
                } catch (Exception ignored) {}
                return new Object[]{new JsonWrapper(null), code};
            }
        } catch (Exception e) {
            LazifyMod.LOGGER.warn("HttpUtil.get error for {}: {}", urlStr, e.getMessage());
            return new Object[]{new JsonWrapper(null), 500};
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
