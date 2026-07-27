package com.wathiq.aivideo.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.concurrent.TimeUnit

class VideoGenerator {
    companion object {
        private const val TAG = "VideoGen"
        
        private val client = OkHttpClient.Builder()
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()

        suspend fun generateVideoFromText(
            prompt: String,
            hfToken: String,
            outputFile: File,
            onStatus: (String) -> Unit,
            onProgress: (Int) -> Unit
        ): Boolean = withContext(Dispatchers.IO) {
            try {
                onStatus("Sending request to AI...")
                onProgress(10)

                // Try multiple text-to-video models
                val models = listOf(
                    "ali-vilab/model-scope-text-to-video",
                    "cerspense/zeroscope_v2_576w",
                    "damo-vilab/text-to-video-ms-1.7b"
                )

                var success = false
                for (model in models) {
                    onStatus("Trying model: ${model.substringAfterLast("/")}")
                    onProgress(30)

                    val result = tryHuggingFaceT2V(prompt, hfToken, model, outputFile, onStatus, onProgress)
                    if (result) {
                        success = true
                        break
                    }
                }

                if (!success) {
                    // Fallback: Generate image + animate with Pollinations
                    onStatus("Using fallback method...")
                    onProgress(50)
                    success = generateAnimatedFallback(prompt, outputFile, onStatus, onProgress)
                }

                success
            } catch (e: Exception) {
                Log.e(TAG, "Error generating video", e)
                false
            }
        }

        private suspend fun tryHuggingFaceT2V(
            prompt: String,
            token: String,
            model: String,
            outputFile: File,
            onStatus: (String) -> Unit,
            onProgress: (Int) -> Unit
        ): Boolean {
            return try {
                val apiUrl = "https://api-inference.huggingface.co/models/$model"
                
                val jsonBody = JSONObject().apply {
                    put("inputs", prompt)
                }

                val request = Request.Builder()
                    .url(apiUrl)
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("Content-Type", "application/json")
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                onStatus("Waiting for AI to generate video...")
                onProgress(50)

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    onStatus("Downloading video...")
                    onProgress(80)

                    val contentType = response.header("Content-Type", "")
                    
                    if (contentType.contains("video") || contentType.contains("octet-stream") || contentType.contains("binary")) {
                        val bytes = response.body?.bytes()
                        if (bytes != null && bytes.size > 1000) {
                            FileOutputStream(outputFile).use { fos ->
                                fos.write(bytes)
                            }
                            onProgress(100)
                            true
                        } else {
                            false
                        }
                    } else {
                        // Might be JSON error
                        val text = response.body?.string()
                        Log.e(TAG, "Unexpected response: $text")
                        false
                    }
                } else {
                    Log.e(TAG, "HTTP ${response.code}: ${response.message}")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "HF T2V error: ${e.message}")
                false
            }
        }

        private suspend fun generateAnimatedFallback(
            prompt: String,
            outputFile: File,
            onStatus: (String) -> Unit,
            onProgress: (Int) -> Unit
        ): Boolean {
            // Fallback: Use Pollinations to get a single image, then create a video with motion
            return try {
                onStatus("Generating image with Pollinations...")
                onProgress(60)

                val encodedPrompt = java.net.URLEncoder.encode(if (prompt.isEmpty()) "cinematic shot" else prompt, "UTF-8")
                val imageUrl = "https://image.pollinations.ai/prompt/$encodedPrompt?width=576&height=320&seed=${System.currentTimeMillis()}&nologo=true"

                val url = URL(imageUrl)
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 60000
                conn.readTimeout = 60000
                conn.instanceFollowRedirects = true

                if (conn.responseCode == java.net.HttpURLConnection.HTTP_OK) {
                    val bitmap = android.graphics.BitmapFactory.decodeStream(conn.inputStream)
                    conn.disconnect()

                    if (bitmap != null) {
                        onStatus("Creating video with motion effects...")
                        onProgress(80)

                        // Create a video with zoom and pan effects
                        val bitmaps = mutableListOf<android.graphics.Bitmap>()
                        val width = 576
                        val height = 320

                        // Create 5 frames with progressive zoom
                        for (i in 0..4) {
                            val zoom = 1.0f + 0.1f * i
                            val srcW = (width / zoom).toInt()
                            val srcH = (height / zoom).toInt()
                            val srcX = ((width - srcW) / 2).toInt()
                            val srcY = ((height - srcH) / 2).toInt()

                            val cropped = android.graphics.Bitmap.createBitmap(bitmap, srcX, srcY, srcW, srcH)
                            val scaled = android.graphics.Bitmap.createScaledBitmap(cropped, width, height, true)
                            bitmaps.add(scaled)
                            
                            if (i > 0) cropped.recycle()
                        }

                        onProgress(90)
                        
                        // Use MediaCodec to create video
                        createVideoFromBitmaps(bitmaps, outputFile, width, height, 8, 10, onProgress)
                    } else {
                        false
                    }
                } else {
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fallback error: ${e.message}")
                false
            }
        }

        private fun createVideoFromBitmaps(
            bitmaps: List<android.graphics.Bitmap>,
            outputFile: File,
            width: Int,
            height: Int,
            fps: Int,
            durationSec: Int,
            onProgress: (Int) -> Unit
        ): Boolean {
            return try {
                val format = android.media.MediaFormat.createVideoFormat(android.media.MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
                format.setInteger(android.media.MediaFormat.KEY_COLOR_FORMAT, android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar)
                format.setInteger(android.media.MediaFormat.KEY_BIT_RATE, 2000000)
                format.setInteger(android.media.MediaFormat.KEY_FRAME_RATE, fps)
                format.setInteger(android.media.MediaFormat.KEY_I_FRAME_INTERVAL, 1)

                val encoder = android.media.MediaCodec.createEncoderByType(android.media.MediaFormat.MIMETYPE_VIDEO_AVC)
                encoder.configure(format, null, null, android.media.MediaCodec.CONFIGURE_FLAG_ENCODE)
                encoder.start()

                val muxer = android.media.MediaMuxer(outputFile.absolutePath, android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                var trackIndex = -1
                var muxerStarted = false
                val bufferInfo = android.media.MediaCodec.BufferInfo()

                val totalFrames = fps * durationSec
                var frameIndex = 0

                while (frameIndex < totalFrames) {
                    val inputBufferIndex = encoder.dequeueInputBuffer(10000)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = encoder.getInputBuffer(inputBufferIndex)!!
                        val bitmapIndex = (frameIndex.toFloat() / totalFrames * bitmaps.size).toInt().coerceAtMost(bitmaps.size - 1)
                        val bmp = bitmaps[bitmapIndex]

                        val yuv = convertToI420(bmp, width, height)
                        inputBuffer.clear()
                        inputBuffer.put(yuv)

                        val pts = (frameIndex * 1000000L / fps)
                        val flags = if (frameIndex == totalFrames - 1) android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                        encoder.queueInputBuffer(inputBufferIndex, 0, yuv.size, pts, flags)
                        frameIndex++
                        onProgress(90 + (frameIndex * 10 / totalFrames))
                    }

                    var drain = true
                    while (drain) {
                        val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 10000)
                        if (outputBufferIndex == android.media.MediaCodec.INFO_TRY_AGAIN_LATER) {
                            drain = false
                        } else if (outputBufferIndex == android.media.MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            trackIndex = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        } else if (outputBufferIndex >= 0) {
                            val outputBuffer = encoder.getOutputBuffer(outputBufferIndex)!!
                            if (bufferInfo.flags and android.media.MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                                bufferInfo.size = 0
                            }
                            if (bufferInfo.size > 0 && muxerStarted) {
                                outputBuffer.position(bufferInfo.offset)
                                outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                                muxer.writeSampleData(trackIndex, outputBuffer, bufferInfo)
                            }
                            encoder.releaseOutputBuffer(outputBufferIndex, false)
                            if (bufferInfo.flags and android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                drain = false
                            }
                        }
                    }
                }

                encoder.stop()
                encoder.release()
                if (muxerStarted) muxer.stop()
                muxer.release()
                onProgress(100)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Video creation error: ${e.message}")
                false
            }
        }

        private fun convertToI420(bitmap: android.graphics.Bitmap, width: Int, height: Int): ByteArray {
            val argb = IntArray(width * height)
            bitmap.getPixels(argb, 0, width, 0, 0, width, height)
            val ySize = width * height
            val yuv = ByteArray(ySize + ySize / 2)

            var yIndex = 0
            var uIndex = ySize
            var vIndex = ySize + ySize / 4

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
