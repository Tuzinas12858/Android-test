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

// Implementuoti FFmpegExecutor.FfmpegListener sąsają atgaliniam ryšiui
public class CombinedFragment extends Fragment implements FfmpegExecutor.FfmpegListener {

    private CombinedViewModel viewModel;
    private ImageView combinedImageView;
    private TextView combinedAudioTitle;
    private TextView nothingSelectedText;
    private LinearLayout combinedPlayerControls;
    private Button playButton, pauseButton, stopButton;
    private Button combineButton; // Mygtukas FFmpeg vykdymui

    private MediaPlayer mediaPlayer;
    private Uri currentAudioUri;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_combined, container, false);

        // Rasti vaizdo elementus (Find views)
        combinedImageView = view.findViewById(R.id.combined_image_view);
        combinedAudioTitle = view.findViewById(R.id.combined_audio_title);
        nothingSelectedText = view.findViewById(R.id.nothing_selected_text);
        combinedPlayerControls = view.findViewById(R.id.combined_player_controls);
        playButton = view.findViewById(R.id.combined_play_button);
        pauseButton = view.findViewById(R.id.combined_pause_button);
        stopButton = view.findViewById(R.id.combined_stop_button);
        // Rasti naują mygtuką sujungimui
        combineButton = view.findViewById(R.id.combine_video_button);

        // Inicijuoti MediaPlayer
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(mp -> stopAudio());

        // Gauti ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(CombinedViewModel.class);

        // Stebėti LiveData
        viewModel.getSelectedImage().observe(getViewLifecycleOwner(), this::updateImageView);
        viewModel.getSelectedAudio().observe(getViewLifecycleOwner(), this::updateAudioView);
        viewModel.getSelectedAudioTitle().observe(getViewLifecycleOwner(), this::updateAudioTitle);

        // Nustatyti mygtukų klausytojus
        playButton.setOnClickListener(v -> playAudio());
        pauseButton.setOnClickListener(v -> pauseAudio());
        stopButton.setOnClickListener(v -> stopAudio());

        // Nustatyti sujungimo mygtuko klausytoją
        combineButton.setOnClickListener(v -> {
            Uri imageUri = viewModel.getSelectedImage().getValue();
            Uri audioUri = viewModel.getSelectedAudio().getValue();

            if (imageUri != null && audioUri != null) {
                // Kviesti FFmpeg vykdymo metodą, perduodant šį fragmentą kaip listener
                FfmpegExecutor.executeVideoCommand(requireContext(), imageUri, audioUri, this);
            } else {
                Toast.makeText(requireContext(), "Pasirinkite ir paveikslėlį, ir garso failą.", Toast.LENGTH_SHORT).show();
            }
        });

        updateNothingSelectedText();

        return view;
    }

    private void updateImageView(Uri uri) {
        if (uri != null) {
            combinedImageView.setImageURI(uri);
            combinedImageView.setVisibility(View.VISIBLE);
        } else {
            combinedImageView.setVisibility(View.GONE);
        }
        updateNothingSelectedText();
    }

    private void updateAudioView(Uri uri) {
        currentAudioUri = uri;
        stopAudio(); // Sustabdyti bet kokį grojamą garsą, jei šaltinis keičiasi
        combinedPlayerControls.setVisibility(uri != null ? View.VISIBLE : View.GONE);
        updateNothingSelectedText();
    }

    private void updateAudioTitle(String title) {
        if (title != null) {
            combinedAudioTitle.setText(title);
            combinedAudioTitle.setVisibility(View.VISIBLE);
        } else {
            combinedAudioTitle.setVisibility(View.GONE);
        }
    }

    private void playAudio() {
        if (currentAudioUri == null) return;

        try {
            if (!mediaPlayer.isPlaying()) {
                mediaPlayer.reset();
                mediaPlayer.setDataSource(requireContext(), currentAudioUri);
                mediaPlayer.prepare();
                mediaPlayer.start();
                playButton.setEnabled(false);
                pauseButton.setEnabled(true);
                stopButton.setEnabled(true);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Klaida grojant garsą", Toast.LENGTH_SHORT).show();
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

    // Ištrauka iš CombinedFragment.java
    private void updateNothingSelectedText() {
        boolean imageSelected = viewModel.getSelectedImage().getValue() != null;
        boolean audioSelected = viewModel.getSelectedAudio().getValue() != null;

        if (!imageSelected && !audioSelected) {
            nothingSelectedText.setVisibility(View.VISIBLE);
            combineButton.setVisibility(View.GONE); // 1. Paslėpta, jei nieko nepasirinkta
        } else {
            nothingSelectedText.setVisibility(View.GONE);
            if (imageSelected && audioSelected) {
                combineButton.setVisibility(View.VISIBLE); // 2. Rodyti TIK jei abu pasirinkti
            } else {
                combineButton.setVisibility(View.GONE); // 3. Paslėpta, jei pasirinktas tik vienas failas
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Atlaisvinti media grotuvą
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    // --- FfmpegListener implementacija ---

    @Override
    public void onFfmpegStart() {
        // Išjungti sujungimo mygtuką, kol vyksta procesas
        combineButton.setEnabled(false);
        Toast.makeText(requireContext(), "Pradedamas vaizdo įrašo kūrimas (tai gali užtrukti)...", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFfmpegSuccess(String outputFilePath) {
        // BŪTINA PATIKRINTI, ar fragmentas vis dar prijungtas prie Activity,
        // ir naudoti runOnUiThread, kad pereitumėte į Pagrindinę giją.
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                // Eilutė 191 iš Jūsų klaidos seka turbūt yra čia.
                // DABAR GALITE SAUGIAI NAUDOTI TOAST IR ATNAUJINTI UI
                Toast.makeText(getContext(), "Vaizdo įrašas paruoštas: " + outputFilePath, Toast.LENGTH_LONG).show();

                // Pavyzdys, kaip atnaujinti UI:
                // binding.progressBar.setVisibility(View.GONE);
                // binding.btnStart.setEnabled(true);

                // Jei norite bendrinti vaizdo įrašą, kvieskite kodą, kuris jį bendrina.
            });
        }
    }

    @Override
    public void onFfmpegFailure(String errorMessage) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                // UI operacijos Pagrindinėje gijoje
                Toast.makeText(getContext(), "Klaida: " + errorMessage, Toast.LENGTH_LONG).show();

                // Pavyzdys, kaip atnaujinti UI:
                // binding.progressBar.setVisibility(View.GONE);
                // binding.btnStart.setEnabled(true);
            });
        }
    }
}