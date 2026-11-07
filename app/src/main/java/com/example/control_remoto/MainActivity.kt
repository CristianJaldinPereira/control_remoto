package com.example.control_remoto

import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.control_remoto.data.RemoteCode
import com.example.control_remoto.data.RemoteStore
import com.example.control_remoto.ir.IrTransmitter
import com.example.control_remoto.ir.NecEncoder

class MainActivity : AppCompatActivity() {

    /** Address NEC extendido del control del foco. */
    private val remoteAddress = 0xEF00

    private lateinit var transmitter: IrTransmitter
    private lateinit var store: RemoteStore
    private lateinit var statusView: TextView
    private lateinit var container: LinearLayout

    private data class ButtonSpec(
        val id: String,
        val name: String,
        val label: String,
        val command: Int,
        val background: Int,
        val content: Int,
        val iconRes: Int? = null,
        val tintIcon: Boolean = true,
        val border: Boolean = false,
        val textSize: Float = 12f
    )

    private val rows: List<List<ButtonSpec>> = listOf(
        listOf(
            ButtonSpec("power", "Encendido", "", 0x00, R.color.circle_power, R.color.white, R.drawable.ic_power),
            ButtonSpec("white", "Blanco", "", 0x01, R.color.circle_white, R.color.circle_white, border = true),
            ButtonSpec("rgb", "RGB", "", 0x02, R.color.circle_white, R.color.circle_ink, R.drawable.ic_rgb, tintIcon = false, border = true),
            ButtonSpec("auto", "Automático", "AUTO", 0x03, R.color.circle_auto, R.color.white, textSize = 10f)
        ),
        listOf(
            ButtonSpec("red", "Rojo", "R", 0x04, R.color.circle_red, R.color.white, textSize = 20f),
            ButtonSpec("green", "Verde", "G", 0x05, R.color.circle_green, R.color.white, textSize = 20f),
            ButtonSpec("blue", "Azul", "B", 0x06, R.color.circle_blue, R.color.white, textSize = 20f),
            ButtonSpec("violet", "Violeta", "V", 0x07, R.color.circle_violet, R.color.white, textSize = 20f)
        ),
        listOf(
            ButtonSpec("yellow", "Amarillo", "Y", 0x08, R.color.circle_yellow, R.color.circle_yellow_ink, textSize = 20f),
            ButtonSpec("fade", "Fundido", "FADE", 0x09, R.color.circle_light, R.color.circle_ink, textSize = 9f),
            ButtonSpec("smooth", "Suave", "SMOOTH", 0x0A, R.color.circle_light, R.color.circle_ink, textSize = 8f),
            ButtonSpec("flash", "Flash", "", 0x0B, R.color.circle_light, R.color.circle_ink, R.drawable.ic_flash)
        ),
        listOf(
            ButtonSpec("mix", "Mezcla", "MIX", 0x0C, R.color.circle_light, R.color.circle_ink, textSize = 9f),
            ButtonSpec("mix2", "Mezcla 2", "MIX2", 0x0D, R.color.circle_light, R.color.circle_ink, textSize = 9f),
            ButtonSpec("cycle", "Ciclo", "", 0x0E, R.color.circle_light, R.color.circle_ink, R.drawable.ic_refresh),
            ButtonSpec("rainbow", "Arcoíris", "RAIN", 0x0F, R.color.circle_light, R.color.circle_ink, textSize = 9f)
        ),
        listOf(
            ButtonSpec("prev", "Anterior", "", 0x10, R.color.circle_light, R.color.circle_ink, R.drawable.ic_prev),
            ButtonSpec("play", "Play/Pausa", "", 0x11, R.color.circle_light, R.color.circle_ink, R.drawable.ic_playpause),
            ButtonSpec("next", "Siguiente", "", 0x12, R.color.circle_light, R.color.circle_ink, R.drawable.ic_next),
            ButtonSpec("motor", "Motor", "MOTOR", 0x13, R.color.circle_surface, R.color.circle_motor_ink, border = true, textSize = 9f)
        ),
        listOf(
            ButtonSpec("dim", "Atenuar", "DIM", 0x14, R.color.circle_surface, R.color.circle_ink, border = true, textSize = 9f),
            ButtonSpec("mute", "Silenciar", "MUTE", 0x15, R.color.circle_surface, R.color.circle_mute_ink, border = true, textSize = 9f),
            ButtonSpec("volume", "Volumen +", "V+", 0x16, R.color.circle_surface, R.color.circle_volume_ink, border = true, textSize = 14f),
            ButtonSpec("laser", "Láser", "LASER", 0x17, R.color.circle_surface, R.color.circle_laser_ink, border = true, textSize = 8f)
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        transmitter = IrTransmitter(this)
        store = RemoteStore(this)
        statusView = findViewById(R.id.tvStatus)
        container = findViewById(R.id.buttonsContainer)

        renderStatus()
        buildButtons()
        setupThemeToggle()
    }

    private fun renderStatus() {
        statusView.text = if (transmitter.hasEmitter) {
            getString(R.string.ir_ready)
        } else {
            getString(R.string.ir_missing)
        }
    }

    private fun setupThemeToggle() {
        val button = findViewById<ImageButton>(R.id.btnTheme)
        val isNight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

        button.setImageResource(if (isNight) R.drawable.ic_light_mode else R.drawable.ic_dark_mode)
        button.contentDescription = getString(if (isNight) R.string.theme_light else R.string.theme_dark)
        button.setOnClickListener {
            val next = if (isNight) {
                AppCompatDelegate.MODE_NIGHT_NO
            } else {
                AppCompatDelegate.MODE_NIGHT_YES
            }
            getSharedPreferences(ControlRemotoApp.PREFS, MODE_PRIVATE)
                .edit()
                .putInt(ControlRemotoApp.KEY_NIGHT_MODE, next)
                .apply()
            AppCompatDelegate.setDefaultNightMode(next)
        }
    }

    private fun buildButtons() {
        rows.forEachIndexed { index, row ->
            val rowView = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                if (index > 0) setPadding(0, dp(6), 0, 0)
            }

            row.forEach { spec ->
                val cell = FrameLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, dp(70), 1f)
                }
                cell.addView(createCircle(spec))
                rowView.addView(cell)
            }
            container.addView(rowView)
        }
    }

    private fun createCircle(spec: ButtonSpec): View {
        val size = dp(62)
        val background = ContextCompat.getColor(this, spec.background)
        val content = ContextCompat.getColor(this, spec.content)

        val circle = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(size, size).apply {
                gravity = Gravity.CENTER
            }
            this.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(background)
                if (spec.border) setStroke(dp(1), ContextCompat.getColor(this@MainActivity, R.color.circle_border))
            }
            foreground = ContextCompat.getDrawable(this@MainActivity, R.drawable.ripple_circle)
            isClickable = true
            isFocusable = true
            elevation = dp(2).toFloat()
            contentDescription = spec.name
            setOnClickListener { view ->
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                send(spec)
            }
            setOnLongClickListener {
                editCode(spec)
                true
            }
        }

        when {
            spec.iconRes != null -> {
                circle.addView(ImageView(this).apply {
                    setImageResource(spec.iconRes)
                    if (spec.tintIcon) {
                        imageTintList = ColorStateList.valueOf(content)
                    }
                    layoutParams = FrameLayout.LayoutParams(dp(26), dp(26)).apply {
                        gravity = Gravity.CENTER
                    }
                })
            }

            spec.label.isNotEmpty() -> {
                circle.addView(TextView(this).apply {
                    text = spec.label
                    setTextColor(content)
                    textSize = spec.textSize
                    gravity = Gravity.CENTER
                    includeFontPadding = false
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                })
            }
        }
        return circle
    }

    private fun codeFor(spec: ButtonSpec): RemoteCode =
        store.get(spec.id) ?: RemoteCode(remoteAddress, spec.command)

    private fun send(spec: ButtonSpec) {
        val code = codeFor(spec)
        val pattern = NecEncoder.encode(code.address, code.command)
        if (!transmitter.transmit(code.frequencyHz, pattern)) {
            Toast.makeText(this, R.string.send_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun editCode(spec: ButtonSpec) {
        val current = codeFor(spec)

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(12), dp(24), 0)
        }

        val addressInput = EditText(this).apply {
            hint = getString(R.string.hint_address)
            inputType = InputType.TYPE_CLASS_TEXT
            setText(String.format("%04X", current.address))
        }

        val commandInput = EditText(this).apply {
            hint = getString(R.string.hint_command)
            inputType = InputType.TYPE_CLASS_TEXT
            setText(String.format("%02X", current.command))
        }

        container.addView(addressInput)
        container.addView(commandInput)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.edit_code_title, spec.name))
            .setMessage(R.string.edit_code_help)
            .setView(container)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val address = parseHex(addressInput.text.toString(), 0xFFFF)
                val command = parseHex(commandInput.text.toString(), 0xFF)
                if (address == null || command == null) {
                    Toast.makeText(this, R.string.invalid_code, Toast.LENGTH_SHORT).show()
                } else {
                    store.save(spec.id, RemoteCode(address, command))
                    Toast.makeText(this, R.string.code_saved, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun parseHex(raw: String, max: Int): Int? {
        val cleaned = raw.trim().removePrefix("0x").removePrefix("0X")
        if (cleaned.isEmpty()) return null
        val value = cleaned.toIntOrNull(16) ?: return null
        return if (value in 0..max) value else null
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
