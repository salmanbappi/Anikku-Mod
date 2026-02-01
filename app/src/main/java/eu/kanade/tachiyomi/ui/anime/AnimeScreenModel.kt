package eu.kanade.tachiyomi.ui.anime

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Immutable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.core.util.addOrRemove
import eu.kanade.core.util.insertSeparators
import eu.kanade.domain.anime.interactor.SetAnimeViewerFlags
import eu.kanade.domain.anime.interactor.UpdateAnime
import eu.kanade.domain.anime.model.downloadedFilter
import eu.kanade.domain.anime.model.episodesFiltered
import eu.kanade.domain.anime.model.toSAnime
import eu.kanade.domain.episode.interactor.SetSeenStatus
import eu.kanade.domain.episode.interactor.SyncEpisodesWithSource
import eu.kanade.domain.track.interactor.AddTracks
import eu.kanade.domain.track.interactor.TrackEpisode
import eu.kanade.domain.track.model.AutoTrackState
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.presentation.anime.DownloadAction
import eu.kanade.presentation.anime.components.EpisodeDownloadAction
import eu.kanade.presentation.util.formattedMessage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.data.download.DownloadCache
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.torrentServer.service.TorrentServerService
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.network.HttpException
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.isSourceForTorrents
import eu.kanade.tachiyomi.source.model.UpdateStrategy as SourceUpdateStrategy
import eu.kanade.tachiyomi.torrentServer.TorrentServerUtils
import eu.kanade.tachiyomi.ui.anime.track.TrackItem
import eu.kanade.tachiyomi.ui.player.settings.GesturePreferences
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import eu.kanade.tachiyomi.ui.player.CastManager
import eu.kanade.tachiyomi.util.AniChartApi
import eu.kanade.tachiyomi.util.episode.getNextUnseen
import eu.kanade.tachiyomi.util.removeCovers
import eu.kanade.tachiyomi.util.system.toast
import exh.util.nullIfEmpty
import exh.util.trimOrNull
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.domain.episode.interactor.FilterEpisodesForDownload
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.common.preference.mapAsCheckboxState
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.source.NoResultsException
import tachiyomi.domain.anime.interactor.GetAnimeWithEpisodes
import tachiyomi.domain.anime.interactor.GetDuplicateLibraryAnime
import tachiyomi.domain.anime.interactor.SetAnimeEpisodeFlags
import tachiyomi.domain.anime.interactor.SetCustomAnimeInfo
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.model.AnimeUpdate
import tachiyomi.domain.anime.model.CustomAnimeInfo
import tachiyomi.domain.anime.model.applyFilter
import tachiyomi.domain.anime.repository.AnimeRepository
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.SetAnimeCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.episode.interactor.SetAnimeDefaultEpisodeFlags
import tachiyomi.domain.episode.interactor.UpdateEpisode
import tachiyomi.domain.episode.model.Episode
import tachiyomi.domain.episode.model.EpisodeUpdate
import tachiyomi.domain.episode.service.calculateChapterGap
import tachiyomi.domain.episode.service.getEpisodeSort
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.storage.service.StoragePreferences
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.i18n.MR
import tachiyomi.source.local.LocalSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.ArrayDeque
import java.util.Calendar
import kotlin.math.floor
import tachiyomi.domain.anime.model.UpdateStrategy as DomainUpdateStrategy

@Suppress("LargeClass")
class AnimeScreenModel(
    val context: Context,
    lifecycle: Lifecycle,
    val animeId: Long,
    val isFromSource: Boolean,
    private val getAnimeAndEpisodes: GetAnimeWithEpisodes = Injekt.get(),
    private val getDuplicateLibraryAnime: GetDuplicateLibraryAnime = Injekt.get(),
    private val getCategories: GetCategories = Injekt.get(),
    private val setAnimeCategories: SetAnimeCategories = Injekt.get(),
    private val updateAnime: UpdateAnime = Injekt.get(),
    private val setCustomAnimeInfo: SetCustomAnimeInfo = Injekt.get(),
    private val setSeenStatus: SetSeenStatus = Injekt.get(),
    private val syncEpisodesWithSource: SyncEpisodesWithSource = Injekt.get(),
    private val updateEpisode: UpdateEpisode = Injekt.get(),
    private val filterEpisodesForDownload: FilterEpisodesForDownload = Injekt.get(),
    private val setAnimeDefaultEpisodeFlags: SetAnimeDefaultEpisodeFlags = Injekt.get(),
    private val setAnimeEpisodeFlags: SetAnimeEpisodeFlags = Injekt.get(),
    val setAnimeViewerFlags: SetAnimeViewerFlags = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val downloadManager: DownloadManager = Injekt.get(),
    private val downloadCache: DownloadCache = Injekt.get(),
    private val trackerManager: TrackerManager = Injekt.get(),
    private val animeRepository: AnimeRepository = Injekt.get(),
    private val getTracks: GetTracks = Injekt.get(),
    private val addTracks: AddTracks = Injekt.get(),
    private val trackEpisode: TrackEpisode = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    private val downloadPreferences: DownloadPreferences = Injekt.get(),
    private val trackPreferences: TrackPreferences = Injekt.get(),
    private val storagePreferences: StoragePreferences = Injekt.get(),
    val gesturePreferences: GesturePreferences = Injekt.get(),
    val playerPreferences: PlayerPreferences = Injekt.get(),
) : StateScreenModel<AnimeScreenModel.State>(State.Loading) {

    private val successState: State.Success?
        get() = state.value as? State.Success

    val anime: Anime?
        get() = successState?.anime

    val source: Source?
        get() = successState?.source

    val isUpdateIntervalEnabled: Boolean
        get() = true

    private var isRefreshing: Boolean = false

    val snackbarHostState = SnackbarHostState()

    val episodeSwipeStartAction = libraryPreferences.swipeEpisodeStartAction().get()
    val episodeSwipeEndAction = libraryPreferences.swipeEpisodeEndAction().get()
    val showNextEpisodeAirTime = trackPreferences.showNextEpisodeAiringTime().get()
    val alwaysUseExternalPlayer = playerPreferences.alwaysUseExternalPlayer().get()
    val useExternalDownloader = downloadPreferences.useExternalDownloader().get()

    // AM (FILE_SIZE) -->
    val showFileSize = storagePreferences.showEpisodeFileSize().get()
    // <-- AM (FILE_SIZE)

    var autoOpenTrack: Boolean = false
    var isFromChangeCategory: Boolean = false

    init {
        screenModelScope.launchIO {
            getAnimeAndEpisodes.subscribe(animeId)
                .flowWithLifecycle(lifecycle)
                .collectLatest { (anime, episodes) ->
                    val source = sourceManager.getOrStub(anime.source)
                    updateState { state ->
                        val newState = if (state is State.Success) {
                            state.copy(anime = anime, episodes = episodes.map { it.toEpisodeItem() }, source = source)
                        } else {
                            State.Success(
                                anime = anime,
                                episodes = episodes.map { it.toEpisodeItem() },
                                source = source,
                                trackingCount = trackerManager.loggedInTrackers().size,
                                isFromSource = isFromSource,
                            )
                        }
                        newState
                    }
                }
        }

        screenModelScope.launchIO {
            getAnimeAndEpisodes.subscribe(animeId)
                .flowWithLifecycle(lifecycle)
                .filter { (anime, _) -> !anime.initialized || isFromSource }
                .distinctUntilChanged { old, new -> old.first.initialized == new.first.initialized }
                .collectLatest { 
                    fetchAllFromSource()
                }
        }

        screenModelScope.launchIO {
            downloadCache.changes
                .flowWithLifecycle(lifecycle)
                .collectLatest {
                    val state = successState ?: return@collectLatest
                    updateState { state } // Trigger lazy property re-evaluation
                }
        }

        screenModelScope.launchIO {
            downloadManager.queueState
                .flowWithLifecycle(lifecycle)
                .collectLatest { 
                    val state = successState ?: return@collectLatest
                    updateState { state }
                }
        }

        screenModelScope.launchIO {
            trackerManager.loggedInTrackersFlow()
                .flowWithLifecycle(lifecycle)
                .collectLatest { loggedInTrackers ->
                    updateState { state ->
                        if (state is State.Success) {
                            state.copy(trackingCount = loggedInTrackers.size)
                        } else {
                            state
                        }
                    }
                }
        }
    }

    private fun Episode.toEpisodeItem(): EpisodeItem {
        val activeDownload = downloadManager.getQueuedDownloadOrNull(id)
        val downloaded = downloadManager.isEpisodeDownloaded(name, scanlator, anime?.ogTitle ?: "", anime?.source ?: -1L)
        return EpisodeItem(
            episode = this,
            downloadState = when {
                activeDownload != null -> activeDownload.status
                downloaded -> Download.State.DOWNLOADED
                else -> Download.State.NOT_DOWNLOADED
            },
            downloadProgress = activeDownload?.progress ?: 0,
        )
    }

    fun fetchAllFromSource() {
        val state = successState ?: return
        if (isRefreshing) return
        isRefreshing = true
        updateState { state.copy(isRefreshing = true) }

        screenModelScope.launchIO {
            try {
                val networkAnime = source!!.getAnimeDetails(state.anime.toSAnime())
                updateAnime.await(
                    AnimeUpdate(
                        id = animeId,
                        author = networkAnime.author,
                        artist = networkAnime.artist,
                        description = networkAnime.description,
                        genre = networkAnime.genre?.split(", "),
                        status = networkAnime.status.toLong(),
                        initialized = true,
                    ),
                )

                val networkEpisodes = source!!.getEpisodeList(state.anime.toSAnime())
                syncEpisodesWithSource.await(networkEpisodes, state.anime, source!!)
            } catch (e: Throwable) {
                logcat(LogPriority.ERROR, e)
                withUIContext {
                    val message = with(context) { e.formattedMessage }
                    context.toast(message)
                }
            } finally {
                isRefreshing = false
                updateState { state.copy(isRefreshing = false) }
            }
        }
    }

    fun toggleFavorite(
        onRemoved: () -> Unit = {},
        checkDuplicate: Boolean = true,
    ) {
        val state = successState ?: return
        val anime = state.anime

        screenModelScope.launchIO {
            if (anime.favorite) {
                updateAnime.await(AnimeUpdate(id = anime.id, favorite = false))
                withUIContext { onRemoved() }
            } else {
                if (checkDuplicate) {
                    val duplicate = getDuplicateLibraryAnime.await(anime).filter { it.id != anime.id }.firstOrNull()
                    if (duplicate != null) {
                        updateState { state.copy(dialog = Dialog.DuplicateAnime(anime, duplicate)) }
                        return@launchIO
                    }
                }

                val categories = getCategories.await()
                val defaultCategory = categories.find { it.isSystemCategory }
                if (categories.size > 1 || defaultCategory == null) {
                    val preselected = getCategories.await(anime.id).map { it.id }
                    updateState {
                        state.copy(
                            dialog = Dialog.ChangeCategory(
                                anime,
                                categories.mapAsCheckboxState { it.id in preselected }.toImmutableList(),
                            ),
                        )
                    }
                } else {
                    moveAnimeToCategoriesAndAddToLibrary(anime, listOf(defaultCategory.id))
                }
            }
        }
    }

    fun moveAnimeToCategoriesAndAddToLibrary(anime: Anime, categoryIds: List<Long>) {
        screenModelScope.launchIO {
            updateAnime.await(
                AnimeUpdate(
                    id = anime.id,
                    favorite = true,
                    dateAdded = System.currentTimeMillis(),
                ),
            )
            setAnimeCategories.await(anime.id, categoryIds)
        }
    }

    fun showMigrateDialog(duplicate: Anime) {
        val state = successState ?: return
        updateState { state.copy(dialog = Dialog.Migrate(state.anime, duplicate)) }
    }

    fun dismissDialog() {
        val state = successState ?: return
        updateState { state.copy(dialog = null) }
    }

    fun showCoverDialog() {
        val state = successState ?: return
        updateState { state.copy(dialog = Dialog.FullCover) }
    }

    fun showTrackDialog() {
        val state = successState ?: return
        updateState { state.copy(dialog = Dialog.TrackSheet) }
    }

    fun showEditAnimeInfoDialog() {
        val state = successState ?: return
        updateState { state.copy(dialog = Dialog.EditAnimeInfo(state.anime)) }
    }

    fun showSettingsDialog() {
        val state = successState ?: return
        updateState { state.copy(dialog = null) } // Close any open dialog
        updateState { state.copy(dialog = Dialog.SettingsSheet) }
    }

    fun showDeleteEpisodeDialog(episodes: List<Episode>) {
        val state = successState ?: return
        updateState { state.copy(dialog = Dialog.DeleteEpisodes(episodes)) }
    }

    fun showSetAnimeFetchIntervalDialog() {
        val state = successState ?: return
        updateState { state.copy(dialog = Dialog.SetAnimeFetchInterval(state.anime)) }
    }

    fun showAnimeSkipIntroDialog() {
        val state = successState ?: return
        updateState { state.copy(dialog = Dialog.ChangeAnimeSkipIntro) }
    }

    fun updateAnimeInfo(
        title: String?,
        author: String?,
        artist: String?,
        thumbnailUrl: String?,
        description: String?,
        tags: List<String>?,
        status: Long?,
    ) {
        val state = successState ?: return
        screenModelScope.launchIO {
            setCustomAnimeInfo.set(
                CustomAnimeInfo(
                    id = state.anime.id,
                    title = title?.trimOrNull(),
                    author = author?.trimOrNull(),
                    artist = artist?.trimOrNull(),
                    thumbnailUrl = thumbnailUrl?.trimOrNull(),
                    description = description?.trimOrNull(),
                    genre = tags?.nullIfEmpty(),
                    status = status,
                ),
            )
        }
    }

    fun setFetchInterval(anime: Anime, interval: Int) {
        screenModelScope.launchIO {
            updateAnime.await(
                AnimeUpdate(
                    id = anime.id,
                    fetchInterval = interval,
                ),
            )
        }
    }

    fun openChangeCategoryDialog() {
        val state = successState ?: return
        screenModelScope.launchIO {
            val categories = getCategories.await()
            val preselected = getCategories.await(animeId).map { it.id }
            updateState {
                state.copy(
                    dialog = Dialog.ChangeCategory(
                        state.anime,
                        categories.mapAsCheckboxState { it.id in preselected }.toImmutableList(),
                    ),
                )
            }
        }
    }

    private fun updateState(block: (State) -> State) {
        mutableState.update(block)
    }

    // Episodes Actions
    fun bookmarkEpisodes(episodes: List<Episode>, bookmarked: Boolean) {
        screenModelScope.launchIO {
            episodes.forEach {
                updateEpisode.await(
                    EpisodeUpdate(
                        id = it.id,
                        bookmark = bookmarked,
                    ),
                )
            }
        }
    }

    // AM (FILLERMARK) -->
    fun fillermarkEpisodes(episodes: List<Episode>, fillermarked: Boolean) {
        screenModelScope.launchIO {
            episodes.forEach {
                updateEpisode.await(
                    EpisodeUpdate(
                        id = it.id,
                        fillermark = fillermarked,
                    ),
                )
            }
        }
    }
    // <-- AM (FILLERMARK)

    fun markEpisodesSeen(episodes: List<Episode>, seen: Boolean) {
        screenModelScope.launchIO {
            setSeenStatus.await(seen = seen, episodes = episodes.toTypedArray())
        }
    }

    fun markPreviousEpisodeSeen(episode: Episode) {
        val state = successState ?: return
        screenModelScope.launchIO {
            val episodesToMark = state.episodes
                .filter { it.episode.episodeNumber < episode.episodeNumber }
                .map { it.episode }
            setSeenStatus.await(seen = true, episodes = episodesToMark.toTypedArray())
        }
    }

    fun deleteEpisodes(episodes: List<Episode>) {
        val state = successState ?: return
        screenModelScope.launchIO {
            downloadManager.deleteEpisodes(episodes, state.anime, state.source)
        }
    }

    fun runEpisodeDownloadActions(episodes: List<EpisodeList.Item>, action: EpisodeDownloadAction) {
        val state = successState ?: return
        val domainEpisodes = episodes.map { it.episode }
        when (action) {
            EpisodeDownloadAction.START -> {
                downloadManager.downloadEpisodes(state.anime, domainEpisodes)
                if (domainEpisodes.size == 1 && domainEpisodes.first().id == state.processedEpisodes.firstOrNull()?.episode?.id) {
                    fetchAllFromSource()
                }
            }
            EpisodeDownloadAction.START_NOW -> {
                downloadManager.startDownloadNow(domainEpisodes.first().id)
            }
            EpisodeDownloadAction.CANCEL -> {
                val downloads = domainEpisodes.mapNotNull { downloadManager.getQueuedDownloadOrNull(it.id) }
                downloadManager.cancelQueuedDownloads(downloads)
            }
            EpisodeDownloadAction.DELETE -> {
                deleteEpisodes(domainEpisodes)
            }
            // SY -->
            EpisodeDownloadAction.SHOW_QUALITIES -> {
                updateState { state.copy(dialog = Dialog.ShowQualities(state.anime, domainEpisodes.first(), state.source)) }
            }
            // SY <--
        }
    }

    fun runDownloadAction(action: DownloadAction) {
        val state = successState ?: return
        val episodes = state.episodes.map { it.episode }
        when (action) {
            DownloadAction.NEXT_1_EPISODE -> downloadUnseenEpisodes(episodes, 1)
            DownloadAction.NEXT_5_EPISODES -> downloadUnseenEpisodes(episodes, 5)
            DownloadAction.NEXT_10_EPISODES -> downloadUnseenEpisodes(episodes, 10)
            DownloadAction.NEXT_25_EPISODES -> downloadUnseenEpisodes(episodes, 25)
            DownloadAction.UNSEEN_EPISODES -> downloadUnseenEpisodes(episodes, null)
        }
    }

    private fun downloadUnseenEpisodes(episodes: List<Episode>, amount: Int?) {
        val state = successState ?: return
        screenModelScope.launchIO {
            val episodesToDownload = episodes
                .filter { !it.seen }
                .let { if (amount != null) it.take(amount) else it }
            downloadManager.downloadEpisodes(state.anime, episodesToDownload)
        }
    }

    fun getNextUnseenEpisode(): Episode? {
        val state = successState ?: return null
        return state.episodes
            .map { it.episode }
            .getNextUnseen(state.anime, downloadManager)
    }

    // Episode filter actions
    fun setDownloadedFilter(filter: TriState) {
        val state = successState ?: return
        val flag = when (filter) {
            TriState.ENABLED_IS -> Anime.EPISODE_SHOW_DOWNLOADED
            TriState.ENABLED_NOT -> Anime.EPISODE_SHOW_NOT_DOWNLOADED
            TriState.DISABLED -> Anime.SHOW_ALL
        }
        screenModelScope.launchIO {
            setAnimeEpisodeFlags.awaitSetDownloadedFilter(state.anime, flag)
        }
    }

    fun setUnseenFilter(filter: TriState) {
        val state = successState ?: return
        val flag = when (filter) {
            TriState.ENABLED_IS -> Anime.EPISODE_SHOW_UNSEEN
            TriState.ENABLED_NOT -> Anime.EPISODE_SHOW_SEEN
            TriState.DISABLED -> Anime.SHOW_ALL
        }
        screenModelScope.launchIO {
            setAnimeEpisodeFlags.awaitSetUnreadFilter(state.anime, flag)
        }
    }

    fun setBookmarkedFilter(filter: TriState) {
        val state = successState ?: return
        val flag = when (filter) {
            TriState.ENABLED_IS -> Anime.EPISODE_SHOW_BOOKMARKED
            TriState.ENABLED_NOT -> Anime.EPISODE_SHOW_NOT_BOOKMARKED
            TriState.DISABLED -> Anime.SHOW_ALL
        }
        screenModelScope.launchIO {
            setAnimeEpisodeFlags.awaitSetBookmarkFilter(state.anime, flag)
        }
    }

    // AM (FILLERMARK) -->
    fun setFillermarkedFilter(filter: TriState) {
        val state = successState ?: return
        val flag = when (filter) {
            TriState.ENABLED_IS -> Anime.EPISODE_SHOW_FILLERMARKED
            TriState.ENABLED_NOT -> Anime.EPISODE_SHOW_NOT_FILLERMARKED
            TriState.DISABLED -> Anime.SHOW_ALL
        }
        screenModelScope.launchIO {
            setAnimeEpisodeFlags.awaitSetFillermarkFilter(state.anime, flag)
        }
    }
    // <-- AM (FILLERMARK)

    fun setSorting(sorting: Long) {
        val state = successState ?: return
        screenModelScope.launchIO {
            setAnimeEpisodeFlags.awaitSetSortingModeOrFlipOrder(state.anime, sorting)
        }
    }

    fun setDisplayMode(displayMode: Long) {
        val state = successState ?: return
        screenModelScope.launchIO {
            setAnimeEpisodeFlags.awaitSetDisplayMode(state.anime, displayMode)
        }
    }

    fun setCurrentSettingsAsDefault() {
        val state = successState ?: return
        screenModelScope.launchIO {
            setAnimeEpisodeFlags.awaitSetAllFlags(
                animeId = animeId,
                unseenFilter = state.anime.unseenFilterRaw,
                downloadedFilter = state.anime.downloadedFilterRaw,
                bookmarkedFilter = state.anime.bookmarkedFilterRaw,
                // AM (FILLERMARK) -->
                fillermarkedFilter = state.anime.fillermarkedFilterRaw,
                // <-- AM (FILLERMARK)
                sortingMode = state.anime.sorting,
                sortingDirection = if (state.anime.sortDescending()) Anime.EPISODE_SORT_DESC else Anime.EPISODE_SORT_ASC,
                displayMode = state.anime.displayMode,
            )
        }
    }

    fun episodeSwipe(episodeItem: EpisodeList.Item, action: LibraryPreferences.EpisodeSwipeAction) {
        val episode = episodeItem.episode
        val episodeItemInternal = EpisodeItem(episode, episodeItem.downloadState, episodeItem.downloadProgress, episodeItem.selected)
        when (action) {
            LibraryPreferences.EpisodeSwipeAction.ToggleSeen -> {
                markEpisodesSeen(listOf(episode), !episode.seen)
            }
            LibraryPreferences.EpisodeSwipeAction.ToggleBookmark -> {
                bookmarkEpisodes(listOf(episode), !episode.bookmark)
            }
            // AM (FILLERMARK) -->
            LibraryPreferences.EpisodeSwipeAction.ToggleFillermark -> {
                fillermarkEpisodes(listOf(episode), !episode.fillermark)
            }
            // <-- AM (FILLERMARK)
            LibraryPreferences.EpisodeSwipeAction.Download -> {
                val download = downloadManager.getQueuedDownloadOrNull(episode.id)
                if (download != null) {
                    runEpisodeDownloadActions(listOf(episodeItem), EpisodeDownloadAction.CANCEL)
                } else if (episodeItem.downloadState != Download.State.DOWNLOADED) {
                    runEpisodeDownloadActions(listOf(episodeItem), EpisodeDownloadAction.START)
                }
            }
            LibraryPreferences.EpisodeSwipeAction.Disabled -> {}
        }
    }

    // Selection actions
    fun toggleSelection(episodeItem: EpisodeList.Item, selected: Boolean, multi: Boolean, invert: Boolean) {
        val state = successState ?: return
        val episodes = state.episodes.toMutableList()
        val index = episodes.indexOfFirst { it.episode.id == episodeItem.id }
        if (index != -1) {
            episodes[index] = episodes[index].copy(selected = selected)
            updateState { (it as State.Success).copy(episodes = episodes) }
        }
    }

    fun toggleAllSelection(selected: Boolean) {
        val state = successState ?: return
        val episodes = state.episodes.map {
            it.copy(selected = selected)
        }
        updateState { (it as State.Success).copy(episodes = episodes) }
    }

    fun invertSelection() {
        val state = successState ?: return
        val episodes = state.processedEpisodes.map {
            it.copy(selected = !it.selected)
        }
        updateState { (it as State.Success).copy(episodes = episodes) }
    }

    sealed interface State {
        data object Loading : State
        data class Success(
            val anime: Anime,
            val episodes: List<EpisodeItem>,
            val source: Source,
            val trackingCount: Int,
            val isRefreshing: Boolean = false,
            val dialog: Dialog? = null,
            // SY -->
            val recommendations: Map<String, List<Anime>> = emptyMap(),
            val isFromSource: Boolean = false,
            // SY <--
        ) : State {
            val hasLoggedInTrackers: Boolean
                get() = trackingCount > 0

            val processedEpisodes by lazy {
                episodes // Filters should be applied here if needed
            }

            val episodeListItems: List<EpisodeList> by lazy {
                processedEpisodes.insertSeparators { before, after ->
                    val (lowerEpisode, higherEpisode) = if (anime.sortDescending()) {
                        after to before
                    } else {
                        before to after
                    }
                    if (higherEpisode == null) return@insertSeparators null

                    if (lowerEpisode == null) {
                        floor(higherEpisode.episode.episodeNumber)
                            .toInt()
                            .minus(1)
                            .coerceAtLeast(0)
                    } else {
                        calculateChapterGap(higherChapterNumber = higherEpisode.episode.episodeNumber, lowerChapterNumber = lowerEpisode.episode.episodeNumber)
                    }
                        .takeIf { it > 0 }
                        ?.let { missingCount ->
                            EpisodeList.MissingCount(
                                id = "${lowerEpisode?.episode?.id}-${higherEpisode.episode.id}",
                                count = missingCount,
                            )
                        }
                }.map { 
                    when (it) {
                        is EpisodeItem -> EpisodeList.Item(it.episode, it.downloadState, it.downloadProgress, null, it.selected)
                        is EpisodeList.MissingCount -> it
                        else -> throw IllegalStateException("Unexpected item type in episode list")
                    }
                }
            }

            val isRefreshingData: Boolean get() = isRefreshing
            val filterActive: Boolean get() = anime.episodesFiltered()
            val airingTime: Long get() = anime.nextEpisodeAiringAt
            val airingEpisodeNumber: Int get() = anime.nextEpisodeToAir
        }
    }

    sealed interface Dialog {
        data object SettingsSheet : Dialog
        data object TrackSheet : Dialog
        data object FullCover : Dialog
        data object ChangeAnimeSkipIntro : Dialog
        data class ChangeCategory(
            val anime: Anime,
            val initialSelection: ImmutableList<CheckboxState<Category>>,
        ) : Dialog
        data class DeleteEpisodes(val episodes: List<Episode>) : Dialog
        data class DuplicateAnime(val anime: Anime, val duplicate: Anime) : Dialog
        data class Migrate(val oldAnime: Anime, val newAnime: Anime) : Dialog
        data class SetAnimeFetchInterval(val anime: Anime) : Dialog
        data class EditAnimeInfo(val anime: Anime) : Dialog
        data class ShowQualities(val anime: Anime, val episode: Episode, val source: Source) : Dialog
    }
}

@Immutable
sealed interface EpisodeList {
    @Immutable
    data class MissingCount(
        val id: String,
        val count: Int,
    ) : EpisodeList

    @Immutable
    data class Item(
        val episode: Episode,
        val downloadState: Download.State,
        val downloadProgress: Int,
        var fileSize: Long? = null,
        val selected: Boolean = false,
    ) : EpisodeList {
        val id = episode.id
    }
}

data class EpisodeItem(
    val episode: Episode,
    val downloadState: Download.State,
    val downloadProgress: Int,
    val selected: Boolean = false,
)