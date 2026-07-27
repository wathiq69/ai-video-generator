package com.wathiq.aivideo.ui

import android.content.ContentValues
import android.content.Intent
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
            val token = binding.etToken.text.toString().trim()
            if (prompt.isEmpty() && selectedImageUri == null) {
                Toast.makeText(this, R.string.enter_prompt, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            generateVideo(prompt, token)
        }

        binding.btnSave.setOnClickListener { saveVideoToGallery() }
        binding.btnShare.setOnClickListener { shareVideo() }
    }

    private fun generateVideo(prompt: String, hfToken: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.progressBar.progress = 0
        binding.btnGenerate.isEnabled = false
        binding.tvStatus.text = getString(R.string.generating)
        binding.tvStatus.visibility = View.VISIBLE
        binding.videoView.visibility = View.GONE
        binding.layoutButtons.visibility = View.GONE

        val finalPrompt = if (prompt.isEmpty()) "cinematic shot, abstract art" else prompt

        lifecycleScope.launch {
            try {
                val outputFile = File(cacheDir, "generated_video.mp4")
                if (outputFile.exists()) outputFile.delete()

                val success = VideoGenerator.generateVideoFromText(
                    prompt = finalPrompt,
                    hfToken = hfToken,
                    outputFile = outputFile,
                    onStatus = { msg ->
                        runOnUiThread { binding.tvStatus.text = msg }
                    },
                    onProgress = { progress ->
                        runOnUiThread { 
                            binding.progressBar.progress = progress
                            if (progress < 100) {
                                binding.tvStatus.text = "Processing: $progress%"
                            }
                        }
                    }
                )

                withContext(Dispatchers.Main) {
                    if (success && outputFile.exists() && outputFile.length() > 1000) {
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
            } catch (e: Exception) {
                Log.e("MainActivity", "Error", e)
                withContext(Dispatchers.Main) {
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
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_video)))
    }
}
