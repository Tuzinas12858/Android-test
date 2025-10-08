package com.example.appsas;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends AppCompatActivity {
    private Button audioButton;
    private Button imageButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        audioButton = findViewById(R.id.audio_section_button);
        imageButton = findViewById(R.id.image_section_button);

        // Load the AudioFragment by default
        if (savedInstanceState == null) {
            loadFragment(new AudioFragment());
        }

        audioButton.setOnClickListener(v -> loadFragment(new AudioFragment()));
        imageButton.setOnClickListener(v -> loadFragment(new ImageFragment()));
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }
}