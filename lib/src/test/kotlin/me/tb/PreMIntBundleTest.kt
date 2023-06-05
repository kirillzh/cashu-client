package me.tb

import fr.acinq.secp256k1.Hex
import kotlin.test.Test
import kotlin.test.assertEquals

class PreMintBundleTest {
    @Test
    fun `PreMint item is correctly initialized 1`() {
        val secret = Secret(secret = "test_message")
        val preMintItem: PreMintItem = PreMintItem.create(
            amount = 1,
            secret = secret,
            blindingFactorBytes = Hex.decode("0000000000000000000000000000000000000000000000000000000000000001")
        )

        assertEquals(
            expected = "02a9acc1e48c25eeeb9289b5031cc57da9fe72f3fe2861d264bdc074209b107ba2",
            actual = preMintItem.blindedSecret.toString()
        )
    }

    @Test
    fun `PreMint item is correctly initialized 2`() {
        val secret = Secret(secret = "hello")
        val preMintItem: PreMintItem = PreMintItem.create(
            amount = 1,
            secret = secret,
            blindingFactorBytes = Hex.decode("6d7e0abffc83267de28ed8ecc8760f17697e51252e13333ba69b4ddad1f95d05")
        )

        assertEquals(
            expected = "0249eb5dbb4fac2750991cf18083388c6ef76cde9537a6ac6f3e6679d35cdf4b0c",
            actual = preMintItem.blindedSecret.toString()
        )
    }
}
