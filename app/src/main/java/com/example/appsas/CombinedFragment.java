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
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.io.File;
import android.util.Log;

public class CombinedFragment extends Fragment implements FfmpegExecutor.FfmpegListener {

    private CombinedViewModel viewModel;
    private ImageView combinedImageView;
    private TextView combinedAudioTitle;
    private TextView nothingSelectedText;
    private LinearLayout combinedPlayerControls;
    private Button playButton, pauseButton;
    private Button combineButton;
    private ProgressBar renderProgressBar;
    private MediaPlayer mediaPlayer;
    private Uri currentAudioUri;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_combined, container, false);

        combinedImageView = view.findViewById(R.id.combined_image_view);
        combinedAudioTitle = view.findViewById(R.id.combined_audio_title);
        nothingSelectedText = view.findViewById(R.id.nothing_selected_text);

        combinedPlayerControls = view.findViewById(R.id.combined_player_controls);

        playButton = view.findViewById(R.id.combined_play_button);
        pauseButton = view.findViewById(R.id.combined_pause_button);
        combineButton = view.findViewById(R.id.combine_video_button);
        renderProgressBar = view.findViewById(R.id.render_progress_bar);
        viewModel = new ViewModelProvider(requireActivity()).get(CombinedViewModel.class);

        viewModel.getSelectedImage().observe(getViewLifecycleOwner(), this::updateCombinedContent);

        viewModel.getSelectedAudio().observe(getViewLifecycleOwner(), audioUri -> {
            currentAudioUri = audioUri;
            updateCombinedContent(viewModel.getSelectedImage().getValue());
        });

        viewModel.getSelectedAudioTitle().observe(getViewLifecycleOwner(), title -> {
            if (title != null && !title.isEmpty()) {
                combinedAudioTitle.setText(title);
                combinedAudioTitle.setVisibility(View.VISIBLE);
                combinedPlayerControls.setVisibility((View.VISIBLE));
            } else {
                combinedAudioTitle.setText("NO AUDIO");
                combinedAudioTitle.setVisibility(View.VISIBLE);
            }
        });

        setupMediaPlayerControls();
        setupCombineButton();

        return view;
    }

    private void updateCombinedContent(Uri imageUri) {
        boolean isImageSelected = imageUri != null;
        boolean isAudioSelected = currentAudioUri != null;

        if (isImageSelected) {
            if (combinedImageView != null) {
                combinedImageView.setImageURI(imageUri);
                combinedImageView.setVisibility(View.VISIBLE);
            }
        } else {
            if (combinedImageView != null) {
                combinedImageView.setVisibility(View.GONE);
            }
        }

        if (isImageSelected && isAudioSelected) {
            if (nothingSelectedText != null) nothingSelectedText.setVisibility(View.GONE);
            if (combinedPlayerControls != null) combinedPlayerControls.setVisibility(View.VISIBLE);
            if (combineButton != null) combineButton.setVisibility(View.VISIBLE);
        } else {
            if (nothingSelectedText != null) nothingSelectedText.setVisibility(View.VISIBLE);
            if (combinedPlayerControls != null) combinedPlayerControls.setVisibility(View.GONE);
            if (combineButton != null) combineButton.setVisibility(View.GONE);
            stopPlayback();
        }
    }

    private void setupMediaPlayerControls() {
        if (playButton != null) {
            playButton.setOnClickListener(v -> startPlayback());
        }
        if (pauseButton != null) {
            pauseButton.setOnClickListener(v -> pausePlayback());
        }
        if (stopButton != null) {
            stopButton.setOnClickListener(v -> stopPlayback());
        }
    }

    private void setupCombineButton() {
        if (combineButton == null) return;

        combineButton.setOnClickListener(v -> {
            Uri imageUri = viewModel.getSelectedImage().getValue();
            Uri audioUri = viewModel.getSelectedAudio().getValue();
            String audioTitle = viewModel.getSelectedAudioTitle().getValue();

            if (imageUri != null && audioUri != null) {
                String safeAudioTitle = (audioTitle != null && !audioTitle.isEmpty()) ? audioTitle : "Audio";

                safeAudioTitle = safeAudioTitle.replaceAll("[^a-zA-Z0-9_\\-]", "_");

                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());

                String outputFileName = "APPSAS_" + safeAudioTitle + "_" + timeStamp + ".mp4";

                FfmpegExecutor.executeVideoCommand(requireContext(), imageUri, audioUri, outputFileName, this);
            } else {
                Toast.makeText(requireContext(), "Trūksta vaizdo arba garso failo!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startPlayback() {
        if (currentAudioUri == null) return;
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            try {
                mediaPlayer.setDataSource(requireContext(), currentAudioUri);
                mediaPlayer.prepare();
                mediaPlayer.start();
            } catch (IOException e) {
                Toast.makeText(requireContext(), "Klaida paleidžiant garsą: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    private void pausePlayback() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    private void stopPlayback() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopPlayback();
    }

    @Override
    public void onFfmpegStart() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (combineButton != null) {
                    combineButton.setVisibility(View.GONE);
                    combineButton.setEnabled(false);
                }
                if (renderProgressBar != null) {
                    renderProgressBar.setVisibility(View.VISIBLE);
                }
                Toast.makeText(requireContext(), "Pradedamas vaizdo įrašo kūrimas...", Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public void onFfmpegSuccess(String outputFilePath) {

        try {
            String audioTitle = viewModel.getSelectedAudioTitle().getValue();
            if (audioTitle == null || audioTitle.isEmpty()) {
                audioTitle = "Nežinomas takelis";
            }
            String videoFileName = new File(outputFilePath).getName();
            GoogleSheetsLogger.logRender(audioTitle, videoFileName);

        } catch (Exception e) {
            Log.e("CombinedFragment", "Klaida bandant registruoti į Google Sheets", e);
        }

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (renderProgressBar != null) {
                    renderProgressBar.setVisibility(View.GONE);
                }
                if (combineButton != null) {
                    combineButton.setVisibility(View.VISIBLE);
                    combineButton.setEnabled(true);
                    combineButton.setText("Sukurti Video");
                }
                Toast.makeText(getContext(), "Vaizdo įrašas paruoštas: " + outputFilePath, Toast.LENGTH_LONG).show();
            });
        }
    }

    @Override
    public void onFfmpegFailure(String errorMessage) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (renderProgressBar != null) {
                    renderProgressBar.setVisibility(View.GONE);
                }
                if (combineButton != null) {
                    combineButton.setVisibility(View.VISIBLE);
                    renderProgressBar.setVisibility(View.GONE);
                    combineButton.setEnabled(true);
                    combineButton.setText("Sukurti Video");
                }
                Toast.makeText(getContext(), "Klaida: " + errorMessage, Toast.LENGTH_LONG).show();
            });
        }
    }
}