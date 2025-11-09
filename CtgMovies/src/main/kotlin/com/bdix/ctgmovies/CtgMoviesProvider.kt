package com.bdix.ctgmovies

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Document

class CtgMoviesProvider : MainAPI() {
    override var mainUrl = "https://ctgmovies.com"
    override var name = "CTGMovies"
    override val hasMainPage = true
    override var lang = "en"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override val icon = "https://www.google.com/s2/favicons?domain=ctgmovies.com&sz=128"

    private val client by lazy {
        app.baseClient.newBuilder()
            .followRedirects(true)
            .followSslRedirects(true)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    override suspend fun getMainPage(): HomePageResponse {
        val doc = app.get(mainUrl, client = client).document
        val items = doc.select(".post, .movie, .card, .grid-item, .entry, a[href]")
            .mapNotNull { el ->
                val a = if (el.tagName() == "a") el else el.selectFirst("a[href]") ?: return@mapNotNull null
                val href = a.absUrl("href").ifBlank { return@mapNotNull null }
                val title = el.selectFirst(".title, h1, h2, h3")?.text()
                    ?: a.attr("title").ifBlank { a.text() }.ifBlank { "Untitled" }
                val poster = el.selectFirst("img[src]")?.absUrl("src")
                newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = poster }
            }.distinctBy { it.url }.take(30)

        if (items.isEmpty()) throw ErrorLoadingException("No content found on CTGMovies homepage.")
        return newHomePageResponse(listOf(HomePageList("Latest", items)))
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val doc = app.get("$mainUrl/?s=" + query.encodeURL(), client = client).document
        return doc.select("article, .post, .movie, .result, .card, a[href]")
            .mapNotNull { el ->
                val a = if (el.tagName() == "a") el else el.selectFirst("a[href]") ?: return@mapNotNull null
                val href = a.absUrl("href")
                val text = (el.text() + " " + href)
                if (!text.contains(query, true)) return@mapNotNull null
                val title = el.selectFirst(".title, h1, h2, h3")?.text()
                    ?: a.attr("title").ifBlank { a.text() }.ifBlank { href.substringAfterLast('/') }
                val poster = el.selectFirst("img[src]")?.absUrl("src")
                newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = poster }
            }.distinctBy { it.url }.take(50)
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url, client = client).document
        val title = doc.selectFirst("meta[property=og:title]")?.attr("content")
            ?: doc.selectFirst("h1, h2, .title, .entry-title")?.text() ?: "Unknown"
        val poster = doc.selectFirst("meta[property=og:image]")?.absUrl("content")
            ?: doc.selectFirst("img[src]")?.absUrl("src")
        val plot = doc.selectFirst(".desc, .summary, .plot, p")?.text()

        val episodeLinks = doc.select(".episode, .episodes a, a[href*=episode], a[href*=S01], a[href*=/E]")
            .map { it.text() to it.absUrl("href") }

        return if (episodeLinks.isNotEmpty()) {
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodeCount = episodeLinks.size) {
                this.posterUrl = poster
                this.plot = plot
                addEpisodes(DubStatus.Subbed, episodeLinks.mapIndexed { i, (name, href) ->
                    Episode(href, name.ifBlank { "Episode ${i + 1}" })
                })
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie) {
                this.posterUrl = poster
                this.plot = plot
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data, client = client).document
        val urls = extractMediaLinks(doc, data)
        urls.map { toExtractorLink(it, data) }.forEach(callback)
        return urls.isNotEmpty()
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