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
        val lists = mutableListOf<HomePageList>()

        fun attempt(selector: String, title: String) {
            val items = doc.select(selector).mapNotNull { it.toSearchResponse() }
            if (items.isNotEmpty()) lists.add(HomePageList(title, items))
        }

        // Try common “card/post/grid” patterns first
        attempt(".post, .movie, .card, .grid-item, article", "Latest")

        // Fallback: any internal anchors
        if (lists.isEmpty()) {
            val items = doc.select("a[href]")
                .mapNotNull { it.toSearchResponse() }
                .distinctBy { it.url }
                .take(40)
            if (items.isNotEmpty()) lists.add(HomePageList("Browse", items))
        }

        if (lists.isEmpty()) throw ErrorLoadingException("No content found on CTGMovies homepage.")
        return newHomePageResponse(lists)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        // Try WordPress-like search; if not present, client-filter homepage
        val url = "$mainUrl/?s=" + query.encodeURL()
        val doc = app.get(url, client = client).document
        val results = doc.select("article, .post, .movie, .result, .card")
            .mapNotNull { it.toSearchResponse() }
            .ifEmpty {
                doc.select("a[href]").mapNotNull { it.toSearchResponse() }
            }
        return results.distinctBy { it.url }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url, client = client).document

        val title = doc.selectFirst("meta[property=og:title]")?.attr("content")
            ?: doc.selectFirst("h1, h2, .title, .entry-title")?.text()
            ?: "Unknown"

        val poster = doc.selectFirst("meta[property=og:image]")?.absUrl("content")
            ?: doc.selectFirst("img[src]")?.absUrl("src")

        val plot = doc.selectFirst(".desc, .summary, .plot, p")?.text()

        val episodeLinks = doc.select(".episode, .episodes a, a[href*=episode], a[href*=/S], a[href*=/E]")
            .map { it.text() to it.absUrl("href") }

        return if (episodeLinks.isNotEmpty()) {
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodeCount = episodeLinks.size) {
                this.posterUrl = poster
                this.plot = plot
                addEpisodes(DubStatus.Subbed, episodeLinks.mapIndexed { i, (n, h) ->
                    Episode(h, n.ifBlank { "Episode ${i + 1}" })
                })
            }
        } else {
            val links = extractPlayableLinks(doc, url)
            newMovieLoadResponse(title, url, TvType.Movie) {
                this.posterUrl = poster
                this.plot = plot
                addLinks(links)
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
        val links = extractPlayableLinks(doc, data)
        links.forEach(callback)
        return links.isNotEmpty()
    }

    // ---------- helpers ----------

    private fun org.jsoup.nodes.Element.toSearchResponse(): SearchResponse? {
        val a = if (tagName() == "a") this else selectFirst("a[href]") ?: return null
        val href = a.absUrl("href").ifBlank { return null }
        val title = selectFirst(".title, h2, h3, h1")?.text()
            ?: a.attr("title").ifBlank { a.text() }
            ?: "Untitled"
        val poster = selectFirst("img[src]")?.absUrl("src")
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = poster
        }
    }

    private fun extractPlayableLinks(doc: Document, pageUrl: String): List<ExtractorLink> {
        val media = doc.select("a[href$=.mp4], a[href$=.mkv], a[href$=.webm], a[href$=.m3u8]")
            .map { it.absUrl("href") }
        val video = doc.select("video source[src], source[src]").map { it.absUrl("src") }
        val iframes = doc.select("iframe[src]").map { it.absUrl("src") }
        val urls = (media + video + iframes).distinct()
        return urls.map { toExtractorLink(it, pageUrl) }
    }

    private fun toExtractorLink(url: String, referer: String): ExtractorLink {
        val quality = when {
            url.contains("2160", true) -> Qualities.P2160.value
            url.contains("1080", true) -> Qualities.P1080.value
            url.contains("720", true)  -> Qualities.P720.value
            url.contains("480", true)  -> Qualities.P480.value
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