package com.investment.analysis.provider;

import com.investment.analysis.common.IndexDataException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;

/**
 * 轻量 HTTP GET 工具：无第三方依赖，支持超时、重试、gzip。
 */
public final class HttpSupport {

    private static final String DEFAULT_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    private HttpSupport() {
    }

    /**
     * 带重试的 GET 请求，返回响应文本（UTF-8）。
     *
     * @param url        请求地址
     * @param referer    防盗链 Referer，可为 null
     * @param timeoutMs  超时毫秒
     * @param retryTimes 重试次数（总请求次数 = retryTimes）
     * @param retryGapMs 重试间隔毫秒
     */
    public static String get(String url, String referer, int timeoutMs, int retryTimes, long retryGapMs) {
        IOException lastError = null;
        int attempts = Math.max(1, retryTimes);
        for (int i = 1; i <= attempts; i++) {
            try {
                return doGet(url, referer, timeoutMs);
            } catch (IOException e) {
                lastError = e;
                if (i < attempts) {
                    sleep(retryGapMs);
                }
            }
        }
        throw new IndexDataException("请求行情接口失败：" + url + "，原因：" + describe(lastError), lastError);
    }

    private static String doGet(String url, String referer, int timeoutMs) throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(timeoutMs);
            connection.setReadTimeout(timeoutMs);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", DEFAULT_UA);
            connection.setRequestProperty("Accept", "*/*");
            connection.setRequestProperty("Accept-Encoding", "gzip,deflate");
            connection.setRequestProperty("Connection", "close");
            if (referer != null) {
                connection.setRequestProperty("Referer", referer);
            }
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IOException("HTTP " + status);
            }
            String encoding = connection.getContentEncoding();
            InputStream stream = connection.getInputStream();
            if (encoding != null && encoding.toLowerCase().contains("gzip")) {
                stream = new GZIPInputStream(stream);
            }
            return readAll(stream);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String readAll(InputStream stream) throws IOException {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream(64 * 1024);
            byte[] chunk = new byte[8192];
            int read;
            while ((read = stream.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return new String(buffer.toByteArray(), "UTF-8");
        } finally {
            try {
                stream.close();
            } catch (IOException ignored) {
                // 关闭失败不影响主流程
            }
        }
    }

    private static void sleep(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String describe(IOException error) {
        if (error == null) {
            return "未知错误";
        }
        String message = error.getMessage();
        return error.getClass().getSimpleName() + (message == null ? "" : " - " + message);
    }
}
