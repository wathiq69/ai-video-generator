package com.wathiq.aivideo.util

import android.graphics.Bitmap
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
            framesPerImage: Int,
            onProgress: (Int) -> Unit,
            onDone: (Boolean) -> Unit
        ) {
            Thread {
                try {
                    val bitrate = 2000000
                    val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
                    format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
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
                    var totalFramesEncoded = 0
                    val totalFramesToEncode = bitmaps.size * framesPerImage
                    
                    while (totalFramesEncoded < totalFramesToEncode) {
                        val inputBufferIndex = encoder.dequeueInputBuffer(10000)
                        if (inputBufferIndex >= 0) {
                            val inputBuffer = encoder.getInputBuffer(inputBufferIndex)!!
                            val currentBitmapIndex = totalFramesEncoded / framesPerImage
                            val bitmap = bitmaps[currentBitmapIndex.coerceAtMost(bitmaps.size - 1)]
                            
                            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
                            val yuv = convertArgbToYUV420(scaledBitmap, width, height)
                            
                            inputBuffer.clear()
                            inputBuffer.put(yuv)
                            
                            val pts = (totalFramesEncoded * 1000000L / fps)
                            if (totalFramesEncoded == totalFramesToEncode - 1) {
                                encoder.queueInputBuffer(inputBufferIndex, 0, yuv.size, pts, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            } else {
                                encoder.queueInputBuffer(inputBufferIndex, 0, yuv.size, pts, 0)
                            }
                            totalFramesEncoded++
                            onProgress((totalFramesEncoded * 100) / totalFramesToEncode)
                        }
                        
                        var drainOutput = true
                        while (drainOutput) {
                            val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 10000)
                            if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                                drainOutput = false
                            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                                trackIndex = muxer.addTrack(encoder.outputFormat)
                                muxer.start()
                                muxerStarted = true
                            } else if (outputBufferIndex >= 0) {
                                val outputBuffer = encoder.getOutputBuffer(outputBufferIndex)!!
                                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                                    bufferInfo.size = 0
                                }
                                if (bufferInfo.size > 0 && muxerStarted) {
                                    outputBuffer.position(bufferInfo.offset)
                                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                                    muxer.writeSampleData(trackIndex, outputBuffer, bufferInfo)
                                }
                                encoder.releaseOutputBuffer(outputBufferIndex, false)
                                
                                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                    drainOutput = false
                                    totalFramesEncoded = totalFramesToEncode
                                }
                            }
                        }
                    }
                    
                    encoder.stop()
                    encoder.release()
                    if (muxerStarted) {
                        muxer.stop()
                    }
                    muxer.release()
                    onDone(true)
                } catch (e: Exception) {
                    Log.e(TAG, "Error generating video", e)
                    onDone(false)
                }
            }.start()
        }
        
        private fun convertArgbToYUV420(bitmap: Bitmap, width: Int, height: Int): ByteArray {
            val argb = IntArray(width * height)
            bitmap.getPixels(argb, 0, width, 0, 0, width, height)
            val yuv = ByteArray(width * height * 3 / 2)
            val ySize = width * height
            var yIndex = 0
            var uvIndex = ySize
            
            for (j in 0 until height) {
                for (i in 0 until width) {
                    val pixel = argb[j * width + i]
                    val R = (pixel shr 16) and 0xFF
                    val G = (pixel shr 8) and 0xFF
                    val B = pixel and 0xFF
                    
                    val Y = ((66 * R + 129 * G + 25 * B + 128) shr 8) + 16
                    val U = ((-38 * R - 74 * G + 112 * B + 128) shr 8) + 128
                    val V = ((112 * R - 94 * G - 18 * B + 128) shr 8) + 128
                    
                    yuv[yIndex++] = (if (Y < 0) 0 else if (Y > 255) 255 else Y).toByte()
                    
                    if (j % 2 == 0 && i % 2 == 0) {
                        yuv[uvIndex++] = (if (U < 0) 0 else if (U > 255) 255 else U).toByte()
                        yuv[uvIndex++] = (if (V < 0) 0 else if (V > 255) 255 else V).toByte()
                    }
                }
            }
            return yuv
        }
    }
}