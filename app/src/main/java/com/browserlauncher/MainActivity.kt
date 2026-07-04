package com.browserlauncher

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    data class Browser(
        val name: String,
        val pkg: String,
        val incognito: Boolean = false
    )

    private val browsers = listOf(
        Browser("Chrome (инкогнито)", "com.android.chrome", incognito = true),
        Browser("DuckDuckGo", "com.duckduckgo.mobile.android"),
        Browser("Dolphin", "mobi.mgeek.TunnyBrowser"),
        Browser("Mi Browser", "com.mi.globalbrowser")
    )

    private lateinit var checkBoxes: List<CheckBox>
    private lateinit var urlInput: EditText
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        urlInput = findViewById(R.id.urlInput)
        statusText = findViewById(R.id.statusText)

        checkBoxes = listOf(
            findViewById(R.id.cbChrome),
            findViewById(R.id.cbDDG),
            findViewById(R.id.cbDolphin),
            findViewById(R.id.cbMi)
        )

        findViewById<Button>(R.id.btnPaste).setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
            if (!text.isNullOrEmpty()) {
                urlInput.setText(text)
                statusText.text = "Вставлено"
            }
        }

        findViewById<Button>(R.id.btnLaunch).setOnClickListener {
            launchAll()
        }
    }

    private fun launchAll() {
        var url = urlInput.text.toString().trim()
        if (url.isEmpty()) {
            statusText.text = "Введи ссылку!"
            return
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
            urlInput.setText(url)
        }

        val selected = browsers.filterIndexed { i, _ -> checkBoxes[i].isChecked }
        if (selected.isEmpty()) {
            statusText.text = "Выбери хотя бы один браузер"
            return
        }

        val handler = Handler(Looper.getMainLooper())
        selected.forEachIndexed { index, browser ->
            handler.postDelayed({
                statusText.text = "Открываю ${browser.name}... (${index + 1}/${selected.size})"
                openInBrowser(url, browser)
                if (index == selected.size - 1) {
                    handler.postDelayed({ statusText.text = "Готово!" }, 1000)
                }
            }, index * 1500L)
        }
    }

    private fun openInBrowser(url: String, browser: Browser) {
        try {
            when {
                // Chrome — запускаем через intent с флагом инкогнито
                browser.pkg == "com.android.chrome" && browser.incognito -> {
                    // Способ 1: через command line extras (работает на большинстве версий)
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        `package` = browser.pkg
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                        putExtra("com.android.browser.application_id", packageName)
                        // Флаги инкогнито для Chrome
                        putExtra("incognito", true)
                        putExtra("new_incognito_tab", true)
                    }
                    // Способ 2: через intent URI с параметром инкогнито
                    val incognitoIntent = Intent.parseUri(
                        "intent://${url.removePrefix("https://").removePrefix("http://")}#Intent;" +
                        "scheme=https;" +
                        "package=com.android.chrome;" +
                        "B.incognito=true;" +
                        "B.create_new_tab=true;" +
                        "end",
                        Intent.URI_INTENT_SCHEME
                    )
                    try {
                        startActivity(incognitoIntent)
                    } catch (e: Exception) {
                        startActivity(intent)
                    }
                }
                // DDG — поддерживает приватный режим по умолчанию
                browser.pkg == "com.duckduckgo.mobile.android" -> {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        `package` = browser.pkg
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                    }
                    startActivity(intent)
                }
                // Остальные браузеры
                else -> {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        `package` = browser.pkg
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                    }
                    startActivity(intent)
                }
            }
        } catch (e: Exception) {
            statusText.text = "${browser.name} не установлен"
        }
    }
}
