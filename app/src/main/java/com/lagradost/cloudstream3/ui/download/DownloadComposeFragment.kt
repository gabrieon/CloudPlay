package com.lagradost.cloudstream3.ui.download

import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.lagradost.cloudstream3.ui.CloudPlayTheme
import com.lagradost.cloudstream3.utils.AppContextUtils.loadResult

class DownloadComposeFragment : Fragment() {
    private val downloadViewModel: DownloadViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val headersState = mutableStateOf<Resource<List<VisualDownloadCached.Header>>?>(null)
        val availableBytesState = mutableStateOf<Long?>(null)
        val usedBytesState = mutableStateOf<Long?>(null)

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
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Downloads",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Storage Info Card
                            val ctx = LocalContext.current
                            val freeText = availableBytesState.value?.let { Formatter.formatShortFileSize(ctx, it) } ?: "..."
                            val usedText = usedBytesState.value?.let { Formatter.formatShortFileSize(ctx, it) } ?: "..."

                            ElevatedCard(
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Storage Used: $usedText", style = MaterialTheme.typography.bodyMedium)
                                        Text("Free: $freeText", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { 0.5f },
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.outlineVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            when (val res = headersState.value) {
                                is Resource.Success -> {
                                    val list = res.value
                                    if (list.isEmpty()) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "No active downloads",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    } else {
                                        LazyColumn(
                                            verticalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            items(list) { header ->
                                                DownloadHeaderCard(header) {
                                                    activity?.loadResult(
                                                        header.data.url,
                                                        header.data.apiName,
                                                        header.data.name,
                                                        0
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
                                            text = "Error loading downloads",
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

        downloadViewModel.headerCards.observe(viewLifecycleOwner) { headersState.value = it }
        downloadViewModel.availableBytes.observe(viewLifecycleOwner) { availableBytesState.value = it }
        downloadViewModel.usedBytes.observe(viewLifecycleOwner) { usedBytesState.value = it }

        return composeView
    }
}

@Composable
fun DownloadHeaderCard(header: VisualDownloadCached.Header, onClick: () -> Unit) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ElevatedCard(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .width(60.dp)
                    .height(90.dp)
            ) {
                if (!header.data.poster.isNullOrEmpty()) {
                    AsyncImage(
                        model = header.data.poster,
                        contentDescription = header.data.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No Image", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = header.data.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Downloads: ${header.totalDownloads}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
