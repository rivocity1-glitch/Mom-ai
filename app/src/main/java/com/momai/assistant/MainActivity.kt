package com.momai.assistant

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var status: TextView
    private lateinit var transcript: TextView
    private lateinit var listenButton: Button
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private val speechRequest = 1001

    private val ink = 0xFF18352A.toInt()
    private val green = 0xFF23734D.toInt()
    private val muted = 0xFF68756D.toInt()
    private val page = 0xFFF5F7F4.toInt()
    private val card = 0xFFFFFFFF.toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = page
        window.navigationBarColor = page
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
            View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        tts = TextToSpeech(this, this)
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(24), dp(22), dp(20))
            setBackgroundColor(page)
        }

        val brandRow = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp(8), 0, dp(24))
        }
        brandRow.addView(label("Mom AI", 30f, ink, true, Gravity.CENTER), matchWrap())
        brandRow.addView(label("तुमची मराठी बोलणारी मदतनीस", 17f, muted, false, Gravity.CENTER).apply {
            setPadding(0, dp(6), 0, 0)
        }, matchWrap())
        root.addView(brandRow, matchWrap())

        val welcome = panel().apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }
        welcome.addView(label("नमस्कार आई! 👋", 23f, ink, true), matchWrap())
        welcome.addView(label("काही विचारायचं असेल तर खालील बटण दाबून बोला.", 16f, muted).apply {
            setPadding(0, dp(8), 0, 0)
        }, matchWrap())
        root.addView(welcome, matchWrap())

        listenButton = Button(this).apply {
            text = "🎙   बोला"
            textSize = 23f
            isAllCaps = false
            setTextColor(card)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            background = rounded(green, 24)
            stateListAnimator = null
            minHeight = dp(78)
            setPadding(dp(20), dp(12), dp(20), dp(12))
            contentDescription = "मराठीत बोलण्यासाठी दाबा"
            setOnClickListener { listen() }
        }
        root.addView(listenButton, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(78)
        ).apply {
            topMargin = dp(18)
            bottomMargin = dp(14)
        })

        val statusPanel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(0xFFE7F2EA.toInt(), 18)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        val statusDot = View(this).apply {
            background = rounded(green, 50)
        }
        statusPanel.addView(statusDot, LinearLayout.LayoutParams(dp(10), dp(10)).apply {
            rightMargin = dp(10)
        })
        status = label("तयार आहे — बटण दाबा", 16f, ink, true)
        statusPanel.addView(status, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(statusPanel, matchWrap())

        root.addView(label("तुम्ही म्हणालात", 17f, ink, true).apply {
            setPadding(dp(2), dp(24), 0, dp(10))
        }, matchWrap())

        val transcriptPanel = panel().apply {
            orientation = LinearLayout.VERTICAL
            minimumHeight = dp(128)
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }
        transcript = label("तुमचं बोलणं इथे दिसेल.", 19f, muted)
        transcriptPanel.addView(transcript, matchWrap())
        root.addView(transcriptPanel, matchWrap())

        root.addView(label("उदाहरणार्थ: “नमस्कार”, “किती वाजले?”", 14f, muted, false, Gravity.CENTER).apply {
            setPadding(0, dp(18), 0, dp(8))
        }, matchWrap())

        root.addView(label(
            "ही सुरुवातीची आवृत्ती आहे. सध्या ती ऐकलेले शब्द दाखवते आणि साधी उत्तरं देते.",
            13f, muted, false, Gravity.CENTER
        ).apply { setPadding(dp(6), dp(8), dp(6), dp(4)) }, matchWrap())

        setContentView(ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            setPadding(0, 0, 0, dp(8))
            addView(root)
        })
    }

    private fun listen() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.text = "या फोनवर आवाज ओळखण्याची सुविधा उपलब्ध नाही."
            speak("माफ करा, या फोनवर आवाज ओळखण्याची सुविधा उपलब्ध नाही.")
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "mr-IN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "mr-IN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "कृपया मराठीत बोला")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }

        try {
            status.text = "ऐकत आहे…"
            listenButton.isEnabled = false
            listenButton.alpha = 0.82f
            startActivityForResult(intent, speechRequest)
        } catch (_: ActivityNotFoundException) {
            restoreListenButton()
            status.text = "आवाज ओळखण्याचं अॅप सापडलं नाही."
            speak("माफ करा, आवाज ओळखण्याचं अॅप सापडलं नाही.")
        } catch (_: SecurityException) {
            restoreListenButton()
            status.text = "मायक्रोफोनची परवानगी तपासा."
        } catch (_: Exception) {
            restoreListenButton()
            status.text = "आत्ता ऐकता येत नाही. पुन्हा प्रयत्न करा."
        }
    }

    @Deprecated("Kept for compatibility with the starter implementation")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != speechRequest) return
        restoreListenButton()

        if (resultCode != RESULT_OK) {
            status.text = "पुन्हा प्रयत्न करण्यासाठी बटण दाबा."
            return
        }

        val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()?.trim().orEmpty()
        if (spoken.isBlank()) {
            transcript.text = "काही ऐकू आलं नाही. कृपया पुन्हा बोला."
            status.text = "पुन्हा प्रयत्न करा"
            return
        }

        transcript.text = spoken
        val normalized = spoken.lowercase(Locale("mr", "IN"))
        val reply = when {
            normalized.contains("नमस्कार") || normalized.contains("हॅलो") ||
                normalized.contains("हॅलो") || normalized.contains("hello") ->
                "नमस्कार आई. मी ऐकत आहे."
            normalized.contains("धन्यवाद") -> "काही हरकत नाही, आई."
            normalized.contains("फोन कर") || normalized.contains("कॉल कर") ||
                normalized.contains("फोन लाव") ->
                "मी अजून फोन लावत नाही. फोनची सुविधा जोडल्यावर आधी नाव सांगून तुमची परवानगी विचारेन."
            normalized.contains("किती वाजले") || normalized.contains("वेळ") ->
                "मी ऐकलं की तुम्ही वेळ विचारत आहात. वेळ सांगण्याची सुविधा पुढील आवृत्तीत जोडू."
            else -> "मला ऐकू आलं: $spoken"
        }
        status.text = reply
        speak(reply)
    }

    private fun restoreListenButton() {
        if (!::listenButton.isInitialized) return
        listenButton.isEnabled = true
        listenButton.alpha = 1f
    }

    override fun onInit(result: Int) {
        if (result != TextToSpeech.SUCCESS) return
        val languageResult = tts?.setLanguage(Locale("mr", "IN")) ?: TextToSpeech.LANG_NOT_SUPPORTED
        ttsReady = languageResult != TextToSpeech.LANG_MISSING_DATA &&
            languageResult != TextToSpeech.LANG_NOT_SUPPORTED
        if (!ttsReady) {
            val fallback = tts?.setLanguage(Locale("hi", "IN"))
            ttsReady = fallback != TextToSpeech.LANG_MISSING_DATA &&
                fallback != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    private fun speak(text: String) {
        if (ttsReady) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "mom-ai-reply")
    }

    private fun label(
        textValue: String,
        size: Float,
        color: Int,
        bold: Boolean = false,
        gravityValue: Int = Gravity.START
    ) = TextView(this).apply {
        text = textValue
        textSize = size
        setTextColor(color)
        gravity = gravityValue
        includeFontPadding = true
        if (bold) setTypeface(typeface, Typeface.BOLD)
        contentDescription = textValue
    }

    private fun panel() = LinearLayout(this).apply {
        background = rounded(card, 22)
        elevation = dp(2).toFloat()
    }

    private fun rounded(color: Int, radiusDp: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun matchWrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    )

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }
}
