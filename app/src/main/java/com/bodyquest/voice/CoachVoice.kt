package com.bodyquest.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Thin wrapper around Android's on-device TTS for real-time coaching cues (spec §15).
 * Utterances are short and QUEUE_FLUSH so a new correction interrupts a stale one instead
 * of queuing behind it — the athlete should always hear the *current* state, not a backlog.
 */
class CoachVoice(context: Context) {
    private var ready = false
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        ready = status == TextToSpeech.SUCCESS
        if (ready) tts?.language = Locale.US
    }.also { it.setOnUtteranceProgressListener(NoopUtteranceListener) }

    /**
     * Speaks [text] immediately, interrupting whatever is currently playing. Callers are
     * responsible for not calling this every frame — an unresolved correction *should* be
     * repeated rep after rep, so there's no text-based dedupe here; throttle at the call site
     * (e.g. keying a LaunchedEffect on the state that changed) instead.
     */
    fun speak(text: String) {
        if (!ready) return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, text.hashCode().toString())
    }

    fun close() {
        tts.stop()
        tts.shutdown()
    }

    private object NoopUtteranceListener : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {}
        override fun onDone(utteranceId: String?) {}
        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {}
    }
}
