package com.example.appsas;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider; // ADD THIS
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ImageFragment extends Fragment {
    private static final String PREFS_NAME = "MyPrefs";
    private static final String IMAGE_LIST_KEY = "imageList";
    private GridView imageGridView;
    private ImageAdapter imageAdapter;
    private List<Uri> imageUris = new ArrayList<>();
    private ActivityResultLauncher<String[]> imagePickerLauncher;
    private CombinedViewModel viewModel; // ADD THIS

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_image, container, false);
        // Initialize UI components from the fragment's view
        imageGridView = view.findViewById(R.id.image_grid_view);
        Button selectImageButton = view.findViewById(R.id.select_image_button);

        // Initialize adapter and launcher
        imageAdapter = new ImageAdapter(requireContext(), imageUris);
        imageGridView.setAdapter(imageAdapter);
        loadImageList();
        viewModel = new ViewModelProvider(requireActivity()).get(CombinedViewModel.class);
        // Set the click listener for the delete button (this is the correct one)
        imageAdapter.setOnDeleteClickListener(position -> {
            // Remove the image from the list
            imageUris.remove(position);

            // Notify the adapter that the data has changed
            imageAdapter.notifyDataSetChanged();

            // Save the updated list to SharedPreferences
            saveImageList();

            Toast.makeText(requireContext(), "Image deleted", Toast.LENGTH_SHORT).show();
        });
        imageGridView.setOnItemClickListener((parent, view1, position, id) -> {
            Uri selectedUri = imageUris.get(position);
            viewModel.selectImage(selectedUri);
            Toast.makeText(requireContext(), "Image selected for combined view", Toast.LENGTH_SHORT).show();
        });
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenMultipleDocuments(),
                uris -> {
                    if (uris != null && !uris.isEmpty()) {
                        Set<Uri> newUris = new HashSet<>(uris);

                        for (Uri uri : newUris) {
                            try {
                                // Check if the URI can be resolved. If it can't, it's likely a bad URI and we should skip it.
                                Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null);
                                if (cursor != null && cursor.moveToFirst()) {
                                    cursor.close(); // Close the cursor immediately

                                    // Only try to take a persistable permission if we don't already have it
                                    if (!imageUris.contains(uri)) {
                                        final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION;
                                        requireContext().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

                                        imageUris.add(uri);
                                    }
                                } else {
                                    // The URI is invalid, so log a warning and skip it.
                                    if (cursor != null) cursor.close();
                                    Log.w("ImageFragment", "Skipping invalid or inaccessible URI: " + uri.toString());
                                    Toast.makeText(requireContext(), "Could not import some images due to a file issue.", Toast.LENGTH_LONG).show();
                                }
                            } catch (Exception e) {
                                // Catch all exceptions related to URI access and log them.
                                Log.e("ImageFragment", "Error processing URI: " + uri.toString(), e);
                                Toast.makeText(requireContext(), "Could not import some images due to a permission issue.", Toast.LENGTH_LONG).show();
                            }
                        }

                        saveImageList();
                        imageAdapter.notifyDataSetChanged();
                    }
                }
        );

        selectImageButton.setOnClickListener(v -> imagePickerLauncher.launch(new String[]{"image/*"}));
        // This is the duplicate call that was causing the crash. It has been removed.

        return view;
    }

    void saveImageList() {
        SharedPreferences sharedPrefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        String uriListString = TextUtils.join(",", imageUris);
        editor.putString(IMAGE_LIST_KEY, uriListString);
        editor.apply();
    }

    void loadImageList() {
        SharedPreferences sharedPrefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String uriListString = sharedPrefs.getString(IMAGE_LIST_KEY, "");

        if (!uriListString.isEmpty()) {
            List<String> uriStrings = Arrays.asList(uriListString.split(","));
            imageUris.clear();
            for (String uriString : uriStrings) {
                try {
                    Uri uri = Uri.parse(uriString);
                    // Check if the URI is valid before adding it to prevent crashes
                    Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        imageUris.add(uri);
                        cursor.close();
                    } else {
                        Log.w("ImageFragment", "Skipping invalid URI: " + uriString);
                        if (cursor != null) cursor.close();
                    }
                } catch (Exception e) {
                    Log.e("ImageFragment", "Error loading a URI: " + uriString, e);
                }
            }
            imageAdapter.notifyDataSetChanged();
        }
    }

    // Add this missing method from your AudioFragment
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