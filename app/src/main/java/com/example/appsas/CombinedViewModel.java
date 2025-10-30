package com.example.appsas;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.net.Uri;

public class CombinedViewModel extends ViewModel {

    // LiveData for the selected image
    private final MutableLiveData<Uri> selectedImageUri = new MutableLiveData<>();

    // LiveData for the selected audio
    private final MutableLiveData<Uri> selectedAudioUri = new MutableLiveData<>();
    private final MutableLiveData<String> selectedAudioTitle = new MutableLiveData<>();

    // --- Image Methods ---
    public void selectImage(Uri uri) {
        selectedImageUri.setValue(uri);
    }

    public LiveData<Uri> getSelectedImage() {
        return selectedImageUri;
    }

    // --- Audio Methods ---
    public void selectAudio(Uri uri, String title) {
        selectedAudioUri.setValue(uri);
        selectedAudioTitle.setValue(title);
    }

    public LiveData<Uri> getSelectedAudio() {
        return selectedAudioUri;
    }

    public LiveData<String> getSelectedAudioTitle() {
        return selectedAudioTitle;
    }
}