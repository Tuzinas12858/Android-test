package com.example.appsas;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import java.io.IOException;

public class CombinedFragment extends Fragment {

    private CombinedViewModel viewModel;
    private ImageView combinedImageView;
    private TextView combinedAudioTitle;
    private TextView nothingSelectedText;
    private LinearLayout combinedPlayerControls;
    private Button playButton, pauseButton, stopButton;

    private MediaPlayer mediaPlayer;
    private Uri currentAudioUri;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_combined, container, false);

        // Find views
        combinedImageView = view.findViewById(R.id.combined_image_view);
        combinedAudioTitle = view.findViewById(R.id.combined_audio_title);
        nothingSelectedText = view.findViewById(R.id.nothing_selected_text);
        combinedPlayerControls = view.findViewById(R.id.combined_player_controls);
        playButton = view.findViewById(R.id.combined_play_button);
        pauseButton = view.findViewById(R.id.combined_pause_button);
        stopButton = view.findViewById(R.id.combined_stop_button);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(CombinedViewModel.class);

        // Initialize MediaPlayer
        mediaPlayer = new MediaPlayer();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Observe selected image
        viewModel.getSelectedImage().observe(getViewLifecycleOwner(), uri -> {
            if (uri != null) {
                combinedImageView.setImageURI(uri);
                combinedImageView.setVisibility(View.VISIBLE);
                nothingSelectedText.setVisibility(View.GONE);
            } else {
                combinedImageView.setVisibility(View.GONE);
            }
            updateNothingSelectedText();
        });

        // Observe selected audio title
        viewModel.getSelectedAudioTitle().observe(getViewLifecycleOwner(), title -> {
            if (title != null && !title.isEmpty()) {
                combinedAudioTitle.setText(title);
                combinedAudioTitle.setVisibility(View.VISIBLE);
                combinedPlayerControls.setVisibility(View.VISIBLE);
                nothingSelectedText.setVisibility(View.GONE);
            } else {
                combinedAudioTitle.setVisibility(View.GONE);
                combinedPlayerControls.setVisibility(View.GONE);
            }
            updateNothingSelectedText();
        });

        // Observe selected audio Uri
        viewModel.getSelectedAudio().observe(getViewLifecycleOwner(), uri -> {
            currentAudioUri = uri;
            // Stop and reset media player if the song changes
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.reset();
        });

        // --- Player Controls ---

        playButton.setOnClickListener(v -> playAudio());
        pauseButton.setOnClickListener(v -> pauseAudio());
        stopButton.setOnClickListener(v -> stopAudio());

        // Reset buttons when audio finishes playing
        mediaPlayer.setOnCompletionListener(mp -> {
            playButton.setEnabled(true);
            pauseButton.setEnabled(false);
            stopButton.setEnabled(false);
        });
    }

    private void playAudio() {
        if (currentAudioUri == null) {
            Toast.makeText(requireContext(), "No audio selected", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            if (!mediaPlayer.isPlaying()) {
                // If paused, just resume
                if (!playButton.isEnabled()) { // Was paused
                    mediaPlayer.start();
                } else { // New play
                    mediaPlayer.reset();
                    mediaPlayer.setDataSource(requireContext(), currentAudioUri);
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                }
                playButton.setEnabled(false);
                pauseButton.setEnabled(true);
                stopButton.setEnabled(true);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Error playing audio", Toast.LENGTH_SHORT).show();
        }
    }

    private void pauseAudio() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            playButton.setEnabled(true);
            pauseButton.setEnabled(false);
        }
    }

    private void stopAudio() {
        if (mediaPlayer.isPlaying() || !playButton.isEnabled()) {
            mediaPlayer.stop();
            mediaPlayer.reset();
            playButton.setEnabled(true);
            pauseButton.setEnabled(false);
            stopButton.setEnabled(false);
        }
    }

    private void updateNothingSelectedText() {
        boolean imageSelected = viewModel.getSelectedImage().getValue() != null;
        boolean audioSelected = viewModel.getSelectedAudio().getValue() != null;

        if (!imageSelected && !audioSelected) {
            nothingSelectedText.setVisibility(View.VISIBLE);
        } else {
            nothingSelectedText.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Release the media player
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}