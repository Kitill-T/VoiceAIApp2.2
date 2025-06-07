package com.example.voiceaiapp

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class AudioRecorder {
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var outputFile: File? = null

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startRecording(file: File) {
        if (isRecording) return

        outputFile = file
        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val encodingFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, encodingFormat)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            encodingFormat,
            bufferSize
        )

        try {
            audioRecord?.startRecording()
            isRecording = true

            Thread {
                writeWavHeader()
                writeAudioDataToFile(bufferSize)
            }.start()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            stopRecording()
        }
    }

    private fun writeWavHeader() {
        outputFile?.let { file ->
            FileOutputStream(file).use { fos ->
                val header = WavHeader.createWavHeader()
                fos.write(header)
            }
        }
    }

    private fun writeAudioDataToFile(bufferSize: Int) {
        val buffer = ByteArray(bufferSize)
        var fos: FileOutputStream? = null

        try {
            fos = FileOutputStream(outputFile, true)
            while (isRecording) {
                val bytesRead = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                if (bytesRead > 0) {
                    fos.write(buffer, 0, bytesRead)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            fos?.close()
        }
    }

    fun stopRecording() {
        if (!isRecording) return

        isRecording = false
        try {
            audioRecord?.stop()
            updateWavHeader()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } finally {
            audioRecord?.release()
            audioRecord = null
        }
    }

    private fun updateWavHeader() {
        outputFile?.let { file ->
            if (file.exists() && file.length() > 44) {
                // Реализация обновления WAV заголовка
            }
        }
    }

    fun isRecording(): Boolean = isRecording
}

object WavHeader {
    fun createWavHeader(): ByteArray {
        // Реализация WAV заголовка
        return byteArrayOf()
    }
}