package com.example.appsas;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.Toast;
import com.example.appsas.R;
import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.ReturnCode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;



public class FfmpegExecutor {

    private static final String TAG = "FfmpegExecutor";


    public static String getFileNameFromUri(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        if (result != null) {
            int dotIndex = result.lastIndexOf('.');
            if (dotIndex > 0) {
                result = result.substring(0, dotIndex);
            }
            result = result.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        }
        return (result != null && !result.trim().isEmpty()) ? result : "Render";
    }

    
    private static File copyUriToFile(Context context, Uri uri, String filePrefix, String fileExtension) throws Exception {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            throw new Exception("Negalima atidaryti įvesties srauto URI: " + uri.toString());
        }

        File tempFile = new File(context.getExternalFilesDir(null), filePrefix + System.currentTimeMillis() + fileExtension);
        try (OutputStream outputStream = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        } finally {
            inputStream.close();
        }
        return tempFile;
    }

    
    private static File copyResourceToFile(Context context, int resourceId, String filePrefix) throws Exception {
        File tempFile = new File(context.getExternalFilesDir(null), filePrefix + System.currentTimeMillis() + ".png");

        try (InputStream is = context.getResources().openRawResource(resourceId);
             OutputStream os = new FileOutputStream(tempFile)) {

            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } catch (Resources.NotFoundException e) {
            throw new Exception("Logo resursas nerastas! Patikrinkite ID: " + resourceId, e);
        }

        return tempFile;
    }


    
    public static void executeVideoCommand(Context context, Uri imageUri, Uri audioUri, String audioTitle, FfmpegListener listener) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        if (listener != null) {
            listener.onFfmpegStart();
        }

        new Thread(() -> {

            File imageFile = null;
            File audioFile = null;
            File logoFile = null;
            File processedImageFile = null;
            File outputFile = null;

            try {
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                String safeAudioTitle = (audioTitle != null && !audioTitle.isEmpty()) ? audioTitle : "Render";

                File outputDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
                if (outputDir != null && !outputDir.exists()) {
                    outputDir.mkdirs();
                }
                outputFile = new File(outputDir, "APPSAS_" + safeAudioTitle + "_" + timeStamp + ".mp4");

                mainHandler.post(() -> Toast.makeText(context, "Kopijuojami failai į laikiną saugyklą...", Toast.LENGTH_SHORT).show());

                String imageExtension = "jpg";
                String mimeType = context.getContentResolver().getType(imageUri);
                if (mimeType != null && mimeType.contains("/")) {
                    imageExtension = mimeType.substring(mimeType.lastIndexOf("/") + 1);
                }
                imageFile = copyUriToFile(context, imageUri, "input_image_", "." + imageExtension);

                String audioExtension = "mp3";
                String audioMimeType = context.getContentResolver().getType(audioUri);
                if (audioMimeType != null && audioMimeType.contains("/")) {
                    audioExtension = audioMimeType.substring(audioMimeType.lastIndexOf("/") + 1);
                }
                audioFile = copyUriToFile(context, audioUri, "input_audio_", "." + audioExtension);

                logoFile = copyResourceToFile(context, R.mipmap.rqem, "app_logo_");
                processedImageFile = new File(context.getExternalFilesDir(null), "processed_image_" + System.currentTimeMillis() + ".png");

            } catch (Exception e) {
                String error = "Klaida kopijuojant failus: " + e.getMessage();
                Log.e(TAG, error);
                mainHandler.post(() -> Toast.makeText(context, error, Toast.LENGTH_LONG).show());
                if (listener != null) {
                    listener.onFfmpegFailure(error);
                }
                if (imageFile != null) imageFile.delete();
                if (audioFile != null) audioFile.delete();
                if (logoFile != null) logoFile.delete();
                return;
            }

            mainHandler.post(() -> Toast.makeText(context, "Pradedamas vaizdo įrašo generavimas (1/2: Nuotraukos apdorojimas)...", Toast.LENGTH_LONG).show());

            final File finalImageFile = imageFile;
            final File finalAudioFile = audioFile;
            final File finalLogoFile = logoFile;
            final File finalProcessedImageFile = processedImageFile;
            final File finalOutputFile = outputFile;

            String imageProcessingCommand = String.format(
                    Locale.US,
                    "-y -i \"%s\" -i \"%s\" -filter_complex \"[0:v]crop=min(iw\\,ih):min(iw\\,ih),scale=1920:1920,format=gray,gblur=sigma=5[bg]; [1:v]scale=iw*1:ih*1[logo]; [bg][logo]overlay=(W-w)/2:(H-h)/2\" \"%s\"",                    finalImageFile.getAbsolutePath(),
                    finalLogoFile.getAbsolutePath(),
                    finalProcessedImageFile.getAbsolutePath()
            );

            Log.d(TAG, "FFmpeg 1 (Image Processing): " + imageProcessingCommand);

            FFmpegKit.executeAsync(imageProcessingCommand, imageSession -> {
                ReturnCode imageReturnCode = imageSession.getReturnCode();

                if (ReturnCode.isSuccess(imageReturnCode)) {
                    Log.d(TAG, "FFmpeg 1 sėkmė. Pradedamas 2 žingsnis: Video generavimas.");
                    mainHandler.post(() -> Toast.makeText(context, "Nuotrauka apdorota. Pradedama video generacija (2/2)...", Toast.LENGTH_SHORT).show());

                    generateFinalVideo(context, finalProcessedImageFile, finalAudioFile, finalOutputFile, listener, finalImageFile, finalLogoFile);

                } else {
                    String log = imageSession.getAllLogsAsString();
                    String message = "Klaida apdorojant nuotrauką. Klaidos kodas: " + imageReturnCode + "\nLog: " + log;
                    Log.e(TAG, message);

                    if (finalImageFile != null) finalImageFile.delete();
                    if (finalAudioFile != null) finalAudioFile.delete();
                    if (finalLogoFile != null) finalLogoFile.delete();
                    if (finalProcessedImageFile != null) finalProcessedImageFile.delete();
                    if (finalOutputFile.exists()) finalOutputFile.delete();

                    mainHandler.post(() -> Toast.makeText(context, message, Toast.LENGTH_LONG).show());
                    if (listener != null) {
                        listener.onFfmpegFailure(message);
                    }
                }
            });
        }).start();
    }
    
    private static void generateFinalVideo(Context context, File processedImageFile, File audioFile, File outputFile, FfmpegListener listener, File originalImageFile, File logoFile) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        String videoResolutionW = "1920";
        String videoResolutionH = "1080";

        String scaleParams = String.format(Locale.US, "%s:%s:force_original_aspect_ratio=decrease", videoResolutionW, videoResolutionH);
        String padParams = String.format(Locale.US, "%s:%s:-1:-1:color=black", videoResolutionW, videoResolutionH);

        String ffmpegCommand = String.format(
                Locale.US,
                "-y -loop 1 -i \"%s\" -i \"%s\" -vf scale=%s,pad=%s -c:v libx264 -crf 20 -preset veryfast -pix_fmt yuv420p -shortest \"%s\"",
                processedImageFile.getAbsolutePath(),
                audioFile.getAbsolutePath(),
                scaleParams,
                padParams,
                outputFile.getAbsolutePath()
        );

        Log.d(TAG, "FFmpeg 2 (Video Generation): " + ffmpegCommand);

        FFmpegKit.executeAsync(ffmpegCommand, videoSession -> {
            ReturnCode videoReturnCode = videoSession.getReturnCode();
            String message;

            if (originalImageFile != null) originalImageFile.delete();
            if (logoFile != null) logoFile.delete();
            if (processedImageFile != null) processedImageFile.delete();
            if (audioFile != null) audioFile.delete();

            if (ReturnCode.isSuccess(videoReturnCode)) {
                message = "Vaizdo įrašas sėkmingai sukurtas VIEŠAME aplanke: " + outputFile.getName();
                Log.d(TAG, message);

                MediaScannerConnection.scanFile(
                        context,
                        new String[] { outputFile.getAbsolutePath() },
                        new String[] { "video/mp4" },
                        null
                );

                if (listener != null) {
                    listener.onFfmpegSuccess(outputFile.getAbsolutePath());
                }
            } else if (ReturnCode.isCancel(videoReturnCode)) {
                message = "Vaizdo įrašo generavimas atšauktas.";
                Log.d(TAG, message);
                if (listener != null) {
                    listener.onFfmpegFailure(message);
                }
                if (outputFile.exists()) outputFile.delete();
            } else {
                String log = videoSession.getAllLogsAsString();
                message = "Klaida generuojant vaizdo įrašą. Klaidos kodas: " + videoReturnCode + "\nLog: " + log;
                Log.e(TAG, message);
                if (listener != null) {
                    listener.onFfmpegFailure(message);
                }
                if (outputFile.exists()) outputFile.delete();
            }

            final String finalMessage = message;
            mainHandler.post(() -> Toast.makeText(context, finalMessage, Toast.LENGTH_LONG).show());
        });
    }

    public interface FfmpegListener {
        void onFfmpegStart();
        void onFfmpegSuccess(String outputFilePath);
        void onFfmpegFailure(String errorMessage);
    }
}