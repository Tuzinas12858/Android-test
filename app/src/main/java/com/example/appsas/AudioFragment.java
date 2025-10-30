package com.example.appsas;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider; // ADD THIS

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AudioFragment extends Fragment {
    private static final String PREFS_NAME = "MyPrefs";
    private static final String AUDIO_LIST_KEY = "audioList";
    private ListView playlistListView;
    private AudioListAdapter playlistAdapter;
    private List<String> songTitles = new ArrayList<>();
    private Handler handler = new Handler(Looper.getMainLooper());
    private List<Uri> playlist = new ArrayList<>();
    private ActivityResultLauncher<String[]> filePickerLauncher;

    private AudioService audioService;
    private boolean isServiceBound = false;

    private LinearLayout playerControlsLayout;
    private TextView songTitleTextView;
    private TextView songNumberTextView;
    private SeekBar songProgressSeekBar;
    private Button selectMp3Button;
    private Button playPauseButton;
    private Button nextButton;
    private Button previousButton;
    private Button stopButton;
    private CombinedViewModel viewModel; // ADD THIS
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_audio, container, false);

        playerControlsLayout = view.findViewById(R.id.player_controls_layout);
        songTitleTextView = view.findViewById(R.id.song_title_text_view);
        songNumberTextView = view.findViewById(R.id.song_number_text_view);
        songProgressSeekBar = view.findViewById(R.id.song_progress_seekbar);
        selectMp3Button = view.findViewById(R.id.select_mp3_button);
        playPauseButton = view.findViewById(R.id.play_pause_button);
        nextButton = view.findViewById(R.id.next_button);
        previousButton = view.findViewById(R.id.previous_button);
        stopButton = view.findViewById(R.id.stop_button);
        playlistListView = view.findViewById(R.id.playlist_list_view);
        viewModel = new ViewModelProvider(requireActivity()).get(CombinedViewModel.class);
        playlistAdapter = new AudioListAdapter(requireContext(), songTitles);
        playlistListView.setAdapter(playlistAdapter);
        loadPlaylist();

        playlistAdapter.setOnDeleteClickListener(position -> {
            playlist.remove(position);
            songTitles.remove(position);
            playlistAdapter.notifyDataSetChanged();
            savePlaylist();
            Toast.makeText(requireContext(), "Song deleted", Toast.LENGTH_SHORT).show();
        });

        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenMultipleDocuments(),
                uris -> {
                    if (uris != null && !uris.isEmpty()) {
                        Set<Uri> newUris = new HashSet<>(uris);
                        for (Uri uri : newUris) {
                            if (!playlist.contains(uri)) {
                                try {
                                    final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION;
                                    requireContext().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    playlist.add(uri);
                                    String fileName = getFileNameFromUri(uri);
                                    songTitles.add(fileName);
                                } catch (SecurityException e) {
                                    e.printStackTrace();
                                    Toast.makeText(requireContext(), "Could not get permanent access for some files.", Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                        playlistAdapter.notifyDataSetChanged();
                        savePlaylist();
                        if (isServiceBound) {
                            audioService.setPlaylist(playlist);
                        }
                    }
                }
        );

        selectMp3Button.setOnClickListener(v -> filePickerLauncher.launch(new String[]{"audio/mpeg", "audio/mp3", "audio/*"}));

        playPauseButton.setOnClickListener(v -> {
            if (isServiceBound && audioService.isPlaying()) {
                audioService.pauseSong();
                playPauseButton.setText("Play");
            } else if (isServiceBound && !playlist.isEmpty()) {
                audioService.resumeSong();
                playPauseButton.setText("Pause");
                updateSongProgress();
            } else if (isServiceBound && playlist.isEmpty()) {
                Toast.makeText(requireContext(), "Please select a song first.", Toast.LENGTH_SHORT).show();
            } else {
                // If service is not bound yet, just wait for it.
            }
        });

        nextButton.setOnClickListener(v -> {
            if (isServiceBound) {
                int nextIndex = audioService.getCurrentSongIndex() + 1;
                if (nextIndex < playlist.size()) {
                    audioService.playSong(nextIndex);
                    updateUI(nextIndex);
                }
            }
        });

        previousButton.setOnClickListener(v -> {
            if (isServiceBound) {
                int prevIndex = audioService.getCurrentSongIndex() - 1;
                if (prevIndex >= 0) {
                    audioService.playSong(prevIndex);
                    updateUI(prevIndex);
                }
            }
        });

        stopButton.setOnClickListener(v -> {
            if (isServiceBound) {
                audioService.stopSong();
                playerControlsLayout.setVisibility(View.GONE);
                playPauseButton.setText("Play");
            }
        });

        songProgressSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && isServiceBound) {
                    audioService.seekTo(progress);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        playlistListView.setOnItemClickListener((parent, view1, position, id) -> {
            if (isServiceBound) {
                // This logic remains the same
                audioService.playSong(position);
                updateUI(position);

                // ADD THIS
                // Get the selected items and update the ViewModel
                Uri selectedUri = playlist.get(position);
                String selectedTitle = songTitles.get(position);
                viewModel.selectAudio(selectedUri, selectedTitle);
                Toast.makeText(requireContext(), "'" + selectedTitle + "' selected for combined view", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    // The ServiceConnection handles binding to the service
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AudioService.LocalBinder binder = (AudioService.LocalBinder) service;
            audioService = binder.getService();
            isServiceBound = true;

            // Set the playlist on the service when it's bound
            audioService.setPlaylist(playlist);

            // Update UI to reflect current song if one is playing
            if (audioService.isPlaying() || audioService.getCurrentSongIndex() != -1) {
                playerControlsLayout.setVisibility(View.VISIBLE);
                updateUI(audioService.getCurrentSongIndex());
                updateSongProgress();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
        }
    };

    // Updates UI elements based on the current song
    private void updateUI(int index) {
        if (index >= 0 && index < songTitles.size()) {
            playerControlsLayout.setVisibility(View.VISIBLE);
            songTitleTextView.setText(songTitles.get(index));
            songNumberTextView.setText("Song " + (index + 1) + " of " + playlist.size());
            playPauseButton.setText("Pause");
        }
    }

    // Updates the seekbar and song progress in a loop
    private void updateSongProgress() {
        if (isServiceBound) {
            songProgressSeekBar.setMax(audioService.getDuration());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (isServiceBound && audioService.isPlaying()) {
                        songProgressSeekBar.setProgress(audioService.getCurrentPosition());
                        handler.postDelayed(this, 1000);
                    }
                }
            });
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Start and bind to the service
        Intent intent = new Intent(requireContext(), AudioService.class);
        requireContext().startService(intent);
        requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        // Unbind from the service but do not stop it
        if (isServiceBound) {
            requireContext().unbindService(serviceConnection);
            isServiceBound = false;
        }
        handler.removeCallbacksAndMessages(null);
    }

    // Your existing loadPlaylist, savePlaylist and getFileNameFromUri methods here
    private void savePlaylist() {
        SharedPreferences sharedPrefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        String uriListString = TextUtils.join(",", playlist);
        editor.putString(AUDIO_LIST_KEY, uriListString);
        editor.apply();
    }

    private void loadPlaylist() {
        SharedPreferences sharedPrefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String uriListString = sharedPrefs.getString(AUDIO_LIST_KEY, "");

        if (!uriListString.isEmpty()) {
            List<String> uriStrings = Arrays.asList(uriListString.split(","));
            playlist.clear();
            songTitles.clear();

            for (String uriString : uriStrings) {
                try {
                    Uri uri = Uri.parse(uriString);
                    Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        playlist.add(uri);
                        songTitles.add(getFileNameFromUri(uri));
                        cursor.close();
                    } else {
                        if (cursor != null) cursor.close();
                    }
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Some files could not be loaded.", Toast.LENGTH_SHORT).show();
                }
            }
            playlistAdapter.notifyDataSetChanged();
        }
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
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
        return result;
    }
}