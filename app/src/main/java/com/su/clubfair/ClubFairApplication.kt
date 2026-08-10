package com.su.clubfair

import android.app.Application
import com.su.clubfair.data.ClubFairStore
import com.su.clubfair.data.FairRepository

/**
 * The app's object graph, which is one object deep.
 *
 * Hand-rolled rather than Hilt, and that is a considered choice rather than a
 * shortcut: there is exactly one repository, it has one dependency, and it lives
 * for the whole process. Hilt would add a compiler plugin, a build-time codegen
 * step and three annotations per class to express `by lazy`. The moment a second
 * scope appears — a per-fair-session graph, a network client with an auth
 * interceptor — this stops being true and a container is worth its keep.
 *
 * A single DataStore instance per process is not optional. DataStore holds a
 * file lock, and constructing a second one over the same file throws; the `by
 * preferencesDataStore` delegate in `ClubFairStore` is on `Context`, so this
 * class holding the only [FairRepository] is what guarantees a single owner.
 */
class ClubFairApplication : Application() {

    val repository: FairRepository by lazy {
        FairRepository(ClubFairStore(this))
    }
}
