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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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

data class HeroItem(
    val name: String,
    val posterUrl: String?,
    val apiName: String,
    val url: String,
    val typeName: String,
    val rawItem: Any
)

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
        val resumeState = mutableStateOf<List<SearchResponse>?>(null)

        val composeView = ComposeView(requireContext()).apply {
            setContent {
                CloudPlayTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val isPageLoading = pageState.value is Resource.Loading

                        if (isPageLoading) {
                            // Single unified loading screen for fast feedback
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        } else {
                            // Derive Hero items instantly from detailed preview or fallback search responses
                            val detailedPreviews = (previewState.value as? Resource.Success)?.value?.second
                            val fallbackSearchItems = randomState.value
                                ?: (pageState.value as? Resource.Success)?.value?.values?.flatMap { it.list.list }?.distinctBy { it.url }

                            val heroItems = remember(detailedPreviews, fallbackSearchItems) {
                                if (!detailedPreviews.isNullOrEmpty()) {
                                    detailedPreviews.take(5).map { loadResp ->
                                        HeroItem(
                                            name = loadResp.name,
                                            posterUrl = loadResp.posterUrl,
                                            apiName = loadResp.apiName,
                                            url = loadResp.url,
                                            typeName = loadResp.type.name,
                                            rawItem = loadResp
                                        )
                                    }
                                } else if (!fallbackSearchItems.isNullOrEmpty()) {
                                    fallbackSearchItems.take(5).map { searchResp ->
                                        HeroItem(
                                            name = searchResp.name ?: "",
                                            posterUrl = searchResp.posterUrl,
                                            apiName = searchResp.apiName ?: "",
                                            url = searchResp.url,
                                            typeName = searchResp.type?.name ?: "Featured",
                                            rawItem = searchResp
                                        )
                                    }
                                } else {
                                    emptyList()
                                }
                            }

                            Box(modifier = Modifier.fillMaxSize()) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 90.dp)
                                ) {
                                    // Apple TV Style Hero Banner Section
                                    if (heroItems.isNotEmpty()) {
                                        item {
                                            AppleTvHeroBanner(
                                                items = heroItems,
                                                apiName = homeViewModel.apiName.value ?: "Provider",
                                                onSelectApi = {
                                                    requireContext().selectHomepage(homeViewModel.apiName.value) { api ->
                                                        homeViewModel.loadAndCancel(api, forceReload = true, fromUI = true)
                                                    }
                                                },
                                                onAccount = { activity?.showAccountSelectLinear() },
                                                onHeroClick = { heroItem ->
                                                    val rootView = requireActivity().window.decorView
                                                    when (val raw = heroItem.rawItem) {
                                                        is LoadResponse -> {
                                                            val loadCb = LoadClickCallback(0, rootView, -1, raw)
                                                            homeViewModel.click(loadCb)
                                                        }
                                                        is SearchResponse -> {
                                                            SearchHelper.handleSearchClickCallback(
                                                                SearchClickCallback(SEARCH_ACTION_LOAD, rootView, -1, raw)
                                                            )
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }

                                    // Continue Watching Row (Apple TV Style)
                                    item {
                                        val continueItems = resumeState.value
                                        if (!continueItems.isNullOrEmpty()) {
                                            Spacer(modifier = Modifier.height(16.dp))
                                            ContinueWatchingSection(
                                                items = continueItems,
                                                onItemClick = { item ->
                                                    val rootView = requireActivity().window.decorView
                                                    SearchHelper.handleSearchClickCallback(
                                                        SearchClickCallback(SEARCH_ACTION_PLAY_FILE, rootView, -1, item)
                                                    )
                                                }
                                            )
                                        }
                                    }

                                    // Homepage Categories
                                    when (val ps = pageState.value) {
                                        is Resource.Success -> {
                                            val map = ps.value
                                            items(map.entries.toList()) { entry ->
                                                Spacer(modifier = Modifier.height(20.dp))
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
                                            }
                                        }
                                        is Resource.Failure -> {
                                            item {
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(16.dp)
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
        }

        homeViewModel.page.observe(viewLifecycleOwner) { pageState.value = it }
        homeViewModel.preview.observe(viewLifecycleOwner) { previewState.value = it }
        homeViewModel.randomItems.observe(viewLifecycleOwner) { randomState.value = it }
        homeViewModel.resumeWatching.observe(viewLifecycleOwner) { resumeState.value = it }

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
fun AppleTvHeroBanner(
    items: List<HeroItem>,
    apiName: String,
    onSelectApi: () -> Unit,
    onAccount: () -> Unit,
    onHeroClick: (HeroItem) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { items.size })

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(440.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = items[page]
            Box(modifier = Modifier.fillMaxSize()) {
                if (!item.posterUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = item.posterUrl,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                // Gradient Scrim Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.5f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f),
                                    Color.Black
                                )
                            )
                        )
                )

                // Hero Item Details
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 32.sp
                        ),
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${item.apiName} • ${item.typeName}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Apple TV Style Play & Bookmark Buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { onHeroClick(item) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp)
                        ) {
                            Text("▶  Play", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        IconButton(
                            onClick = { onHeroClick(item) },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f))
                        ) {
                            Text("+", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Pager Indicator Dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(items.size) { index ->
                            val isSelected = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .height(6.dp)
                                    .width(if (isSelected) 20.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.4f))
                            )
                        }
                    }
                }
            }
        }

        // Top Header Overlay (Logo, Source, Account)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "CloudPlay",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    ),
                    color = Color.White
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = onSelectApi,
                    label = { Text(apiName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color.Black.copy(alpha = 0.45f),
                        labelColor = Color.White
                    ),
                    border = null
                )

                IconButton(
                    onClick = onAccount,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                ) {
                    Text("👤", fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun ContinueWatchingSection(items: List<SearchResponse>, onItemClick: (SearchResponse) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Continue Watching on CloudPlay",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("›", fontSize = 22.sp, color = MaterialTheme.colorScheme.onBackground)
        }
        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(items) { item ->
                ElevatedCard(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .width(200.dp)
                        .height(118.dp)
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
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                    )
                                )
                        )
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "▶",
                                fontSize = 12.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = item.name ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeSection(title: String, items: List<SearchResponse>, onClick: (SearchResponse) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 10.dp)
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
