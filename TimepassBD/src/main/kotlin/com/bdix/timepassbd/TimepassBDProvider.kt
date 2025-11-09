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

    private val client by lazy {
        app.baseClient.newBuilder()
            .followRedirects(true)
            .followSslRedirects(true)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    override suspend fun getMainPage(): HomePageResponse {
        val doc = app.get(mainUrl, client = client).document
        val lists = mutableListOf<HomePageList>()

        val items = doc.select("a[href]")
            .map { it.absUrl("href") to (it.text().ifBlank { it.absUrl("href").substringAfterLast('/') }) }
            .distinctBy { it.first }
            .map { (href, title) ->
                newMovieSearchResponse(title, href, TvType.Movie) {}
            }
            .take(30)

        if (items.isEmpty()) throw ErrorLoadingException("No entries on homepage.")
        lists.add(HomePageList("Browse", items))
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
        val media = extractMediaLinks(doc, url)
        val title = url.substringAfterLast('/').ifBlank { name }

        return if (media.size > 1) {
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodeCount = media.size) {
                addEpisodes(DubStatus.Subbed, media.mapIndexed { idx, href -> Episode(href, "Episode ${idx + 1}") })
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
        val links = extractMediaLinks(doc, data)
        links.forEach { url -> callback(toExtractorLink(url, data)) }
        return links.isNotEmpty()
    }

    private fun extractMediaLinks(doc: Document, base: String): List<String> {
        val anchors = doc.select("a[href$=.mp4], a[href$=.mkv], a[href$=.webm], a[href$=.m3u8]").map { it.absUrl("href") }
        val sources = doc.select("video source[src], source[src]").map { it.absUrl("src") }
        val iframes = doc.select("iframe[src]").map { it.absUrl("src") }
        return (anchors + sources + iframes).distinct()
    }

    private fun toExtractorLink(url: String, referer: String): ExtractorLink {
        val q = when {
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
            quality = q,
            isM3u8 = url.endsWith(".m3u8", true)
        )
    }
}