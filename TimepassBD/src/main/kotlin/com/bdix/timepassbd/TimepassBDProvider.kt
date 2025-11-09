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
        val anchors = doc.select("a[href]")
            .map { it.absUrl("href") }
            .filter { it.startsWith(mainUrl) }
            .distinct()

        val movies = mutableListOf<SearchResponse>()
        val series = mutableListOf<SearchResponse>()
        val live = mutableListOf<SearchResponse>()

        anchors.forEach { href ->
            val title = href.substringAfterLast('/').replace('-', ' ').replace('_', ' ').ifBlank { href }
            val sr = newMovieSearchResponse(title, href, TvType.Movie) {}
            when {
                title.contains("tv", true) || title.contains("live", true) -> live.add(sr)
                title.contains("series", true) || title.contains("season", true) -> series.add(sr)
                else -> movies.add(sr)
            }
        }

        val lists = buildList {
            if (movies.isNotEmpty()) add(HomePageList("Movies", movies.take(30)))
            if (series.isNotEmpty()) add(HomePageList("Series", series.take(30)))
            if (live.isNotEmpty()) add(HomePageList("Live / TV", live.take(30)))
        }

        if (lists.isEmpty()) throw ErrorLoadingException("No entries found on TimepassBD homepage.")
        return newHomePageResponse(lists)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val doc = app.get(mainUrl, client = client).document
        return doc.select("a[href]").mapNotNull { a ->
            val href = a.absUrl("href")
            val title = a.text().ifBlank { href.substringAfterLast('/') }
            if ((title + href).contains(query, true))
                newMovieSearchResponse(title, href, TvType.Movie) {}
            else null
        }.distinctBy { it.url }.take(50)
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url, client = client).document
        val links = extractMediaLinks(doc)
        val title = url.substringAfterLast('/').ifBlank { name }

        return if (links.size > 1) {
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodeCount = links.size) {
                addEpisodes(DubStatus.Subbed, links.mapIndexed { i, href -> Episode(href, "Episode ${i + 1}") })
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie) {}
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data, client = client).document
        val links = extractMediaLinks(doc)
        links.map { toExtractorLink(it, data) }.forEach(callback)
        return links.isNotEmpty()
    }

    private fun extractMediaLinks(doc: Document): List<String> {
        val anchors = doc.select("a[href$=.mp4], a[href$=.mkv], a[href$=.webm], a[href$=.m3u8]")
            .map { it.absUrl("href") }
        val sources = doc.select("video source[src], source[src]").map { it.absUrl("src") }
        val iframes = doc.select("iframe[src]").map { it.absUrl("src") }
        return (anchors + sources + iframes).distinct()
    }

    private fun toExtractorLink(url: String, referer: String): ExtractorLink {
        val quality = when {
            url.contains("2160", true) -> Qualities.P2160.value
            url.contains("1080", true) -> Qualities.P1080.value
            url.contains("720", true) -> Qualities.P720.value
            url.contains("480", true) -> Qualities.P480.value
            else -> Qualities.Unknown.value
        }
        return ExtractorLink(
            source = name,
            name = name,
            url = url,
            referer = referer,
            quality = quality,
            isM3u8 = url.endsWith(".m3u8", true)
        )
    }
}