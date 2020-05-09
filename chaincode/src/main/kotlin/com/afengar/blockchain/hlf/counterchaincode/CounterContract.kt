package com.afengar.blockchain.hlf.counterchaincode

import org.hyperledger.fabric.contract.Context
import org.hyperledger.fabric.contract.ContractInterface
import org.hyperledger.fabric.contract.annotation.*

@Contract(name = "CounterContract",
        info = Info(title = "Counter contract",
                description = "Kotlin gradle dsl and Kotlin Contract",
                version = "1.0.0",
                license =
                License(name = "Apache-2.0",
                        url = ""),
                contact = Contact(email = "eli.afengar@gmail.com",
                        name = "Eli Afengar",
                        url = "https://github.com/eliafengar")))
@Default
class CounterContract : ContractInterface {
    private val key: String = "Counter"
    private val privateCollectionKey = "collectionName"

    @Transaction
    private fun getCollectionName(ctx: Context): String? {
        var collectionName: String? = null

        if (ctx.stub.transient != null) {
            if (ctx.stub.transient.containsKey(this.privateCollectionKey))
                collectionName = ctx.stub.transient[this.privateCollectionKey]?.toString(Charsets.UTF_8) ?: ""
        }

        return collectionName
    }

    @Transaction
    private fun counterExists(ctx: Context, collectionName: String?): Boolean {
        var buffer: ByteArray
        if (collectionName != null)
            buffer = ctx.stub.getPrivateData(collectionName, this.key)
        else
            buffer = ctx.stub.getState(this.key)
        return (buffer != null && buffer.isNotEmpty())
    }

    @Transaction
    fun readCounter(ctx: Context): String {
        val collectionName = this.getCollectionName(ctx)
        val exists = counterExists(ctx, collectionName)
        if (!exists) {
            return ""
        }

        if (collectionName != null)
            return ctx.stub.getPrivateData(collectionName, this.key).toString(Charsets.UTF_8)
        else
            return ctx.stub.getState(this.key).toString(Charsets.UTF_8)
    }

    @Transaction
    fun increment(ctx: Context, delta: String) {
        val collectionName = this.getCollectionName(ctx)

        var counts = readCounter(ctx)
        println("Counts value from State is $counts")
        if (counts.isNullOrEmpty())
            counts = "0";

        println("Delta value is $delta")
        if (delta.isNullOrEmpty()) {
            counts = (counts.toInt() + 1).toString()
        } else {
            counts = (counts.toInt() + delta.toInt()).toString()
        }

        if (collectionName != null)
            ctx.stub.putPrivateData(collectionName, this.key, counts.toByteArray(Charsets.UTF_8))
        else
            ctx.stub.putState(this.key, counts.toByteArray(Charsets.UTF_8))
    }

    @Transaction
    fun decrement(ctx: Context, delta: String) {
        val collectionName = this.getCollectionName(ctx)

        var counts = readCounter(ctx)
        println("Counts value from State is $counts")
        if (counts.isNullOrEmpty())
            counts = "0";

        println("Delta value is $delta")
        if (delta.isNullOrEmpty()) {
            counts = (counts.toInt() - 1).toString()
        } else {
            counts = (counts.toInt() - delta.toInt()).toString()
        }

        if (collectionName != null)
            ctx.stub.putPrivateData(collectionName, this.key, counts.toByteArray(Charsets.UTF_8))
        else
            ctx.stub.putState(this.key, counts.toByteArray(Charsets.UTF_8))

    }
}