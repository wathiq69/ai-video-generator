package com.wathiq.aivideo.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.nio.ByteBuffer

class VideoGenerator {
    companion object {
        private const val TAG = "VideoGen"

        fun generateVideo(
            bitmaps: List<Bitmap>,
            outputFile: File,
            width: Int,
            height: Int,
            fps: Int,
            durationPerImageSec: Int,
            onProgress: (Int) -> Unit,
            onDone: (Boolean) -> Unit
        ) {
            Thread {
                try {
                    val bitrate = 4000000
                    val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
                    format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar)
                    format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
                    format.setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                    format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)

                    val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
                    encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                    encoder.start()

                    val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                    var trackIndex = -1
                    var muxerStarted = false

                    val bufferInfo = MediaCodec.BufferInfo()
                    val framesPerImage = fps * durationPerImageSec
                    val totalFrames = bitmaps.size * framesPerImage
                    var framesGenerated = 0

                    for ((imgIndex, bitmap) in bitmaps.withIndex()) {
                        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)

                        for (frame in 0 until framesPerImage) {
                            val progress = frame.toFloat() / framesPerImage

                            // Ken Burns zoom effect
                            val zoom = 1.0f + 0.15f * progress
                            val srcW = (width / zoom).toInt().coerceAtLeast(1)
                            val srcH = (height / zoom).toInt().coerceAtLeast(1)
                            val srcX = ((width - srcW) / 2f * progress).toInt()
                            val srcY = ((height - srcH) / 2f * progress).toInt()

                            val cropped = Bitmap.createBitmap(scaledBitmap, srcX, srcY, srcW, srcH)
                            val finalBitmap = Bitmap.createScaledBitmap(cropped, width, height, true)

                            val yuv = convertToI420(finalBitmap, width, height)

                            val inputBufferIndex = encoder.dequeueInputBuffer(10000)
                            if (inputBufferIndex >= 0) {
                                val inputBuffer = encoder.getInputBuffer(inputBufferIndex)!!
                                inputBuffer.clear()
                                inputBuffer.put(yuv)

                                val pts = (framesGenerated * 1000000L / fps)
                                val flags = if (framesGenerated == totalFrames - 1) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                                encoder.queueInputBuffer(inputBufferIndex, 0, yuv.size, pts, flags)
                            }

                            framesGenerated++
                            onProgress((framesGenerated * 100) / totalFrames)

                            drainEncoder(encoder, bufferInfo, muxer, { idx, started ->
                                trackIndex = idx
                                muxerStarted = started
                            }, trackIndex, muxerStarted, false)

                            finalBitmap.recycle()
                            if (cropped != scaledBitmap) cropped.recycle()
                        }
                    }

                    drainEncoder(encoder, bufferInfo, muxer, { idx, started ->
                        trackIndex = idx
                        muxerStarted = started
                    }, trackIndex, muxerStarted, true)

                    encoder.stop()
                    encoder.release()
                    if (muxerStarted) muxer.stop()
                    muxer.release()

                    onDone(true)
                } catch (e: Exception) {
                    Log.e(TAG, "Error generating video", e)
                    onDone(false)
                }
            }.start()
        }

        private fun drainEncoder(
            encoder: MediaCodec,
            bufferInfo: MediaCodec.BufferInfo,
            muxer: MediaMuxer,
            onFormatChange: (Int, Boolean) -> Unit,
            trackIndex: Int,
            muxerStarted: Boolean,
            endOfStream: Boolean
        ) {
            var trackIdx = trackIndex
            var started = muxerStarted

            while (true) {
                val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, if (endOfStream) 10000 else 0)
                if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (!endOfStream) break
                } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    trackIdx = muxer.addTrack(encoder.outputFormat)
                    muxer.start()
                    started = true
                    onFormatChange(trackIdx, started)
                } else if (outputBufferIndex >= 0) {
                    val outputBuffer = encoder.getOutputBuffer(outputBufferIndex)!!
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        bufferInfo.size = 0
                    }
                    if (bufferInfo.size > 0 && started) {
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(trackIdx, outputBuffer, bufferInfo)
                    }
                    encoder.releaseOutputBuffer(outputBufferIndex, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                }
            }
        }

        private fun convertToI420(bitmap: Bitmap, width: Int, height: Int): ByteArray {
            val argb = IntArray(width * height)
            bitmap.getPixels(argb, 0, width, 0, 0, width, height)
            val ySize = width * height
            val uvSize = ySize / 4
            val yuv = ByteArray(ySize + uvSize * 2)

            var yIndex = 0
            var uIndex = ySize
            var vIndex = ySize + uvSize

            for (j in 0 until height) {
                for (i in 0 until width) {
                    val pixel = argb[j * width + i]
                    val R = (pixel shr 16) and 0xFF
                    val G = (pixel shr 8) and 0xFF
                    val B = pixel and 0xFF

                    val Y = ((66 * R + 129 * G + 25 * B + 128) shr 8) + 16
                    yuv[yIndex++] = (if (Y < 0) 0 else if (Y > 255) 255 else Y).toByte()

                    if (j % 2 == 0 && i % 2 == 0) {
                        val U = ((-38 * R - 74 * G + 112 * B + 128) shr 8) + 128
                        val V = ((112 * R - 94 * G - 18 * B + 128) shr 8) + 128
                        yuv[uIndex++] = (if (U < 0) 0 else if (U > 255) 255 else U).toByte()
                        yuv[vIndex++] = (if (V < 0) 0 else if (V > 255) 255 else V).toByte()
                    }
                }
            }
            return yuv
        }
    }
}
