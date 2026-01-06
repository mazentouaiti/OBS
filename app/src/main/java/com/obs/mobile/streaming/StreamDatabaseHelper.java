package com.obs.mobile.streaming;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class StreamDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "streams.db";
    private static final int DATABASE_VERSION = 1;

    // Table: streams
    private static final String TABLE_STREAMS = "streams";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_STREAM_ID = "stream_id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_DESCRIPTION = "description";
    private static final String COLUMN_START_TIME = "start_time";
    private static final String COLUMN_END_TIME = "end_time";
    private static final String COLUMN_DURATION = "duration";
    private static final String COLUMN_FILE_PATH = "file_path";
    private static final String COLUMN_FILE_SIZE = "file_size";
    private static final String COLUMN_THUMBNAIL_PATH = "thumbnail_path";
    private static final String COLUMN_MAX_VIEWERS = "max_viewers";
    private static final String COLUMN_CHAT_COUNT = "chat_count";
    private static final String COLUMN_RTMP_URL = "rtmp_url";
    private static final String COLUMN_STREAM_KEY = "stream_key";
    private static final String COLUMN_TAGS = "tags";
    private static final String COLUMN_CREATED_AT = "created_at";

    // Table: chat_messages (for local chat replay)
    private static final String TABLE_CHAT = "chat_messages";
    private static final String COLUMN_MESSAGE_ID = "message_id";
    private static final String COLUMN_STREAM_FK = "stream_fk";
    private static final String COLUMN_USER_ID = "user_id";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_MESSAGE = "message";
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_MESSAGE_TYPE = "message_type";

    public StreamDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create streams table
        String CREATE_STREAMS_TABLE = "CREATE TABLE " + TABLE_STREAMS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_STREAM_ID + " TEXT UNIQUE,"
                + COLUMN_TITLE + " TEXT,"
                + COLUMN_DESCRIPTION + " TEXT,"
                + COLUMN_START_TIME + " INTEGER,"
                + COLUMN_END_TIME + " INTEGER,"
                + COLUMN_DURATION + " INTEGER,"
                + COLUMN_FILE_PATH + " TEXT,"
                + COLUMN_FILE_SIZE + " INTEGER,"
                + COLUMN_THUMBNAIL_PATH + " TEXT,"
                + COLUMN_MAX_VIEWERS + " INTEGER,"
                + COLUMN_CHAT_COUNT + " INTEGER,"
                + COLUMN_RTMP_URL + " TEXT,"
                + COLUMN_STREAM_KEY + " TEXT,"
                + COLUMN_TAGS + " TEXT,"
                + COLUMN_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP"
                + ")";
        db.execSQL(CREATE_STREAMS_TABLE);

        // Create chat messages table
        String CREATE_CHAT_TABLE = "CREATE TABLE " + TABLE_CHAT + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_MESSAGE_ID + " TEXT,"
                + COLUMN_STREAM_FK + " TEXT,"
                + COLUMN_USER_ID + " TEXT,"
                + COLUMN_USERNAME + " TEXT,"
                + COLUMN_MESSAGE + " TEXT,"
                + COLUMN_TIMESTAMP + " INTEGER,"
                + COLUMN_MESSAGE_TYPE + " TEXT,"
                + "FOREIGN KEY(" + COLUMN_STREAM_FK + ") REFERENCES "
                + TABLE_STREAMS + "(" + COLUMN_STREAM_ID + ")"
                + ")";
        db.execSQL(CREATE_CHAT_TABLE);

        // Create index for faster queries
        db.execSQL("CREATE INDEX idx_stream_fk ON " + TABLE_CHAT + "(" + COLUMN_STREAM_FK + ")");
        db.execSQL("CREATE INDEX idx_stream_time ON " + TABLE_STREAMS + "(" + COLUMN_START_TIME + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_STREAMS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHAT);
        onCreate(db);
    }

    // ======================== STREAM CRUD OPERATIONS ========================

    /**
     * Save a stream metadata to database
     */
    public long saveStream(StreamMetadata metadata) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_STREAM_ID, metadata.streamId);
        values.put(COLUMN_TITLE, metadata.streamTitle);
        values.put(COLUMN_DESCRIPTION, metadata.description != null ? metadata.description : "");
        values.put(COLUMN_START_TIME, metadata.startTime);
        values.put(COLUMN_END_TIME, metadata.endTime);
        values.put(COLUMN_DURATION, metadata.duration);
        values.put(COLUMN_FILE_PATH, metadata.localRecordingPath != null ? metadata.localRecordingPath : "");
        values.put(COLUMN_FILE_SIZE, metadata.fileSize);
        values.put(COLUMN_THUMBNAIL_PATH, metadata.thumbnailPath != null ? metadata.thumbnailPath : "");
        values.put(COLUMN_MAX_VIEWERS, metadata.maxViewers);
        values.put(COLUMN_CHAT_COUNT, metadata.chatMessageCount);
        values.put(COLUMN_RTMP_URL, metadata.rtmpUrl != null ? metadata.rtmpUrl : "");
        values.put(COLUMN_STREAM_KEY, metadata.streamKey != null ? metadata.streamKey : "");
        values.put(COLUMN_TAGS, metadata.tags != null ? metadata.tags : "");

        long id = db.insert(TABLE_STREAMS, null, values);
        db.close();

        Log.d("StreamDB", "Stream saved with ID: " + id);
        return id;
    }

    /**
     * Get a single stream by ID
     */
    public StreamMetadata getStream(String streamId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_STREAMS,
                null,
                COLUMN_STREAM_ID + "=?",
                new String[]{streamId},
                null,
                null,
                null
        );

        StreamMetadata metadata = null;
        if (cursor.moveToFirst()) {
            metadata = cursorToStreamMetadata(cursor);
        }

        cursor.close();
        db.close();
        return metadata;
    }

    /**
     * Get all streams, ordered by start time (newest first)
     */
    public List<StreamMetadata> getAllStreams() {
        List<StreamMetadata> streams = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_STREAMS,
                null,
                null,
                null,
                null,
                null,
                COLUMN_START_TIME + " DESC"
        );

        if (cursor.moveToFirst()) {
            do {
                streams.add(cursorToStreamMetadata(cursor));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return streams;
    }

    /**
     * Update stream metadata
     */
    public boolean updateStream(StreamMetadata metadata) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_TITLE, metadata.streamTitle);
        values.put(COLUMN_DESCRIPTION, metadata.description);
        values.put(COLUMN_END_TIME, metadata.endTime);
        values.put(COLUMN_DURATION, metadata.duration);
        values.put(COLUMN_FILE_SIZE, metadata.fileSize);
        values.put(COLUMN_MAX_VIEWERS, metadata.maxViewers);
        values.put(COLUMN_CHAT_COUNT, metadata.chatMessageCount);

        int rowsUpdated = db.update(
                TABLE_STREAMS,
                values,
                COLUMN_STREAM_ID + "=?",
                new String[]{metadata.streamId}
        );

        db.close();
        return rowsUpdated > 0;
    }

    /**
     * Delete a stream and all associated chat messages
     */
    public boolean deleteStream(String streamId) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Delete chat messages first (foreign key constraint)
        db.delete(TABLE_CHAT, COLUMN_STREAM_FK + "=?", new String[]{streamId});

        // Delete stream
        int rowsDeleted = db.delete(TABLE_STREAMS, COLUMN_STREAM_ID + "=?", new String[]{streamId});

        db.close();
        return rowsDeleted > 0;
    }

    // ======================== CHAT MESSAGE CRUD OPERATIONS ========================

    /**
     * Save a chat message to database
     */
    public long saveChatMessage(String streamId, ChatManager.ChatMessage message) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_MESSAGE_ID, message.id);
        values.put(COLUMN_STREAM_FK, streamId);
        values.put(COLUMN_USER_ID, message.userId);
        values.put(COLUMN_USERNAME, message.username);
        values.put(COLUMN_MESSAGE, message.message);
        values.put(COLUMN_TIMESTAMP, message.timestamp);
        values.put(COLUMN_MESSAGE_TYPE, message.type.name());

        long id = db.insert(TABLE_CHAT, null, values);
        db.close();

        return id;
    }

    /**
     * Get all chat messages for a stream
     */
    public List<ChatManager.ChatMessage> getChatMessages(String streamId) {
        List<ChatManager.ChatMessage> messages = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_CHAT,
                null,
                COLUMN_STREAM_FK + "=?",
                new String[]{streamId},
                null,
                null,
                COLUMN_TIMESTAMP + " ASC"
        );

        if (cursor.moveToFirst()) {
            do {
                ChatManager.ChatMessage msg = new ChatManager.ChatMessage();
                msg.id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_ID));
                msg.userId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_ID));
                msg.username = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME));
                msg.message = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE));
                msg.timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP));
                msg.type = ChatManager.ChatMessage.Type.valueOf(
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_TYPE))
                );
                messages.add(msg);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return messages;
    }

    /**
     * Delete all chat messages for a stream
     */
    public boolean deleteChatMessages(String streamId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsDeleted = db.delete(TABLE_CHAT, COLUMN_STREAM_FK + "=?", new String[]{streamId});
        db.close();
        return rowsDeleted > 0;
    }

    /**
     * Get chat message count for a stream
     */
    public int getChatMessageCount(String streamId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_CHAT,
                null,
                COLUMN_STREAM_FK + "=?",
                new String[]{streamId},
                null,
                null,
                null
        );

        int count = cursor.getCount();
        cursor.close();
        db.close();
        return count;
    }

    // ======================== HELPER METHODS ========================

    /**
     * Convert cursor row to StreamMetadata object
     */
    private StreamMetadata cursorToStreamMetadata(Cursor cursor) {
        StreamMetadata metadata = new StreamMetadata();
        metadata.streamId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STREAM_ID));
        metadata.streamTitle = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
        metadata.description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION));
        metadata.startTime = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_START_TIME));
        metadata.endTime = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_END_TIME));
        metadata.duration = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DURATION));
        metadata.localRecordingPath = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FILE_PATH));
        metadata.fileSize = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_FILE_SIZE));
        metadata.thumbnailPath = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_THUMBNAIL_PATH));
        metadata.maxViewers = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MAX_VIEWERS));
        metadata.chatMessageCount = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CHAT_COUNT));
        metadata.rtmpUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RTMP_URL));
        metadata.streamKey = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STREAM_KEY));
        metadata.tags = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TAGS));
        return metadata;
    }
}