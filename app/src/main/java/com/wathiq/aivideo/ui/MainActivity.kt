package com.wathiq.aivideo.ui

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
                Toast.makeText(this, R.string.enter_prompt, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            generateVideo(prompt)
        }

        binding.btnSave.setOnClickListener { saveVideoToGallery() }
        binding.btnShare.setOnClickListener { shareVideo() }
    }

    private fun generateVideo(prompt: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.progressBar.progress = 0
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
                    // 1. Add user image if exists
                    selectedImageUri?.let { uri ->
                        val inputStream = contentResolver.openInputStream(uri)
                        val bmp = BitmapFactory.decodeStream(inputStream)
                        bmp?.let { bitmaps.add(it) }
                    }

                    // 2. Generate images from Pollinations AI
                    val framesNeeded = 4 - bitmaps.size
                    for (i in 1..framesNeeded.coerceAtLeast(3)) {
                        val seed = Random.nextInt(1000, 9999)
                        val encodedPrompt = URLEncoder.encode(if (prompt.isEmpty()) "cinematic shot, abstract art" else prompt, "UTF-8")
                        val apiUrl = "https://image.pollinations.ai/prompt/$encodedPrompt?width=$width&height=$height&seed=$seed&nologo=true"
                        
                        updateStatus(getString(R.string.generating_image, i, framesNeeded.coerceAtLeast(3)))
                        
                        try {
                            val url = URL(apiUrl)
                            val conn = url.openConnection() as HttpURLConnection
                            conn.connectTimeout = 30000
                            conn.readTimeout = 30000
                            conn.instanceFollowRedirects = true
                            
                            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                                val input: InputStream = conn.inputStream
                                val bmp = BitmapFactory.decodeStream(input)
                                if (bmp != null) {
                                    bitmaps.add(bmp)
                                }
                            }
                            conn.disconnect()
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Image $i failed: ${e.message}")
                        }
                        
                        // If image failed, add a placeholder
                        if (bitmaps.size < i) {
                            val placeholder = createPlaceholderBitmap(width, height, "Frame $i")
                            bitmaps.add(placeholder)
                        }
                    }
                }

                if (bitmaps.isEmpty()) {
                    throw Exception("No images generated")
                }

                updateStatus(getString(R.string.merging_video))
                
                // 3. Generate video locally
                val outputFile = File(cacheDir, "generated_video.mp4")
                if (outputFile.exists()) outputFile.delete()

                VideoGenerator.generateVideo(
                    bitmaps = bitmaps,
                    outputFile = outputFile,
                    width = width,
                    height = height,
                    fps = 24,
                    framesPerImage = 72, // 3 seconds per image
                    onProgress = { progress -> 
                        runOnUiThread { 
                            binding.progressBar.progress = progress
                            binding.tvStatus.text = getString(R.string.processing, progress)
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
                                binding.btnSave.performClick()
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

    private fun createPlaceholderBitmap(width: Int, height: Int, text: String): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.parseColor("#1A1A2E"))
        val paint = Paint().apply {
            color = Color.WHITE
            textSize = 48f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(text, width / 2f, height / 2f, paint)
        return bmp
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
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_video)))
    }

    private fun updateStatus(msg: String) {
        runOnUiThread { binding.tvStatus.text = msg }
    }
}
