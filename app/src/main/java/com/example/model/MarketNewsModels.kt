package com.example.model

enum class MarketNewsTopic(
    val label: String,
    val searchFocus: String,
) {
    ALL(
        label = "الكل",
        searchFocus = "latest global gold spot price XAU/USD news, Federal Reserve interest rate signals, central bank gold demand, and local Egypt gold market trends",
    ),
    GLOBAL_SPOT(
        label = "الذهب العالمي",
        searchFocus = "latest XAU/USD global spot gold price movement, resistance/support levels, and bullion market headlines today",
    ),
    CENTRAL_BANKS(
        label = "الفيدرالي والبنوك",
        searchFocus = "latest US Federal Reserve interest rate news, inflation data, US dollar index, and central bank gold reserves buying",
    ),
    LOCAL_MARKET(
        label = "السوق المصري",
        searchFocus = "أحدث أخبار أسعار الذهب في مصر اليوم عيار 21 وعيار 24 وسعر الصرف وتوقعات شعبة الذهب والصاغة",
    ),
}

enum class NewsImpactDirection(
    val label: String,
) {
    BULLISH("داعم للصعود"),
    BEARISH("ضغط هبوطي"),
    NEUTRAL("متوازن"),
}

data class MarketNewsSource(
    val title: String,
    val url: String,
    val domain: String,
)

data class MarketNewsArticle(
    val id: String,
    val title: String,
    val summary: String,
    val impact: NewsImpactDirection,
    val regionTag: String,
    val publishedHint: String,
    val sourceTitle: String,
    val sourceUrl: String,
)

data class MarketNewsFeed(
    val topic: MarketNewsTopic,
    val macroSummary: String,
    val articles: List<MarketNewsArticle>,
    val groundingSources: List<MarketNewsSource>,
    val searchQueries: List<String>,
    val fetchedAtLabel: String,
)

sealed interface MarketNewsUiState {
    data object Idle : MarketNewsUiState
    data object Loading : MarketNewsUiState
    data class Success(val feed: MarketNewsFeed) : MarketNewsUiState
    data class Error(val message: String) : MarketNewsUiState
}
