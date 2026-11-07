package com.example.control_remoto.data

/**
 * Codigo IR de un boton.
 *
 * address: 8 bits (NEC estandar) o 16 bits (NEC extendido).
 * command: 8 bits.
 */
data class RemoteCode(
    val address: Int,
    val command: Int,
    val frequencyHz: Int = 38_000
) {
    fun addressHex(): String = String.format("%04X", address)

    fun commandHex(): String = String.format("%02X", command)
}
