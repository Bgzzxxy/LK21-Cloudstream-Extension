package com.indostream

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class NontonDramaProvider : MainAPI() {
    override var mainUrl = "https://tv9.nontondrama.my"
    override var name = "NontonDrama Series"
    override val hasMainPage = true
    override var lang = "id"
    override val supportedTypes = setOf(TvType.TvSeries)

    private val headers = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36"
    )

    override val mainPage = mainPageOf(
        "$mainUrl/latest-series/" to "Series Terbaru",
        "$mainUrl/popular-series/" to "Series Populer"
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

        return newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
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
        val title = doc.selectFirst("h1.entry-title, h1.title")?.text()?.trim() ?: "Unknown Series"
        val poster = fixUrlNull(doc.selectFirst("div.poster img, .content-poster img")?.attr("src"))
        val plot = doc.selectFirst("blockquote, .synopsis, .entry-content p")?.text()?.trim()

        val episodes = mutableListOf<Episode>()

        // Scrape Episode S1 E4 dll.
        doc.select(".episode-list a, .nav-episodes a, ul.list-episode li a, div.episodes-list a, .bottom-nav a").forEach { ep ->
            val epUrl = fixUrlNull(ep.attr("href")) ?: return@forEach
            val epName = ep.text().trim()
            val (season, episodeNum) = parseSeasonEpisode(epName)

            episodes.add(
                Episode(
                    data = epUrl,
                    name = epName,
                    season = season,
                    episode = episodeNum
                )
            )
        }

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
            this.posterUrl = poster
            this.plot = plot
        }
    }

    private fun parseSeasonEpisode(name: String): Pair<Int?, Int?> {
        val sMatch = Regex("(?i)s(\d+)").find(name)
        val eMatch = Regex("(?i)e(\d+)").find(name)
        val season = sMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1
        val episode = eMatch?.groupValues?.get(1)?.toIntOrNull()
        return Pair(season, episode)
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data, headers = headers).document

        val iframes = doc.select("iframe[src*=player], iframe[src*=embed], .embed-container iframe")
            .mapNotNull { fixUrlNull(it.attr("src")) }

        for (iframe in iframes) {
            loadExtractor(iframe, data, subtitleCallback, callback)
        }

        doc.select("select#player-option option, .player-options option, div.player-switcher a, [data-player], ul.mux li a").forEach { opt ->
            val embedUrl = fixUrlNull(opt.attr("value").ifEmpty { opt.attr("data-src").ifEmpty { opt.attr("href") } })
            if (!embedUrl.isNullOrEmpty() && embedUrl.startsWith("http")) {
                loadExtractor(embedUrl, data, subtitleCallback, callback)
            }
        }

        return true
    }
}
