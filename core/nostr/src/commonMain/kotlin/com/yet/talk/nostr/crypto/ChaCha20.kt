package com.yet.talk.nostr.crypto

/** ChaCha20 (RFC 8439) keystream XOR, as NIP-44 v2 uses it: 32-byte key, 12-byte nonce, counter from 0. */
internal object ChaCha20 {

    fun xor(key: ByteArray, nonce: ByteArray, data: ByteArray, counter: Int = 0): ByteArray {
        require(key.size == 32 && nonce.size == 12)
        val state = IntArray(16)
        state[0] = 0x61707865
        state[1] = 0x3320646e
        state[2] = 0x79622d32
        state[3] = 0x6b206574
        for (i in 0 until 8) state[4 + i] = key.leInt(i * 4)
        for (i in 0 until 3) state[13 + i] = nonce.leInt(i * 4)
        val out = ByteArray(data.size)
        val block = IntArray(16)
        var blockCounter = counter
        var offset = 0
        while (offset < data.size) {
            state[12] = blockCounter++
            state.copyInto(block)
            repeat(10) {
                quarter(block, 0, 4, 8, 12); quarter(block, 1, 5, 9, 13); quarter(block, 2, 6, 10, 14); quarter(block, 3, 7, 11, 15)
                quarter(block, 0, 5, 10, 15); quarter(block, 1, 6, 11, 12); quarter(block, 2, 7, 8, 13); quarter(block, 3, 4, 9, 14)
            }
            for (i in 0 until 16) {
                val word = block[i] + state[i]
                for (b in 0 until 4) {
                    val index = offset + i * 4 + b
                    if (index < data.size) out[index] = (data[index].toInt() xor (word ushr (8 * b))).toByte()
                }
            }
            offset += 64
        }
        return out
    }

    private fun quarter(s: IntArray, a: Int, b: Int, c: Int, d: Int) {
        s[a] += s[b]; s[d] = (s[d] xor s[a]).rotateLeft(16)
        s[c] += s[d]; s[b] = (s[b] xor s[c]).rotateLeft(12)
        s[a] += s[b]; s[d] = (s[d] xor s[a]).rotateLeft(8)
        s[c] += s[d]; s[b] = (s[b] xor s[c]).rotateLeft(7)
    }

    private fun ByteArray.leInt(at: Int): Int =
        (this[at].toInt() and 0xff) or ((this[at + 1].toInt() and 0xff) shl 8) or
            ((this[at + 2].toInt() and 0xff) shl 16) or ((this[at + 3].toInt() and 0xff) shl 24)
}
