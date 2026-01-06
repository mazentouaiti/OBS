package com.example.videoapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class VideoDetailActivity extends AppCompatActivity {

    PlayerView playerView;
    ExoPlayer player;

    ImageView btnLike, btnComment, btnFullscreen;
    TextView textLikes, textCommentCount;

    FirebaseFirestore db;
    DocumentReference videoRef;

    String videoId;
    String videoUrl;
    String userId = "test_user";
    String userName = "Current User"; // Vous devriez récupérer cela du profil utilisateur

    boolean isLiked = false;
    boolean isFullscreen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_detail);

        // GET DATA FROM INTENT (IMPORTANT)
        videoId = getIntent().getStringExtra("videoId");
        videoUrl = getIntent().getStringExtra("videoUrl");

        db = FirebaseFirestore.getInstance();
        videoRef = db.collection("videos").document(videoId);

        // UI
        playerView = findViewById(R.id.playerView);
        btnLike = findViewById(R.id.btnLike);
        btnComment = findViewById(R.id.btnComment);
        btnFullscreen = findViewById(R.id.btnFullscreen);
        textLikes = findViewById(R.id.textLikes);
        textCommentCount = findViewById(R.id.textCommentCount);

        // PLAYER
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        player.setMediaItem(MediaItem.fromUri(videoUrl));
        player.prepare();
        player.play();

        listenLikes();
        listenCommentsCount();

        btnLike.setOnClickListener(v -> toggleLike());
        btnComment.setOnClickListener(v -> openComments());
        btnFullscreen.setOnClickListener(v -> toggleFullscreen());
    }

    private void listenLikes() {
        videoRef.addSnapshotListener((snap, e) -> {
            if (snap == null || !snap.exists()) return;

            Long likes = snap.getLong("likes");
            Map<String, Boolean> likedUsers =
                    (Map<String, Boolean>) snap.get("likedUsers");

            isLiked = likedUsers != null && likedUsers.containsKey(userId);
            btnLike.setImageResource(
                    isLiked ? R.drawable.ic_like_filled : R.drawable.ic_like_outline
            );

            textLikes.setText(String.valueOf(likes != null ? likes : 0));
        });
    }

    private void toggleLike() {
        db.runTransaction(t -> {
            DocumentSnapshot s = t.get(videoRef);

            long likes = s.getLong("likes") != null ? s.getLong("likes") : 0;
            Map<String, Boolean> users =
                    (Map<String, Boolean>) s.get("likedUsers");

            if (users == null) users = new HashMap<>();

            if (users.containsKey(userId)) {
                likes--;
                users.remove(userId);
            } else {
                likes++;
                users.put(userId, true);
            }

            t.update(videoRef, "likes", likes);
            t.update(videoRef, "likedUsers", users);
            return null;
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to update like", Toast.LENGTH_SHORT).show();
        });
    }

    private void listenCommentsCount() {
        videoRef.collection("comments")
                .addSnapshotListener((snap, e) -> {
                    if (snap != null)
                        textCommentCount.setText(String.valueOf(snap.size()));
                });
    }

    private void openComments() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View v = getLayoutInflater()
                .inflate(R.layout.bottom_sheet_comments, null);
        dialog.setContentView(v);

        RecyclerView recycler = v.findViewById(R.id.recyclerComments);
        EditText input = v.findViewById(R.id.inputComment);
        ImageView send = v.findViewById(R.id.btnSendComment);

        recycler.setLayoutManager(new LinearLayoutManager(this));
        List<Comment> commentList = new ArrayList<>();

        CommentAdapter.OnCommentActionListener actionListener = new CommentAdapter.OnCommentActionListener() {
            @Override
            public void onEditComment(Comment comment, String currentText) {
                showEditCommentDialog(comment, currentText);
            }

            @Override
            public void onDeleteComment(Comment comment) {
                showDeleteConfirmationDialog(comment);
            }
        };

        CommentAdapter adapter = new CommentAdapter(commentList, userId, actionListener);
        recycler.setAdapter(adapter);

        CollectionReference commentsRef = videoRef.collection("comments");

        // Listen for comments with real-time updates
        commentsRef.orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(VideoDetailActivity.this, "Error loading comments", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    commentList.clear();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Comment comment = doc.toObject(Comment.class);
                            if (comment != null) {
                                comment.id = doc.getId(); // Store document ID
                                commentList.add(comment);
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                });

        // Send new comment
        send.setOnClickListener(v1 -> {
            String text = input.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(VideoDetailActivity.this, "Comment cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            Comment newComment = new Comment();
            newComment.text = text;
            newComment.userId = userId;
            newComment.userName = userName;
            newComment.timestamp = Timestamp.now();

            commentsRef.add(newComment)
                    .addOnSuccessListener(documentReference -> {
                        input.setText("");
                        Toast.makeText(VideoDetailActivity.this, "Comment added", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(VideoDetailActivity.this, "Failed to add comment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        // Clear input when dialog is dismissed
        dialog.setOnDismissListener(dialogInterface -> {
            input.setText("");
        });

        dialog.show();
    }

    private void showEditCommentDialog(Comment comment, String currentText) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Comment");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setText(currentText);
        input.setSelection(currentText.length());
        input.setMinLines(3);
        input.setMaxLines(5);
        builder.setView(input);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newText = input.getText().toString().trim();
                if (newText.isEmpty()) {
                    Toast.makeText(VideoDetailActivity.this, "Comment cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!newText.equals(currentText)) {
                    updateComment(comment.id, newText);
                }
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updateComment(String commentId, String newText) {
        DocumentReference commentRef = videoRef.collection("comments").document(commentId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("text", newText);
        updates.put("edited", true);
        updates.put("editTimestamp", FieldValue.serverTimestamp());

        commentRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Comment updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update comment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showDeleteConfirmationDialog(final Comment comment) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Comment")
                .setMessage("Are you sure you want to delete this comment?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteComment(comment.id);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteComment(String commentId) {
        DocumentReference commentRef = videoRef.collection("comments").document(commentId);

        commentRef.delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Comment deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete comment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void toggleFullscreen() {
        if (isFullscreen) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }
        isFullscreen = !isFullscreen;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player != null) {
            player.play();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}