package com.bdix.timepassbd

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Document

class TimepassBDProvider : MainAPI() {
    override var mainUrl = "http://timepassbd.live"
    override var name = "TimepassBD"
    override val hasMainPage = true
    override var lang = "en"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries, TvType.Live)
    override val icon = "https://www.google.com/s2/favicons?domain=timepassbd.live&sz=128"

    private val client by lazy {
        app.baseClient.newBuilder()
            .followRedirects(true)
            .followSslRedirects(true)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    override suspend fun getMainPage(): HomePageResponse {
        val doc = app.get(mainUrl, client = client).document

        val anchors = doc.select("a[href]").map { it.absUrl("href") }.distinct()

        val movies = mutableListOf<SearchResponse>()
        val series = mutableListOf<SearchResponse>()
        val live = mutableListOf<SearchResponse>()

        anchors.forEach { href ->
            val title = href.substringAfterLast("/").replace('-', ' ').replace('_', ' ').ifBlank { href }
            val item = newMovieSearchResponse(title, href, TvType.Movie) {}

            when {
                title.contains("live", true) || title.contains("tv", true) -> live.add(item)
                title.contains("series", true) || title.contains("season", true) -> series.add(item)
                else -> movies.add(item)
            }
        }

        return newHomePageResponse(
            listOfNotNull(
                if (movies.isNotEmpty()) HomePageList("Movies", movies) else null,
                if (series.isNotEmpty()) HomePageList("Series", series) else null,
                if (live.isNotEmpty()) HomePageList("Live / TV", live) else null
            )
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val doc = app.get(mainUrl, client = client).document
        return doc.select("a[href]").mapNotNull {
            val href = it.absUrl("href")
            val text = it.text().ifBlank { href.substringAfterLast("/") }
            if ((text + href).contains(query, true))
                newMovieSearchResponse(text, href, TvType.Movie) {}
            else null
        }.distinctBy { it.url }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url, client = client).document
        val media = extractMediaLinks(doc, url)

        val title = url.substringAfterLast("/").ifBlank { name }

        return if (media.size > 1) {
            newTvSeriesLoadResponse(title, url, TvType.TvSeries) {
                addEpisodes(DubStatus.Subbed, media.mapIndexed { i, link -> Episode(link, "Episode ${i+1}") })
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie) {}
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val doc = app.get(data, client = client).document
        extractMediaLinks(doc, data).map { toExtractorLink(it, data) }.forEach(callback)
        return true
    }

    private fun extractMediaLinks(doc: Document, ref: String): List<String> {
        val a = doc.select("a[href$=.mp4], a[href$=.mkv], a[href$=.webm], a[href$=.m3u8]").map { it.absUrl("href") }
        val s = doc.select("source[src], video source[src]").map { it.absUrl("src") }
        val f = doc.select("iframe[src]").map { it.absUrl("src") }
        return (a + s + f).distinct()
    }

    private fun toExtractorLink(url: String, ref: String): ExtractorLink {
        val q = when {
            url.contains("2160", true) -> Qualities.P2160.value
            url.contains("1080", true) -> Qualities.P1080.value
            url.contains("720", true) -> Qualities.P720.value
            url.contains("480", true) -> Qualities.P480.value
            else -> Qualities.Unknown.value
        }
        return ExtractorLink(name, name, url, ref, q, url.endsWith(".m3u8", true))
    }
}
