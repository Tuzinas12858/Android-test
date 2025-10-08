package com.example.appsas;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class AudioListAdapter extends ArrayAdapter<String> {

    private final Context context;
    private final List<String> songTitles;
    private OnDeleteClickListener onDeleteClickListener;

    public interface OnDeleteClickListener {
        void onDeleteClick(int position);
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.onDeleteClickListener = listener;
    }

    public AudioListAdapter(Context context, List<String> songTitles) {
        super(context, 0, songTitles);
        this.context = context;
        this.songTitles = songTitles;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if (listItem == null) {
            listItem = LayoutInflater.from(context).inflate(R.layout.list_item_audio, parent, false);
        }

        String currentSongTitle = songTitles.get(position);

        TextView songTitleTextView = listItem.findViewById(R.id.song_title_item_text_view);
        songTitleTextView.setText(currentSongTitle);

        Button deleteButton = listItem.findViewById(R.id.delete_song_button);
        deleteButton.setOnClickListener(v -> {
            if (onDeleteClickListener != null) {
                onDeleteClickListener.onDeleteClick(position);
            }
        });

        return listItem;
    }
}