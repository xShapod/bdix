package com.bdix.ctgmovies

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.network.CloudflareKiller
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.Jsoup

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
        val res = app.get(mainUrl, client = client)
        val doc = res.document
        val sections = mutableListOf<HomePageList>()

        fun parse(selector: String, title: String) {
            val items = doc.select(selector).mapNotNull { el -> el.toSearchResponse() }
            if (items.isNotEmpty()) sections.add(HomePageList(title, items))
        }

        parse(".post, .movie, .card, .entry", "Latest")

        if (sections.isEmpty()) {
            val items = doc.select("a[href]")
                .mapNotNull { it.toSearchResponse() }
                .take(30)
            if (items.isNotEmpty()) sections.add(HomePageList("Browse", items))
        }

        return newHomePageResponse(sections)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val doc = app.get("$mainUrl/?s=${query.encodeURL()}", client = client).document
        return doc.select("a[href]").mapNotNull { it.toSearchResponse() }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url, client = client).document
        val title = doc.selectFirst("h1, h2, .title")?.text() ?: "Untitled"
        val poster = doc.selectFirst("img[src]")?.absUrl("src")
        val plot = doc.selectFirst("p")?.text()

        val episodes = doc.select("a[href*=/E], a[href*=/S0]").map {
            Episode(it.absUrl("href"), it.text())
        }

        return if (episodes.isNotEmpty()) {
            newTvSeriesLoadResponse(title, url, TvType.TvSeries) {
                this.posterUrl = poster
                this.plot = plot
                addEpisodes(DubStatus.Subbed, episodes)
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie) {
                this.posterUrl = poster
                this.plot = plot
                addLinks(extract(doc, url))
            }
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val res = app.get(data, client = client).document
        extract(res, data).forEach(callback)
        return true
    }

    private fun Element.toSearchResponse(): SearchResponse? {
        val a = selectFirst("a[href]") ?: return null
        val href = a.absUrl("href")
        val title = a.text().ifBlank { href.substringAfterLast("/") }
        return newMovieSearchResponse(title, href, TvType.Movie) {}
    }

    private fun extract(doc: Document, ref: String): List<ExtractorLink> {
        return doc.select("a[href$=.mp4], a[href$=.mkv], a[href$=.m3u8], source[src], iframe[src]")
            .map { it.absUrl("href") }
            .distinct()
            .map {
                ExtractorLink(name, name, it, ref, qualityFromName(it), it.endsWith(".m3u8"))
            }
    }

    private fun qualityFromName(url: String) = when {
        url.contains("1080", true) -> Qualities.P1080.value
        url.contains("720", true) -> Qualities.P720.value
        else -> Qualities.Unknown.value
    }
}
