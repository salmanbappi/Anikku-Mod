package eu.kanade.presentation.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import eu.kanade.domain.ai.AiPreferences
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.toast
import tachiyomi.presentation.core.util.collectAsState
import tachiyomi.presentation.core.util.secondaryItemAlpha
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun LogoHeader() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val aiPreferences = remember { Injekt.get<AiPreferences>() }
    val displayName by aiPreferences.displayName().collectAsState()
    val profilePhotoUri by aiPreferences.profilePhotoUri().collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    context.toast("User: $displayName")
                },
            contentAlignment = Alignment.Center
        ) {
            if (profilePhotoUri.isNotEmpty()) {
                AsyncImage(
                    model = profilePhotoUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_launcher_monochrome),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = displayName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "AniZen v${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.secondaryItemAlpha()
        )

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
    }
}
