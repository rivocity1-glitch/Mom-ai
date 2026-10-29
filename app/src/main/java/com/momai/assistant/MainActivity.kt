package com.momai.assistant

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
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
import android.widget.Toast
import java.net.URLEncoder
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var status: TextView
    private lateinit var transcript: TextView
    private lateinit var listenButton: Button
    private lateinit var confirmPanel: LinearLayout
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pendingAction: (() -> Unit)? = null
    private val speechRequest = 1001
    private val micRequest = 1002
    private var pendingMessage = ""

    private val ink = 0xFF18352A.toInt()
    private val green = 0xFF23734D.toInt()
    private val muted = 0xFF68756D.toInt()
    private val page = 0xFFF5F7F4.toInt()
    private val card = 0xFFFFFFFF.toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = page
        window.navigationBarColor = page
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        tts = TextToSpeech(this, this)
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(24))
            setBackgroundColor(page)
        }
        root.addView(label("Mom AI", 30f, ink, true, Gravity.CENTER), matchWrap())
        root.addView(label("तुमची मराठी बोलणारी मदतनीस", 17f, muted, false, Gravity.CENTER).apply {
            setPadding(0, dp(4), 0, dp(18))
        }, matchWrap())

        val welcome = panel().apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }
        welcome.addView(label("नमस्कार आई! 👋", 23f, ink, true), matchWrap())
        welcome.addView(label("बोला, मी मदत करण्याचा प्रयत्न करेन.", 16f, muted).apply {
            setPadding(0, dp(7), 0, 0)
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
            minHeight = dp(76)
            setPadding(dp(18), dp(12), dp(18), dp(12))
            contentDescription = "मराठीत बोलण्यासाठी दाबा"
            setOnClickListener { requestMicAndListen() }
        }
        root.addView(listenButton, LinearLayout.LayoutParams(-1, dp(76)).apply {
            topMargin = dp(16); bottomMargin = dp(12)
        })

        val statusPanel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(0xFFE7F2EA.toInt(), 18)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        statusPanel.addView(View(this).apply { background = rounded(green, 50) },
            LinearLayout.LayoutParams(dp(10), dp(10)).apply { rightMargin = dp(10) })
        status = label("तयार आहे — बटण दाबा", 15f, ink, true)
        statusPanel.addView(status, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(statusPanel, matchWrap())

        confirmPanel = panel().apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }
        confirmPanel.addView(label("कृपया कृतीची पुष्टी करा", 17f, ink, true), matchWrap())
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(Button(this).apply {
            text = "हो, पुढे जा"; isAllCaps = false; setOnClickListener {
                val action = pendingAction
                pendingAction = null
                confirmPanel.visibility = View.GONE
                action?.invoke()
            }
        }, LinearLayout.LayoutParams(0, dp(52), 1f).apply { rightMargin = dp(8) })
        actions.addView(Button(this).apply {
            text = "नाही"; isAllCaps = false; setOnClickListener {
                pendingAction = null
                confirmPanel.visibility = View.GONE
                status.text = "कृती रद्द केली."
                speak("ठीक आहे. कृती रद्द केली.")
            }
        }, LinearLayout.LayoutParams(0, dp(52), 1f))
        confirmPanel.addView(actions, matchWrap())
        root.addView(confirmPanel, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })

        root.addView(label("तुम्ही म्हणालात", 17f, ink, true).apply {
            setPadding(dp(2), dp(20), 0, dp(8))
        }, matchWrap())
        val transcriptPanel = panel().apply {
            orientation = LinearLayout.VERTICAL
            minimumHeight = dp(90)
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        transcript = label("तुमचं बोलणं इथे दिसेल.", 18f, muted)
        transcriptPanel.addView(transcript, matchWrap())
        root.addView(transcriptPanel, matchWrap())

        root.addView(label("उदाहरणे", 16f, ink, true).apply { setPadding(0, dp(18), 0, dp(6)) }, matchWrap())
        root.addView(label(
            "• “WhatsApp उघड”\n• “YouTube उघड”\n• “अथर्वला फोन लाव”\n• “मागे जा”\n• “टाइप नमस्कार”\n• “WhatsApp वर मेसेज पाठव…”",
            15f, muted
        ).apply { setPadding(dp(4), 0, 0, dp(10)) }, matchWrap())

        root.addView(label(
            "ही Lite आवृत्ती फक्त आवश्यक मायक्रोफोन परवानगी मागते. इतर अॅप्समधील बटणं आपोआप दाबणे उपलब्ध नाही.",
            13f, muted
        ).apply { setPadding(dp(4), dp(12), dp(4), 0) }, matchWrap())

        setContentView(ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            addView(root)
        })
    }

    private fun requestMicAndListen() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), micRequest)
        } else listen()
    }

    private fun listen() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.text = "या फोनवर आवाज ओळखण्याची सुविधा उपलब्ध नाही."
            speak("माफ करा, आवाज ओळखण्याची सुविधा उपलब्ध नाही.")
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "mr-IN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "mr-IN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "कृपया मराठीत बोला")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        try {
            status.text = "ऐकत आहे…"
            listenButton.isEnabled = false
            startActivityForResult(intent, speechRequest)
        } catch (_: ActivityNotFoundException) {
            listenButton.isEnabled = true
            status.text = "आवाज ओळखण्याचं अॅप सापडलं नाही."
        } catch (_: Exception) {
            listenButton.isEnabled = true
            status.text = "आत्ता ऐकता येत नाही. पुन्हा प्रयत्न करा."
        }
    }

    @Deprecated("Speech recognition activity callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != speechRequest) return
        listenButton.isEnabled = true
        if (resultCode != RESULT_OK) {
            status.text = "पुन्हा प्रयत्न करण्यासाठी बटण दाबा."
            return
        }
        val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.trim().orEmpty()
        if (spoken.isBlank()) {
            transcript.text = "काही ऐकू आलं नाही. कृपया पुन्हा बोला."
            status.text = "पुन्हा प्रयत्न करा"
            return
        }
        transcript.text = spoken
        handleCommand(spoken)
    }

    private fun handleCommand(spoken: String) {
        val command = spoken.lowercase(Locale("mr", "IN")).trim()
        when {
            command.contains("हो") && pendingAction != null -> {
                val action = pendingAction; pendingAction = null; confirmPanel.visibility = View.GONE
                action?.invoke()
            }
            command.contains("नाही") && pendingAction != null -> {
                pendingAction = null; confirmPanel.visibility = View.GONE
                status.text = "कृती रद्द केली."; speak("ठीक आहे. कृती रद्द केली.")
            }
            command.contains("मागे जा") || command == "back" || command.contains("बॅक जा") ->
                respond("या Lite आवृत्तीत इतर अॅप्समध्ये नेव्हिगेशन उपलब्ध नाही. फोनचा Back बटण वापरा.")
            command.startsWith("टाइप ") || command.startsWith("type ") || command.startsWith("लिही ") ->
                respond("या Lite आवृत्तीत इतर अॅपमध्ये आपोआप टाइप करता येत नाही.")
            command.startsWith("टॅप ") || command.startsWith("click ") || command.startsWith("क्लिक ") ->
                respond("या Lite आवृत्तीत इतर अॅपमधील बटणं आपोआप दाबता येत नाहीत.")
            command.contains("फोन कर") || command.contains("फोन लाव") || command.contains("कॉल कर") || command.contains("कॉल लाव") -> {
                askConfirmation("फोन डायलर उघडू का? तिथे संपर्क निवडा आणि नंबर तपासा.") {
                    startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:")))
                }
            }
            command.contains("whatsapp") || command.contains("व्हॉट्सअॅप") || command.contains("व्हाट्सअप") -> handleWhatsAppCommand(spoken)
            command.contains("उघड") || command.contains("ओपन") || command.startsWith("open ") -> openAppFromCommand(spoken)
            command.contains("नमस्कार") || command.contains("हॅलो") || command.contains("hello") ->
                respond("नमस्कार आई. काय मदत करू?")
            command.contains("धन्यवाद") -> respond("काही हरकत नाही, आई.")
            command.contains("किती वाजले") || command.contains("वेळ सांग") -> {
                val time = java.text.SimpleDateFormat("h:mm a", Locale("mr", "IN")).format(java.util.Date())
                respond("आत्ता वेळ आहे $time")
            }
            else -> respond("मी ऐकलं: $spoken. अॅप उघडण्यासाठी अॅपचं नाव आणि उघड असं सांगा.")
        }
    }

    private fun openAppFromCommand(spoken: String) {
        val raw = spoken.lowercase(Locale("mr", "IN"))
            .replace("उघड", "").replace("ओपन", "").replace("open", "").trim()
        val candidates = listOf(
            Triple("whatsapp", "com.whatsapp", "WhatsApp"),
            Triple("व्हॉट्सअॅप", "com.whatsapp", "WhatsApp"),
            Triple("व्हाट्सअप", "com.whatsapp", "WhatsApp"),
            Triple("youtube", "com.google.android.youtube", "YouTube"),
            Triple("यूट्यूब", "com.google.android.youtube", "YouTube"),
            Triple("फोन", "com.google.android.dialer", "फोन"),
            Triple("कॅमेरा", "com.sec.android.app.camera", "कॅमेरा"),
            Triple("camera", "com.sec.android.app.camera", "Camera"),
            Triple("सेटिंग", "com.android.settings", "Settings"),
            Triple("settings", "com.android.settings", "Settings"),
            Triple("मेसेज", "com.google.android.apps.messaging", "Messages"),
            Triple("messages", "com.google.android.apps.messaging", "Messages"),
            Triple("chrome", "com.android.chrome", "Chrome"),
            Triple("गॅलरी", "com.sec.android.gallery3d", "Gallery"),
            Triple("gallery", "com.sec.android.gallery3d", "Gallery")
        )
        val match = candidates.firstOrNull { raw.contains(it.first) }
        if (match == null) {
            val launchable = packageManager.getInstalledApplications(0).firstOrNull {
                packageManager.getApplicationLabel(it).toString().lowercase().contains(raw) && raw.isNotBlank()
            }
            if (launchable != null) {
                val launch = packageManager.getLaunchIntentForPackage(launchable.packageName)
                if (launch != null) { startActivity(launch); respond("अॅप उघडत आहे."); return }
            }
            respond("हे अॅप सापडलं नाही. अॅपचं नाव पुन्हा सांगा.")
            return
        }
        val intent = packageManager.getLaunchIntentForPackage(match.second)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            respond("${match.third} उघडत आहे.")
        } else respond("${match.third} फोनवर सापडलं नाही.")
    }

    private fun handleWhatsAppCommand(spoken: String) {
        val lower = spoken.lowercase(Locale("mr", "IN"))
        if (lower.contains("उघड") || lower.contains("ओपन") || lower.contains("open")) {
            openAppFromCommand("WhatsApp उघड"); return
        }
        val numberMatch = Regex("(?:\\+?91[ -]?)?[6-9][0-9]{9}").find(spoken)
        val message = spoken.substringAfter("ला", "").substringBefore("पाठव").trim()
        if (numberMatch == null || message.isBlank()) {
            respond("संदेशासाठी सांगा: WhatsApp वर ९८१२३४५६७८ ला नमस्कार पाठव. मी संदेश तयार करेन; पाठवण्यापूर्वी WhatsApp मध्ये तपासा.")
            return
        }
        var digits = numberMatch.value.filter { it.isDigit() }
        if (digits.length == 10) digits = "91$digits"
        val encoded = java.net.URLEncoder.encode(message, "UTF-8")
        askConfirmation("या नंबरच्या WhatsApp चॅटमध्ये हा संदेश तयार करू का? $message") {
            try {
                val url = Uri.parse("https://wa.me/$digits?text=$encoded")
                startActivity(Intent(Intent.ACTION_VIEW, url))
                respond("WhatsApp उघडलं. संदेश तपासा आणि पाठवण्यासाठी WhatsApp मधील Send बटण दाबा.")
            } catch (_: Exception) {
                respond("WhatsApp चॅट उघडता आलं नाही. WhatsApp इन्स्टॉल आहे का तपासा.")
            }
        }
    }

    private fun askConfirmation(question: String, action: () -> Unit) {
        status.text = question
        confirmPanel.visibility = View.VISIBLE
        (confirmPanel.getChildAt(0) as? TextView)?.text = question
        pendingAction = action
        speak(question + " हो किंवा नाही सांगा, किंवा खालील बटण दाबा.")
    }

    private fun respond(message: String) {
        status.text = message
        speak(message)
    }

    private fun speak(text: String) {
        if (ttsReady) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "mom-ai-reply")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == micRequest) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) listen()
            else respond("मायक्रोफोन परवानगीशिवाय मी ऐकू शकत नाही. Settings मधून परवानगी द्या.")
        }
    }

    override fun onInit(result: Int) {
        if (result != TextToSpeech.SUCCESS) return
        val languageResult = tts?.setLanguage(Locale("mr", "IN")) ?: TextToSpeech.LANG_NOT_SUPPORTED
        ttsReady = languageResult != TextToSpeech.LANG_MISSING_DATA && languageResult != TextToSpeech.LANG_NOT_SUPPORTED
        if (!ttsReady) {
            val fallback = tts?.setLanguage(Locale("hi", "IN"))
            ttsReady = fallback != TextToSpeech.LANG_MISSING_DATA && fallback != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    private fun label(textValue: String, size: Float, color: Int, bold: Boolean = false, gravityValue: Int = Gravity.START) =
        TextView(this).apply {
            text = textValue; textSize = size; setTextColor(color); gravity = gravityValue
            includeFontPadding = true
            if (bold) setTypeface(typeface, Typeface.BOLD)
            contentDescription = textValue
        }

    private fun panel() = LinearLayout(this).apply { background = rounded(card, 22); elevation = dp(2).toFloat() }
    private fun rounded(color: Int, radiusDp: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radiusDp).toFloat() }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun matchWrap() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

    override fun onDestroy() {
        tts?.stop(); tts?.shutdown(); tts = null
        super.onDestroy()
    }
}