package com.historytalk.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.historytalk.app.ui.FigureCard
import com.historytalk.app.ui.FigureListViewModel
import com.historytalk.app.ui.components.EmptyState
import com.historytalk.app.ui.components.FigureAvatar
import com.historytalk.app.ui.components.LoadingState
import com.historytalk.app.ui.components.eraGroups
import com.historytalk.app.ui.components.roleGroups

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FigureListScreen(
    onOpenFigure: (String) -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    vm: FigureListViewModel = hiltViewModel(),
) {
    val figures by vm.figures.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val engine by vm.engine.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("گفتگوی تاریخ", style = MaterialTheme.typography.titleLarge)
                        engine?.let {
                            Text(
                                "موتور: ${it.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onHistory) { Icon(Icons.Default.DateRange, contentDescription = "تاریخچه و جست‌وجو در گفتگوها") }
                    IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, contentDescription = "تنظیمات") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            OutlinedTextField(
                value = filter.query,
                onValueChange = vm::setQuery,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("جست‌وجوی شخصیت، دوره، تمدن…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (filter.query.isNotEmpty()) {
                        IconButton(onClick = { vm.setQuery("") }) { Icon(Icons.Default.Close, contentDescription = "پاک کردن") }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )

            val chips = buildList {
                add(Triple("fav", "علاقه‌مندی‌ها", filter.favoritesOnly))
                eraGroups.forEach { add(Triple("era:${it.first}", it.second, filter.era == it.first)) }
                roleGroups.forEach { add(Triple("role:${it.first}", it.second, filter.role == it.first)) }
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(chips) { (key, label, selected) ->
                    FilterChip(
                        selected = selected,
                        onClick = {
                            when {
                                key == "fav" -> vm.toggleFavoritesOnly()
                                key.startsWith("era:") -> vm.toggleEra(key.removePrefix("era:"))
                                else -> vm.toggleRole(key.removePrefix("role:"))
                            }
                        },
                        label = { Text(label) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            val list = figures
            when {
                list == null -> LoadingState()
                list.isEmpty() -> EmptyState(
                    title = "شخصیتی پیدا نشد",
                    hint = "عبارت جست‌وجو یا فیلترها را تغییر دهید.",
                    action = { TextButton(onClick = vm::clearFilters) { Text("پاک کردن فیلترها") } },
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "${list.size} شخصیت — برای دیدن پروفایل و شروع گفتگو یکی را انتخاب کنید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(list, key = { it.figure.id }) { card ->
                        FigureCardItem(card, onClick = { onOpenFigure(card.figure.id) }, onFavorite = { vm.toggleFavorite(card) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FigureCardItem(card: FigureCard, onClick: () -> Unit, onFavorite: () -> Unit) {
    val f = card.figure
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween) {
                FigureAvatar(f.name, f.colorHex, size = 56.dp)
                IconButton(
                    onClick = onFavorite,
                    modifier = Modifier.size(40.dp).semantics {
                        contentDescription = if (card.favorite) "حذف ${f.name} از علاقه‌مندی‌ها" else "افزودن ${f.name} به علاقه‌مندی‌ها"
                    },
                ) {
                    Icon(
                        if (card.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (card.favorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                    )
                }
            }
            Text(f.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(f.lifespan, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(f.role, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                "${f.civilization} · ${f.era}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (f.sensitive) {
                Text("شخصیت حساس · روایت تاریخی", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
