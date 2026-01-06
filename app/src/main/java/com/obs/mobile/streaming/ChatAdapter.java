package com.obs.mobile.streaming;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.obs.mobile.R;

import java.util.Locale;
import java.util.Queue;
import java.util.ArrayList;
import java.util.List;

/**
 * ChatAdapter - Displays chat messages in RecyclerView
 */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private final List<ChatManager.ChatMessage> messages = new ArrayList<>();

    public ChatAdapter(Queue<ChatManager.ChatMessage> messages) {
        updateMessages(messages);
    }

    /**
     * Update messages from Queue to List
     */
    public void updateMessages(Queue<ChatManager.ChatMessage> newMessages) {
        messages.clear();
        messages.addAll(newMessages);
        notifyDataSetChanged();
    }

    /**
     * Add a single message
     */
    public void addMessage(ChatManager.ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Instead of creating a TextView, inflate the chat message layout
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        if (position < messages.size()) {
            ChatManager.ChatMessage message = messages.get(position);
            holder.bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    /**
     * ViewHolder for chat messages
     */
    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvUsername;
        private final TextView tvMessage;
        private final TextView tvTime;

        public ChatViewHolder(@NonNull android.view.View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tv_chat_username);
            tvMessage = itemView.findViewById(R.id.tv_chat_message);
            tvTime = itemView.findViewById(R.id.tv_chat_time);

            // Initialize badges TextView if exists
            TextView tvBadges = itemView.findViewById(R.id.tv_chat_badges);
        }

        public void bind(ChatManager.ChatMessage message) {
            tvUsername.setText(message.username);
            tvMessage.setText(message.message);

            // Format timestamp
            long seconds = message.timestamp / 1000;
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            seconds = seconds % 60;

            String timeText = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
            tvTime.setText(timeText);

            // Set colors based on type
            if (message.type == ChatManager.ChatMessage.Type.BROADCASTER) {
                tvUsername.setTextColor(0xFFFF4444); // Red
            } else if (message.type == ChatManager.ChatMessage.Type.MODERATOR) {
                tvUsername.setTextColor(0xFF44FF44); // Green
            } else {
                tvUsername.setTextColor(0xFFFFFFFF); // White
            }

            // Set badge text if applicable
            // ... (you can add badge logic here)
        }
    }

}