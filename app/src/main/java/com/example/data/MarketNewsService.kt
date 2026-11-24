package com.example.data

import android.util.Log
import com.example.model.MarketNewsArticle
import com.example.model.MarketNewsFeed
import com.example.model.MarketNewsSource
import com.example.model.MarketNewsTopic
import com.example.model.NewsImpactDirection
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.Tool
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun interface MarketNewsGateway {
    suspend fun fetchLatestGoldMarketNews(topic: MarketNewsTopic): MarketNewsFeed
}

class FirebaseMarketNewsGateway : MarketNewsGateway {

    // Lazily initialized per Firebase AI Logic checklist, with Google Search Grounding enabled.
    private val groundedModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            tools = listOf(Tool.googleSearch()),
        )
    }

    override suspend fun fetchLatestGoldMarketNews(topic: MarketNewsTopic): MarketNewsFeed {
        val prompt = MarketNewsParser.buildGoogleSearchPrompt(topic)
        return try {
            val response = withTimeout(TIMEOUT_MS) {
                groundedModel.generateContent(prompt)
            }
            val nowLabel = SimpleDateFormat("h:mm a", Locale.forLanguageTag("ar")).format(Date())
            val feed = MarketNewsParser.parseResponse(
                topic = topic,
                response = response,
                fetchedAtLabel = nowLabel,
            )
            if (feed.articles.isEmpty() && feed.macroSummary.isBlank()) {
                throw IllegalStateException("لم يُرجع بحث Google تحديثات أخبار صالحة للعرض حالياً.")
            }
            feed
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Google Search grounded market news request timed out", e)
            throw IllegalStateException("انتهت مهلة البحث عبر Google بعد 60 ثانية. يرجى المحاولة مرة أخرى.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch Google Search grounded gold market news", e)
            throw e
        }
    }

    private companion object {
        const val TAG = "MarketNewsGateway"
        const val MODEL_NAME = "gemini-3.8-flash"
        const val TIMEOUT_MS = 60_000L
    }
}

object MarketNewsParser {

    fun buildGoogleSearchPrompt(topic: MarketNewsTopic): String {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return """
            استخدم أداة بحث Google (Google Search) للبحث عن أحدث أخبار أسعار الذهب العالمية واتجاهات السوق اليوم ($today).
            نطاق البحث المطلوب: ${topic.searchFocus}

            أعد الرد باللغة العربية بصيغة JSON صالحة فقط بدون أي نص تمهيدي أو علامات Markdown بالشكل التالي:
            {
              "macroSummary": "ملخص موجز من جملتين لأهم محركات أسعار الذهب العالمية والاتجاه العام اليوم",
              "articles": [
                {
                  "title": "عنوان الخبر الموجز",
                  "summary": "ملخص عملي من جملتين يوضح الأرقام أو الحدث وتأثيره على الذهب",
                  "impact": "BULLISH | BEARISH | NEUTRAL",
                  "regionTag": "عالمي | الفيدرالي | البنوك المركزية | السوق المصري",
                  "publishedHint": "اليوم أو التاريخ التقريبي للخبر",
                  "sourceTitle": "اسم المصدر مثل Reuters أو Bloomberg أو Kitco أو Trading Economics",
                  "sourceUrl": "الرابط المباشر للخبر إن توفر وإلا اتركه فارغاً"
                }
              ]
            }

            الشروط:
            - قدّم من 4 إلى 5 أخبار حديثة ومستندة مباشرة إلى نتائج بحث Google.
            - لا تخترع روابط غير موجودة في نتائج البحث.
        """.trimIndent()
    }

    fun parseResponse(
        topic: MarketNewsTopic,
        response: GenerateContentResponse,
        fetchedAtLabel: String,
    ): MarketNewsFeed {
        val candidate = response.candidates.firstOrNull()
        val grounding = candidate?.groundingMetadata
        val groundingSources = grounding?.groundingChunks
            .orEmpty()
            .mapNotNull { chunk ->
                val web = chunk.web ?: return@mapNotNull null
                val uri = web.uri?.trim().orEmpty()
                if (!isSafeHttpUrl(uri)) return@mapNotNull null
                val title = web.title?.trim().takeIf { !it.isNullOrBlank() }
                    ?: web.domain?.trim().takeIf { !it.isNullOrBlank() }
                    ?: "مصدر Google"
                val domain = web.domain?.trim().takeIf { !it.isNullOrBlank() }
                    ?: extractDomain(uri)
                MarketNewsSource(title = title, url = uri, domain = domain)
            }
            .distinctBy { it.url }

        val searchQueries = grounding?.webSearchQueries
            .orEmpty()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        return parseRawText(
            topic = topic,
            rawText = response.text.orEmpty(),
            groundingSources = groundingSources,
            searchQueries = searchQueries,
            fetchedAtLabel = fetchedAtLabel,
        )
    }

    fun parseRawText(
        topic: MarketNewsTopic,
        rawText: String,
        groundingSources: List<MarketNewsSource>,
        searchQueries: List<String>,
        fetchedAtLabel: String,
    ): MarketNewsFeed {
        val cleanedJson = extractJsonPayload(rawText)
        if (cleanedJson != null) {
            try {
                val root = JSONObject(cleanedJson)
                val macroSummary = root.optString("macroSummary", "").trim()
                val arr = root.optJSONArray("articles") ?: JSONArray()
                val articles = buildList {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val title = obj.optString("title", "").trim()
                        val summary = obj.optString("summary", "").trim()
                        if (title.isBlank() || summary.isBlank()) continue
                        val rawUrl = obj.optString("sourceUrl", "").trim()
                        val fallbackSource = groundingSources.getOrNull(i) ?: groundingSources.firstOrNull()
                        val resolvedUrl = when {
                            isSafeHttpUrl(rawUrl) -> rawUrl
                            fallbackSource != null -> fallbackSource.url
                            else -> ""
                        }
                        val resolvedSourceTitle = obj.optString("sourceTitle", "").trim()
                            .ifBlank { fallbackSource?.title ?: "نتائج بحث Google" }
                        add(
                            MarketNewsArticle(
                                id = "${topic.name}_$i",
                                title = title,
                                summary = summary,
                                impact = parseImpact(obj.optString("impact", "")),
                                regionTag = obj.optString("regionTag", "عالمي").trim().ifBlank { "عالمي" },
                                publishedHint = obj.optString("publishedHint", "تحديث اليوم").trim().ifBlank { "تحديث اليوم" },
                                sourceTitle = resolvedSourceTitle,
                                sourceUrl = resolvedUrl,
                            ),
                        )
                    }
                }
                if (articles.isNotEmpty() || macroSummary.isNotBlank()) {
                    return MarketNewsFeed(
                        topic = topic,
                        macroSummary = macroSummary.ifBlank {
                            articles.firstOrNull()?.summary.orEmpty()
                        },
                        articles = articles,
                        groundingSources = groundingSources,
                        searchQueries = searchQueries,
                        fetchedAtLabel = fetchedAtLabel,
                    )
                }
            } catch (_: Exception) {
                // Fall back cleanly to plain-text parsing if model returned prose instead of JSON.
            }
        }

        val lines = rawText.lines()
            .map { it.trim().trimStart('-', '*', '•', ' ') }
            .filter { it.length > 15 }
        val fallbackArticles = lines.take(5).mapIndexed { idx, line ->
            val source = groundingSources.getOrNull(idx) ?: groundingSources.firstOrNull()
            MarketNewsArticle(
                id = "${topic.name}_fallback_$idx",
                title = line.take(72).let { if (line.length > 72) "$it..." else it },
                summary = line,
                impact = NewsImpactDirection.NEUTRAL,
                regionTag = topic.label,
                publishedHint = "تحديث مباشر",
                sourceTitle = source?.title ?: "Google Search",
                sourceUrl = source?.url.orEmpty(),
            )
        }
        return MarketNewsFeed(
            topic = topic,
            macroSummary = lines.firstOrNull().orEmpty(),
            articles = fallbackArticles,
            groundingSources = groundingSources,
            searchQueries = searchQueries,
            fetchedAtLabel = fetchedAtLabel,
        )
    }

    fun isSafeHttpUrl(url: String): Boolean {
        val normalized = url.trim().lowercase(Locale.US)
        return normalized.startsWith("https://") || normalized.startsWith("http://")
    }

    private fun extractJsonPayload(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) return trimmed
        val firstBrace = trimmed.indexOf('{')
        val lastBrace = trimmed.lastIndexOf('}')
        return if (firstBrace in 0 until lastBrace) {
            trimmed.substring(firstBrace, lastBrace + 1)
        } else {
            null
        }
    }

    private fun parseImpact(raw: String): NewsImpactDirection {
        val upper = raw.trim().uppercase(Locale.US)
        return when {
            upper.contains("BULL") || upper.contains("صعود") -> NewsImpactDirection.BULLISH
            upper.contains("BEAR") || upper.contains("هبوط") -> NewsImpactDirection.BEARISH
            else -> NewsImpactDirection.NEUTRAL
        }
    }

    private fun extractDomain(url: String): String {
        val withoutScheme = url.substringAfter("://", url)
        return withoutScheme.substringBefore('/').removePrefix("www.")
    }
}
