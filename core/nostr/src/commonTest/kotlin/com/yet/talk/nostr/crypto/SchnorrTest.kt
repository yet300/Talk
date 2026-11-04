package com.yet.talk.nostr.crypto

import com.yet.talk.nostr.encoding.hexToBytes
import com.yet.talk.nostr.encoding.toHex
import kotlin.test.Test
import kotlin.test.assertEquals

/** Official BIP-340 vectors 0–14 (bitcoin/bips, bip-0340/test-vectors.csv); 15–18 use non-32-byte messages, which Nostr never signs. */
class SchnorrTest {

    private class Vector(
        val index: Int,
        val secretKey: String,
        val publicKey: String,
        val auxRand: String,
        val message: String,
        val signature: String,
        val valid: Boolean,
    )

    private val vectors = listOf(
        Vector(0, "0000000000000000000000000000000000000000000000000000000000000003", "f9308a019258c31049344f85f89d5229b531c845836f99b08601f113bce036f9", "0000000000000000000000000000000000000000000000000000000000000000", "0000000000000000000000000000000000000000000000000000000000000000", "e907831f80848d1069a5371b402410364bdf1c5f8307b0084c55f1ce2dca821525f66a4a85ea8b71e482a74f382d2ce5ebeee8fdb2172f477df4900d310536c0", true),
        Vector(1, "b7e151628aed2a6abf7158809cf4f3c762e7160f38b4da56a784d9045190cfef", "dff1d77f2a671c5f36183726db2341be58feae1da2deced843240f7b502ba659", "0000000000000000000000000000000000000000000000000000000000000001", "243f6a8885a308d313198a2e03707344a4093822299f31d0082efa98ec4e6c89", "6896bd60eeae296db48a229ff71dfe071bde413e6d43f917dc8dcf8c78de33418906d11ac976abccb20b091292bff4ea897efcb639ea871cfa95f6de339e4b0a", true),
        Vector(2, "c90fdaa22168c234c4c6628b80dc1cd129024e088a67cc74020bbea63b14e5c9", "dd308afec5777e13121fa72b9cc1b7cc0139715309b086c960e18fd969774eb8", "c87aa53824b4d7ae2eb035a2b5bbbccc080e76cdc6d1692c4b0b62d798e6d906", "7e2d58d8b3bcdf1abadec7829054f90dda9805aab56c77333024b9d0a508b75c", "5831aaeed7b44bb74e5eab94ba9d4294c49bcf2a60728d8b4c200f50dd313c1bab745879a5ad954a72c45a91c3a51d3c7adea98d82f8481e0e1e03674a6f3fb7", true),
        Vector(3, "0b432b2677937381aef05bb02a66ecd012773062cf3fa2549e44f58ed2401710", "25d1dff95105f5253c4022f628a996ad3a0d95fbf21d468a1b33f8c160d8f517", "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", "7eb0509757e246f19449885651611cb965ecc1a187dd51b64fda1edc9637d5ec97582b9cb13db3933705b32ba982af5af25fd78881ebb32771fc5922efc66ea3", true),
        Vector(4, "", "d69c3509bb99e412e68b0fe8544e72837dfa30746d8be2aa65975f29d22dc7b9", "", "4df3c3f68fcc83b27e9d42c90431a72499f17875c81a599b566c9889b9696703", "00000000000000000000003b78ce563f89a0ed9414f5aa28ad0d96d6795f9c6376afb1548af603b3eb45c9f8207dee1060cb71c04e80f593060b07d28308d7f4", true),
        Vector(5, "", "eefdea4cdb677750a420fee807eacf21eb9898ae79b9768766e4faa04a2d4a34", "", "243f6a8885a308d313198a2e03707344a4093822299f31d0082efa98ec4e6c89", "6cff5c3ba86c69ea4b7376f31a9bcb4f74c1976089b2d9963da2e5543e17776969e89b4c5564d00349106b8497785dd7d1d713a8ae82b32fa79d5f7fc407d39b", false),
        Vector(6, "", "dff1d77f2a671c5f36183726db2341be58feae1da2deced843240f7b502ba659", "", "243f6a8885a308d313198a2e03707344a4093822299f31d0082efa98ec4e6c89", "fff97bd5755eeea420453a14355235d382f6472f8568a18b2f057a14602975563cc27944640ac607cd107ae10923d9ef7a73c643e166be5ebeafa34b1ac553e2", false),
        Vector(7, "", "dff1d77f2a671c5f36183726db2341be58feae1da2deced843240f7b502ba659", "", "243f6a8885a308d313198a2e03707344a4093822299f31d0082efa98ec4e6c89", "1fa62e331edbc21c394792d2ab1100a7b432b013df3f6ff4f99fcb33e0e1515f28890b3edb6e7189b630448b515ce4f8622a954cfe545735aaea5134fccdb2bd", false),
        Vector(8, "", "dff1d77f2a671c5f36183726db2341be58feae1da2deced843240f7b502ba659", "", "243f6a8885a308d313198a2e03707344a4093822299f31d0082efa98ec4e6c89", "6cff5c3ba86c69ea4b7376f31a9bcb4f74c1976089b2d9963da2e5543e177769961764b3aa9b2ffcb6ef947b6887a226e8d7c93e00c5ed0c1834ff0d0c2e6da6", false),
        Vector(9, "", "dff1d77f2a671c5f36183726db2341be58feae1da2deced843240f7b502ba659", "", "243f6a8885a308d313198a2e03707344a4093822299f31d0082efa98ec4e6c89", "0000000000000000000000000000000000000000000000000000000000000000123dda8328af9c23a94c1feecfd123ba4fb73476f0d594dcb65c6425bd186051", false),
        Vector(10, "", "dff1d77f2a671c5f36183726db2341be58feae1da2deced843240f7b502ba659", "", "243f6a8885a308d313198a2e03707344a4093822299f31d0082efa98ec4e6c89", "00000000000000000000000000000000000000000000000000000000000000017615fbaf5ae28864013c099742deadb4dba87f11ac6754f93780d5a1837cf197", false),
        Vector(11, "", "dff1d77f2a671c5f36183726db2341be58feae1da2deced843240f7b502ba659", "", "243f6a8885a308d313198a2e03707344a4093822299f31d0082efa98ec4e6c89", "4a298dacae57395a15d0795ddbfd1dcb564da82b0f269bc70a74f8220429ba1d69e89b4c5564d00349106b8497785dd7d1d713a8ae82b32fa79d5f7fc407d39b", false),
        Vector(12, "", "dff1d77f2a671c5f36183726db2341be58feae1da2deced843240f7b502ba659", "", "243f6a8885a308d313198a2e03707344a4093822299f31d0082efa98ec4e6c89", "fffffffffffffffffffffffffffffffffffffffffffffffffffffffefffffc2f69e89b4c5564d00349106b8497785dd7d1d713a8ae82b32fa79d5f7fc407d39b", false),
        Vector(13, "", "dff1d77f2a671c5f36183726db2341be58feae1da2deced843240f7b502ba659", "", "243f6a8885a308d313198a2e03707344a4093822299f31d0082efa98ec4e6c89", "6cff5c3ba86c69ea4b7376f31a9bcb4f74c1976089b2d9963da2e5543e177769fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364141", false),
        Vector(14, "", "fffffffffffffffffffffffffffffffffffffffffffffffffffffffefffffc30", "", "243f6a8885a308d313198a2e03707344a4093822299f31d0082efa98ec4e6c89", "6cff5c3ba86c69ea4b7376f31a9bcb4f74c1976089b2d9963da2e5543e17776969e89b4c5564d00349106b8497785dd7d1d713a8ae82b32fa79d5f7fc407d39b", false),
    )

    @Test
    fun derivesPublicKeys() {
        vectors.filter { it.secretKey.isNotEmpty() }.forEach {
            assertEquals(it.publicKey, Schnorr.publicKey(it.secretKey.hexToBytes()).toHex(), "vector ${it.index}")
        }
    }

    @Test
    fun signsDeterministicallyForGivenAuxRand() {
        vectors.filter { it.secretKey.isNotEmpty() }.forEach {
            val signature = Schnorr.sign(it.message.hexToBytes(), it.secretKey.hexToBytes(), it.auxRand.hexToBytes())
            assertEquals(it.signature, signature.toHex(), "vector ${it.index}")
        }
    }

    @Test
    fun verifiesAllVectors() {
        vectors.forEach {
            val result = Schnorr.verify(it.signature.hexToBytes(), it.message.hexToBytes(), it.publicKey.hexToBytes())
            assertEquals(it.valid, result, "vector ${it.index}")
        }
    }

    @Test
    fun rejectsInvalidSecretKeys() {
        assertEquals(false, Schnorr.isValidSecretKey(ByteArray(32)))
        assertEquals(false, Schnorr.isValidSecretKey(ByteArray(31) { 1 }))
        assertEquals(false, Schnorr.isValidSecretKey("fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364141".hexToBytes()))
    }

    @Test
    fun secureRandomBytesHaveRequestedSizeAndVary() {
        assertEquals(32, secureRandomBytes(32).size)
        assertEquals(false, secureRandomBytes(32).contentEquals(secureRandomBytes(32)))
    }
}
