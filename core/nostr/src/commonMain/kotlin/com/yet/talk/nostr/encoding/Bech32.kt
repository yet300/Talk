package com.yet.talk.nostr.encoding

/**
 * Bech32 (BIP-173) as NIP-19 uses it. The 90-character limit of BIP-173 is not enforced, because
 * NIP-19 TLV entities are longer.
 */
internal object Bech32 {

    private const val CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"
    private val GENERATORS = intArrayOf(0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3)

    fun encode(hrp: String, data: ByteArray): String {
        val values = convertBits(data.map { it.toInt() and 0xFF }, from = 8, to = 5, pad = true)
            ?: error("Unreachable: padding is enabled")
        val checksum = checksum(hrp, values)
        return buildString {
            append(hrp).append('1')
            (values + checksum).forEach { append(CHARSET[it]) }
        }
    }

    /** Returns the human-readable part and the payload, or null for malformed input or a bad checksum. */
    fun decode(input: String): Pair<String, ByteArray>? {
        if (input.lowercase() != input && input.uppercase() != input) return null
        val text = input.lowercase()
        val separator = text.lastIndexOf('1')
        if (separator < 1 || separator + 7 > text.length) return null
        val hrp = text.substring(0, separator)
        if (hrp.any { it.code < 33 || it.code > 126 }) return null
        val values = text.substring(separator + 1).map { char -> CHARSET.indexOf(char).takeIf { it >= 0 } ?: return null }
        if (polymod(expandHrp(hrp) + values) != 1) return null
        val bytes = convertBits(values.dropLast(6), from = 5, to = 8, pad = false) ?: return null
        return hrp to ByteArray(bytes.size) { bytes[it].toByte() }
    }

    private fun checksum(hrp: String, values: List<Int>): List<Int> {
        val mod = polymod(expandHrp(hrp) + values + List(6) { 0 }) xor 1
        return List(6) { (mod shr (5 * (5 - it))) and 31 }
    }

    private fun expandHrp(hrp: String): List<Int> =
        hrp.map { it.code shr 5 } + 0 + hrp.map { it.code and 31 }

    private fun polymod(values: List<Int>): Int {
        var checksum = 1
        for (value in values) {
            val top = checksum ushr 25
            checksum = ((checksum and 0x1ffffff) shl 5) xor value
            GENERATORS.forEachIndexed { index, generator ->
                if ((top shr index) and 1 == 1) checksum = checksum xor generator
            }
        }
        return checksum
    }

    private fun convertBits(data: List<Int>, from: Int, to: Int, pad: Boolean): List<Int>? {
        var accumulator = 0
        var bits = 0
        val maxValue = (1 shl to) - 1
        val result = ArrayList<Int>(data.size * from / to + 1)
        for (value in data) {
            if (value < 0 || value shr from != 0) return null
            accumulator = (accumulator shl from) or value
            bits += from
            while (bits >= to) {
                bits -= to
                result += (accumulator shr bits) and maxValue
            }
        }
        if (pad) {
            if (bits > 0) result += (accumulator shl (to - bits)) and maxValue
        } else if (bits >= from || ((accumulator shl (to - bits)) and maxValue) != 0) {
            return null
        }
        return result
    }
}
