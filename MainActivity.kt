package com.gcaforce.iptvpro

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var content: FrameLayout
    private val deviceId by lazy {
        Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    }

    private val gold = Color.rgb(255, 205, 45)
    private val blue = Color.rgb(24, 119, 255)
    private val text = Color.WHITE
    private val muted = Color.rgb(205, 216, 232)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        status = findViewById(R.id.status)
        content = findViewById(R.id.content)

        findViewById<Button>(R.id.homeBtn).setOnClickListener { home() }
        findViewById<Button>(R.id.liveBtn).setOnClickListener { live() }
        findViewById<Button>(R.id.activateBtn).setOnClickListener { activate() }
        findViewById<Button>(R.id.devicesBtn).setOnClickListener { devices() }

        configureTvFocus()
        login()
    }

    private fun configureTvFocus() {
        listOf(R.id.homeBtn, R.id.liveBtn, R.id.activateBtn, R.id.devicesBtn).forEach { id ->
            findViewById<View>(id).apply {
                isFocusable = true
                isFocusableInTouchMode = true
            }
        }
    }

    private fun clearContent() = content.removeAllViews()

    private fun title(value: String, size: Float = 26f): TextView = TextView(this).apply {
        text = value
        textColor = gold
        textSize = size
        typeface = Typeface.DEFAULT_BOLD
        setPadding(12, 10, 12, 10)
    }

    private fun description(value: String): TextView = TextView(this).apply {
        text = value
        textColor = muted
        textSize = 16f
        setPadding(12, 4, 12, 16)
    }

    private fun actionButton(label: String, listener: () -> Unit): Button = Button(this).apply {
        text = label
        textSize = 15f
        isAllCaps = false
        isFocusable = true
        setTextColor(text)
        setOnClickListener { listener() }
    }

    private fun login() {
        clearContent()
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(80, 20, 80, 20)
        }

        box.addView(title("ARIS IPTV 4K", 32f))
        box.addView(description("Connectez-vous avec votre Username et Password."))

        val username = EditText(this).apply {
            hint = "Username"
            setSingleLine(true)
            setTextColor(text)
            setHintTextColor(muted)
        }
        val password = EditText(this).apply {
            hint = "Password"
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setTextColor(text)
            setHintTextColor(muted)
        }
        val btn = actionButton("ENTRER") {
            submitLogin(username.text.toString(), password.text.toString())
        }

        box.addView(username, matchParams())
        box.addView(password, matchParams())
        box.addView(btn, matchParams())
        content.addView(box)
        username.requestFocus()
        status.text = "Non connecté"
    }

    private fun submitLogin(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            status.text = "Username et Password requis"
            return
        }
        status.text = "Connexion en cours…"
        val body = JSONObject().put("username", username.trim()).put("password", password)
        ApiClient.post("api/login", body) { ok, res ->
            runOnUiThread {
                if (ok) {
                    ApiClient.token = JSONObject(res).getString("token")
                    status.text = "Connecté : $username"
                    home()
                } else {
                    status.text = "Username ou Password incorrect"
                }
            }
        }
    }

    private fun home() {
        clearContent()
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.BOTTOM
            setPadding(20, 10, 20, 18)
        }

        box.addView(title("ARIS IPTV", 30f))
        box.addView(description("TV • FILMS • SÉRIES • SPORTS\nLe meilleur de la TV dans vos mains."))

        val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row1.addView(actionButton("📺  En direct") { live() }, weightParams())
        row1.addView(actionButton("🎬  Films") { message("Films", "Le catalogue de films sera chargé depuis votre serveur autorisé.") }, weightParams())
        row1.addView(actionButton("📚  Séries") { message("Séries", "Le catalogue de séries sera chargé depuis votre serveur autorisé.") }, weightParams())
        row2.addView(actionButton("⚽  Sports") { message("Sports", "Les contenus sportifs autorisés seront chargés depuis votre serveur.") }, weightParams())
        row2.addView(actionButton("🔄  Changer de playlist") { message("Playlist", "Sélectionnez ou ajoutez une playlist autorisée.") }, weightParams())
        row2.addView(actionButton("⚙  Paramètres") { message("Paramètres", "Configuration de l’application ARIS IPTV.") }, weightParams())
        box.addView(row1)
        box.addView(row2)

        val note = TextView(this).apply {
            text = "Appareil : $deviceId"
            textColor = muted
            textSize = 13f
            gravity = Gravity.END
            setPadding(12, 10, 12, 0)
        }
        box.addView(note)
        content.addView(box)
    }

    private fun live() {
        message("EN DIRECT", "Les chaînes et flux autorisés seront chargés depuis le backend ARIS IPTV.\n\nLe lecteur vidéo pourra être branché aux sources dont vous détenez les droits.")
    }

    private fun activate() {
        clearContent()
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 20)
        }
        box.addView(title("Activation ARIS IPTV", 28f))
        box.addView(description("Entrez votre code d’activation pour associer cet appareil."))
        val code = EditText(this).apply {
            hint = "Code d’activation"
            setSingleLine(true)
            setTextColor(text)
            setHintTextColor(muted)
        }
        val btn = actionButton("ACTIVER MON ABONNEMENT") {
            val value = code.text.toString().trim()
            if (value.isBlank()) {
                status.text = "Entrez un code d’activation"
            } else {
                status.text = "Activation en cours…"
                val body = JSONObject()
                    .put("code", value)
                    .put("deviceId", deviceId)
                    .put("deviceName", "Android TV")
                    .put("platform", "Android TV")
                ApiClient.post("api/activate", body) { ok, _ ->
                    runOnUiThread {
                        status.text = if (ok) "Activation réussie" else "Code invalide ou déjà utilisé"
                    }
                }
            }
        }
        box.addView(code, matchParams())
        box.addView(btn, matchParams())
        content.addView(box)
        code.requestFocus()
    }

    private fun devices() {
        status.text = "Chargement des appareils…"
        ApiClient.get("api/devices") { ok, res ->
            runOnUiThread {
                if (ok) message("Mes appareils", res) else message("Mes appareils", "Impossible de charger les appareils.")
            }
        }
    }

    private fun message(header: String, body: String) {
        clearContent()
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 18, 40, 18)
        }
        box.addView(title(header, 30f))
        box.addView(description(body))
        box.addView(actionButton("← RETOUR À L’ACCUEIL") { home() }, matchParams())
        content.addView(box)
    }

    private fun matchParams(): LinearLayout.LayoutParams = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { setMargins(0, 8, 0, 8) }

    private fun weightParams(): LinearLayout.LayoutParams = LinearLayout.LayoutParams(
        0,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        1f
    ).apply { setMargins(6, 6, 6, 6) }
}
