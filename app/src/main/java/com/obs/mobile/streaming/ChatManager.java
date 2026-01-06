package com.obs.mobile.streaming;

import android.content.Context;
import android.util.Log;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * ChatManager - Handles WebSocket connection for live chat
 */
public class ChatManager {

    private static final String TAG = "ChatManager";

    private final String userId;
    private final ConcurrentLinkedQueue<ChatMessage> messages = new ConcurrentLinkedQueue<>();

    private WebSocketClient webSocketClient;
    private String streamId;
    private ChatEventListener chatEventListener;

    public ChatManager(Context context) {
        // Context parameter kept for future use (e.g., shared preferences, notifications)
        this.userId = generateUserId();
    }

    public void connect(String streamId) {
        this.streamId = streamId;

        try {
            // WebSocket URL (configure based on your server)
            String wsUrl = "wss://your-chat-server.com/ws/" + streamId + "?userId=" + userId;
            URI serverUri = new URI(wsUrl);

            webSocketClient = new WebSocketClient(serverUri) {
                @Override
                @SuppressWarnings("unused")
                public void onOpen(ServerHandshake handshake) {
                    Log.i(TAG, "Chat WebSocket connected");

                    // Send join message
                    JSONObject joinMsg = new JSONObject();
                    try {
                        joinMsg.put("type", "join");
                        joinMsg.put("streamId", streamId);
                        joinMsg.put("userId", userId);
                        joinMsg.put("username", "Broadcaster");
                        send(joinMsg.toString());
                    } catch (Exception e) {
                        Log.e(TAG, "Error creating join message", e);
                    }
                }

                @Override
                @SuppressWarnings("unused")
                public void onMessage(String message) {
                    handleIncomingMessage(message);
                }

                @Override
                @SuppressWarnings("unused")
                public void onClose(int code, String reason, boolean remote) {
                    Log.i(TAG, "Chat WebSocket closed: " + reason);
                    if (chatEventListener != null) {
                        chatEventListener.onChatDisconnected();
                    }
                }

                @Override
                @SuppressWarnings("unused")
                public void onError(Exception ex) {
                    Log.e(TAG, "Chat WebSocket error", ex);
                }
            };

            webSocketClient.connect();

        } catch (URISyntaxException e) {
            Log.e(TAG, "Invalid WebSocket URL", e);
        }
    }

    private void handleIncomingMessage(String message) {
        try {
            JSONObject json = new JSONObject(message);
            String type = json.optString("type");

            switch (type) {
                case "chat":
                    ChatMessage chatMsg = parseChatMessage(json);
                    messages.offer(chatMsg);

                    if (chatEventListener != null) {
                        chatEventListener.onNewMessage(chatMsg);
                    }
                    break;

                case "viewer_count":
                    int count = json.optInt("count", 0);
                    if (chatEventListener != null) {
                        chatEventListener.onViewerCountChanged(count);
                    }
                    break;

                case "system":
                    String systemMsg = json.optString("message");
                    if (chatEventListener != null) {
                        chatEventListener.onSystemMessage(systemMsg);
                    }
                    break;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error parsing chat message", e);
        }
    }

    private ChatMessage parseChatMessage(JSONObject json) {
        ChatMessage msg = new ChatMessage();
        msg.id = json.optString("id");
        msg.userId = json.optString("userId");
        msg.username = json.optString("username");
        msg.message = json.optString("message");
        msg.timestamp = json.optLong("timestamp", System.currentTimeMillis());
        msg.type = ChatMessage.Type.USER;

        // Check for moderator/broadcaster badges
        if (json.optBoolean("isModerator", false)) {
            msg.badges.add(ChatMessage.Badge.MODERATOR);
        }
        if (json.optBoolean("isSubscriber", false)) {
            msg.badges.add(ChatMessage.Badge.SUBSCRIBER);
        }

        return msg;
    }

    public void sendMessage(String message) {
        try {
            // Send via WebSocket if connected
            if (webSocketClient != null && webSocketClient.isOpen()) {
                JSONObject msg = new JSONObject();
                msg.put("type", "chat");
                msg.put("streamId", streamId);
                msg.put("userId", userId);
                msg.put("username", "Broadcaster");
                msg.put("message", message);
                msg.put("timestamp", System.currentTimeMillis());
                msg.put("isBroadcaster", true);

                webSocketClient.send(msg.toString());
            }

            // Always add to local messages
            ChatMessage chatMsg = new ChatMessage();
            chatMsg.id = "local_" + System.currentTimeMillis();
            chatMsg.userId = userId;
            chatMsg.username = "You";
            chatMsg.message = message;
            chatMsg.timestamp = System.currentTimeMillis();
            chatMsg.type = ChatMessage.Type.BROADCASTER;

            messages.offer(chatMsg);

        } catch (Exception e) {
            Log.e(TAG, "Error sending chat message", e);
        }
    }

    public void disconnect() {
        if (webSocketClient != null) {
            webSocketClient.close();
        }
        messages.clear();
    }

    private String generateUserId() {
        return "user_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
    }

    public ConcurrentLinkedQueue<ChatMessage> getMessages() {
        return messages;
    }

    @SuppressWarnings("unused")
    public void setChatEventListener(ChatEventListener listener) {
        this.chatEventListener = listener;
    }

    // Chat message model
    public static class ChatMessage {
        public String id;
        public String userId;
        public String username;
        public String message;
        public long timestamp;
        public Type type;
        public List<Badge> badges = new ArrayList<>();

        public enum Type {
            USER,
            @SuppressWarnings("unused")
            MODERATOR,
            BROADCASTER,
            @SuppressWarnings("unused")
            SYSTEM
        }

        public enum Badge {
            @SuppressWarnings("unused")
            MODERATOR,
            SUBSCRIBER,
            @SuppressWarnings("unused")
            VIP,
            @SuppressWarnings("unused")
            BROADCASTER
        }
    }

    public interface ChatEventListener {
        void onNewMessage(ChatMessage message);
        void onViewerCountChanged(int count);
        void onSystemMessage(String message);
        void onChatDisconnected();
    }
}