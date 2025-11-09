package com.bdix.ctgmovies

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class CtgMoviesPlugin : Plugin() {
    override fun load(context: Context) {
        // Register the provider so CloudStream can discover it
        registerMainAPI(CtgMoviesProvider())
    }
}