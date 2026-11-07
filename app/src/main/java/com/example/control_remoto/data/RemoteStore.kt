package com.example.control_remoto.data

import android.content.Context
import org.json.JSONObject

/**
 * Persistencia simple de los codigos, un JSON en SharedPreferences.
 * Alcanza para un control; si despues hay varios equipos conviene Room.
 */
class RemoteStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun get(buttonId: String): RemoteCode? {
        val root = readRoot() ?: return null
        val obj = root.optJSONObject(buttonId) ?: return null
        return RemoteCode(
            address = obj.optInt(KEY_ADDRESS, 0),
            command = obj.optInt(KEY_COMMAND, 0),
            frequencyHz = obj.optInt(KEY_FREQUENCY, 38_000)
        )
    }

    fun save(buttonId: String, code: RemoteCode) {
        val root = readRoot() ?: JSONObject()
        root.put(buttonId, JSONObject().apply {
            put(KEY_ADDRESS, code.address)
            put(KEY_COMMAND, code.command)
            put(KEY_FREQUENCY, code.frequencyHz)
        })
        prefs.edit().putString(KEY_CODES, root.toString()).apply()
    }

    private fun readRoot(): JSONObject? {
        val raw = prefs.getString(KEY_CODES, null) ?: return null
        return try {
            JSONObject(raw)
        } catch (_: Exception) {
            null
        }
    }

    private companion object {
        const val PREFS = "remote_codes"
        const val KEY_CODES = "codes"
        const val KEY_ADDRESS = "address"
        const val KEY_COMMAND = "command"
        const val KEY_FREQUENCY = "frequencyHz"
    }
}
