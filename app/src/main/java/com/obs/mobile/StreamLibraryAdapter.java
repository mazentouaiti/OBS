package com.obs.mobile;

import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.obs.mobile.streaming.StreamMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.Date;

public class StreamLibraryAdapter extends RecyclerView.Adapter<StreamLibraryAdapter.ViewHolder> {

    private List<StreamMetadata> streams = new ArrayList<>();
    private final OnStreamClickListener listener;

    public interface OnStreamClickListener {
        void onStreamClick(StreamMetadata stream);
        void onStreamDelete(StreamMetadata stream);
    }

    public StreamLibraryAdapter(OnStreamClickListener listener) {
        this.listener = listener;
    }

    public void setStreams(List<StreamMetadata> streams) {
        this.streams = new ArrayList<>(streams);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TextView tv = new TextView(parent.getContext());
        tv.setPadding(24, 24, 24, 24);
        return new ViewHolder(tv);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StreamMetadata s = streams.get(position);
        String date = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).format(new Date(s.startTime));
        long sizeMB = s.fileSize / (1024 * 1024);
        String text = s.streamTitle + "\n" + "Duration: " + formatDuration(s.duration) + " | Size: " + sizeMB + "MB\n" + "Date: " + date;
        holder.textView.setText(text);
        holder.itemView.setOnClickListener(v -> { if (listener != null) listener.onStreamClick(s); });
    }

    @Override
    public int getItemCount() { return streams.size(); }

    private static String formatDuration(long ms) {
        long seconds = ms / 1000;
        long hrs = seconds / 3600;
        long mins = (seconds % 3600) / 60;
        long secs = seconds % 60;
        if (hrs > 0) return String.format(Locale.US, "%d:%02d:%02d", hrs, mins, secs);
        return String.format(Locale.US, "%02d:%02d", mins, secs);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        public ViewHolder(@NonNull android.view.View itemView) {
            super(itemView);
            textView = (TextView) itemView;
        }
    }
}

