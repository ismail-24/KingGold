package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.SearchHistoryHelper
import com.example.model.ProductCategory
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText

@Composable
fun SearchHistorySuggestionsPanel(
    query: String,
    searchHistory: List<String>,
    selectedCategory: String?,
    catalogHints: List<String>,
    onSelectHistoryQuery: (String) -> Unit,
    onRemoveHistoryQuery: (String) -> Unit,
    onClearHistory: () -> Unit,
    onSelectCategory: (ProductCategory) -> Unit,
    onSelectCatalogHint: (String) -> Unit,
    onDismiss: () -> Unit,
    tagPrefix: String,
    modifier: Modifier = Modifier,
) {
    val payload = remember(query, searchHistory, catalogHints) {
        SearchHistoryHelper.buildSuggestions(
            query = query,
            history = searchHistory,
            catalogHints = catalogHints,
            maxHistory = 4,
            maxCatalogMatches = 3,
        )
    }

    if (payload.isEmpty) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("${tagPrefix}_suggestions_panel"),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, AppColors.Brass.copy(alpha = 0.4f)),
        shadowElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (payload.recentSearches.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = null,
                            tint = AppColors.Brass,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.search_history_recent_title),
                            style = AppText.body.copy(
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.Ink,
                            ),
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = onClearHistory,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("${tagPrefix}_clear_all"),
                        ) {
                            Text(
                                text = stringResource(R.string.search_history_clear_all),
                                style = AppText.body.copy(
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.Alert,
                                ),
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    payload.recentSearches.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable { onSelectHistoryQuery(item) }
                                .padding(horizontal = 4.dp)
                                .testTag("${tagPrefix}_history_item_$index"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = null,
                                tint = AppColors.Muted,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = item,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = AppText.body.copy(
                                    fontSize = 13.sp,
                                    color = AppColors.Ink,
                                ),
                            )
                            IconButton(
                                onClick = { onRemoveHistoryQuery(item) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("${tagPrefix}_history_remove_$index"),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = stringResource(R.string.search_history_remove_cd),
                                    tint = AppColors.Muted,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }

            if (payload.recentSearches.isNotEmpty() &&
                (payload.categorySuggestions.isNotEmpty() || payload.matchingItems.isNotEmpty())
            ) {
                HorizontalDivider(color = AppColors.Line)
            }

            if (payload.matchingItems.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.search_history_matches_title),
                    style = AppText.body.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Ink,
                    ),
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    payload.matchingItems.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable { onSelectCatalogHint(item) }
                                .padding(horizontal = 4.dp)
                                .testTag("${tagPrefix}_match_item_$index"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = AppColors.Brass,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = item,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = AppText.body.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = AppColors.Ink,
                                ),
                            )
                        }
                    }
                }
            }

            if (payload.categorySuggestions.isNotEmpty()) {
                if (payload.matchingItems.isNotEmpty()) {
                    HorizontalDivider(color = AppColors.Line)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = AppColors.Brass,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.search_history_categories_title),
                            style = AppText.body.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.Ink,
                            ),
                        )
                    }
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("${tagPrefix}_dismiss_suggestions"),
                    ) {
                        Text(
                            text = stringResource(R.string.search_history_hide_suggestions),
                            style = AppText.body.copy(
                                fontSize = 11.5.sp,
                                color = AppColors.Muted,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                    }
                }

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("${tagPrefix}_category_suggestions_row"),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(payload.categorySuggestions, key = { it.name }) { category ->
                        val isSelected = selectedCategory == category.arabicLabel
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectCategory(category) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("${tagPrefix}_category_suggestion_${category.name.lowercase()}"),
                            label = {
                                Text(
                                    text = category.arabicLabel,
                                    style = AppText.body.copy(
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AppColors.Brass,
                                selectedLabelColor = Color.White,
                                containerColor = AppColors.ParchmentDeep,
                                labelColor = AppColors.Ink,
                            ),
                        )
                    }
                }
            }
        }
    }
}
