package com.marcel.personaltrainer

import android.content.Context
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.marcel.personaltrainer.ui.TimerSound
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect

@Composable
fun TimerSoundEffects(events: Flow<TimerSound>) {
    val context = LocalContext.current
    val player = remember(context) { TimerSoundPlayer(context) }

    DisposableEffect(player) {
        onDispose(player::release)
    }
    LaunchedEffect(events) {
        events.collect(player::play)
    }
}

private class TimerSoundPlayer(context: Context) {
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
    private val completionRingtone: Ringtone? = RingtoneManager.getRingtone(
        context,
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
    )

    fun play(sound: TimerSound) {
        when (sound) {
            TimerSound.COUNTDOWN -> toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
            TimerSound.COMPLETE -> {
                if (completionRingtone != null) {
                    completionRingtone.stop()
                    completionRingtone.play()
                } else {
                    toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 700)
                }
            }
        }
    }

    fun release() {
        completionRingtone?.stop()
        toneGenerator.release()
    }
}
