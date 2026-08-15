package com.lazify.api;

import com.lazify.LazifyMod;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;

public class HttpUtil {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    /**
     * Performs an HTTP GET request and returns [JsonWrapper, httpCode].
     * On error returns [JsonWrapper(null), 500].
     */
    public static Object[] get(String urlStr, int timeout) {
        return get(urlStr, timeout, null);
    }

    public static Object[] get(String urlStr, int timeout, Map<String, String> extraHeaders) {
        return request("GET", urlStr, timeout, extraHeaders, null);
    }

    /**
     * Performs an HTTP POST with a UTF-8 body and returns [JsonWrapper, httpCode].
     */
    public static Object[] post(String urlStr, int timeout, Map<String, String> extraHeaders, String body) {
        return request("POST", urlStr, timeout, extraHeaders, body);
    }

    private static Object[] request(String method, String urlStr, int timeout,
                                    Map<String, String> extraHeaders, String body) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
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
            if (body != null) {
                conn.setDoOutput(true);
                if (conn.getRequestProperty("Content-Type") == null) {
                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                }
                byte[] bytes = body.getBytes(UTF8);
                conn.setFixedLengthStreamingMode(bytes.length);
                OutputStream os = conn.getOutputStream();
                try {
                    os.write(bytes);
                    os.flush();
                } finally {
                    os.close();
                }
            }

            int code = conn.getResponseCode();

            if (code >= 200 && code < 300) {
                return new Object[]{JsonWrapper.parse(readStream(conn.getInputStream())), code};
            } else {
                try {
                    return new Object[]{JsonWrapper.parse(readStream(conn.getErrorStream())), code};
                } catch (Exception ignored) {}
                return new Object[]{new JsonWrapper(null), code};
            }
        } catch (Exception e) {
            LazifyMod.LOGGER.warn("HttpUtil.{} error for {}: {}", method, urlStr, e.getMessage());
            return new Object[]{new JsonWrapper(null), 500};
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String readStream(java.io.InputStream in) throws Exception {
        if (in == null) return "";
        BufferedReader br = new BufferedReader(new InputStreamReader(in, UTF8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        return sb.toString();
    }
}
