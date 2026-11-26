package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.ProductCategory
import com.example.ui.components.normalizeShopSearchText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val Context.searchHistoryDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "kinggold_search_history",
)

data class SearchSuggestionsPayload(
    val recentSearches: List<String> = emptyList(),
    val categorySuggestions: List<ProductCategory> = emptyList(),
    val matchingItems: List<String> = emptyList(),
) {
    val isEmpty: Boolean
        get() = recentSearches.isEmpty() && categorySuggestions.isEmpty() && matchingItems.isEmpty()
}

object SearchHistoryHelper {
    const val MAX_HISTORY_ITEMS = 10
    const val MIN_QUERY_LENGTH = 2

    private val CATEGORY_SEARCH_KEYWORDS: Map<ProductCategory, List<String>> = mapOf(
        ProductCategory.COINS to listOf("جنيه ذهب", "جنيه", "عملة ذهبية", "عمله", "سبيكة جنيه"),
        ProductCategory.BARS_BULLION to listOf("سبيكة", "سبيكة ذهب", "سبيكه", "اونصة", "أونصة", "bullion"),
        ProductCategory.BRACELETS to listOf("أسورة", "اسورة", "اسوره", "غويشة", "غويشه", "انسيال", "إنسيال"),
        ProductCategory.RINGS to listOf("خاتم", "دبلة", "دبله", "سوليتير", "توينز"),
        ProductCategory.CHAINS to listOf("سلسلة", "سلسله", "قلادة", "كوليه", "عقد"),
        ProductCategory.EARRINGS to listOf("حلق", "قرط", "بريمة"),
        ProductCategory.JEWELRY_SETS to listOf("طقم", "شبكة", "شبكه", "زفاف"),
        ProductCategory.DESIGNS to listOf("دلاية", "تعليقة", "بروش", "آية الكرسي", "فرعوني"),
    )

    fun normalizeQuery(raw: String): String =
        raw.trim().replace(Regex("\\s+"), " ")

    fun addQuery(
        current: List<String>,
        rawQuery: String,
        maxSize: Int = MAX_HISTORY_ITEMS,
    ): List<String> {
        val cleaned = normalizeQuery(rawQuery)
        if (cleaned.length < MIN_QUERY_LENGTH) return current
        val normalizedTarget = normalizeShopSearchText(cleaned)
        val filtered = current.filterNot { existing ->
            normalizeShopSearchText(existing) == normalizedTarget
        }
        return (listOf(cleaned) + filtered).take(maxSize.coerceAtLeast(1))
    }

    fun removeQuery(
        current: List<String>,
        query: String,
    ): List<String> {
        val normalizedTarget = normalizeShopSearchText(query)
        if (normalizedTarget.isEmpty()) return current
        return current.filterNot { existing ->
            normalizeShopSearchText(existing) == normalizedTarget
        }
    }

    fun encodeHistory(items: List<String>): String =
        items
            .map { normalizeQuery(it) }
            .filter { it.length >= MIN_QUERY_LENGTH }
            .joinToString(separator = "\n") { item ->
                buildString(item.length) {
                    for (ch in item) {
                        when (ch) {
                            '\\' -> append("\\\\")
                            '\n' -> append("\\n")
                            '\r' -> append("\\r")
                            else -> append(ch)
                        }
                    }
                }
            }

    fun decodeHistory(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        val result = mutableListOf<String>()
        val seen = mutableSetOf<String>()
        raw.split('\n').forEach { line ->
            if (line.isEmpty()) return@forEach
            val decoded = buildString(line.length) {
                var i = 0
                while (i < line.length) {
                    val c = line[i]
                    if (c == '\\' && i + 1 < line.length) {
                        when (line[i + 1]) {
                            '\\' -> {
                                append('\\')
                                i += 2
                            }
                            'n' -> {
                                append('\n')
                                i += 2
                            }
                            'r' -> {
                                append('\r')
                                i += 2
                            }
                            else -> {
                                append(c)
                                i++
                            }
                        }
                    } else {
                        append(c)
                        i++
                    }
                }
            }
            val cleaned = normalizeQuery(decoded)
            val key = normalizeShopSearchText(cleaned)
            if (cleaned.length >= MIN_QUERY_LENGTH && seen.add(key)) {
                result.add(cleaned)
            }
        }
        return result.take(MAX_HISTORY_ITEMS)
    }

    fun buildSuggestions(
        query: String,
        history: List<String>,
        catalogHints: List<String> = emptyList(),
        maxHistory: Int = 6,
        maxCatalogMatches: Int = 4,
    ): SearchSuggestionsPayload {
        val normalizedQuery = normalizeShopSearchText(query)
        val recent = if (normalizedQuery.isEmpty()) {
            history.take(maxHistory)
        } else {
            history.filter { item ->
                normalizeShopSearchText(item).contains(normalizedQuery)
            }.take(maxHistory)
        }

        val categories = if (normalizedQuery.isEmpty()) {
            ProductCategory.entries
        } else {
            ProductCategory.entries.filter { category ->
                normalizeShopSearchText(category.arabicLabel).contains(normalizedQuery) ||
                    category.legacyAliases.any { alias ->
                        normalizeShopSearchText(alias).contains(normalizedQuery)
                    } ||
                    CATEGORY_SEARCH_KEYWORDS[category].orEmpty().any { kw ->
                        normalizeShopSearchText(kw).contains(normalizedQuery)
                    }
            }
        }

        val recentKeys = recent.map { normalizeShopSearchText(it) }.toSet()
        val categoryKeys = categories.map { normalizeShopSearchText(it.arabicLabel) }.toSet()
        val matchingItems = if (normalizedQuery.isEmpty()) {
            emptyList()
        } else {
            catalogHints
                .asSequence()
                .map { normalizeQuery(it) }
                .filter { it.length >= MIN_QUERY_LENGTH }
                .distinctBy { normalizeShopSearchText(it) }
                .filter { candidate ->
                    val key = normalizeShopSearchText(candidate)
                    key.contains(normalizedQuery) &&
                        !recentKeys.contains(key) &&
                        !categoryKeys.contains(key)
                }
                .take(maxCatalogMatches)
                .toList()
        }

        return SearchSuggestionsPayload(
            recentSearches = recent,
            categorySuggestions = categories,
            matchingItems = matchingItems,
        )
    }
}

interface SearchHistoryStore {
    val historyFlow: Flow<List<String>>
    suspend fun readHistory(): List<String>
    suspend fun recordQuery(query: String): List<String>
    suspend fun removeQuery(query: String): List<String>
    suspend fun clearHistory(): List<String>
}

class InMemorySearchHistoryStore(
    initialItems: List<String> = emptyList(),
) : SearchHistoryStore {
    private val mutex = Mutex()
    private val state = MutableStateFlow(
        initialItems.fold(emptyList<String>()) { acc, item ->
            SearchHistoryHelper.addQuery(acc, item)
        },
    )

    override val historyFlow: Flow<List<String>> = state

    override suspend fun readHistory(): List<String> = state.value

    override suspend fun recordQuery(query: String): List<String> = mutex.withLock {
        val updated = SearchHistoryHelper.addQuery(state.value, query)
        state.value = updated
        updated
    }

    override suspend fun removeQuery(query: String): List<String> = mutex.withLock {
        val updated = SearchHistoryHelper.removeQuery(state.value, query)
        state.value = updated
        updated
    }

    override suspend fun clearHistory(): List<String> = mutex.withLock {
        state.value = emptyList()
        emptyList()
    }
}

class DataStoreSearchHistoryStore(
    context: Context,
) : SearchHistoryStore {
    private val dataStore = context.applicationContext.searchHistoryDataStore

    override val historyFlow: Flow<List<String>> = dataStore.data.map { prefs ->
        SearchHistoryHelper.decodeHistory(prefs[KEY_RECENT_SEARCHES])
    }

    override suspend fun readHistory(): List<String> = historyFlow.first()

    override suspend fun recordQuery(query: String): List<String> {
        var result: List<String> = emptyList()
        dataStore.edit { prefs ->
            val current = SearchHistoryHelper.decodeHistory(prefs[KEY_RECENT_SEARCHES])
            val updated = SearchHistoryHelper.addQuery(current, query)
            prefs[KEY_RECENT_SEARCHES] = SearchHistoryHelper.encodeHistory(updated)
            result = updated
        }
        return result
    }

    override suspend fun removeQuery(query: String): List<String> {
        var result: List<String> = emptyList()
        dataStore.edit { prefs ->
            val current = SearchHistoryHelper.decodeHistory(prefs[KEY_RECENT_SEARCHES])
            val updated = SearchHistoryHelper.removeQuery(current, query)
            prefs[KEY_RECENT_SEARCHES] = SearchHistoryHelper.encodeHistory(updated)
            result = updated
        }
        return result
    }

    override suspend fun clearHistory(): List<String> {
        dataStore.edit { prefs ->
            prefs.remove(KEY_RECENT_SEARCHES)
        }
        return emptyList()
    }

    companion object {
        private val KEY_RECENT_SEARCHES = stringPreferencesKey("recent_search_queries_v1")
    }
}
