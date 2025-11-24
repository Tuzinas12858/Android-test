package com.example.appsas;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.antonkarpenko.ffmpegkit.FFmpegKit;
import com.antonkarpenko.ffmpegkit.ReturnCode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class FfmpegExecutor {

    private static final String TAG = "FFMPEG_EXECUTOR";

    /**
     * Executes the FFmpeg command to combine an image and audio file into a video.
     * @param context Application context, used for file access and toasts.
     * @param imageUri Content URI of the input image.
     * @param audioUri Content URI of the input audio.
     */
    public static void executeVideoCommand(Context context, Uri imageUri, Uri audioUri) {

        Toast.makeText(context, "Video combination process started...", Toast.LENGTH_SHORT).show();

        // --- 1. PREPARE FILES ---
        String imagePath = getReadableFilePath(context, imageUri, ".jpg");
        String audioPath = getReadableFilePath(context, audioUri, ".mp3");
        File outputFile = getOutputVideoFile(context);
        String outputPath = outputFile.getAbsolutePath();

        if (imagePath == null || audioPath == null) {
            Toast.makeText(context, "Error: Could not prepare input files. Check Logcat.", Toast.LENGTH_LONG).show();
            return;
        }

        Log.d(TAG, "Input Image Path: " + imagePath);
        Log.d(TAG, "Input Audio Path: " + audioPath);
        Log.d(TAG, "Output Video Path: " + outputPath);

        // --- 2. CONSTRUCT COMMAND (FILTER REMOVED for COLOR) ---
        String command = String.format(
                // The black/white filter has been removed.
                // Command now loops image, adds audio, encodes, and stops at audio end.
                "-y -loop 1 -i %s -i %s -c:v libx264 -pix_fmt yuv420p -shortest %s",
                imagePath, audioPath, outputPath
        );

        // --- 3. EXECUTE FFmpeg ---
        FFmpegKit.executeAsync(command, session -> {

            // --- 4. CLEANUP & FEEDBACK (Runs on completion) ---

            // Cleanup: Delete the temporary files created from the URIs
            new File(imagePath).delete();
            new File(audioPath).delete();

            ReturnCode returnCode = session.getReturnCode();

            if (ReturnCode.isSuccess(returnCode)) {
                Toast.makeText(context, "✅ Video created successfully! Saved to: " + outputFile.getName(), Toast.LENGTH_LONG).show();
            } else if (ReturnCode.isCancel(returnCode)) {
                Toast.makeText(context, "Video creation cancelled.", Toast.LENGTH_LONG).show();
            } else {
                String output = session.getAllLogsAsString();
                Log.e(TAG, "FFmpeg Command failed. Return Code: " + returnCode);
                Log.e(TAG, "FFmpeg Output: " + output);
                Toast.makeText(context, "❌ Video creation failed! Check Logcat for details.", Toast.LENGTH_LONG).show();
            }
        });
    }

    // --- Helper methods remain the same ---

    private static String getReadableFilePath(Context context, Uri uri, String preferredExtension) {
        if (uri == null) return null;

        String fileName = "temp_" + System.currentTimeMillis() + preferredExtension;
        File tempFile = new File(context.getCacheDir(), fileName);

        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             OutputStream outputStream = new FileOutputStream(tempFile)) {

            if (inputStream == null) {
                Log.e(TAG, "Input stream is null for URI: " + uri);
                return null;
            }

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            return tempFile.getAbsolutePath();

        } catch (Exception e) {
            Log.e(TAG, "Error copying Uri to file. Check persistent permission grant.", e);
            return null;
        }
    }

    private static File getOutputVideoFile(Context context) {
        File moviesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        if (moviesDir == null) {
            moviesDir = context.getCacheDir();
        }
        if (!moviesDir.exists()) {
            moviesDir.mkdirs();
        }
        return new File(moviesDir, "combined_video_" + System.currentTimeMillis() + ".mp4");
    }
}