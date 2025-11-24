package com.example.appsas;

import android.os.Bundle;
import android.widget.Button;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.fragment.app.FragmentManager;
import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.ReturnCode;
public class MainActivity extends AppCompatActivity {
    private Button audioButton;
    private Button imageButton;
    private Button combinedButton; // ADD THIS
    private Button videoButton;
    private CombinedViewModel viewModel; // ADD THIS

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        viewModel = new ViewModelProvider(this).get(CombinedViewModel.class);

        audioButton = findViewById(R.id.audio_section_button);
        imageButton = findViewById(R.id.image_section_button);
        combinedButton = findViewById(R.id.combined_section_button); // ADD THIS
        videoButton = findViewById(R.id.video_section_button);
        // Load the AudioFragment by default
        if (savedInstanceState == null) {
            loadFragment(new AudioFragment());
        }

        audioButton.setOnClickListener(v -> loadFragment(new AudioFragment()));
        imageButton.setOnClickListener(v -> loadFragment(new ImageFragment()));
        combinedButton.setOnClickListener(v -> loadFragment(new CombinedFragment())); // ADD THIS
        videoButton.setOnClickListener(v -> replaceFragment(new VideoListFragment())); // <--- NAUJAS
    }
    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // Naudokite tą patį fragmentų konteinerio ID, kurį nurodėte activity_main.xml
        fragmentTransaction.replace(R.id.fragment_container, fragment);

        // Išvalyti atgalinį steką, kad būtų išvengta klaidų paspaudus Back mygtuką
        fragmentTransaction.disallowAddToBackStack();

        fragmentTransaction.commit();
    }
    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

}