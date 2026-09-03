package com.lagradost.cloudstream3.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import coil3.compose.AsyncImage
import com.lagradost.cloudstream3.BuildConfig
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.ui.CloudPlayTheme
import com.lagradost.cloudstream3.ui.account.AccountHelper.showAccountSelectLinear
import com.lagradost.cloudstream3.utils.DataStoreHelper
import com.lagradost.cloudstream3.utils.UiImage
import com.lagradost.cloudstream3.utils.UIHelper.navigate

data class SettingItem(
    val title: String,
    val subtitle: String,
    val iconEmoji: String,
    val navigationId: Int
)

class SettingsComposeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val composeView = ComposeView(requireContext()).apply {
            setContent {
                CloudPlayTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Text(
                                    text = "Settings",
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            // Account Profile Card
                            item {
                                val context = LocalContext.current
                                val currentAccount = DataStoreHelper.accounts.firstOrNull {
                                    it.keyIndex == DataStoreHelper.selectedKeyIndex
                                } ?: DataStoreHelper.getDefaultAccount(context)

                                val imageModel: Any? = when (val img = currentAccount.image) {
                                    is UiImage.Image -> img.url
                                    is UiImage.Drawable -> img.resId
                                    is UiImage.Bitmap -> img.bitmap
                                }

                                ElevatedCard(
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { activity?.showAccountSelectLinear() },
                                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (imageModel != null) {
                                                AsyncImage(
                                                    model = imageModel,
                                                    contentDescription = currentAccount.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Text("👤", fontSize = 28.sp)
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = currentAccount.name,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            Text(
                                                text = "Tap to switch account or manage sync",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            // Settings Section Items
                            val settingsList = listOf(
                                SettingItem("General", "App behavior, languages, and network", "⚙️", R.id.action_navigation_global_to_navigation_settings_general),
                                SettingItem("Player", "Subtitles, gesture controls, and video quality", "🎬", R.id.action_navigation_global_to_navigation_settings_player),
                                SettingItem("User Interface", "Themes, posters, and layout preferences", "🎨", R.id.action_navigation_global_to_navigation_settings_ui),
                                SettingItem("Providers", "Filter active scrapers and language preferences", "🔌", R.id.action_navigation_global_to_navigation_settings_providers),
                                SettingItem("Extensions", "Manage installed plugins and repositories", "📦", R.id.action_navigation_global_to_navigation_settings_extensions),
                                SettingItem("Account & Sync", "Manage MAL, AniList, Simkl, and OpenSubtitles", "🔄", R.id.action_navigation_global_to_navigation_settings_account),
                                SettingItem("Updates", "Check for application and extension updates", "🚀", R.id.action_navigation_global_to_navigation_settings_updates),
                            )

                            items(settingsList) { setting ->
                                SettingsRowCard(setting) {
                                    activity?.navigate(setting.navigationId, Bundle())
                                }
                            }

                            // App Info Footer
                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedCard(
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "CloudPlay v${BuildConfig.VERSION_NAME}",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Material You 3 Architecture",
                                            style = MaterialTheme.typography.bodySmall,
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

        return composeView
    }
}

@Composable
fun SettingsRowCard(item: SettingItem, onClick: () -> Unit) {
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(item.iconEmoji, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Text("›", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
