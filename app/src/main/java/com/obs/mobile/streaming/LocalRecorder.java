package com.obs.mobile.streaming;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * LocalRecorder - Records video/audio streams to MP4 file
 */
public class LocalRecorder {

    private static final String TAG = "LocalRecorder";

    // Video configuration
    private static final String VIDEO_MIME_TYPE = "video/avc"; // H.264
    private static final int VIDEO_BITRATE = 2500000; // 2.5 Mbps
    private static final int VIDEO_FRAME_RATE = 30;
    private static final int VIDEO_IFRAME_INTERVAL = 1;

    // Audio configuration
    private static final String AUDIO_MIME_TYPE = "audio/mp4a-latm"; // AAC
    private static final int AUDIO_SAMPLE_RATE = 44100;
    private static final int AUDIO_BITRATE = 128000; // 128 kbps
    private static final int AUDIO_CHANNEL_COUNT = 2;

    private final String outputPath;
    private final int videoWidth;
    private final int videoHeight;

    private MediaCodec videoEncoder;
    private MediaCodec audioEncoder;
    private MediaMuxer mediaMuxer;

    private int videoTrackIndex = -1;
    private int audioTrackIndex = -1;
    private boolean isRecording = false;
    private boolean muxerStarted = false;

    private long videoStartTime = 0;
    private long audioStartTime = 0;

    public LocalRecorder(String outputPath, int videoWidth, int videoHeight) {
        this.outputPath = outputPath;
        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;
    }

    /**
     * Start recording
     */
    public void startRecording() {
        try {
            // Create output directory if needed
            File outputFile = new File(outputPath);
            File parentDir = outputFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    Log.w(TAG, "Failed to create output directory");
                }
            }

            // Initialize media muxer
            mediaMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            // Initialize video encoder
            initializeVideoEncoder();

            // Initialize audio encoder
            initializeAudioEncoder();

            isRecording = true;
            Log.i(TAG, "Recording started: " + outputPath);

        } catch (IOException e) {
            Log.e(TAG, "Failed to start recording", e);
            isRecording = false;
        }
    }

    /**
     * Initialize video encoder
     */
    private void initializeVideoEncoder() throws IOException {
        MediaFormat format = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, videoWidth, videoHeight);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, VIDEO_BITRATE);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, VIDEO_FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, VIDEO_IFRAME_INTERVAL);

        videoEncoder = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE);
        videoEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        // Note: Surface would be obtained from videoEncoder.createInputSurface()
        Log.d(TAG, "Video encoder initialized");
    }

    /**
     * Initialize audio encoder
     */
    private void initializeAudioEncoder() throws IOException {
        MediaFormat format = MediaFormat.createAudioFormat(AUDIO_MIME_TYPE, AUDIO_SAMPLE_RATE, AUDIO_CHANNEL_COUNT);
        format.setInteger(MediaFormat.KEY_BIT_RATE, AUDIO_BITRATE);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);

        audioEncoder = MediaCodec.createEncoderByType(AUDIO_MIME_TYPE);
        audioEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        Log.d(TAG, "Audio encoder initialized");
    }

    /**
     * Write video frame to encoder
     */
    public void writeVideoFrame(byte[] frameData, long timestamp) {
        if (!isRecording || videoEncoder == null) {
            return;
        }

        try {
            if (videoStartTime == 0) {
                videoStartTime = timestamp;
            }

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int inputBufferIndex = videoEncoder.dequeueInputBuffer(10000);

            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = videoEncoder.getInputBuffer(inputBufferIndex);
                inputBuffer.clear();
                inputBuffer.put(frameData);

                videoEncoder.queueInputBuffer(
                        inputBufferIndex,
                        0,
                        frameData.length,
                        timestamp - videoStartTime,
                        0
                );

                drainVideoEncoder();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error writing video frame", e);
        }
    }

    /**
     * Write audio data to encoder
     */
    public void writeAudioData(byte[] audioData, long timestamp) {
        if (!isRecording || audioEncoder == null) {
            return;
        }

        try {
            if (audioStartTime == 0) {
                audioStartTime = timestamp;
            }

            int inputBufferIndex = audioEncoder.dequeueInputBuffer(10000);

            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = audioEncoder.getInputBuffer(inputBufferIndex);
                inputBuffer.clear();
                inputBuffer.put(audioData);

                audioEncoder.queueInputBuffer(
                        inputBufferIndex,
                        0,
                        audioData.length,
                        timestamp - audioStartTime,
                        0
                );

                drainAudioEncoder();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error writing audio data", e);
        }
    }

    /**
     * Extract encoded data from video encoder
     */
    private void drainVideoEncoder() {
        if (videoEncoder == null) return;

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        while (true) {
            int outputBufferIndex = videoEncoder.dequeueOutputBuffer(bufferInfo, 0);

            if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // Add video track to muxer
                if (videoTrackIndex == -1) {
                    MediaFormat newFormat = videoEncoder.getOutputFormat();
                    videoTrackIndex = mediaMuxer.addTrack(newFormat);
                    tryStartMuxer();
                }
            } else if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                break;
            } else if (outputBufferIndex >= 0) {
                // Write encoded data to muxer
                if (muxerStarted && videoTrackIndex != -1) {
                    ByteBuffer encodedData = videoEncoder.getOutputBuffer(outputBufferIndex);
                    mediaMuxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo);
                }

                videoEncoder.releaseOutputBuffer(outputBufferIndex, false);
            }
        }
    }

    /**
     * Extract encoded data from audio encoder
     */
    private void drainAudioEncoder() {
        if (audioEncoder == null) return;

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        while (true) {
            int outputBufferIndex = audioEncoder.dequeueOutputBuffer(bufferInfo, 0);

            if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // Add audio track to muxer
                if (audioTrackIndex == -1) {
                    MediaFormat newFormat = audioEncoder.getOutputFormat();
                    audioTrackIndex = mediaMuxer.addTrack(newFormat);
                    tryStartMuxer();
                }
            } else if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                break;
            } else if (outputBufferIndex >= 0) {
                // Write encoded data to muxer
                if (muxerStarted && audioTrackIndex != -1) {
                    ByteBuffer encodedData = audioEncoder.getOutputBuffer(outputBufferIndex);
                    mediaMuxer.writeSampleData(audioTrackIndex, encodedData, bufferInfo);
                }

                audioEncoder.releaseOutputBuffer(outputBufferIndex, false);
            }
        }
    }

    /**
     * Start muxer once both tracks are ready
     */
    private void tryStartMuxer() {
        if (!muxerStarted && videoTrackIndex != -1 && audioTrackIndex != -1) {
            mediaMuxer.start();
            muxerStarted = true;
            Log.d(TAG, "MediaMuxer started");
        }
    }

    /**
     * Stop recording and finalize the file
     */
    public void stopRecording() {
        isRecording = false;

        try {
            if (videoEncoder != null) {
                videoEncoder.signalEndOfInputStream();
                drainVideoEncoder();
                videoEncoder.stop();
                videoEncoder.release();
                videoEncoder = null;
            }

            if (audioEncoder != null) {
                audioEncoder.signalEndOfInputStream();
                drainAudioEncoder();
                audioEncoder.stop();
                audioEncoder.release();
                audioEncoder = null;
            }

            if (mediaMuxer != null) {
                if (muxerStarted) {
                    mediaMuxer.stop();
                }
                mediaMuxer.release();
                mediaMuxer = null;
            }

            Log.i(TAG, "Recording stopped");

        } catch (Exception e) {
            Log.e(TAG, "Error stopping recording", e);
        }
    }

    /**
     * Get the output file path
     */
    public String getOutputPath() {
        return outputPath;
    }

    /**
     * Check if recording is active
     */
    public boolean isRecording() {
        return isRecording;
    }

    /**
     * Get output file size
     */
    public long getFileSize() {
        try {
            File file = new File(outputPath);
            if (file.exists()) {
                return file.length();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting file size", e);
        }
        return 0;
    }
}

