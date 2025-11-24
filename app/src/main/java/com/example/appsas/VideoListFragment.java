package com.example.appsas;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.content.FileProvider; // Reikalingas importas FileProvider

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VideoListFragment extends Fragment {

    private ListView videoListView;
    private TextView emptyListText;
    private ArrayAdapter<String> adapter;
    private List<File> videoFiles = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Nurodyti fragmento layout failą. Tikriausiai Jums reikia sukurti layout'ą pavadinimu fragment_video_list.xml
        View view = inflater.inflate(R.layout.fragment_video_list, container, false);
        videoListView = view.findViewById(R.id.video_list_view);
        emptyListText = view.findViewById(R.id.empty_video_list_text);

        // Nustato sąrašo adapterį
        adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, new ArrayList<>());
        videoListView.setAdapter(adapter);

        // Pridėti sąrašo elemento paspaudimo įvykį (leidžia paleisti video)
        videoListView.setOnItemClickListener((parent, view1, position, id) -> {
            File selectedFile = videoFiles.get(position);
            playVideo(selectedFile);
        });

        loadVideoFiles();
        return view;
    }

    private void loadVideoFiles() {
        // Griežtai naudoti viešąjį "Movies" katalogą, kaip nurodyta FfmpegExecutor
        File moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);

        videoFiles.clear();
        adapter.clear();

        if (moviesDir != null && moviesDir.exists()) {
            // Filtravimas: tikrina .mp4 failus, kurie prasideda "APPSAS_Render_"
            File[] files = moviesDir.listFiles(pathname ->
                    pathname.getName().toLowerCase().endsWith(".mp4") &&
                            pathname.getName().startsWith("APPSAS_Render_")
            );

            if (files != null) {
                // Rūšiuoti failus pagal paskutinio modifikavimo laiką (naujausi viršuje)
                Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

                videoFiles.addAll(Arrays.asList(files));
                for (File file : videoFiles) {
                    adapter.add(file.getName());
                }
            }
        }

        // Pakeisti sąrašo būseną (tuščias ar pilnas)
        if (videoFiles.isEmpty()) {
            emptyListText.setVisibility(View.VISIBLE);
            videoListView.setVisibility(View.GONE);
            // Jei nėra R.string.no_videos_found, naudojame tiesioginį tekstą
            String dirPath = moviesDir != null ? moviesDir.getAbsolutePath() : "viešasis 'Movies' aplankas";
            emptyListText.setText("Nerasta jokių sukurtų vaizdo įrašų (APPSAS_Render_*.mp4) aplanke:\n" + dirPath);
        } else {
            emptyListText.setVisibility(View.GONE);
            videoListView.setVisibility(View.VISIBLE);
        }

        adapter.notifyDataSetChanged();
    }

    // Paleisti video failą naudojant numatytąją Android programą
    private void playVideo(File videoFile) {
        try {
            Context context = requireContext();

            // Svarbu: VIEŠOJO failo paleidimui reikalingas FileProvider.
            // Būtina, kad AndroidManifest.xml faile būtų deklaruotas FileProvider su atitinkamu 'authorities'
            Uri videoUri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider", // Turi atitikti manifesto authority
                    videoFile
            );

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(videoUri, "video/mp4");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // Leidimas kitai programai perskaityti

            startActivity(intent);
        } catch (IllegalArgumentException e) {
            // Dažniausiai įvyksta, jei neteisingas FileProvider authorities arba trūksta <provider> manifesto
            Toast.makeText(requireContext(), "Klaida: Patikrinkite FileProvider nustatymus Manifeste.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Klaida paleidžiant video: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Užtikrinti, kad sąrašas būtų atnaujinamas grįžus į fragmentą
    @Override
    public void onResume() {
        super.onResume();
        loadVideoFiles();
    }
}