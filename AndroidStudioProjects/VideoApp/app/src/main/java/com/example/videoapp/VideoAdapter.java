package com.example.videoapp;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.Holder> {

    public interface OnClick {
        void onVideoClick(Video video);
    }

    private final List<Video> list;
    private final OnClick listener;

    public VideoAdapter(List<Video> list, OnClick listener) {
        this.list = list;
        this.listener = listener;
    }

    static class Holder extends RecyclerView.ViewHolder {

        ImageView thumbnail;
        TextView title, uploader, views;

        Holder(View v) {
            super(v);
            thumbnail = v.findViewById(R.id.thumbnail);
            title = v.findViewById(R.id.videoTitle);
            uploader = v.findViewById(R.id.videoUploader);
            views = v.findViewById(R.id.videoViews);
        }
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_video, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Video video = list.get(position);

        h.title.setText(video.title);
        h.uploader.setText(video.uploaderName);
        h.views.setText(video.views + " views");

        // Thumbnail image
        Glide.with(h.thumbnail.getContext())
                .load(video.thumbnailUrl)
                .into(h.thumbnail);

        // Optional background color
        if (video.thumbnailColor != null) {
            try {
                h.thumbnail.setBackgroundColor(
                        Color.parseColor(video.thumbnailColor)
                );
            } catch (Exception ignored) {}
        }

        h.itemView.setOnClickListener(v ->
                listener.onVideoClick(video)
        );
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}
