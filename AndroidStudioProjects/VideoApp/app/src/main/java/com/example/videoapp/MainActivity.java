package com.example.videoapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private VideoAdapter adapter;
    private final List<Video> videos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerVideos);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new VideoAdapter(videos, video -> {
            Intent intent = new Intent(MainActivity.this, VideoDetailActivity.class);
            intent.putExtra("videoId", video.id);
            intent.putExtra("title", video.title);
            intent.putExtra("videoUrl", video.videoUrl);
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);

        loadVideosFromFirestore();
    }

    private void loadVideosFromFirestore() {
        FirebaseFirestore.getInstance()
                .collection("videos")
                .addSnapshotListener((snapshot, error) -> {
                    if (snapshot == null) return;

                    videos.clear();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Video video = doc.toObject(Video.class);
                        if (video != null) {
                            video.id = doc.getId();
                            videos.add(video);
                        }
                    }

                    adapter.notifyDataSetChanged();
                });
    }
}
