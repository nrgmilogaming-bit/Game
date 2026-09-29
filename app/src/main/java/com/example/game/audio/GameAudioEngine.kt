package com.example.game.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

class GameAudioEngine {
    var isSoundEnabled: Boolean = true

    private val sampleRate = 22050
    private var rollingTrack: AudioTrack? = null
    private var rollJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    @Volatile
    var currentSpeedFraction: Float = 0f

    init {
        try {
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            rollingTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf.coerceAtLeast(sampleRate / 4))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            rollingTrack?.play()
            startRollingRumbleLoop()
        } catch (_: Exception) {
            // AudioTrack init fallback
        }
    }

    private fun startRollingRumbleLoop() {
        rollJob = scope.launch {
            val track = rollingTrack ?: return@launch
            val bufferSize = 1024
            val buffer = ShortArray(bufferSize)
            var phase = 0.0

            while (isActive) {
                if (!isSoundEnabled || currentSpeedFraction < 0.05f) {
                    buffer.fill(0)
                    track.write(buffer, 0, bufferSize)
                    kotlinx.coroutines.delay(30)
                    continue
                }

                // Rolling rumble frequency: 60Hz to 180Hz based on speed
                val speed = currentSpeedFraction.coerceIn(0f, 1f)
                val freq = 60.0 + speed * 120.0
                val volume = (speed * 0.35f).coerceIn(0f, 0.4f)
                val phaseInc = 2.0 * PI * freq / sampleRate

                for (i in 0 until bufferSize) {
                    // Layer low sine with a bit of noise texture for realistic gravel/surface rolling
                    val noise = (Math.random() * 2.0 - 1.0) * 0.15
                    val s = sin(phase) * 0.85 + noise
                    buffer[i] = (s * volume * Short.MAX_VALUE).toInt().toShort()
                    phase += phaseInc
                    if (phase > 2.0 * PI) phase -= 2.0 * PI
                }
                track.write(buffer, 0, bufferSize)
            }
        }
    }

    fun playGemPickup(combo: Int = 0) {
        if (!isSoundEnabled) return
        scope.launch {
            val baseFreq = 523.25 // C5
            val semitone = (combo % 8) * 2
            val freq = baseFreq * Math.pow(1.05946, semitone.toDouble())
            playTone(freq, durationMs = 120, attackMs = 10, decayMs = 110, volume = 0.45f)
        }
    }

    fun playJumpPad() {
        if (!isSoundEnabled) return
        scope.launch {
            playSweep(startFreq = 220.0, endFreq = 580.0, durationMs = 160, volume = 0.5f)
        }
    }

    fun playSpeedBoost() {
        if (!isSoundEnabled) return
        scope.launch {
            playSweep(startFreq = 300.0, endFreq = 900.0, durationMs = 240, volume = 0.45f)
        }
    }

    fun playCollision(intensity: Float = 0.5f) {
        if (!isSoundEnabled) return
        scope.launch {
            val clamped = intensity.coerceIn(0.1f, 1.0f)
            playTone(95.0, durationMs = 80, attackMs = 5, decayMs = 75, volume = 0.35f * clamped)
        }
    }

    fun playFall() {
        if (!isSoundEnabled) return
        scope.launch {
            playSweep(startFreq = 350.0, endFreq = 80.0, durationMs = 400, volume = 0.4f)
        }
    }

    fun playVictory() {
        if (!isSoundEnabled) return
        scope.launch {
            // Victorious fanfare arpeggio: C5 -> E5 -> G5 -> C6
            val notes = listOf(523.25, 659.25, 783.99, 1046.50)
            for (note in notes) {
                playTone(note, durationMs = 140, attackMs = 10, decayMs = 120, volume = 0.5f)
                kotlinx.coroutines.delay(100)
            }
        }
    }

    private fun playTone(
        freq: Double,
        durationMs: Int,
        attackMs: Int = 10,
        decayMs: Int = 80,
        volume: Float = 0.5f
    ) {
        try {
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val samples = ShortArray(numSamples)
            val phaseInc = 2.0 * PI * freq / sampleRate
            var phase = 0.0

            val attackSamples = (sampleRate * (attackMs / 1000.0)).toInt().coerceAtLeast(1)
            val decaySamples = (sampleRate * (decayMs / 1000.0)).toInt().coerceAtLeast(1)

            for (i in 0 until numSamples) {
                var env = 1.0f
                if (i < attackSamples) {
                    env = i.toFloat() / attackSamples
                } else {
                    val remaining = numSamples - i
                    if (remaining < decaySamples) {
                        env = remaining.toFloat() / decaySamples
                    }
                }
                val sampleVal = sin(phase) * env * volume
                samples[i] = (sampleVal * Short.MAX_VALUE).toInt().toShort()
                phase += phaseInc
            }

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(samples, 0, samples.size)
            track.play()
            scope.launch {
                kotlinx.coroutines.delay(durationMs.toLong() + 50)
                try {
                    track.stop()
                    track.release()
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    private fun playSweep(
        startFreq: Double,
        endFreq: Double,
        durationMs: Int,
        volume: Float = 0.5f
    ) {
        try {
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val samples = ShortArray(numSamples)
            var phase = 0.0

            for (i in 0 until numSamples) {
                val t = i.toFloat() / numSamples
                val currentFreq = startFreq + (endFreq - startFreq) * t
                val phaseInc = 2.0 * PI * currentFreq / sampleRate
                val env = sin(t * PI).toFloat() // Smooth arc
                val sampleVal = sin(phase) * env * volume
                samples[i] = (sampleVal * Short.MAX_VALUE).toInt().toShort()
                phase += phaseInc
            }

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(samples, 0, samples.size)
            track.play()
            scope.launch {
                kotlinx.coroutines.delay(durationMs.toLong() + 50)
                try {
                    track.stop()
                    track.release()
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    fun release() {
        rollJob?.cancel()
        try {
            rollingTrack?.stop()
            rollingTrack?.release()
        } catch (_: Exception) {}
    }
}
