package com.obs.mobile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.obs.mobile.streaming.StreamDatabaseHelper;
import com.obs.mobile.streaming.StreamMetadata;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.Date;

public class StreamsLibraryActivity extends AppCompatActivity {

    private static final String TAG = "StreamsLibrary";

    private RecyclerView rvStreams;
    private StreamDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create layout programmatically
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        ));
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(16, 16, 16, 16);

        // Header
        LinearLayout headerLayout = new LinearLayout(this);
        headerLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);

        TextView titleTv = new TextView(this);
        titleTv.setText("Saved Streams");
        titleTv.setTextSize(20);
        titleTv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        headerLayout.addView(titleTv);

        Button btnRefresh = new Button(this);
        btnRefresh.setText("Refresh");
        btnRefresh.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        headerLayout.addView(btnRefresh);

        rootLayout.addView(headerLayout);

        // RecyclerView
        rvStreams = new RecyclerView(this);
        rvStreams.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        ));
        rvStreams.setLayoutManager(new LinearLayoutManager(this));
        rootLayout.addView(rvStreams);

        setContentView(rootLayout);

        dbHelper = new StreamDatabaseHelper(this);

        // Inline adapter to avoid external dependency
        InlineStreamAdapter adapter = new InlineStreamAdapter(new ArrayList<>());
        rvStreams.setAdapter(adapter);

        btnRefresh.setOnClickListener(v -> loadStreams(adapter));

        loadStreams(adapter);
    }

    private void loadStreams(InlineStreamAdapter adapter) {
        new Thread(() -> {
            try {
                List<StreamMetadata> streams = dbHelper.getAllStreams();
                runOnUiThread(() -> {
                    adapter.setStreams(streams);
                    if (streams.isEmpty()) {
                        Toast.makeText(StreamsLibraryActivity.this, "No saved streams", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading streams", e);
                runOnUiThread(() -> Toast.makeText(StreamsLibraryActivity.this, "Error loading streams", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) dbHelper.close();
    }

    // Inline adapter class
    private class InlineStreamAdapter extends RecyclerView.Adapter<InlineStreamAdapter.VH> {
        private List<StreamMetadata> items;

        InlineStreamAdapter(List<StreamMetadata> items) { this.items = items; }

        void setStreams(List<StreamMetadata> newItems) {
            this.items = new ArrayList<>(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setPadding(16, 16, 16, 16);
            return new VH(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            StreamMetadata s = items.get(position);
            String date = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).format(new Date(s.startTime));
            long sizeMB = s.fileSize / (1024 * 1024);
            String text = s.streamTitle + "\n" + "Duration: " + formatDuration(s.duration) + " | Size: " + sizeMB + "MB\n" + "Date: " + date;
            holder.tv.setText(text);

            holder.itemView.setOnClickListener(v -> {
                // Start playback via explicit classname to avoid compile-time reference
                try {
                    Intent intent = new Intent();
                    intent.setClassName(StreamsLibraryActivity.this, "com.obs.mobile.StreamPlaybackActivity");
                    intent.putExtra("stream_id", s.streamId);
                    intent.putExtra("stream_title", s.streamTitle);
                    intent.putExtra("stream_path", s.localRecordingPath);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to start playback", e);
                    Toast.makeText(StreamsLibraryActivity.this, "Cannot start playback", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tv;
            VH(android.view.View itemView) {
                super(itemView);
                tv = (TextView) itemView;
            }
        }

        private String formatDuration(long ms) {
            long seconds = ms / 1000;
            long hrs = seconds / 3600;
            long mins = (seconds % 3600) / 60;
            long secs = seconds % 60;
            if (hrs > 0) return String.format(Locale.US, "%d:%02d:%02d", hrs, mins, secs);
            return String.format(Locale.US, "%02d:%02d", mins, secs);
        }
    }
}
