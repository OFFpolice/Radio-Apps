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
        super.attachBaseContext(base)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            // Utilize GPU rendering directly via Hardware Bitmaps
            .allowHardware(true)
            // Parallelize image downloading and pre-processing tasks across all core CPUs
            .interceptorDispatcher(Dispatchers.IO)
            .fetcherDispatcher(Dispatchers.IO)
            .decoderDispatcher(Dispatchers.Default)
            // Enable caching rules
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            // Memory caching: 12% of total available RAM
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.12)
                    .strongReferencesEnabled(true)
                    .build()
            }
            // Disk caching: limited to 15MB to prevent bloating on entry-level phones
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(15 * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
