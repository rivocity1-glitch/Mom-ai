package com.momai.assistant

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var status: TextView
    private lateinit var transcript: TextView
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private val speechRequest = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(32), dp(24), dp(24))
            setBackgroundColor(0xFFF6F8F5.toInt())
        }
        root.addView(TextView(this).apply {
            text = "Mom AI"
            textSize = 32f
            gravity = Gravity.CENTER
            setTextColor(0xFF174B35.toInt())
            setPadding(0, 0, 0, dp(10))
        }, matchWrap())

        root.addView(TextView(this).apply {
            text = "आई, माझ्याशी मराठीत बोला"
            textSize = 21f
            gravity = Gravity.CENTER
            setTextColor(0xFF222222.toInt())
            setPadding(0, 0, 0, dp(24))
        }, matchWrap())

        root.addView(Button(this).apply {
            text = "🎙️  बोला"
            textSize = 25f
            isAllCaps = false
            setOnClickListener { listen() }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(76)).apply {
            bottomMargin = dp(18)
        })

        status = TextView(this).apply {
            text = "तयार आहे"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(0xFF236747.toInt())
        }
        root.addView(status, matchWrap())

        root.addView(TextView(this).apply {
            text = "तुम्ही म्हणालात:"
            textSize = 16f
            setTextColor(0xFF333333.toInt())
            setPadding(0, dp(22), 0, dp(8))
        }, matchWrap())

        transcript = TextView(this).apply {
            text = "तुमचे बोलणे येथे दिसेल."
            textSize = 20f
            setTextColor(0xFF161616.toInt())
            setPadding(dp(14), dp(14), dp(14), dp(14))
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        root.addView(transcript, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        root.addView(TextView(this).apply {
            text = "ही सुरुवातीची चाचणी आवृत्ती आहे. फोन करण्याची सुविधा अजून जोडलेली नाही."
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(0xFF626762.toInt())
            setPadding(0, dp(18), 0, 0)
        }, matchWrap())

        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun listen() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "mr-IN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "mr-IN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "मराठीत बोला")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        try {
            status.text = "ऐकत आहे..."
            startActivityForResult(intent, speechRequest)
        } catch (_: ActivityNotFoundException) {
            status.text = "आवाज ओळखण्याची सुविधा उपलब्ध नाही."
            speak("माफ करा, आवाज ओळखण्याची सुविधा उपलब्ध नाही.")
        } catch (_: Exception) {
            status.text = "आवाज सुरू करता आला नाही."
        }
    }

    @Deprecated("Kept for compatibility with the starter implementation")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != speechRequest) return
        if (resultCode != RESULT_OK) {
            status.text = "पुन्हा प्रयत्न करण्यासाठी बटण दाबा."
            return
        }
        val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
        transcript.text = spoken.ifBlank { "काही ऐकू आले नाही." }
        val reply = when {
            spoken.contains("नमस्कार") || spoken.contains("हॅलो") -> "नमस्कार आई. मी ऐकत आहे."
            spoken.contains("धन्यवाद") -> "काही हरकत नाही."
            spoken.contains("फोन कर") || spoken.contains("कॉल कर") ->
                "मी अजून फोन करू शकत नाही. पुढील आवृत्तीत आधी तुमची पुष्टी विचारेन."
            spoken.isBlank() -> "मला नीट ऐकू आले नाही. पुन्हा बोला."
            else -> "मला ऐकू आले: $spoken"
        }
        status.text = reply
        speak(reply)
    }

    override fun onInit(result: Int) {
        if (result == TextToSpeech.SUCCESS) {
            val languageResult = tts?.setLanguage(Locale("mr", "IN")) ?: TextToSpeech.LANG_NOT_SUPPORTED
            ttsReady = languageResult != TextToSpeech.LANG_MISSING_DATA &&
                languageResult != TextToSpeech.LANG_NOT_SUPPORTED
            if (!ttsReady) tts?.setLanguage(Locale("hi", "IN"))
        }
    }

    private fun speak(text: String) {
        if (ttsReady) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "mom-ai-reply")
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun matchWrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    )

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
