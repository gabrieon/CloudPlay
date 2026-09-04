package com.lagradost.cloudstream3.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import coil3.compose.AsyncImage
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.mvvm.Resource
import com.lagradost.cloudstream3.ui.CloudPlayTheme

class SearchComposeFragment : Fragment() {
    private val searchViewModel: SearchViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        searchViewModel.updateHistory()

        val searchState = mutableStateOf<Resource<ExpandableSearchList>?>(null)
        val historyState = mutableStateOf<List<SearchHistoryItem>>(emptyList())
        val queryState = mutableStateOf("")

        val composeView = ComposeView(requireContext()).apply {
            setContent {
                CloudPlayTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Search",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = queryState.value,
                                onValueChange = { query ->
                                    queryState.value = query
                                    searchViewModel.fetchSuggestions(query)
                                    if (query.length >= 2) {
                                        searchViewModel.searchAndCancel(query)
                                    } else if (query.isEmpty()) {
                                        searchViewModel.clearSearch()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Search movies, TV series, anime...") },
                                singleLine = true,
                                shape = RoundedCornerShape(28.dp),
                                trailingIcon = {
                                    if (queryState.value.isNotEmpty()) {
                                        IconButton(onClick = {
                                            queryState.value = ""
                                            searchViewModel.clearSearch()
                                        }) {
                                            Text("✕")
                                        }
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Search History Chips
                            if (queryState.value.isEmpty() && historyState.value.isNotEmpty()) {
                                Text(
                                    text = "Recent Searches",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(historyState.value) { item ->
                                        AssistChip(
                                            onClick = {
                                                queryState.value = item.searchText
                                                searchViewModel.searchAndCancel(item.searchText)
                                            },
                                            label = { Text(item.searchText) },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            // Search Results
                            when (val res = searchState.value) {
                                is Resource.Success -> {
                                    val list = res.value.list
                                    if (list.isEmpty()) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (queryState.value.isEmpty()) "Start typing to search" else "No results found",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    } else {
                                        LazyVerticalGrid(
                                            columns = GridCells.Adaptive(minSize = 110.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            items(list) { item ->
                                                SearchPosterCard(item) { searchItem ->
                                                    val rootView = requireActivity().window.decorView
                                                    SearchHelper.handleSearchClickCallback(
                                                        SearchClickCallback(SEARCH_ACTION_LOAD, rootView, -1, searchItem)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                is Resource.Loading -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                is Resource.Failure -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Error: ${res.errorString}",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                else -> {
                                    if (queryState.value.isEmpty()) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Search across available providers",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        searchViewModel.searchResponse.observe(viewLifecycleOwner) { searchState.value = it }
        searchViewModel.currentHistory.observe(viewLifecycleOwner) { historyState.value = it }

        return composeView
    }
}

@Composable
fun SearchPosterCard(item: SearchResponse, onClick: (SearchResponse) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(item) }
    ) {
        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(165.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (!item.posterUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No Poster", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.name ?: "",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
