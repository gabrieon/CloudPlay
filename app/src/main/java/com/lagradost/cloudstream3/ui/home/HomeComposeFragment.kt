package com.lagradost.cloudstream3.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import coil3.compose.AsyncImage
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.mvvm.Resource
import com.lagradost.cloudstream3.ui.CloudPlayTheme
import com.lagradost.cloudstream3.ui.account.AccountHelper.showAccountSelectLinear
import com.lagradost.cloudstream3.ui.home.HomeFragment.Companion.loadHomepageList
import com.lagradost.cloudstream3.ui.home.HomeFragment.Companion.selectHomepage
import com.lagradost.cloudstream3.ui.search.SEARCH_ACTION_LOAD
import com.lagradost.cloudstream3.ui.search.SEARCH_ACTION_PLAY_FILE
import com.lagradost.cloudstream3.ui.search.SearchClickCallback
import com.lagradost.cloudstream3.ui.search.SearchHelper

class HomeComposeFragment : Fragment() {
    private val homeViewModel: HomeViewModel by activityViewModels()
    private var bottomSheetDialog: BottomSheetDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val pageState = mutableStateOf<Resource<Map<String, HomeViewModel.ExpandableHomepageList>>>(Resource.Loading())
        val previewState = mutableStateOf<Resource<Pair<Boolean, List<LoadResponse>>>>(Resource.Loading())
        val randomState = mutableStateOf<List<SearchResponse>?>(null)

        val composeView = ComposeView(requireContext()).apply {
            setContent {
                CloudPlayTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                item {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HomeTopBar(
                                        apiName = homeViewModel.apiName.value ?: "Select Provider",
                                        onSelectApi = {
                                            requireContext().selectHomepage(homeViewModel.apiName.value) { api ->
                                                homeViewModel.loadAndCancel(api, forceReload = true, fromUI = true)
                                            }
                                        },
                                        onAccount = {
                                            activity?.showAccountSelectLinear()
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                // Featured Preview Carousel
                                item {
                                    when (val prev = previewState.value) {
                                        is Resource.Success -> {
                                            val responses = prev.value.second
                                            if (responses.isNotEmpty()) {
                                                Text(
                                                    text = "Featured",
                                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onBackground,
                                                    modifier = Modifier.padding(bottom = 12.dp)
                                                )
                                                HeroPreviewCarousel(
                                                    items = responses,
                                                    onItemClick = { loadResp ->
                                                        val rootView = requireActivity().window.decorView
                                                        val loadCb = LoadClickCallback(0, rootView, -1, loadResp)
                                                        homeViewModel.click(loadCb)
                                                    }
                                                )
                                                Spacer(modifier = Modifier.height(24.dp))
                                            }
                                        }
                                        is Resource.Loading -> {
                                            LinearProgressIndicator(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 8.dp)
                                            )
                                        }
                                        else -> {}
                                    }
                                }

                                // Homepage Sections
                                when (val ps = pageState.value) {
                                    is Resource.Success -> {
                                        val map = ps.value
                                        items(map.entries.toList()) { entry ->
                                            HomeSection(
                                                title = entry.key,
                                                items = entry.value.list.list,
                                                onClick = { item ->
                                                    val rootView = requireActivity().window.decorView
                                                    val action = if (item is com.lagradost.cloudstream3.utils.DataStoreHelper.ResumeWatchingResult) SEARCH_ACTION_PLAY_FILE else SEARCH_ACTION_LOAD
                                                    SearchHelper.handleSearchClickCallback(
                                                        SearchClickCallback(action, rootView, -1, item)
                                                    )
                                                }
                                            )
                                            Spacer(modifier = Modifier.height(20.dp))
                                        }
                                    }
                                    is Resource.Loading -> {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(200.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                    is Resource.Failure -> {
                                        item {
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 16.dp)
                                            ) {
                                                Text(
                                                    text = "Error: ${ps.errorString}",
                                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                                    modifier = Modifier.padding(16.dp)
                                                )
                                            }
                                        }
                                    }
                                    else -> {}
                                }
                            }

                            // Floating Random Action Button
                            val showRandom = randomState.value?.isNotEmpty() == true
                            if (showRandom) {
                                ExtendedFloatingActionButton(
                                    onClick = {
                                        val pick = randomState.value?.distinctBy { it.url }?.randomOrNull()
                                        pick?.let { item ->
                                            val rootView = requireActivity().window.decorView
                                            SearchHelper.handleSearchClickCallback(
                                                SearchClickCallback(SEARCH_ACTION_LOAD, rootView, -1, item)
                                            )
                                        }
                                    },
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(16.dp)
                                ) {
                                    Text("Random Pick", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }

        homeViewModel.page.observe(viewLifecycleOwner) { pageState.value = it }
        homeViewModel.preview.observe(viewLifecycleOwner) { previewState.value = it }
        homeViewModel.randomItems.observe(viewLifecycleOwner) { randomState.value = it }

        homeViewModel.popup.observe(viewLifecycleOwner) { item ->
            if (item == null) {
                bottomSheetDialog?.dismiss()
                bottomSheetDialog = null
                return@observe
            }
            if (bottomSheetDialog != null) return@observe
            val (expandableList, deleteCallback) = item
            bottomSheetDialog = activity?.loadHomepageList(
                expandableList,
                deleteCallback = deleteCallback,
                expandCallback = { name -> homeViewModel.expandAndReturn(name) },
                dismissCallback = {
                    homeViewModel.popup(null)
                    bottomSheetDialog = null
                }
            )
        }

        return composeView
    }
}

@Composable
fun HomeTopBar(apiName: String, onSelectApi: () -> Unit, onAccount: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "CloudPlay",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.primary
            )
            AssistChip(
                onClick = onSelectApi,
                label = { Text(apiName, fontSize = 12.sp) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        }
        IconButton(
            onClick = onAccount,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text("👤", fontSize = 18.sp)
        }
    }
}

@Composable
fun HeroPreviewCarousel(items: List<LoadResponse>, onItemClick: (LoadResponse) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { item ->
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .width(280.dp)
                    .height(160.dp)
                    .clickable { onItemClick(item) },
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (!item.posterUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = item.posterUrl,
                            contentDescription = item.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                                    startY = 50f
                                )
                            )
                    )
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HomeSection(title: String, items: List<SearchResponse>, onClick: (SearchResponse) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items) { item ->
                PosterCardM3(item, onClick)
            }
        }
    }
}

@Composable
fun PosterCardM3(item: SearchResponse, onClick: (SearchResponse) -> Unit) {
    Column(
        modifier = Modifier
            .width(130.dp)
            .clickable { onClick(item) }
    ) {
        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .width(130.dp)
                .height(195.dp),
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
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = item.name ?: "",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
