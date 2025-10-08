package com.example.appsas;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;

import java.util.List;

public class ImageAdapter extends BaseAdapter {
    private Context context;
    private List<Uri> imageUris;
    private OnDeleteClickListener onDeleteClickListener;

    public interface OnDeleteClickListener {
        void onDeleteClick(int position);
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.onDeleteClickListener = listener;
    }

    public ImageAdapter(Context context, List<Uri> imageUris) {
        this.context = context;
        this.imageUris = imageUris;
    }

    @Override
    public int getCount() {
        return imageUris.size();
    }

    @Override
    public Object getItem(int position) {
        return imageUris.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View gridItem;
        if (convertView == null) {
            gridItem = LayoutInflater.from(context).inflate(R.layout.grid_item_image_with_delete, parent, false);
        } else {
            gridItem = convertView;
        }

        ImageView imageView = gridItem.findViewById(R.id.grid_image_view);
        Button deleteButton = gridItem.findViewById(R.id.delete_image_button);

        imageView.setImageURI(imageUris.get(position));

        // Remove the two lines below that were causing the crash
        // int size = parent.getWidth() / 3;
        // imageView.setLayoutParams(new ViewGroup.LayoutParams(size, size));

        deleteButton.setOnClickListener(v -> {
            if (onDeleteClickListener != null) {
                onDeleteClickListener.onDeleteClick(position);
            }
        });

        return gridItem;
    }
}