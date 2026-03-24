package com.stavros.timechime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.sin

private const val WAKE_LOCK_TAG = "timechime:chime"
private const val WAKE_LOCK_TIMEOUT_MS = 10_000L

// Duration of a single short beep (for non-zero counts).
private const val SHORT_BEEP_MS = 200

// Duration of the single long beep played at the top of the hour.
private const val LONG_BEEP_MS = 400

// Silence gap between consecutive short beeps.
private const val GAP_BETWEEN_BEEPS_MS = 100

private const val SAMPLE_RATE_HZ = 44100
private const val TONE_FREQUENCY_HZ = 10000.0

// Generates a PCM 16-bit mono sine wave buffer at TONE_FREQUENCY_HZ for the given duration,
// plays it via AudioTrack in MODE_STATIC (which auto-stops when the buffer is exhausted),
// and schedules a release on the provided handler after the duration elapses.
internal fun playBeep(durationMs: Int, handler: Handler) {
    val sampleCount = SAMPLE_RATE_HZ * durationMs / 1000
    val buffer = ShortArray(sampleCount) { index ->
        val angle = 2.0 * PI * TONE_FREQUENCY_HZ * index / SAMPLE_RATE_HZ
        (sin(angle) * Short.MAX_VALUE).toInt().toShort()
    }

    val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    val audioFormat = AudioFormat.Builder()
        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        .setSampleRate(SAMPLE_RATE_HZ)
        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
        .build()

    val track = AudioTrack.Builder()
        .setAudioAttributes(audioAttributes)
        .setAudioFormat(audioFormat)
        .setBufferSizeInBytes(sampleCount * 2)
        .setTransferMode(AudioTrack.MODE_STATIC)
        .build()

    track.write(buffer, 0, sampleCount)
    track.play()

    // AudioTrack in MODE_STATIC stops automatically when the buffer is exhausted.
    // We still need to release it explicitly to free the native resource.
    handler.postDelayed({ track.release() }, durationMs.toLong())
}

// Plays `count` short beeps in-process from the UI thread. Creates its own Handler so it
// can be called from anywhere without needing an existing looper reference. No wake lock or
// PendingResult is involved — this is purely for in-process use (e.g. the test button).
internal fun playBeepSequence(count: Int) {
    val handler = Handler(Looper.getMainLooper())
    scheduleBeepsOnHandler(handler, count, 0L)
}

private fun scheduleBeepsOnHandler(handler: Handler, remaining: Int, delayMillis: Long) {
    if (remaining <= 0) return

    handler.postDelayed({
        playBeep(SHORT_BEEP_MS, handler)
    }, delayMillis)

    scheduleBeepsOnHandler(
        handler = handler,
        remaining = remaining - 1,
        delayMillis = delayMillis + SHORT_BEEP_MS + GAP_BETWEEN_BEEPS_MS,
    )
}

class ChimeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // goAsync() extends the receiver's active lifetime past the return of onReceive() so
        // the system does not kill the process before the Handler runnables finish playing beeps.
        val pendingResult = goAsync()

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
        wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS)

        val minutes = Calendar.getInstance().get(Calendar.MINUTE)
        val count = minutes / 10

        val handler = Handler(Looper.getMainLooper())

        if (count == 0) {
            // Top of the hour: one long beep.
            playBeep(LONG_BEEP_MS, handler)
            handler.postDelayed({
                wakeLock.release()
                pendingResult.finish()
            }, LONG_BEEP_MS.toLong())
        } else {
            // Schedule `count` short beeps, then release resources after the last one finishes.
            val totalDurationMs = (count * SHORT_BEEP_MS + (count - 1) * GAP_BETWEEN_BEEPS_MS).toLong()
            playBeepSequence(count)
            handler.postDelayed({
                wakeLock.release()
                pendingResult.finish()
            }, totalDurationMs)
        }

        scheduleNextChimeAlarm(context)
    }
}
