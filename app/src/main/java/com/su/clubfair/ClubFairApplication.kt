package com.su.clubfair

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.su.clubfair.data.ClubFairStore
import com.su.clubfair.data.FairRepository
import com.su.clubfair.data.net.ClubFairApi
import com.su.clubfair.data.net.Http

/**
 * The app's object graph, which is four objects deep.
 *
 * Hand-rolled rather than Hilt, and still a considered choice rather than a
 * shortcut: one store, one HTTP client, one API, one repository, all
 * process-lived. Hilt would add a compiler plugin, a codegen step and three
 * annotations per class to express `by lazy`. The moment a second scope appears —
 * something per-screen, something per-fair-session — a container earns its keep.
 *
 * Two things here are single-instance by necessity rather than by taste:
 *
 *  - **DataStore** holds a file lock, so constructing a second one over the same
 *    file throws. The `by preferencesDataStore` delegate in `ClubFairStore` is on
 *    `Context`, so this class owning the only store is what guarantees one owner.
 *  - **OkHttpClient** carries the connection and thread pools. One per request
 *    would throw away connection reuse and leak threads.
 */
class ClubFairApplication : Application(), SingletonImageLoader.Factory {

    private val store: ClubFairStore by lazy { ClubFairStore(this) }

    /**
     * The process's one OkHttp client, shared by the API and by Coil.
     *
     * Safe to share because the bearer token is not on this client — `ClubFairApi`
     * attaches it per request, so nothing here can send a session token to
     * `googleusercontent.com` while fetching an avatar. If an auth interceptor is
     * ever added to this builder, Coil needs its own client that day.
     */
    private val httpClient by lazy { Http.client() }

    val repository: FairRepository by lazy {
        FairRepository(
            store = store,
            api = ClubFairApi(
                baseUrl = Http.baseUrl,
                client = httpClient,
                // A function, not the token itself: it changes underneath this
                // long-lived instance at every sign-in and sign-out, and capturing
                // one value would send a stale token for the rest of the process.
                tokenProvider = store::currentToken,
            ),
        )
    }

    /**
     * Coil's loader, built here so the avatar goes through the client above.
     *
     * A disk cache is the point of overriding the default at all. The one image
     * this app loads is the student's own account photo on a screen they open
     * repeatedly, often in a hall with no usable signal — memory-only would mean
     * a blank circle after every process death, which is exactly when the pass
     * gets opened. 8 MB is generous for one photo and still nothing next to the
     * booth cache.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { httpClient })) }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("avatars"))
                    .maxSizeBytes(8L * 1024 * 1024)
                    .build()
            }
            .build()
}
