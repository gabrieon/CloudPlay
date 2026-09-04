package com.lagradost.cloudstream3.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import coil3.compose.AsyncImage
import com.lagradost.cloudstream3.mvvm.Resource
import com.lagradost.cloudstream3.syncproviders.SyncAPI
import com.lagradost.cloudstream3.ui.CloudPlayTheme
import com.lagradost.cloudstream3.ui.search.SEARCH_ACTION_LOAD
import com.lagradost.cloudstream3.ui.search.SearchClickCallback
import com.lagradost.cloudstream3.ui.search.SearchHelper

class LibraryComposeFragment : Fragment() {
    private val libraryViewModel: LibraryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        libraryViewModel.reloadPages(forceReload = false)

        val pagesState = mutableStateOf<Resource<List<SyncAPI.Page>>?>(null)
        val currentPageIndex = mutableStateOf(0)
        val currentApiNameState = mutableStateOf("")

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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Library",
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.weight(1f)
                                )

                                val apiNames = libraryViewModel.availableApiNames
                                if (apiNames.isNotEmpty()) {
                                    AssistChip(
                                        onClick = {
                                            val current = currentApiNameState.value
                                            val nextIndex = (apiNames.indexOf(current) + 1) % apiNames.size
                                            libraryViewModel.switchList(apiNames[nextIndex])
                                        },
                                        label = { Text(currentApiNameState.value.ifEmpty { "Sync Provider" }) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            when (val res = pagesState.value) {
                                is Resource.Success -> {
                                    val pages = res.value
                                    if (pages.isNotEmpty()) {
                                        ScrollableTabRow(
                                            selectedTabIndex = currentPageIndex.value.coerceIn(0, pages.size - 1),
                                            edgePadding = 0.dp,
                                            containerColor = MaterialTheme.colorScheme.background
                                        ) {
                                            pages.forEachIndexed { index, page ->
                                                Tab(
                                                    selected = currentPageIndex.value == index,
                                                    onClick = {
                                                        currentPageIndex.value = index
                                                        libraryViewModel.switchPage(index)
                                                    },
                                                    text = {
                                                        Text(
                                                            text = page.title.asString(LocalContext.current),
                                                            fontWeight = if (currentPageIndex.value == index) FontWeight.Bold else FontWeight.Normal
                                                        )
                                                    }
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        val selectedPage = pages.getOrNull(currentPageIndex.value.coerceIn(0, pages.size - 1))
                                        val items = selectedPage?.items ?: emptyList()

                                        if (items.isEmpty()) {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "No items in this category",
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
                                                items(items) { item ->
                                                    LibraryPosterCard(item) { libraryItem ->
                                                        val rootView = requireActivity().window.decorView
                                                        SearchHelper.handleSearchClickCallback(
                                                            SearchClickCallback(SEARCH_ACTION_LOAD, rootView, -1, libraryItem)
                                                        )
                                                    }
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
                                else -> {}
                            }
                        }
                    }
                }
            }
        }

        libraryViewModel.pages.observe(viewLifecycleOwner) { pagesState.value = it }
        libraryViewModel.currentPage.observe(viewLifecycleOwner) { currentPageIndex.value = it }
        libraryViewModel.currentApiName.observe(viewLifecycleOwner) { currentApiNameState.value = it }

        return composeView
    }
}

@Composable
fun LibraryPosterCard(item: com.lagradost.cloudstream3.syncproviders.SyncAPI.LibraryItem, onClick: (com.lagradost.cloudstream3.syncproviders.SyncAPI.LibraryItem) -> Unit) {
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
            text = item.name,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
