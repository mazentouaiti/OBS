package com.obs.mobile.streaming;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * LocalRecorder - Records video/audio streams to MP4 file
 * Fixed version with proper encoder initialization and data writing
 */
public class LocalRecorder {

    private static final String TAG = "LocalRecorder";

    // Video configuration
    private static final String VIDEO_MIME_TYPE = "video/avc"; // H.264
    private static final int VIDEO_BITRATE = 2500000; // 2.5 Mbps
    private static final int VIDEO_FRAME_RATE = 30;
    private static final int VIDEO_IFRAME_INTERVAL = 1;
    private static final int VIDEO_COLOR_FORMAT = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;

    // Audio configuration
    private static final String AUDIO_MIME_TYPE = "audio/mp4a-latm"; // AAC
    private static final int AUDIO_SAMPLE_RATE = 44100;
    private static final int AUDIO_BITRATE = 128000; // 128 kbps
    private static final int AUDIO_CHANNEL_COUNT = 2;
    private static final int AUDIO_AAC_PROFILE = MediaCodecInfo.CodecProfileLevel.AACObjectLC;

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
    private boolean videoEncoderConfigured = false;
    private boolean audioEncoderConfigured = false;

    private long videoStartTimeUs = -1;
    private long audioStartTimeUs = -1;

    // Input surface for video encoder
    private Surface inputSurface;

    public LocalRecorder(String outputPath, int videoWidth, int videoHeight) {
        this.outputPath = outputPath;
        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;

        Log.d(TAG, "LocalRecorder created: " + outputPath +
                " Size: " + videoWidth + "x" + videoHeight);
    }

    /**
     * Start recording
     */
    public void startRecording() {
        Log.d(TAG, "startRecording called");

        try {
            // Create output directory if needed
            File outputFile = new File(outputPath);
            File parentDir = outputFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    Log.w(TAG, "Failed to create output directory");
                }
            }

            // Delete existing file if it exists (0 byte file)
            if (outputFile.exists() && outputFile.length() == 0) {
                outputFile.delete();
            }

            // Initialize media muxer
            mediaMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            Log.d(TAG, "MediaMuxer initialized: " + outputPath);

            // Initialize video encoder
            initializeVideoEncoder();

            // Initialize audio encoder
            initializeAudioEncoder();

            // Start encoders
            videoEncoder.start();
            audioEncoder.start();

            // Get input surface from video encoder
            inputSurface = videoEncoder.createInputSurface();

            isRecording = true;
            Log.i(TAG, "Recording started successfully");

        } catch (IOException e) {
            Log.e(TAG, "Failed to start recording", e);
            isRecording = false;
            releaseResources();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error starting recording", e);
            isRecording = false;
            releaseResources();
        }
    }

    /**
     * Initialize video encoder
     */
    private void initializeVideoEncoder() throws IOException {
        Log.d(TAG, "Initializing video encoder: " + videoWidth + "x" + videoHeight);

        MediaFormat format = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, videoWidth, videoHeight);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, VIDEO_COLOR_FORMAT);
        format.setInteger(MediaFormat.KEY_BIT_RATE, VIDEO_BITRATE);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, VIDEO_FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, VIDEO_IFRAME_INTERVAL);

        // For some devices, need to set profile/level
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline);
            format.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel31);
        }

        videoEncoder = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE);
        videoEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        videoEncoderConfigured = true;

        Log.d(TAG, "Video encoder configured successfully");
    }

    /**
     * Initialize audio encoder
     */
    private void initializeAudioEncoder() throws IOException {
        Log.d(TAG, "Initializing audio encoder");

        MediaFormat format = MediaFormat.createAudioFormat(AUDIO_MIME_TYPE,
                AUDIO_SAMPLE_RATE, AUDIO_CHANNEL_COUNT);
        format.setInteger(MediaFormat.KEY_BIT_RATE, AUDIO_BITRATE);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, AUDIO_AAC_PROFILE);

        // For AAC encoding
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 4096);
        }

        audioEncoder = MediaCodec.createEncoderByType(AUDIO_MIME_TYPE);
        audioEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        audioEncoderConfigured = true;

        Log.d(TAG, "Audio encoder configured successfully");
    }

    /**
     * Get input surface for camera
     */
    public Surface getInputSurface() {
        return inputSurface;
    }

    /**
     * Write video frame to encoder - FOR TESTING ONLY
     * In real implementation, camera feeds surface directly
     */
    public void writeVideoFrame(byte[] frameData, long timestampUs) {
        if (!isRecording || videoEncoder == null) {
            Log.w(TAG, "Cannot write video frame - not recording or encoder null");
            return;
        }

        try {
            if (videoStartTimeUs == -1) {
                videoStartTimeUs = timestampUs;
                Log.d(TAG, "Video start time set: " + videoStartTimeUs);
            }

            long presentationTimeUs = timestampUs - videoStartTimeUs;

            Log.d(TAG, "Writing video frame, size: " + frameData.length +
                    ", time: " + presentationTimeUs + "us");

            // Drain encoder output
            drainEncoder(videoEncoder, false);

        } catch (Exception e) {
            Log.e(TAG, "Error writing video frame", e);
        }
    }

    /**
     * Write audio data to encoder
     */
    public void writeAudioData(byte[] audioData, long timestampUs) {
        if (!isRecording || audioEncoder == null) {
            Log.w(TAG, "Cannot write audio data - not recording or encoder null");
            return;
        }

        try {
            if (audioStartTimeUs == -1) {
                audioStartTimeUs = timestampUs;
                Log.d(TAG, "Audio start time set: " + audioStartTimeUs);
            }

            long presentationTimeUs = timestampUs - audioStartTimeUs;

            Log.d(TAG, "Writing audio data, size: " + audioData.length +
                    ", time: " + presentationTimeUs + "us");

            // Get input buffer
            int inputBufferIndex = audioEncoder.dequeueInputBuffer(10000);
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = audioEncoder.getInputBuffer(inputBufferIndex);
                if (inputBuffer != null) {
                    inputBuffer.clear();
                    inputBuffer.put(audioData);

                    audioEncoder.queueInputBuffer(
                            inputBufferIndex,
                            0,
                            audioData.length,
                            presentationTimeUs,
                            0
                    );
                }
            }

            // Drain encoder output
            drainEncoder(audioEncoder, true);

        } catch (Exception e) {
            Log.e(TAG, "Error writing audio data", e);
        }
    }

    /**
     * Drain encoder output to muxer
     */
    private void drainEncoder(MediaCodec encoder, boolean isAudio) {
        if (encoder == null) return;

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        while (true) {
            int encoderStatus = encoder.dequeueOutputBuffer(bufferInfo, 10000);

            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // No output available yet
                break;

            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // Format changed - add track to muxer
                MediaFormat newFormat = encoder.getOutputFormat();
                Log.d(TAG, "Encoder output format changed: " + newFormat);

                synchronized (this) {
                    if (isAudio) {
                        if (audioTrackIndex == -1) {
                            audioTrackIndex = mediaMuxer.addTrack(newFormat);
                            Log.d(TAG, "Audio track added: " + audioTrackIndex);
                        }
                    } else {
                        if (videoTrackIndex == -1) {
                            videoTrackIndex = mediaMuxer.addTrack(newFormat);
                            Log.d(TAG, "Video track added: " + videoTrackIndex);
                        }
                    }

                    // Start muxer if both tracks are ready
                    if (!muxerStarted && videoTrackIndex != -1 && audioTrackIndex != -1) {
                        mediaMuxer.start();
                        muxerStarted = true;
                        Log.d(TAG, "Muxer started");
                    }
                }

            } else if (encoderStatus >= 0) {
                // Valid output buffer
                ByteBuffer encodedData = encoder.getOutputBuffer(encoderStatus);
                if (encodedData == null) {
                    Log.w(TAG, "Encoder output buffer " + encoderStatus + " was null");
                    encoder.releaseOutputBuffer(encoderStatus, false);
                    continue;
                }

                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // Codec config buffer
                    Log.d(TAG, "Codec config buffer");
                    bufferInfo.size = 0;
                }

                if (bufferInfo.size > 0) {
                    if (muxerStarted) {
                        int trackIndex = isAudio ? audioTrackIndex : videoTrackIndex;
                        if (trackIndex >= 0) {
                            encodedData.position(bufferInfo.offset);
                            encodedData.limit(bufferInfo.offset + bufferInfo.size);

                            mediaMuxer.writeSampleData(trackIndex, encodedData, bufferInfo);
                            Log.d(TAG, "Wrote " + (isAudio ? "audio" : "video") +
                                    " sample, size: " + bufferInfo.size +
                                    ", time: " + bufferInfo.presentationTimeUs + "us");
                        }
                    } else {
                        Log.w(TAG, "Muxer not started, dropping frame");
                    }
                }

                encoder.releaseOutputBuffer(encoderStatus, false);

                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(TAG, "End of stream reached for " + (isAudio ? "audio" : "video"));
                    break;
                }
            }
        }
    }

    /**
     * Stop recording and finalize the file
     */
    public void stopRecording() {
        Log.d(TAG, "stopRecording called");

        isRecording = false;

        try {
            // Signal end of stream to encoders
            if (videoEncoder != null && videoEncoderConfigured) {
                Log.d(TAG, "Signaling EOS to video encoder");
                signalEndOfStream(videoEncoder, false);
                drainEncoder(videoEncoder, false);
            }

            if (audioEncoder != null && audioEncoderConfigured) {
                Log.d(TAG, "Signaling EOS to audio encoder");
                signalEndOfStream(audioEncoder, true);
                drainEncoder(audioEncoder, true);
            }

            // Stop and release resources
            releaseResources();

            // Verify file was created
            File outputFile = new File(outputPath);
            if (outputFile.exists()) {
                long fileSize = outputFile.length();
                Log.i(TAG, "Recording stopped. File: " + outputPath +
                        ", Size: " + fileSize + " bytes");

                if (fileSize == 0) {
                    Log.w(TAG, "WARNING: Output file is 0 bytes!");
                }
            } else {
                Log.w(TAG, "Output file does not exist: " + outputPath);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error stopping recording", e);
        }
    }

    /**
     * Signal end of stream to encoder
     */
    private void signalEndOfStream(MediaCodec encoder, boolean isAudio) {
        try {
            int inputBufferIndex = encoder.dequeueInputBuffer(10000);
            if (inputBufferIndex >= 0) {
                encoder.queueInputBuffer(
                        inputBufferIndex,
                        0,
                        0,
                        0,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                );
                Log.d(TAG, "EOS queued for " + (isAudio ? "audio" : "video") + " encoder");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error signaling EOS", e);
        }
    }

    /**
     * Release all resources
     */
    private void releaseResources() {
        Log.d(TAG, "Releasing resources");

        try {
            if (inputSurface != null) {
                inputSurface.release();
                inputSurface = null;
            }

            if (videoEncoder != null) {
                videoEncoder.stop();
                videoEncoder.release();
                videoEncoder = null;
                videoEncoderConfigured = false;
            }

            if (audioEncoder != null) {
                audioEncoder.stop();
                audioEncoder.release();
                audioEncoder = null;
                audioEncoderConfigured = false;
            }

            if (mediaMuxer != null) {
                if (muxerStarted) {
                    mediaMuxer.stop();
                }
                mediaMuxer.release();
                mediaMuxer = null;
                muxerStarted = false;
            }

            videoTrackIndex = -1;
            audioTrackIndex = -1;

        } catch (Exception e) {
            Log.e(TAG, "Error releasing resources", e);
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
                long size = file.length();
                Log.d(TAG, "File size check: " + size + " bytes");
                return size;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting file size", e);
        }
        return 0;
    }

    /**
     * Force stop recording (emergency cleanup)
     */
    public void forceStop() {
        Log.w(TAG, "Force stopping recording");
        isRecording = false;
        releaseResources();
    }
}