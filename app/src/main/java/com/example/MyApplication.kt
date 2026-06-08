package com.example

import android.app.Application
import android.content.Context
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import kotlinx.coroutines.Dispatchers

class MyApplication : Application(), ImageLoaderFactory {
    override fun attachBaseContext(base: Context?) {
        if (base != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val attributionContext = base.createAttributionContext("webradio")
            super.attachBaseContext(attributionContext)
        } else {
            super.attachBaseContext(base)
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .allowHardware(true)
            .interceptorDispatcher(Dispatchers.IO)
            .fetcherDispatcher(Dispatchers.IO)
            .decoderDispatcher(Dispatchers.Default)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.12)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(15 * 1024 * 1024)
                    .build()
            }
            .crossfade(false)
            .build()
    }
}
