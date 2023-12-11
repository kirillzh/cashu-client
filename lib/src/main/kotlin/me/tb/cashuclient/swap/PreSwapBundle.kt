/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE file.
 */
 
package me.tb.cashuclient.swap

import fr.acinq.bitcoin.PrivateKey
import fr.acinq.bitcoin.PublicKey
import me.tb.cashuclient.types.KeysetId
import me.tb.cashuclient.types.Secret
import me.tb.cashuclient.db.DBProof
import me.tb.cashuclient.db.DBSettings
import me.tb.cashuclient.decomposeAmount
import me.tb.cashuclient.types.BlindedMessage
import me.tb.cashuclient.types.BlindingData
import me.tb.cashuclient.types.PreRequestBundle
import me.tb.cashuclient.types.Proof
import me.tb.cashuclient.types.createBlindingData
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * The data bundle Alice must create prior to communicating with the mint requesting a swap. Once the mint sends a
 * response [SwapResponse], the data from this [PreSwapBundle] object is combined with it to create valid tokens
 * (promises).
 *
 * @property proofsToSwap The list of [Proof]s the wallet intends to send to the mint for swapping.
 * @property blindingDataItems The list of [PreSwapItem]s that will be used to create the [BlindedMessage]s sent to the
 * mint.
 * @property keysetId The [KeysetId] of the keyset the wallet expects will be signing the [BlindedMessage]s.
 */
public class PreSwapBundle private constructor(
    public val proofsToSwap: List<Proof>,
    public override val blindingDataItems: List<PreSwapItem>,
    public override val keysetId: KeysetId
) : PreRequestBundle {
    public fun buildSwapRequest(): SwapRequest {
        val outputs: List<BlindedMessage> = blindingDataItems.map { preSplitItem ->
            BlindedMessage(
                amount = preSplitItem.amount,
                id = keysetId.value,
                blindedSecret = preSplitItem.blindedSecret.toString()
            )
        }

        return SwapRequest(
            proofsToSwap,
            outputs
        )
    }

    public companion object {
        // TODO: This currently only supports swapping a single denomination, but should support any number of them.
        /**
         * Creates a [PreSwapBundle] from a denomination the user wishes to swap and a target amount.
         *
         * @param denominationToSwap The denomination we wish to use to create the swap.
         * @param requiredAmount The target amount.
         */
        public fun create(
            denominationToSwap: ULong,
            requiredAmount: ULong,
            keysetId: KeysetId,
        ): PreSwapBundle {
            val requiredDenominations: List<ULong> = decomposeAmount(requiredAmount)
            val changeDenominations: List<ULong> = decomposeAmount(denominationToSwap - requiredAmount)
            val requestDenominations = requiredDenominations + changeDenominations

            val preSwapItem: List<PreSwapItem> = requestDenominations.map { amount ->
                PreSwapItem.create(
                    amount = amount,
                )
            }

            // Go get a proof in the database for the denomination required
            DBSettings.db
            val proof: Proof = transaction {
                SchemaUtils.create(DBProof)
                val proof: Proof? = DBProof.select { DBProof.amount eq denominationToSwap }.firstOrNull()?.let {
                    Proof(
                        amount = it[DBProof.amount],
                        id = it[DBProof.id],
                        secret = it[DBProof.secret],
                        C = it[DBProof.C],
                        script = it[DBProof.script]
                    )
                }
                proof ?: throw Exception("No proof found for denomination $denominationToSwap")
            }

            return PreSwapBundle(listOf(proof), preSwapItem, keysetId)
        }
    }
}

/**
 * The data structures that get combined into a [PreSwapBundle] required to build a [SwapRequest], and are combined
 * with the mint's response to create [Proof]s.
 *
 * The amount and blindedSecret are required to build the [BlindedMessage]s that are sent to the mint and which the mint
 * sign. Upon return, the signed [me.tb.cashuclient.types.BlindedSignature]s are unblinded using the blindingFactor and, combined with the
 * secret, stored as [Proof]s in the database.
 *
 * @param amount The amount of the token.
 * @param secret The secret x that is used in hashToCurve(x) to create Y.
 * @param blindedSecret The blinded secret B_ that is sent to the mint.
 * @param blindingFactor The blinding factor r, private key of the point R that is used to blind key Y.
 */
public class PreSwapItem private constructor(
    public override val amount: ULong,
    public override val secret: Secret,
    public override val blindedSecret: PublicKey,
    public override val blindingFactor: PrivateKey
) : BlindingData {
    public companion object {
        public fun create(amount: ULong): PreSwapItem {
            val (secret, blindedSecret, blindingFactor) = createBlindingData()
            return PreSwapItem(amount, secret, blindedSecret, blindingFactor)
        }
    }
}
