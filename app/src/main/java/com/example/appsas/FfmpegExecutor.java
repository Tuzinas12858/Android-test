package com.example.appsas;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment; // PRIDĖTA: nauja importo eilutė
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.ReturnCode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

/**
 * Sąsaja, skirta FFmpeg komandos vykdymo rezultatams perduoti.
 */


public class FfmpegExecutor {

    private static final String TAG = "FfmpegExecutor";

    /**
     * Nukopijuoja turinio URI (Content URI) failą į viešą aplanką, kad FFmpeg galėtų jį pasiekti.
     * Naudosime privačią saugyklą tik laikinam nuotraukos ir garso saugojimui.
     */
    private static File copyUriToFile(Context context, Uri uri, String filePrefix, String fileExtension) throws Exception {
        // Laikinus failus kopijuojame į privačią saugyklą, kad nereikėtų papildomų leidimų
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

    /**
     * Vykdo FFmpeg komandą, kad sujungtų statinį vaizdą ir garso įrašą į MP4 vaizdo įrašą.
     * Išvestis saugoma viešajame "Movies" aplanke.
     * @param listener Klausytojas, kuris praneš apie sėkmę arba nesėkmę.
     */
    public static void executeVideoCommand(Context context, Uri imageUri, Uri audioUri, FfmpegListener listener) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        // 1. Nustatyti išvesties vietą (VIEŠAS "MOVIES" APLANKAS)
        // Šis kelias turėtų būti pasiekiamas per failų tvarkyklę.
        File outputDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        if (outputDir != null && !outputDir.exists()) {
            // Svarbu: Android Q+ gali apriboti šį metodą, tačiau jis geriausiai tinka
            // platesniam suderinamumui. Tikrasis kelias yra priklausomas nuo API lygio.
            outputDir.mkdirs();
        }

        File outputFile = new File(outputDir, "APPSAS_Render_" + System.currentTimeMillis() + ".mp4");
        // 2. Nukopijuoti įvesties failus į laikiną privačią saugyklą
        File imageFile = null;
        File audioFile = null;

        try {
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

        } catch (Exception e) {
            String error = "Klaida kopijuojant failus: " + e.getMessage();
            Log.e(TAG, error);
            mainHandler.post(() -> Toast.makeText(context, error, Toast.LENGTH_LONG).show());
            if (listener != null) {
                listener.onFfmpegFailure(error);
            }
            if (imageFile != null) imageFile.delete();
            if (audioFile != null) audioFile.delete();
            return;
        }

        // 3. Sudaryti FFmpeg komandą
        String ffmpegCommand = String.format(
                Locale.US,
                "-y -loop 1 -i \"%s\" -i \"%s\" -vf format=gray,scale=1080:1080 -c:v libx264 -preset ultrafast -pix_fmt yuv420p -shortest \"%s\"",
                imageFile.getAbsolutePath(),
                audioFile.getAbsolutePath(),
                outputFile.getAbsolutePath()
        );

        // 4. Vykdyti FFmpeg
        Toast.makeText(context, "Pradedamas vaizdo įrašo generavimas į VIEŠĄJĮ aplanką 'Movies'...", Toast.LENGTH_LONG).show();

        File finalImageFile = imageFile;
        File finalAudioFile = audioFile;

        FFmpegKit.executeAsync(ffmpegCommand, session -> {
            com.arthenica.ffmpegkit.ReturnCode returnCode = session.getReturnCode();
            String message;

            // Išvalyti LAIKINUS privačius įvesties failus
            if (finalImageFile != null) finalImageFile.delete();
            if (finalAudioFile != null) finalAudioFile.delete();

            if (ReturnCode.isSuccess(returnCode)) {
                message = "Vaizdo įrašas sėkmingai sukurtas VIEŠAME aplanke: " + outputFile.getAbsolutePath();
                Log.d(TAG, message);

                // **PRANEŠAME MEDIA SCANNER'IUI APIE SUKURTĄ FAILĄ VIEŠOJE SAUGYKLOJE**
                MediaScannerConnection.scanFile(
                        context,
                        new String[] { outputFile.getAbsolutePath() },
                        new String[] { "video/mp4" },
                        null
                );

                if (listener != null) {
                    listener.onFfmpegSuccess(outputFile.getAbsolutePath());
                }
            } else if (ReturnCode.isCancel(returnCode)) {
                message = "Vaizdo įrašo generavimas atšauktas.";
                Log.d(TAG, message);
                if (listener != null) {
                    listener.onFfmpegFailure(message);
                }
                if (outputFile.exists()) outputFile.delete();
            } else {
                String log = session.getAllLogsAsString();
                message = "Klaida generuojant vaizdo įrašą. Klaidos kodas: " + returnCode + "\nLog: " + log;
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