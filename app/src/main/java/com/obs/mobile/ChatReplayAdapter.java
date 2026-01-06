package com.obs.mobile;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.obs.mobile.streaming.ChatManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChatReplayAdapter extends RecyclerView.Adapter<ChatReplayAdapter.ChatReplayViewHolder> {

    private List<ChatManager.ChatMessage> messages = new ArrayList<>();

    public ChatReplayAdapter(List<ChatManager.ChatMessage> messages) {
        this.messages = messages;
    }

    public void setChatMessages(List<ChatManager.ChatMessage> messages) {
        this.messages = new ArrayList<>(messages);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChatReplayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TextView tv = new TextView(parent.getContext());
        tv.setPadding(16, 8, 16, 8);
        return new ChatReplayViewHolder(tv);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatReplayViewHolder holder, int position) {
        ChatManager.ChatMessage message = messages.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public static class ChatReplayViewHolder extends RecyclerView.ViewHolder {

        private final TextView textView;

        public ChatReplayViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = (TextView) itemView;
        }

        public void bind(ChatManager.ChatMessage message) {
            long seconds = message.timestamp / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            seconds = seconds % 60;
            minutes = minutes % 60;

            String timeStr;
            if (hours > 0) {
                timeStr = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
            } else {
                timeStr = String.format(Locale.US, "%02d:%02d", minutes, seconds);
            }

            String text = "[" + timeStr + "] " + message.username + ": " + message.message;
            textView.setText(text);

            if (message.type == ChatManager.ChatMessage.Type.BROADCASTER) {
                textView.setTextColor(0xFFFF4444);
            } else if (message.type == ChatManager.ChatMessage.Type.MODERATOR) {
                textView.setTextColor(0xFF44FF44);
            } else {
                textView.setTextColor(0xFFFFFFFF);
            }
        }
    }
}

