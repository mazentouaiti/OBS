package com.example.videoapp;

import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class VideoSuggestedAdapter
        extends RecyclerView.Adapter<VideoSuggestedAdapter.Holder> {

    List<Video> list;
    OnClick listener;

    public interface OnClick {
        void click(Video video);
    }

    public VideoSuggestedAdapter(List<Video> list, OnClick l) {
        this.list = list;
        this.listener = l;
    }

    static class Holder extends RecyclerView.ViewHolder {
        ImageView thumb;
        TextView title, meta;

        Holder(View v) {
            super(v);
            thumb = v.findViewById(R.id.imageThumb);
            title = v.findViewById(R.id.textTitle);
            meta = v.findViewById(R.id.textMeta);
        }
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_suggested_video, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(Holder h, int i) {
        Video v = list.get(i);

        h.title.setText(v.title);
        h.meta.setText(v.uploaderName + " • " + v.views + " views");

        Glide.with(h.thumb.getContext())
                .load(v.thumbnailUrl)
                .into(h.thumb);

        h.itemView.setOnClickListener(view -> listener.click(v));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}
