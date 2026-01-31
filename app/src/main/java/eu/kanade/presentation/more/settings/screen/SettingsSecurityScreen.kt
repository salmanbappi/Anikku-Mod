package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.util.system.AuthenticatorUtil.authenticate
import eu.kanade.tachiyomi.util.system.AuthenticatorUtil.isAuthenticationSupported
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableMap
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsSecurityScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_security

    @Composable
    override fun getPreferences(): List<Preference> {
        val context = LocalContext.current
        val securityPreferences = remember { Injekt.get<SecurityPreferences>() }
        val authSupported = remember { context.isAuthenticationSupported() }

        val useAuthPref = securityPreferences.useAuthenticator()
        val useAuth by useAuthPref.collectAsState()

        return listOf(
            Preference.PreferenceItem.SwitchPreference(
                pref = useAuthPref,
                title = stringResource(MR.strings.lock_with_biometrics),
                enabled = authSupported,
                onValueChanged = {
                    (context as FragmentActivity).authenticate(
                        title = context.stringResource(MR.strings.lock_with_biometrics),
                    )
                },
            ),
            Preference.PreferenceItem.ListPreference(
                pref = securityPreferences.lockAppAfter(),
                title = stringResource(MR.strings.lock_when_idle),
                enabled = authSupported && useAuth,
                entries = LockAfterValues
                    .associateWith {
                        when (it) {
                            -1 -> stringResource(MR.strings.lock_never)
                            0 -> stringResource(MR.strings.lock_always)
                            else -> pluralStringResource(
                                MR.plurals.lock_after_mins,
                                count = it,
                                it,
                            )
                        }
                    }
                    .toImmutableMap(),
                onValueChanged = {
                    (context as FragmentActivity).authenticate(
                        title = context.stringResource(MR.strings.lock_when_idle),
                    )
                },
            ),
            Preference.PreferenceItem.SwitchPreference(
                pref = securityPreferences.hideNotificationContent(),
                title = stringResource(MR.strings.hide_notification_content),
            ),
            Preference.PreferenceItem.ListPreference(
                pref = securityPreferences.secureScreen(),
                title = stringResource(MR.strings.secure_screen),
                entries = SecurityPreferences.SecureScreenMode.entries
                    .associateWith { stringResource(it.titleRes) }
                    .toImmutableMap(),
            ),
            Preference.PreferenceItem.InfoPreference(stringResource(MR.strings.secure_screen_summary)),

            // SY -->
            Preference.PreferenceGroup(
                title = stringResource(SYMR.strings.pref_security),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        pref = securityPreferences.encryptDatabase(),
                        title = stringResource(SYMR.strings.encrypt_database),
                        subtitle = stringResource(SYMR.strings.encrypt_database_subtitle),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        pref = securityPreferences.encryptionType(),
                        title = stringResource(SYMR.strings.encryption_type),
                        entries = SecurityPreferences.EncryptionType.entries
                            .associateWith { stringResource(it.titleRes) }
                            .toImmutableMap(),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = securityPreferences.passwordProtectDownloads(),
                        title = stringResource(SYMR.strings.password_protect_downloads),
                        subtitle = stringResource(SYMR.strings.password_protect_downloads_summary),
                    ),
                ),
            ),
            // SY <--
        )
    }
}

private val LockAfterValues = persistentListOf(
    0, // Always
    1,
    2,
    5,
    10,
    -1, // Never
)