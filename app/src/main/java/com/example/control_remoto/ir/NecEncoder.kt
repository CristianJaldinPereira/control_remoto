package com.example.control_remoto.ir

/**
 * Codificador del protocolo NEC, el mas usado por controles de
 * focos y tiras LED economicas.
 *
 * Formato del mensaje (LSB primero):
 *   address (8 bits) + ~address + command (8 bits) + ~command
 *
 * Si el address es de 16 bits se usa NEC extendido:
 *   address low + address high + command + ~command
 *
 * Los tiempos estan tomados del control original del foco
 * (address 0xEF00) para que la senal sea identica.
 */
object NecEncoder {

    const val FREQUENCY_HZ = 38_000

    private const val HEADER_ON = 9_022
    private const val HEADER_OFF = 4_498
    private const val BIT_ON = 572
    private const val ZERO_OFF = 572
    private const val ONE_OFF = 1_690
    private const val TRAILER = 572

    private const val GAP = 39_702
    private const val REPEAT_ON = 9_022
    private const val REPEAT_OFF = 2_262
    private const val TRAILING_GAP = 95_992

    /**
     * @param withRepeat agrega la trama de repeticion NEC, igual que
     * el control fisico. Conviene dejarlo en true.
     */
    fun encode(address: Int, command: Int, withRepeat: Boolean = true): IntArray {
        val addr = address and 0xFFFF
        val cmd = command and 0xFF

        val bytes = ArrayList<Int>(4)
        if (addr <= 0xFF) {
            bytes += addr
            bytes += addr.inv() and 0xFF
        } else {
            bytes += addr and 0xFF
            bytes += (addr shr 8) and 0xFF
        }
        bytes += cmd
        bytes += cmd.inv() and 0xFF

        val pattern = ArrayList<Int>(2 + bytes.size * 16 + 5)
        pattern += HEADER_ON
        pattern += HEADER_OFF
        for (byte in bytes) {
            for (bit in 0 until 8) {
                pattern += BIT_ON
                pattern += if ((byte shr bit) and 1 == 1) ONE_OFF else ZERO_OFF
            }
        }
        pattern += TRAILER

        if (withRepeat) {
            pattern += GAP
            pattern += REPEAT_ON
            pattern += REPEAT_OFF
            pattern += TRAILER
            pattern += TRAILING_GAP
        }
        return pattern.toIntArray()
    }
}
