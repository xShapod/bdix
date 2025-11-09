package com.bdix.timepassbd

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class TimepassBDPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(TimepassBDProvider())
    }
}