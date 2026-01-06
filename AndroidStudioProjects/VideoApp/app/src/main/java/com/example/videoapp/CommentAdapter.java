package com.example.videoapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.Holder> {

    private List<Comment> list;
    private String currentUserId;
    private OnCommentActionListener listener;
    private int selectedPosition = -1; // Track which comment shows action buttons

    public interface OnCommentActionListener {
        void onEditComment(Comment comment, String newText);
        void onDeleteComment(Comment comment);
    }

    public CommentAdapter(List<Comment> list, String currentUserId, OnCommentActionListener listener) {
        this.list = list;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView textUserName, textTime, textComment;
        Button btnEdit, btnDelete;
        View layoutActions;

        Holder(View v) {
            super(v);
            textUserName = v.findViewById(R.id.textUserName);
            textTime = v.findViewById(R.id.textTime);
            textComment = v.findViewById(R.id.textComment);
            btnEdit = v.findViewById(R.id.btnEdit);
            btnDelete = v.findViewById(R.id.btnDelete);
            layoutActions = v.findViewById(R.id.layoutActions);
        }
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Comment comment = list.get(position);

        // Set user name (or default)
        h.textUserName.setText(comment.userName != null ? comment.userName : "User");
        h.textComment.setText(comment.text);

        // Format timestamp
        if (comment.timestamp != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, HH:mm", Locale.getDefault());
            h.textTime.setText(sdf.format(comment.timestamp.toDate()));
        }

        // Show action buttons only for current user's comments
        boolean isCurrentUserComment = comment.userId.equals(currentUserId);

        // Toggle action buttons visibility
        boolean showActions = (selectedPosition == position) && isCurrentUserComment;
        h.layoutActions.setVisibility(showActions ? View.VISIBLE : View.GONE);

        // Handle click on comment item
        h.itemView.setOnClickListener(v -> {
            if (isCurrentUserComment) {
                int oldPosition = selectedPosition;
                selectedPosition = (selectedPosition == position) ? -1 : position;
                notifyItemChanged(oldPosition);
                notifyItemChanged(position);
            } else {
                // If not current user's comment, hide any open action panels
                if (selectedPosition != -1) {
                    int oldPosition = selectedPosition;
                    selectedPosition = -1;
                    notifyItemChanged(oldPosition);
                }
            }
        });

        // Edit button
        h.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                // In a real app, you might show a dialog to edit
                // For now, let's use a simple approach
                listener.onEditComment(comment, comment.text);
            }
        });

        // Delete button
        h.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteComment(comment);
                // Reset selection
                selectedPosition = -1;
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    // Method to update a comment
    public void updateComment(int position, Comment updatedComment) {
        list.set(position, updatedComment);
        notifyItemChanged(position);
    }

    // Method to remove a comment
    public void removeComment(int position) {
        list.remove(position);
        notifyItemRemoved(position);
        if (position < list.size()) {
            notifyItemRangeChanged(position, list.size() - position);
        }
    }

    // Method to find comment position by ID
    public int findCommentPositionById(String commentId) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(commentId)) {
                return i;
            }
        }
        return -1;
    }
}