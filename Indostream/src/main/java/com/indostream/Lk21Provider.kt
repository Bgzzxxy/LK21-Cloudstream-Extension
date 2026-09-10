package com.indostream

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class Lk21Provider : MainAPI() {
    override var mainUrl = "https://tv12.lk21official.cc"
    override var name = "LK21 Official"
    override val hasMainPage = true
    override var lang = "id"
    override val supportedTypes = setOf(TvType.Movie)

    // Bypass & Headers standard Cloudstream
    private val headers = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36"
    )

    override val mainPage = mainPageOf(
        "$mainUrl/latest/" to "Movie Terbaru",
        "$mainUrl/populer/" to "Movie Populer",
        "$mainUrl/rating/" to "Top Rating"
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val url = if (page <= 1) request.data else "${request.data}page/$page/"
        val doc = app.get(url, headers = headers).document
        val items = doc.select("article.item, div.grid-archive article, .film-item").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, items)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst(".entry-title a, h3 a, .title")?.text()?.trim() ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(
            this.selectFirst("img")?.attr("data-src")?.ifEmpty { null }
                ?: this.selectFirst("img")?.attr("src")
        )

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/?s=${query.replace(" ", "+")}"
        val doc = app.get(url, headers = headers).document
        return doc.select("article.item, div.grid-archive article, .film-item").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url, headers = headers).document
        val title = doc.selectFirst("h1.entry-title, h1.title, .name")?.text()?.trim() ?: "Unknown Movie"
        val poster = fixUrlNull(doc.selectFirst("div.poster img, .content-poster img, .movie-info img")?.attr("src"))
        val plot = doc.selectFirst("blockquote, .synopsis, .entry-content p")?.text()?.trim()
        val year = doc.selectFirst(".year, .release-date")?.text()?.filter { it.isDigit() }?.toIntOrNull()
        val rating = doc.selectFirst(".rating, .score")?.text()?.toRatingInt()

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.plot = plot
            this.year = year
            this.rating = rating
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data, headers = headers).document

        // Scrape iframe pertama
        val playerIframes = doc.select("iframe[src*=player], iframe[src*=embed], #player-option iframe, .embed-container iframe")
            .mapNotNull { fixUrlNull(it.attr("src")) }

        for (iframe in playerIframes) {
            loadExtractor(iframe, data, subtitleCallback, callback)
        }

        // Scrape tombol player alternatif (Turbovip, Hydrax, dsb)
        doc.select("select#player-option option, .player-options option, ul.ganti-player li, button[data-src], ul.mux li a").forEach { opt ->
            val embedUrl = fixUrlNull(opt.attr("value").ifEmpty { opt.attr("data-src").ifEmpty { opt.attr("href") } })
            if (!embedUrl.isNullOrEmpty() && embedUrl.startsWith("http")) {
                loadExtractor(embedUrl, data, subtitleCallback, callback)
            }
        }

        return true
    }
}
