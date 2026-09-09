package com.bodyquest.llm

import android.content.Context
import android.util.Log
import com.bodyquest.coach.CoachingPhraseProvider
import com.bodyquest.coach.CoachingPhrases
import com.bodyquest.form.FormIssue
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "GemmaCoachEngine"
private const val MODEL_FILENAME = "gemma3-1b-it-int4.task"

/**
 * On-device Gemma [CoachingPhraseProvider]: rewords the same deterministic [FormIssue] the
 * static [CoachingPhrases] table already covers — it never invents which issue to raise or
 * changes a score (spec principle: LLM's job is phrasing, never the numbers).
 *
 * The model (~500MB+) is too large to bundle in the APK, so it's expected to already be
 * present under [Context.filesDir] (downloaded separately) — this app doesn't fetch it.
 * Until it's there and loaded, and on any generation failure, this falls back to
 * [CoachingPhrases] so coaching never goes silent.
 */
class GemmaCoachEngine(private val context: Context) : CoachingPhraseProvider {

    @Volatile private var inference: LlmInference? = null

    /** Loads the model off the main thread, mirroring [com.bodyquest.pose.PoseSession.warmUp].
     *  No-ops if the model file isn't on disk yet, or is already loaded. */
    suspend fun warmUp() {
        if (inference != null) return
        withContext(Dispatchers.Default) {
            val modelFile = File(context.filesDir, MODEL_FILENAME)
            if (!modelFile.exists()) {
                Log.i(TAG, "No Gemma model at ${modelFile.path} yet; using canned phrases")
                return@withContext
            }
            try {
                val options = LlmInferenceOptions.builder()
                    .setModelPath(modelFile.absolutePath)
                    .setMaxTokens(64)
                    .build()
                inference = LlmInference.createFromOptions(context, options)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load Gemma model, falling back to canned phrases", e)
            }
        }
    }

    /** Blocking by design, same contract as [com.bodyquest.pose.PoseEngine.process] — call
     *  from a background thread (e.g. the camera analysis thread), never from the UI thread. */
    override fun instructionFor(issue: FormIssue): String {
        val session = inference ?: return CoachingPhrases.instructionFor(issue)
        return try {
            session.generateResponse(promptFor(issue)).trim().ifBlank { CoachingPhrases.instructionFor(issue) }
        } catch (e: Exception) {
            Log.w(TAG, "Gemma generation failed for $issue, falling back to canned phrase", e)
            CoachingPhrases.instructionFor(issue)
        }
    }

    fun close() {
        inference?.close()
        inference = null
    }

    private fun promptFor(issue: FormIssue): String {
        val canned = CoachingPhrases.instructionFor(issue)
        return "You are a supportive squat coach. In one short sentence (under 12 words), " +
            "rephrase this correction naturally without changing its meaning or adding new " +
            "claims: \"$canned\""
    }
}
