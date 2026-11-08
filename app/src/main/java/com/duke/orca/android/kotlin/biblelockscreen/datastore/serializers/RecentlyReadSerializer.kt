package com.duke.orca.android.kotlin.biblelockscreen.datastore.serializers

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import com.duke.orca.android.kotlin.biblelockscreen.datastore.RecentlyRead
import java.io.InputStream
import java.io.OutputStream

object RecentlyReadSerializer : Serializer<RecentlyRead> {
    override val defaultValue: RecentlyRead
        get() = RecentlyRead.newBuilder()
            .setBook(1)
            .setChapter(1)
            .build()

    override suspend fun readFrom(input: InputStream): RecentlyRead {
        try {
            return RecentlyRead.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: RecentlyRead, output: OutputStream) {
        t.writeTo(output)
    }
}