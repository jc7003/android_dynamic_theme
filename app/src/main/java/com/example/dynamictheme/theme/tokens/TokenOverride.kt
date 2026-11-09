package com.example.dynamictheme.theme.tokens

import android.content.res.Resources
import android.content.res.loader.ResourcesLoader
import android.content.res.loader.ResourcesProvider
import android.os.ParcelFileDescriptor
import android.system.Os
import androidx.annotation.RequiresApi
import java.io.FileOutputStream

/** Wraps an in-memory resource table in a ResourcesLoader and attaches it to Resources. */
@RequiresApi(30)
internal object TokenOverride {

    private var current: ResourcesLoader? = null
    private var previous: ResourcesLoader? = null

    /** Makes [table] the active override. Call [applyTo] afterwards for each Resources. */
    fun install(table: ByteArray) {
        val loader = createLoader(table)
        previous = current
        current = loader
    }

    /** Removes the active override (back to the XML defaults). */
    fun clear() {
        previous = current
        current = null
    }

    /** Swaps the previous override for the current one on [resources]. Safe to call repeatedly. */
    fun applyTo(resources: Resources) {
        previous?.let { resources.removeLoaders(it) }
        current?.let { resources.addLoaders(it) } // no-op if already present
    }

    private fun createLoader(table: ByteArray): ResourcesLoader {
        val fd = Os.memfd_create("theme_tokens.arsc", 0)
        try {
            FileOutputStream(fd).use { it.write(table) }
            ParcelFileDescriptor.dup(fd).use { pfd ->
                return ResourcesLoader().apply {
                    addProvider(ResourcesProvider.loadFromTable(pfd, null))
                }
            }
        } finally {
            Os.close(fd)
        }
    }
}
