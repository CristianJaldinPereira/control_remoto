package com.example.control_remoto.ir

import android.content.Context
import android.hardware.ConsumerIrManager

/**
 * Envoltorio fino sobre ConsumerIrManager.
 *
 * Solo funciona en telefonos con emisor IR fisico. En el resto,
 * hasEmitter devuelve false y transmit() no hace nada.
 */
class IrTransmitter(context: Context) {

    private val manager: ConsumerIrManager? =
        context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    val hasEmitter: Boolean
        get() = manager?.hasIrEmitter() == true

    /** Rangos de portadora soportados, en Hz (min a max). Vacio si no hay emisor. */
    val carrierRanges: List<Pair<Int, Int>>
        get() = try {
            manager?.carrierFrequencies?.map { it.minFrequency to it.maxFrequency } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

    /**
     * @param frequencyHz frecuencia de portadora (38_000 para NEC).
     * @param pattern microsegundos alternando encendido/apagado, empieza encendido.
     * @return true si el hardware acepto la transmision.
     */
    fun transmit(frequencyHz: Int, pattern: IntArray): Boolean {
        val ir = manager ?: return false
        if (!ir.hasIrEmitter()) return false
        return try {
            ir.transmit(frequencyHz, pattern)
            true
        } catch (_: Exception) {
            false
        }
    }
}
