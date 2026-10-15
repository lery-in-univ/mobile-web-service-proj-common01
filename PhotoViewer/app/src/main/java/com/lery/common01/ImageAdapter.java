package com.lery.common01;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {
    private List<Bitmap> imageList;

    public interface OnImageClickListener {
        void onClick(int position);
    }

    private final OnImageClickListener listener;

    public ImageAdapter(List<Bitmap> imageList, OnImageClickListener listener) {
        this.listener = listener;
        this.imageList = imageList;
    }

    @Override
    public ImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ImageViewHolder holder, int position) {
        Bitmap bitmap = imageList.get(position);
        holder.imageView.setImageBitmap(bitmap);
        holder.itemView.setOnClickListener(v -> {
            int current = holder.getBindingAdapterPosition();
            if (current != RecyclerView.NO_POSITION) listener.onClick(current);
        });
    }

    @Override
    public int getItemCount() {
        return imageList.size();
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ImageViewHolder(View imageView) {
            super(imageView);
            this.imageView = itemView.findViewById(R.id.imageViewItem);
        }
    }
}
