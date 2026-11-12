package com.luigivampa92.ndefemulator.ui

import android.content.Intent
import android.net.Uri
import android.nfc.NfcAdapter
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.luigivampa92.ndefemulation.NdefEmulation
import com.luigivampa92.ndefemulation.ndef.UriNdefData
import com.luigivampa92.ndefemulator.R

class DemoActivity : BaseActivity() {

    private lateinit var ndefEmulation: NdefEmulation
    private var nfcAdapter: NfcAdapter? = null

    private lateinit var cardStatus: LinearLayout
    private lateinit var textStatusTitle: TextView
    private lateinit var textStatusUrl: TextView
    private lateinit var inputUrl: EditText
    private lateinit var buttonStart: Button
    private lateinit var buttonStop: Button
    private lateinit var bannerNfcDisabled: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_scroll)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        ndefEmulation = NdefEmulation(this)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        cardStatus = findViewById(R.id.card_status)
        textStatusTitle = findViewById(R.id.text_status_title)
        textStatusUrl = findViewById(R.id.text_status_url)
        inputUrl = findViewById(R.id.input_url)
        buttonStart = findViewById(R.id.button_start)
        buttonStop = findViewById(R.id.button_stop)
        bannerNfcDisabled = findViewById(R.id.banner_nfc_disabled)

        bannerNfcDisabled.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
        }

        buttonStart.setOnClickListener {
            val rawUrl = inputUrl.text.toString().trim()
            if (rawUrl.isEmpty()) {
                toast(getString(R.string.toast_invalid_url))
                return@setOnClickListener
            }
            val formattedUrl = if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
                "https://$rawUrl"
            } else {
                rawUrl
            }
            inputUrl.setText(formattedUrl)
            ndefEmulation.currentEmulatedNdefData = UriNdefData(formattedUrl)
            updateUiState()
            toast(getString(R.string.toast_emulation_started, formattedUrl))
        }

        buttonStop.setOnClickListener {
            ndefEmulation.currentEmulatedNdefData = null
            updateUiState()
            toast(getString(R.string.toast_emulation_stopped))
        }

        // Setup Preset Buttons
        findViewById<Button>(R.id.btn_preset_web).setOnClickListener {
            inputUrl.setText("https://www.google.com")
            inputUrl.setSelection(inputUrl.text.length)
        }
        findViewById<Button>(R.id.btn_preset_wa).setOnClickListener {
            inputUrl.setText("https://wa.me/56912345678")
            inputUrl.setSelection(inputUrl.text.length)
        }
        findViewById<Button>(R.id.btn_preset_linkedin).setOnClickListener {
            inputUrl.setText("https://linkedin.com/in/usuario")
            inputUrl.setSelection(inputUrl.text.length)
        }
        findViewById<Button>(R.id.btn_preset_ig).setOnClickListener {
            inputUrl.setText("https://instagram.com/usuario")
            inputUrl.setSelection(inputUrl.text.length)
        }

        updateUiState()
    }

    override fun onResume() {
        super.onResume()
        checkNfcAvailability()
        // Prioritize this HCE service in foreground over payment wallets (Google Wallet)
        ndefEmulation.setPreferredService(this)
        updateUiState()
    }

    override fun onPause() {
        super.onPause()
        // Release foreground priority
        ndefEmulation.unsetPreferredService(this)
    }

    private fun checkNfcAvailability() {
        if (nfcAdapter == null) {
            bannerNfcDisabled.visibility = View.VISIBLE
            bannerNfcDisabled.text = "⚠️ Este dispositivo no cuenta con hardware NFC."
        } else if (!nfcAdapter!!.isEnabled) {
            bannerNfcDisabled.visibility = View.VISIBLE
            bannerNfcDisabled.setText(R.string.nfc_disabled_warning)
        } else {
            bannerNfcDisabled.visibility = View.GONE
        }
    }

    private fun updateUiState() {
        val currentData = ndefEmulation.currentEmulatedNdefData
        if (currentData is UriNdefData) {
            cardStatus.setBackgroundResource(R.drawable.bg_status_active)
            textStatusTitle.setText(R.string.status_active_title)
            textStatusTitle.setTextColor(ContextCompat.getColor(this, R.color.color_text_active))
            textStatusUrl.text = currentData.uri
            textStatusUrl.setTextColor(ContextCompat.getColor(this, R.color.color_text_primary))
            if (inputUrl.text.isNullOrEmpty()) {
                inputUrl.setText(currentData.uri)
            }
        } else {
            cardStatus.setBackgroundResource(R.drawable.bg_status_inactive)
            textStatusTitle.setText(R.string.status_inactive_title)
            textStatusTitle.setTextColor(ContextCompat.getColor(this, R.color.color_text_inactive))
            textStatusUrl.text = "Ningún enlace activo"
            textStatusUrl.setTextColor(ContextCompat.getColor(this, R.color.color_text_secondary))
        }
    }

    private fun toast(message: String?) {
        if (!message.isNullOrBlank()) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
}
