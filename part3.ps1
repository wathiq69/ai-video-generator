 $RepoPath = "C:\Users\E1\Documents\GitHub\ai-video-generator"
 $java = "$RepoPath\app\src\main\java\com\wathiq\aivideo"

function W([string]$p, [string]$c) { 
    $e = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllText($p, $c, $e) 
}

# 1. proguard-rules.pro
W "$RepoPath\app\proguard-rules.pro" @'
-keep class com.wathiq.aivideo.** { *; }
'@

# 2. App.kt
W "$java\App.kt" @'
package com.wathiq.aivideo

import android.app.Application

class App : Application()
'@

# 3. VideoGenerator.kt (مولد الفيديو محلياً)
W "$java\util\VideoGenerator.kt" @'
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
'@

# 4. MainActivity.kt (الواجهة الرئيسية)
W "$java\ui\MainActivity.kt" @'
package com.wathiq.aivideo.ui

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.wathiq.aivideo.R
import com.wathiq.aivideo.databinding.ActivityMainBinding
import com.wathiq.aivideo.util.VideoGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var selectedImageUri: Uri? = null
    private var generatedVideoPath: String? = null

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            binding.tvImageStatus.text = getString(R.string.image_selected)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSelectImage.setOnClickListener { imagePicker.launch("image/*") }
        
        binding.btnGenerate.setOnClickListener {
            val prompt = binding.etPrompt.text.toString().trim()
            if (prompt.isEmpty() && selectedImageUri == null) {
                Toast.makeText(this, "الرجاء كتابة وصف أو اختيار صورة", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            generateVideo(prompt)
        }

        binding.btnSave.setOnClickListener { saveVideoToGallery() }
        binding.btnShare.setOnClickListener { shareVideo() }
    }

    private fun generateVideo(prompt: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnGenerate.isEnabled = false
        binding.tvStatus.text = getString(R.string.generating)
        binding.tvStatus.visibility = View.VISIBLE
        binding.videoView.visibility = View.GONE
        binding.layoutButtons.visibility = View.GONE

        val isPortrait = binding.rbPortrait.isChecked
        val width = if (isPortrait) 720 else 1280
        val height = if (isPortrait) 1280 else 720

        lifecycleScope.launch {
            val bitmaps = mutableListOf<Bitmap>()
            try {
                withContext(Dispatchers.IO) {
                    // 1. إضافة صورة المستخدم أولاً (إذا وجدت)
                    selectedImageUri?.let { uri ->
                        val inputStream = contentResolver.openInputStream(uri)
                        val bmp = BitmapFactory.decodeStream(inputStream)
                        bmp?.let { bitmaps.add(it) }
                    }

                    // 2. توليد 3 صور إضافية من Pollinations AI
                    val framesNeeded = 4 - bitmaps.size
                    for (i in 1..framesNeeded.coerceAtLeast(3)) {
                        val seed = Random.nextInt(1000, 9999)
                        val encodedPrompt = URLEncoder.encode(if (prompt.isEmpty()) "cinematic shot, abstract art" else prompt, "UTF-8")
                        val apiUrl = "https://image.pollinations.ai/prompt/$encodedPrompt?width=$width&height=$height&seed=$seed&nologo=true"
                        
                        updateStatus("جاري توليد الصورة $i من 3...")
                        
                        val url = URL(apiUrl)
                        val conn = url.openConnection() as HttpURLConnection
                        conn.connectTimeout = 60000
                        conn.readTimeout = 60000
                        conn.instanceFollowRedirects = true
                        
                        if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                            val input: InputStream = conn.inputStream
                            val bmp = BitmapFactory.decodeStream(input)
                            if (bmp != null) bitmaps.add(bmp)
                        }
                        conn.disconnect()
                    }
                }

                if (bitmaps.isEmpty()) {
                    throw Exception("لم يتم توليد أي صور")
                }

                updateStatus("جاري دمج الصور في فيديو...")
                
                // 3. توليد الفيديو محلياً
                val outputFile = File(cacheDir, "generated_video.mp4")
                if (outputFile.exists()) outputFile.delete()

                VideoGenerator.generateVideo(
                    bitmaps = bitmaps,
                    outputFile = outputFile,
                    width = width,
                    height = height,
                    fps = 24,
                    framesPerImage = 72, // 3 ثواني لكل صورة (24 * 3 = 72)
                    onProgress = { progress -> 
                        runOnUiThread { 
                            binding.progressBar.progress = progress
                            binding.tvStatus.text = "جاري المعالجة: $progress%"
                        }
                    },
                    onDone = { success ->
                        runOnUiThread {
                            if (success) {
                                generatedVideoPath = outputFile.absolutePath
                                binding.videoView.setVideoPath(generatedVideoPath)
                                binding.videoView.setOnPreparedListener { mp -> mp.isLooping = true }
                                binding.videoView.start()
                                binding.videoView.visibility = View.VISIBLE
                                binding.layoutButtons.visibility = View.VISIBLE
                                binding.tvStatus.text = getString(R.string.done)
                                binding.btnSave.performClick() // حفظ تلقائي
                            } else {
                                binding.tvStatus.text = getString(R.string.error)
                            }
                            binding.progressBar.visibility = View.GONE
                            binding.btnGenerate.isEnabled = true
                        }
                    }
                )

            } catch (e: Exception) {
                Log.e("MainActivity", "Error", e)
                runOnUiThread {
                    binding.tvStatus.text = getString(R.string.error)
                    binding.progressBar.visibility = View.GONE
                    binding.btnGenerate.isEnabled = true
                }
            }
        }
    }

    private fun saveVideoToGallery() {
        val path = generatedVideoPath ?: return
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "AI_Video_${System.currentTimeMillis()}.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/AI_Video_Maker")
            }
        }
        
        val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        uri?.let {
            contentResolver.openOutputStream(it)?.use { out ->
                File(path).inputStream().use { input ->
                    input.copyTo(out)
                }
            }
            Toast.makeText(this, getString(R.string.saved), Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareVideo() {
        val path = generatedVideoPath ?: return
        val uri = Uri.parse(path)
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
        }
        startActivity(Intent.createChooser(shareIntent, "مشاركة الفيديو"))
    }

    private fun updateStatus(msg: String) {
        runOnUiThread { binding.tvStatus.text = msg }
    }
}
'@

Write-Host "Part 3 Done: Kotlin files created successfully!" -ForegroundColor Green