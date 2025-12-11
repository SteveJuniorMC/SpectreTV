package com.spectretv.app.data.remote

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.lang.reflect.ParameterizedType

/**
 * Gson TypeAdapterFactory that handles API responses where an array is expected
 * but an object might be returned (e.g., error responses or empty results).
 * Returns an empty list when encountering an object instead of an array.
 */
class LenientListTypeAdapterFactory : TypeAdapterFactory {
    override fun <T : Any?> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        // Only handle List types
        if (!List::class.java.isAssignableFrom(type.rawType)) {
            return null
        }

        // Get the delegate adapter for the actual list type
        val delegate = gson.getDelegateAdapter(this, type)
        val elementType = (type.type as? ParameterizedType)?.actualTypeArguments?.firstOrNull()
        val elementAdapter = elementType?.let { gson.getAdapter(TypeToken.get(it)) }

        @Suppress("UNCHECKED_CAST")
        return object : TypeAdapter<T>() {
            override fun write(out: JsonWriter, value: T) {
                delegate.write(out, value)
            }

            override fun read(reader: JsonReader): T {
                return when (reader.peek()) {
                    JsonToken.BEGIN_ARRAY -> {
                        // Normal case - parse the array
                        delegate.read(reader)
                    }
                    JsonToken.BEGIN_OBJECT -> {
                        // API returned an object instead of array (error or empty)
                        // Skip the object and return empty list
                        reader.skipValue()
                        emptyList<Any>() as T
                    }
                    JsonToken.NULL -> {
                        reader.nextNull()
                        emptyList<Any>() as T
                    }
                    JsonToken.STRING -> {
                        // Some APIs return empty string for empty arrays
                        val str = reader.nextString()
                        emptyList<Any>() as T
                    }
                    else -> {
                        // Unexpected token, try delegate
                        delegate.read(reader)
                    }
                }
            }
        } as TypeAdapter<T>
    }
}
