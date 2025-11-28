package com.example.appsas;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AudioService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener {

    private MediaPlayer mediaPlayer;
    private final IBinder binder = new LocalBinder();
    private List<Uri> playlist = new ArrayList<>();
    private int currentSongIndex = -1;
    private AudioServiceCallback callback;

    public interface AudioServiceCallback {
        void onSongPrepared(int duration, int index);
        void onSongCompletion();
    }

    public void setCallback(AudioServiceCallback callback) {
        this.callback = callback;
    }

    public class LocalBinder extends Binder {
        AudioService getService() {
            return AudioService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnPreparedListener(this);
    }

    public void setPlaylist(List<Uri> newPlaylist) {
        this.playlist = newPlaylist;
    }

    public void playSong(int index) {
        if (index < 0 || index >= playlist.size()) return;

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
        } else {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setOnPreparedListener(this);
        }

        currentSongIndex = index;
        Uri currentUri = playlist.get(currentSongIndex);

        try {
            mediaPlayer.setDataSource(getApplicationContext(), currentUri);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            Log.e("AudioService", "Error setting data source or preparing", e);
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
        if (callback != null) {
            callback.onSongPrepared(mp.getDuration(), currentSongIndex);
        }
    }

    public void pauseSong() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    public void resumeSong() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    public void stopSong() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
            currentSongIndex = -1;
            if (callback != null) {
                callback.onSongCompletion();
            }
        }
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public int getCurrentPosition() {
        try {
            return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
        } catch (IllegalStateException e) {
            return 0;
        }
    }

    public int getDuration() {
        try {
            return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
        } catch (IllegalStateException e) {
            return 0;
        }
    }

    public void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
        }
    }

    public int getCurrentSongIndex() {
        return currentSongIndex;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        currentSongIndex++;
        if (currentSongIndex < playlist.size()) {
            playSong(currentSongIndex);
        } else {
            stopSong();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}