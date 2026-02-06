package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.browse.ExtensionReposScreen
import eu.kanade.tachiyomi.util.system.AuthenticatorUtil.authenticate
import kotlinx.collections.immutable.persistentListOf
import mihon.domain.extensionrepo.interactor.GetExtensionRepoCount
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.i18n.kmk.KMR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsBrowseScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.browse

    @Composable
    override fun getPreferences(): List<Preference> {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        val sourcePreferences = remember { Injekt.get<SourcePreferences>() }
        val getExtensionRepoCount = remember { Injekt.get<GetExtensionRepoCount>() }

        val animeReposCount by getExtensionRepoCount.subscribe().collectAsState(0)

        return listOf(
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.label_sources),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        pref = sourcePreferences.hideInAnimeLibraryItems(),
                        title = stringResource(MR.strings.pref_hide_in_anime_library_items),
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.label_anime_extension_repos),
                        subtitle = pluralStringResource(
                            MR.plurals.num_repos,
                            animeReposCount,
                            animeReposCount,
                        ),
                        onClick = {
                            navigator.push(ExtensionReposScreen())
                        },
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.pref_category_nsfw_content),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        pref = sourcePreferences.showNsfwSource(),
                        title = stringResource(MR.strings.pref_show_nsfw_source),
                        subtitle = stringResource(MR.strings.requires_app_restart),
                        onValueChanged = {
                            (context as FragmentActivity).authenticate(
                                title = context.stringResource(MR.strings.pref_category_nsfw_content),
                            )
                        },
                    ),
                    Preference.PreferenceItem.InfoPreference(
                        stringResource(MR.strings.parental_controls_info),
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(tachiyomi.i18n.kmk.KMR.strings.pref_source_related_mangas),
                preferenceItems = persistentListOf(
                    Preference.PreferenceItem.SwitchPreference(
                        pref = sourcePreferences.relatedAnimeShowSource(),
                        title = stringResource(tachiyomi.i18n.kmk.KMR.strings.pref_source_related_mangas),
                        subtitle = stringResource(tachiyomi.i18n.kmk.KMR.strings.pref_source_related_mangas_summary),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = sourcePreferences.relatedAnimeExpand(),
                        title = stringResource(tachiyomi.i18n.kmk.KMR.strings.pref_expand_related_mangas),
                        subtitle = stringResource(tachiyomi.i18n.kmk.KMR.strings.pref_expand_related_mangas_summary),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = sourcePreferences.relatedAnimeInOverflow(),
                        title = stringResource(tachiyomi.i18n.kmk.KMR.strings.put_related_mangas_in_overflow),
                        subtitle = stringResource(tachiyomi.i18n.kmk.KMR.strings.put_related_mangas_in_overflow_summary),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        pref = sourcePreferences.relatedAnimeShowHome(),
                        title = stringResource(tachiyomi.i18n.kmk.KMR.strings.pref_show_home_on_related_mangas),
                        subtitle = stringResource(tachiyomi.i18n.kmk.KMR.strings.pref_show_home_on_related_mangas_summary),
                    ),
                ),
            ),
        )
    }
}
