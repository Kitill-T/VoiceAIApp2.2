package com.example.voiceaiapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.voiceaiapp.databinding.ActivityMainBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val audioRecorder = AudioRecorder()
    private var audioFile: File? = null

    // Индикатор записи
    private var recordHandler: Handler? = null
    private var recordStartTime: Long = 0L
    private var isServerOnline: Boolean = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            startRecording()
        } else {
            showToast("Необходимы разрешения для записи аудио")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupButtons()
        checkServerConnection()
    }

    private fun setupButtons() {
        binding.btnRecord.setOnClickListener {
            if (audioRecorder.isRecording()) {
                stopRecording()
            } else {
                checkAndRequestPermissions()
            }
        }

        binding.btnSend.setOnClickListener {
            audioFile?.let { file ->
                sendAudioToServer(file)
            } ?: showToast("Сначала запишите аудио")
        }

        binding.serverStatusBar.setOnClickListener {
            checkServerConnection()
        }
    }

    private fun checkAndRequestPermissions() {
        val requiredPermissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        val notGranted = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isEmpty()) {
            startRecording()
        } else {
            requestPermissionLauncher.launch(notGranted.toTypedArray())
        }
    }

    private fun createAudioFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File.createTempFile(
            "AUDIO_${timeStamp}_",
            ".wav",
            externalCacheDir
        )
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startRecording() {
        try {
            audioFile = createAudioFile()
            audioRecorder.startRecording(audioFile!!)
            binding.btnRecord.text = "Остановить запись"
            showToast("Запись начата")
            startRecordProgress()
        } catch (e: Exception) {
            showToast("Ошибка записи: ${e.localizedMessage}")
        }
    }

    private fun stopRecording() {
        audioRecorder.stopRecording()
        binding.btnRecord.text = "Начать запись"
        showToast("Запись сохранена: ${audioFile?.name}")
        stopRecordProgress()
    }

    // --- Вызов только ApiClient для отправки файла ---
    private fun sendAudioToServer(audioFile: File) {
        ApiClient.uploadAudio(
            file = audioFile,
            onSuccess = { transcription, aiResponse ->
                runOnUiThread {
                    showToast("Аудио успешно отправлено")
                    // Вы можете отобразить transcription и aiResponse в отдельном TextView
                    // Например:
                    // binding.tvTranscription.text = transcription
                    // binding.tvAIResponse.text = aiResponse
                }
            },
            onError = { errorMsg ->
                runOnUiThread {
                    showToast(errorMsg)
                }
            }
        )
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // --- Индикатор статуса сервера ---
    private fun updateServerStatus(isOnline: Boolean) {
        isServerOnline = isOnline
        val icon = if (isOnline) R.drawable.ic_circle_green else R.drawable.ic_circle_red
        val text = if (isOnline) "Сервер: онлайн" else "Сервер: оффлайн"
        binding.serverStatusIcon.setImageResource(icon)
        binding.serverStatusText.text = text
    }

    private fun checkServerConnection() {
        ApiClient.checkServerConnection { isOnline ->
            runOnUiThread { updateServerStatus(isOnline) }
        }
    }

    // --- Индикатор записи (таймер) ---
    private fun startRecordProgress() {
        recordStartTime = System.currentTimeMillis()
        binding.recordProgress.visibility = View.VISIBLE
        binding.recordProgress.text = "00:00"
        recordHandler = Handler(Looper.getMainLooper())
        recordHandler?.post(recordProgressRunnable)
    }

    private fun stopRecordProgress() {
        recordHandler?.removeCallbacks(recordProgressRunnable)
        binding.recordProgress.visibility = View.GONE
    }

    private val recordProgressRunnable = object : Runnable {
        override fun run() {
            val elapsed = (System.currentTimeMillis() - recordStartTime) / 1000
            val minutes = elapsed / 60
            val seconds = elapsed % 60
            binding.recordProgress.text = String.format("%02d:%02d", minutes, seconds)
            recordHandler?.postDelayed(this, 500)
        }
    }

    override fun onStop() {
        super.onStop()
        if (audioRecorder.isRecording()) {
            audioRecorder.stopRecording()
            stopRecordProgress()
        }
    }
}