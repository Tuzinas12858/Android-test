package com.example.appsas;

import android.util.Log;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class GoogleSheetsLogger {

    private static final String TAG = "GoogleSheetsLogger";

    private static final String API_URL = "https://script.google.com/macros/s/AKfycbyevzwHCHeuJ6tbVornjKwJ1CO_o706O8GUSmU-kiMGIazPcuawZ8gKN6QBcdM29IjqmA/exec";

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    
    public static void logRender(String audioTitle, String videoFileName) {

        final String jsonInputString = String.format(
                "{\"audioTitle\": \"%s\", \"videoFileName\": \"%s\", \"timestamp\": %d}",
                escapeJson(audioTitle),
                escapeJson(videoFileName),
                System.currentTimeMillis()
        );

        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(API_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; utf-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);

                // Siųsti JSON duomenis
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.i(TAG, "Render logged successfully: " + audioTitle);
                } else {
                    Log.e(TAG, "Failed to log render. Response Code: " + responseCode);
                }

            } catch (Exception e) {
                Log.e(TAG, "Network error during logging: " + e.getMessage(), e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }
    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"");
    }
}