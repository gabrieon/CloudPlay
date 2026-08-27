package com.lagradost.cloudstream3.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import coil3.compose.AsyncImage
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.mvvm.Resource
import com.lagradost.cloudstream3.ui.CloudPlayTheme
import com.lagradost.cloudstream3.ui.search.SEARCH_ACTION_LOAD
import com.lagradost.cloudstream3.ui.search.SEARCH_ACTION_PLAY_FILE
import com.lagradost.cloudstream3.ui.search.SearchClickCallback
import com.lagradost.cloudstream3.ui.search.SearchHelper
import com.lagradost.cloudstream3.ui.home.HomeFragment.Companion.loadHomepageList
import com.lagradost.cloudstream3.ui.home.HomeFragment.Companion.selectHomepage
import com.lagradost.cloudstream3.ui.account.AccountHelper.showAccountSelectLinear
import com.lagradost.cloudstream3.ui.result.ResultData
import com.lagradost.cloudstream3.utils.UIHelper

class HomeComposeFragment : Fragment() {
    private val homeViewModel: HomeViewModel by activityViewModels()

    private var bottomSheetDialog: BottomSheetDialog? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Local mutable states that will be updated from LiveData observers below
        val pageState = mutableStateOf<Resource<Map<String, HomeViewModel.ExpandableHomepageList>>>(Resource.Loading())
        val previewState = mutableStateOf<Resource<Pair<Boolean, List<LoadResponse>>>>(Resource.Loading())
        val randomState = mutableStateOf<List<SearchResponse>?>(null)

        val composeView = ComposeView(requireContext()).apply {
            setContent {
                CloudPlayTheme {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                                TopBar(
                                    apiName = homeViewModel.apiName.value ?: "",
                                    onSelectApi = {
                                        requireContext().selectHomepage(homeViewModel.apiName.value) { api ->
                                            homeViewModel.loadAndCancel(api, forceReload = true, fromUI = true)
                                        }
                                    },
                                    onAccount = {
                                        activity?.showAccountSelectLinear()
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // Preview carousel (if available)
                                when (val prev = previewState.value) {
                                    is Resource.Success -> {
                                        val pair = prev.value
                                        val responses = pair.second
                                        if (responses.isNotEmpty()) {
                                            Text("Preview", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp))
                                            Spacer(modifier = Modifier.height(8.dp))
                                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                items(responses) { resp ->
                                                    // show poster/title
                                                    val title = resp.name
                                                    PosterCardForLoad(resp, onClick = { loadResp ->
                                                        val rootView = requireActivity().window.decorView
                                                        // Call HomeViewModel.click with LoadClickCallback
                                                        val action = 0
                                                        val loadCb = LoadClickCallback(action, rootView, -1, loadResp)
                                                        homeViewModel.click(loadCb)
                                                    })
                                                }
                                                if (pair.first) {
                                                    item {
                                                        TextButton(onClick = { homeViewModel.loadMoreHomeScrollResponses() }) { Text("Load more") }
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                    }
                                    }
                                    else -> {}
                                }

                                when (val ps = pageState.value) {
                                    is Resource.Success -> {
                                        val map = ps.value
                                        for ((key, value) in map) {
                                            Section(title = key, items = value.list.list) { item ->
                                                // use activity root as fallback view
                                                val rootView = requireActivity().window.decorView
                                                val action = if (item is com.lagradost.cloudstream3.utils.DataStoreHelper.ResumeWatchingResult) SEARCH_ACTION_PLAY_FILE else SEARCH_ACTION_LOAD
                                                SearchHelper.handleSearchClickCallback(
                                                    SearchClickCallback(action, rootView, -1, item)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                        }
                                    }

                                    is Resource.Loading -> {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("Loading…", fontSize = 18.sp)
                                        }
                                    }

                                    is Resource.Failure -> {
                                        val msg = ps.errorString
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("Error: $msg", color = MaterialTheme.colorScheme.error)
                                        }
                                    }

                                    else -> {}
                                }
                            }

                            // Floating Random FAB
                            val showRandom = randomState.value?.isNotEmpty() == true
                            if (showRandom) {
                                ExtendedFloatingActionButton(
                                    onClick = {
                                        val distinct = randomState.value!!.distinctBy { it.url }
                                        val pick = distinct.randomOrNull()
                                        pick?.let { item ->
                                            val rootView = requireActivity().window.decorView
                                            SearchHelper.handleSearchClickCallback(
                                                SearchClickCallback(SEARCH_ACTION_LOAD, rootView, -1, item)
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(16.dp)
                                ) {
                                    Text("Random")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Observe LiveData lifecycle-aware and update Compose states
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
            bottomSheetDialog = activity?.loadHomepageList(expandableList, deleteCallback = deleteCallback, expandCallback = { name ->
                homeViewModel.expandAndReturn(name)
            }, dismissCallback = {
                homeViewModel.popup(null)
                bottomSheetDialog = null
            })
        }

        return composeView
    }
}

@Composable
fun TopBar(apiName: String, onSelectApi: () -> Unit, onAccount: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(apiName, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        TextButton(onClick = onSelectApi) { Text("Source") }
        TextButton(onClick = onAccount) { Text("Account") }
    }
}

@Composable
fun Section(title: String, items: List<SearchResponse>, onClick: (SearchResponse) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp))
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items) { item ->
                PosterCard(item, onClick)
            }
        }
    }
}

@Composable
fun PosterCard(item: SearchResponse, onClick: (SearchResponse) -> Unit) {
    Column(modifier = Modifier.width(140.dp).clickable { onClick(item) }) {
        if (!item.posterUrl.isNullOrEmpty()) {
            AsyncImage(
                model = item.posterUrl,
                contentDescription = item.name,
                modifier = Modifier
                    .height(200.dp)
                    .fillMaxWidth()
            )
        } else {
            // Fallback placeholder
            Box(modifier = Modifier
                .height(200.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                Text("No image", style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(item.name ?: "", maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 4.dp))
    }
}

@Composable
fun PosterCardForLoad(item: LoadResponse, onClick: (LoadResponse) -> Unit) {
    Column(modifier = Modifier.width(160.dp).clickable { onClick(item) }) {
        val title = item.name
        val poster = item.posterUrl
        if (!poster.isNullOrEmpty()) {
            AsyncImage(model = poster, contentDescription = title, modifier = Modifier.height(220.dp).fillMaxWidth())
        } else {
            Box(modifier = Modifier.height(220.dp).fillMaxWidth().background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                Text("No image", style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(title, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 4.dp))
    }
}
