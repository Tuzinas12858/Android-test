package com.example.appsas;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
// ADD THIS IMPORT
import androidx.lifecycle.ViewModelProvider;

public class MainActivity extends AppCompatActivity {
    private Button audioButton;
    private Button imageButton;
    private Button combinedButton; // ADD THIS

    private CombinedViewModel viewModel; // ADD THIS

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ADD THIS
        // Initialize the shared ViewModel
        viewModel = new ViewModelProvider(this).get(CombinedViewModel.class);

        audioButton = findViewById(R.id.audio_section_button);
        imageButton = findViewById(R.id.image_section_button);
        combinedButton = findViewById(R.id.combined_section_button); // ADD THIS

        // Load the AudioFragment by default
        if (savedInstanceState == null) {
            loadFragment(new AudioFragment());
        }

        audioButton.setOnClickListener(v -> loadFragment(new AudioFragment()));
        imageButton.setOnClickListener(v -> loadFragment(new ImageFragment()));
        combinedButton.setOnClickListener(v -> loadFragment(new CombinedFragment())); // ADD THIS
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }
}