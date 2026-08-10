package com.su.clubfair

import android.app.Application
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
class ClubFairApplication : Application() {

    private val store: ClubFairStore by lazy { ClubFairStore(this) }

    val repository: FairRepository by lazy {
        FairRepository(
            store = store,
            api = ClubFairApi(
                baseUrl = Http.baseUrl,
                client = Http.client(),
                // A function, not the token itself: it changes underneath this
                // long-lived instance at every sign-in and sign-out, and capturing
                // one value would send a stale token for the rest of the process.
                tokenProvider = store::currentToken,
            ),
        )
    }
}
