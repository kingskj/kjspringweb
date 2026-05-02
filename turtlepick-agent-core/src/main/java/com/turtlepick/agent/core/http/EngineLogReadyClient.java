package com.turtlepick.agent.core.http;

import com.turtlepick.agent.core.config.AgentConfig;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class EngineLogReadyClient {

    private final LogReadyJsonCodec codec;

    public EngineLogReadyClient(LogReadyJsonCodec codec) {
        this.codec = codec;
    }

    public LogReadyResponse send(CompletedTraceFile file, AgentConfig config) {
        HttpURLConnection connection = null;
        try {
            String endpoint = normalizeBaseUrl(config.getEngineBaseUrl()) + "/api/agent/log-ready";
            connection = openConnection(new URL(endpoint), config.getEngineLogReadyTimeoutMs());
            writeBody(connection, codec.encodeRequest(file));

            int responseCode = connection.getResponseCode();
            String responseBody = readBody(responseCode >= 400
                    ? connection.getErrorStream()
                    : connection.getInputStream());

            if (responseCode != HttpURLConnection.HTTP_OK) {
                return LogReadyResponse.failure(responseCode, "HTTP_" + responseCode);
            }

            return codec.decodeResponse(responseBody);
        } catch (Exception e) {
            return LogReadyResponse.failure(0, "HTTP_ERROR:" + e.getClass().getSimpleName());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    HttpURLConnection openConnection(URL url, int timeoutMs) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(timeoutMs);
        connection.setReadTimeout(timeoutMs);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);
        connection.setUseCaches(false);
        return connection;
    }

    private void writeBody(HttpURLConnection connection, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        connection.setFixedLengthStreamingMode(bytes.length);

        OutputStream outputStream = null;
        try {
            outputStream = connection.getOutputStream();
            outputStream.write(bytes);
            outputStream.flush();
        } finally {
            closeQuietly(outputStream);
        }
    }

    private String readBody(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (!first) {
                    builder.append('\n');
                }
                builder.append(line);
                first = false;
            }
            return builder.toString();
        } finally {
            closeQuietly(reader);
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl != null && baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }

    private void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignore) {
            }
        }
    }
}
