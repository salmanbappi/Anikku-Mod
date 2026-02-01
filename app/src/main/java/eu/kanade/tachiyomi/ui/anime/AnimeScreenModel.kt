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
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.data.ai.AiManager
import eu.kanade.tachiyomi.data.download.DownloadCache
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.torrentServer.service.TorrentServerService
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.network.HttpException
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.isSourceForTorrents
import eu.kanade.tachiyomi.torrentServer.TorrentServerUtils
import eu.kanade.tachiyomi.ui.anime.track.TrackItem
import eu.kanade.tachiyomi.ui.player.settings.GesturePreferences
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
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
import tachiyomi.source.local.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Calendar
import kotlin.math.floor

class AnimeScreenModel(
    private val context: Context,
    private val lifecycle: Lifecycle,
    private val animeId: Long,
    private val isFromSource: Boolean,
    private val downloadPreferences: DownloadPreferences = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    private val trackPreferences: TrackPreferences = Injekt.get(),
    internal val playerPreferences: PlayerPreferences = Injekt.get(),
    internal val gesturePreferences: GesturePreferences = Injekt.get(),
    private val trackerManager: TrackerManager = Injekt.get(),
    private val trackEpisode: TrackEpisode = Injekt.get(),
    private val downloadManager: DownloadManager = Injekt.get(),
    private val downloadCache: DownloadCache = Injekt.get(),
    private val downloadProvider: eu.kanade.tachiyomi.data.download.DownloadProvider = Injekt.get(),
    private val getAnimeAndEpisodes: GetAnimeWithEpisodes = Injekt.get(),
    // SY -->
    private val sourceManager: SourceManager = Injekt.get(),
    private val setCustomAnimeInfo: SetCustomAnimeInfo = Injekt.get(),
    // SY <--
    private val getDuplicateLibraryAnime: GetDuplicateLibraryAnime = Injekt.get(),
    private val setAnimeEpisodeFlags: SetAnimeEpisodeFlags = Injekt.get(),
    private val setAnimeDefaultEpisodeFlags: SetAnimeDefaultEpisodeFlags = Injekt.get(),
    private val setSeenStatus: SetSeenStatus = Injekt.get(),
    private val updateEpisode: UpdateEpisode = Injekt.get(),
    private val updateAnime: UpdateAnime = Injekt.get(),
    private val addTracks: AddTracks = Injekt.get(),
    private val aiManager: AiManager = Injekt.get(),
    private val storagePreferences: StoragePreferences = Injekt.get(),
    internal val setAnimeViewerFlags: SetAnimeViewerFlags = Injekt.get(),
    private val animeRepository: AnimeRepository = Injekt.get(),
    private val getCategoriesInteractor: GetCategories = Injekt.get(),
    private val setAnimeCategoriesInteractor: SetAnimeCategories = Injekt.get(),
    private val syncEpisodesWithSourceInteractor: SyncEpisodesWithSource = Injekt.get(),
    private val getTracksInteractor: GetTracks = Injekt.get(),
    private val filterEpisodesForDownloadInteractor: FilterEpisodesForDownload = Injekt.get(),
) : StateScreenModel<AnimeScreenModel.State>(State.Loading) {

    val snackbarHostState = SnackbarHostState()

    private val successState: State.Success?
        get() = state.value as? State.Success

    val anime: Anime?
        get() = successState?.anime

    val source: Source?
        get() = successState?.source

    private val isFavorited: Boolean
        get() = anime?.favorite ?: false

    private val processedEpisodes: List<EpisodeList.Item>?
        get() = successState?.processedEpisodes

    val episodeSwipeStartAction = libraryPreferences.swipeEpisodeEndAction().get()
    val episodeSwipeEndAction = libraryPreferences.swipeEpisodeStartAction().get()
    var autoTrackState = trackPreferences.autoUpdateTrackOnMarkRead().get()

    val showNextEpisodeAirTime = trackPreferences.showNextEpisodeAiringTime().get()
    val alwaysUseExternalPlayer = playerPreferences.alwaysUseExternalPlayer().get()
    val useExternalDownloader = downloadPreferences.useExternalDownloader().get()

    val isUpdateIntervalEnabled =
        LibraryPreferences.ANIME_OUTSIDE_RELEASE_PERIOD in libraryPreferences.autoUpdateAnimeRestrictions().get()

    private val selectedPositions: Array<Int> = arrayOf(-1, -1) // first and last selected index in list
    private val selectedEpisodeIds: HashSet<Long> = HashSet()

    internal var isFromChangeCategory: Boolean = false

    internal val autoOpenTrack: Boolean
        get() = successState?.trackingAvailable == true && trackPreferences.trackOnAddingToLibrary().get()

    // AM (FILE_SIZE) -->
    val showFileSize = storagePreferences.showEpisodeFileSize().get()
    // <-- AM (FILE_SIZE)

    /**
     * Helper function to update the UI state only if it's currently in success state
     */
    private inline fun updateSuccessState(func: (State.Success) -> State.Success) {
        mutableState.update {
            when (it) {
                State.Loading -> it
                is State.Success -> func(it)
            }
        }
    }

    init {
        screenModelScope.launchIO {
            combine(
                getAnimeAndEpisodes.subscribe(animeId).distinctUntilChanged(),
                downloadCache.changes,
                downloadManager.queueState,
            ) { animeAndEpisodes, _, _ -> animeAndEpisodes }
                .flowWithLifecycle(lifecycle)
                .collectLatest { (anime, episodes) ->
                    updateSuccessState {
                        it.copy(
                            anime = anime,
                            episodes = episodes.toEpisodeListItems(anime),
                        )
                    }
                }
        }

        observeDownloads()

        screenModelScope.launchIO {
            val anime = getAnimeAndEpisodes.awaitManga(animeId)
            val episodes = getAnimeAndEpisodes.awaitChapters(animeId)
                .toEpisodeListItems(anime)

            if (!anime.favorite) {
                setAnimeDefaultEpisodeFlags.await(anime)
            }

            val needRefreshInfo = !anime.initialized
            val needRefreshEpisode = episodes.isEmpty()

            val animeSource = sourceManager.getOrStub(anime.source)
            // --> (Torrent)
            if (animeSource.isSourceForTorrents()) {
                TorrentServerService.start()
                TorrentServerService.wait(10)
                TorrentServerUtils.setTrackersList()
            }
            // <-- (Torrent)

            // Show what we have earlier
            mutableState.update {
                State.Success(
                    anime = anime,
                    source = animeSource,
                    isFromSource = isFromSource,
                    episodes = episodes,
                    isRefreshingData = needRefreshInfo || needRefreshEpisode,
                    dialog = null,
                )
            }
            // Start observe tracking since it only needs animeId
            observeTrackers()

            // Fetch info-episodes when needed
            if (screenModelScope.isActive) {
                val fetchFromSourceTasks = listOf(
                    async { if (needRefreshInfo) fetchAnimeFromSource() },
                    async { if (needRefreshEpisode) fetchEpisodesFromSource() },
                )
                fetchFromSourceTasks.awaitAll()
            }

            // Initial loading finished
            updateSuccessState { it.copy(isRefreshingData = false) }
        }

        screenModelScope.launchIO {
            val tracks = getTracksInteractor.await(animeId)
            updateSuccessState { it.copy(hasLoggedInTrackers = tracks.isNotEmpty()) }
        }
    }

    fun fetchAllFromSource(manualFetch: Boolean = true) {
        screenModelScope.launch {
            updateSuccessState { it.copy(isRefreshingData = true) }
            val fetchFromSourceTasks = listOf(
                async { fetchAnimeFromSource(manualFetch) },
                async { fetchEpisodesFromSource(manualFetch) },
            )
            fetchFromSourceTasks.awaitAll()
            updateSuccessState { it.copy(isRefreshingData = false) }
            successState?.let { updateAiringTime(it.anime, it.trackItems, manualFetch) }
        }
    }

    // Anime info - start

    /**
     * Fetch anime information from source.
     */
    private suspend fun fetchAnimeFromSource(manualFetch: Boolean = false) {
        val state = successState ?: return
        try {
            withIOContext {
                val networkAnime = state.source.getAnimeDetails(state.anime.toSAnime())
                updateAnime.awaitUpdateFromSource(state.anime, networkAnime, manualFetch)
            }
        } catch (e: Throwable) {
            // Ignore early hints "errors" that aren't handled by OkHttp
            if (e is HttpException && e.code == 103) return

            logcat(LogPriority.ERROR, e)
            screenModelScope.launch {
                snackbarHostState.showSnackbar(message = with(context) { e.formattedMessage })
            }
        }
    }

    // SY -->
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
        var anime = state.anime
        if (state.anime.isLocal()) {
            val newTitle = if (title.isNullOrBlank()) anime.url else title.trim()
            val newAuthor = author?.trimOrNull()
            val newArtist = artist?.trimOrNull()
            val newDesc = description?.trimOrNull()
            anime = anime.copy(
                ogTitle = newTitle,
                ogAuthor = author?.trimOrNull(),
                ogArtist = artist?.trimOrNull(),
                ogDescription = description?.trimOrNull(),
                ogGenre = tags?.nullIfEmpty(),
                ogStatus = status ?: 0,
                lastUpdate = anime.lastUpdate + 1,
            )
            (sourceManager.get(LocalSource.ID) as LocalSource).updateAnimeInfo(
                anime.toSAnime(),
            )
            screenModelScope.launchNonCancellable {
                updateAnime.await(
                    AnimeUpdate(
                        anime.id,
                        title = newTitle,
                        author = newAuthor,
                        artist = newArtist,
                        description = newDesc,
                        genre = tags?.nullIfEmpty(),
                        status = status,
                        lastUpdate = anime.lastUpdate + 1,
                    ),
                )
            }
        } else {
            val customAnimeInfo = CustomAnimeInfo(
                anime.id,
                title?.trimOrNull(),
                author?.trimOrNull(),
                artist?.trimOrNull(),
                description?.trimOrNull(),
                tags?.nullIfEmpty(),
                status,
            )
            screenModelScope.launchNonCancellable {
                setCustomAnimeInfo.await(customAnimeInfo)
            }
        }
    }

    fun resetInfo() {
        val state = successState ?: return
        screenModelScope.launchNonCancellable {
            setCustomAnimeInfo.await(
                CustomAnimeInfo(
                    state.anime.id,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                ),
            )
        }
    }
    // SY <--

    fun toggleFavorite() {
        val anime = successState?.anime ?: return
        toggleFavorite(
            onRemoved = { _ ->
                screenModelScope.launch {
                    if (hasDownloads()) {
                        updateSuccessState { it.copy(dialog = Dialog.RemoveAnime(anime)) }
                    } else {
                        changeAnimeFavorite(anime)
                    }
                }
            },
        )
    }

    /**
     * Update favorite status of anime, (removes point)
     */
    fun toggleFavorite(
        onRemoved: (Anime) -> Unit,
        checkDuplicate: Boolean = true,
    ) {
        val state = successState ?: return
        screenModelScope.launchIO {
            val anime = state.anime
            if (isFavorited) {
                onRemoved(anime)
            } else {
                if (checkDuplicate) {
                    val duplicate = getDuplicateLibraryAnime.await(anime)
                    if (duplicate != null) {
                        updateSuccessState { it.copy(dialog = Dialog.DuplicateAnime(anime, duplicate)) }
                        return@launchIO
                    }
                }

                val categories = getCategories()
                val defaultCategoryId = libraryPreferences.defaultCategory().get()
                val defaultCategory = categories.find { it.id == defaultCategoryId.toLong() }

                when {
                    // Default category set
                    defaultCategory != null -> {
                        moveAnimeToCategories(anime, defaultCategory)

                        changeAnimeFavorite(anime)
                    }

                    // Automatic 'Default' or no categories
                    defaultCategoryId == 0 || categories.isEmpty() -> {
                        moveAnimeToCategories(anime)

                        changeAnimeFavorite(anime)
                    }

                    // Choose a category
                    else -> {
                        val preselectedIds = getAnimeCategoryIds(anime)
                        val items = categories.mapAsCheckboxState { it.id in preselectedIds }
                        updateSuccessState { it.copy(dialog = Dialog.ChangeCategory(anime, items.toImmutableList())) }
                    }
                }
            }
        }
    }

    /**
     * Adds or removes an anime from the library.
     *
     * @param anime the anime to update.
     */
    fun changeAnimeFavorite(anime: Anime) {
        screenModelScope.launch {
            var new = anime.copy(
                favorite = !anime.favorite,
                dateAdded = when (anime.favorite) {
                    true -> 0
                    false -> Instant.now().toEpochMilli()
                },
            )

            if (!new.favorite) {
                new = new.removeCovers(coverCache)
            } else {
                setAnimeDefaultEpisodeFlags.await(anime)
                addTracks.bindEnhancedTrackers(anime, source)
            }

            updateAnime.await(new.toAnimeUpdate())
        }
    }

    fun addFavorite(anime: Anime) {
        screenModelScope.launch {
            val categories = getCategories()
            val defaultCategoryId = libraryPreferences.defaultCategory().get()
            val defaultCategory = categories.find { it.id == defaultCategoryId.toLong() }

            when {
                // Default category set
                defaultCategory != null -> {
                    moveAnimeToCategories(anime, defaultCategory)

                    changeAnimeFavorite(anime)
                }
                // Automatic 'Default' or no categories
                defaultCategoryId == 0 || categories.isEmpty() -> {
                    moveAnimeToCategories(anime)

                    changeAnimeFavorite(anime)
                }

                // Choose a category
                else -> {
                    val preselectedIds = getAnimeCategoryIds(anime)
                    updateSuccessState {
                        it.copy(
                            dialog = Dialog.ChangeCategory(
                                anime,
                                categories.mapAsCheckboxState { it.id in preselectedIds }.toImmutableList(),
                            ),
                        )
                    }
                }
            }
        }
    }

    /**
     * Get user categories.
     *
     * @return List of categories, not including the default category
     */
    suspend fun getCategories(): List<Category> {
        return getCategoriesInteractor.await().filterNot { it.isSystemCategory }
    }

    /**
     * Gets the category id's the anime is in, if the anime is not in a category, returns the default id.
     *
     * @param anime the anime to get categories from.
     * @return Array of category ids the anime is in, if none returns default id
     */
    private suspend fun getAnimeCategoryIds(anime: Anime): List<Long> {
        return getCategoriesInteractor.await(anime.id)
            .map { it.id }
    }

    fun moveAnimeToCategoriesAndAddToLibrary(anime: Anime, categories: List<Long>) {
        moveAnimeToCategory(categories)
        if (anime.favorite) return

        screenModelScope.launchIO {
            updateAnime.awaitUpdateFavorite(anime.id, true)
        }
    }

    /**
     * Move the given anime to categories.
     *
     * @param categories the selected categories.
     */
    private fun moveAnimeToCategories(categories: List<Category>) {
        val categoryIds = categories.map { it.id }
        moveAnimeToCategory(categoryIds)
    }

    private fun moveAnimeToCategory(categoryIds: List<Long>) {
        screenModelScope.launchIO {
            setAnimeCategoriesInteractor.await(animeId, categoryIds)
        }
    }

    // Anime info - end

    // Episodes list - start

    private fun observeDownloads() {
        screenModelScope.launchIO {
            downloadManager.queueState
                .flowWithLifecycle(lifecycle)
                .collectLatest {
                    updateSuccessState { it.copy(episodes = it.episodes) }
                }
        }

        screenModelScope.launchIO {
            downloadCache.changes
                .flowWithLifecycle(lifecycle)
                .collectLatest {
                    updateSuccessState { it.copy(episodes = it.episodes) }
                }
        }
    }

    private fun List<Episode>.toEpisodeListItems(anime: Anime): List<EpisodeList.Item> {
        val selectedEpisodeIds = selectedEpisodeIds
        return map { episode ->
            val activeDownload = downloadManager.getQueuedDownloadOrNull(episode.id)
            val downloadState = when {
                activeDownload != null -> activeDownload.status
                downloadManager.isEpisodeDownloaded(episode.name, episode.url, anime.ogTitle, anime.source) -> Download.State.DOWNLOADED
                else -> Download.State.NOT_DOWNLOADED
            }

            EpisodeList.Item(
                episode = episode,
                downloadState = downloadState,
                downloadProgress = activeDownload?.progress ?: 0,
                selected = episode.id in selectedEpisodeIds,
            )
        }
    }

    /**
     * Requests an updated list of episodes from the source.
     */
    private suspend fun fetchEpisodesFromSource(manualFetch: Boolean = false) {
        val state = successState ?: return
        try {
            withIOContext {
                val episodes = state.source.getEpisodeList(state.anime.toSAnime())

                val newEpisodes = syncEpisodesWithSourceInteractor.await(
                    episodes,
                    state.anime,
                    state.source,
                    manualFetch,
                )

                if (manualFetch) {
                    downloadNewEpisodes(newEpisodes)
                }
            }
        } catch (e: Throwable) {
            val message = if (e is NoResultsException) {
                context.stringResource(MR.strings.no_episodes_error)
            } else {
                logcat(LogPriority.ERROR, e)
                with(context) { e.formattedMessage }
            }

            screenModelScope.launch {
                snackbarHostState.showSnackbar(message = message)
            }
            val newAnime = animeRepository.getAnimeById(animeId)
            updateSuccessState { it.copy(anime = newAnime, isRefreshingData = false) }
        }
    }

    /**
     * @throws IllegalStateException if the swipe action is [LibraryPreferences.EpisodeSwipeAction.Disabled]
     */
    fun episodeSwipe(episodeItem: EpisodeList.Item, swipeAction: LibraryPreferences.EpisodeSwipeAction) {
        screenModelScope.launch {
            executeEpisodeSwipeAction(episodeItem, swipeAction)
        }
    }

    /**
     * @throws IllegalStateException if the swipe action is [LibraryPreferences.EpisodeSwipeAction.Disabled]
     */
    private fun executeEpisodeSwipeAction(
        episodeItem: EpisodeList.Item,
        swipeAction: LibraryPreferences.EpisodeSwipeAction,
    ) {
        val episode = episodeItem.episode
        when (swipeAction) {
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
                val downloadAction: EpisodeDownloadAction = when (episodeItem.downloadState) {
                    Download.State.ERROR,
                    Download.State.NOT_DOWNLOADED,
                    -> EpisodeDownloadAction.START_NOW
                    Download.State.QUEUE,
                    Download.State.DOWNLOADING,
                    -> EpisodeDownloadAction.CANCEL
                    Download.State.DOWNLOADED -> EpisodeDownloadAction.DELETE
                }
                runEpisodeDownloadActions(
                    items = listOf(episodeItem),
                    action = downloadAction,
                )
            }
            LibraryPreferences.EpisodeSwipeAction.Disabled -> throw IllegalStateException()
        }
    }

    /**
     * Returns the next unseen episode or null if everything is seen.
     */
    fun getNextUnseenEpisode(): Episode? {
        val successState = successState ?: return null
        return successState.processedEpisodes.getNextUnseen(successState.anime)
    }

    fun toggleSelection(
        item: EpisodeList.Item,
        selected: Boolean,
        userSelected: Boolean = false,
        fromLongClick: Boolean = false,
    ) {
        updateSuccessState { state ->
            val newEpisodes = state.episodes.map {
                if (it.id == item.id) it.copy(selected = selected) else it
            }
            if (selected) {
                selectedEpisodeIds.add(item.id!!)
            } else {
                selectedEpisodeIds.remove(item.id)
            }

            if (userSelected && lastSelectionIndex != -1 && fromLongClick) {
                // Handled in ToggleAllSelection for now
            }
            lastSelectionIndex = state.episodes.indexOf(item)

            state.copy(episodes = newEpisodes)
        }
    }

    private var lastSelectionIndex: Int = -1

    fun toggleAllSelection(selected: Boolean) {
        updateSuccessState { state ->
            val newEpisodes = state.episodes.map {
                it.copy(selected = selected)
            }
            if (selected) {
                selectedEpisodeIds.addAll(state.episodes.fastMap { it.id!! })
            } else {
                selectedEpisodeIds.clear()
            }
            state.copy(episodes = newEpisodes)
        }
    }

    fun invertSelection() {
        updateSuccessState { state ->
            val newEpisodes = state.episodes.map {
                it.copy(selected = !it.selected)
            }
            selectedEpisodeIds.clear()
            newEpisodes.forEach {
                if (it.selected) selectedEpisodeIds.add(it.id!!)
            }
            state.copy(episodes = newEpisodes)
        }
    }

    fun setDownloadedFilter(state: TriState) {
        screenModelScope.launchIO {
            updateAnime.awaitUpdateEpisodeFlags(
                SetAnimeEpisodeFlags.EpisodeFlags(
                    animeId = animeId,
                    downloadedFilter = state,
                ),
            )
        }
    }

    fun setUnseenFilter(state: TriState) {
        screenModelScope.launchIO {
            updateAnime.awaitUpdateEpisodeFlags(
                SetAnimeEpisodeFlags.EpisodeFlags(
                    animeId = animeId,
                    unseenFilter = state,
                ),
            )
        }
    }

    fun setBookmarkedFilter(state: TriState) {
        screenModelScope.launchIO {
            updateAnime.awaitUpdateEpisodeFlags(
                SetAnimeEpisodeFlags.EpisodeFlags(
                    animeId = animeId,
                    bookmarkedFilter = state,
                ),
            )
        }
    }

    fun setFillermarkedFilter(state: TriState) {
        screenModelScope.launchIO {
            updateAnime.awaitUpdateEpisodeFlags(
                SetAnimeEpisodeFlags.EpisodeFlags(
                    animeId = animeId,
                    fillermarkedFilter = state,
                ),
            )
        }
    }

    fun setSorting(mode: Long) {
        screenModelScope.launchIO {
            updateAnime.awaitUpdateEpisodeFlags(
                SetAnimeEpisodeFlags.EpisodeFlags(
                    animeId = animeId,
                    sortingMode = mode,
                ),
            )
        }
    }

    fun setDisplayMode(mode: Long) {
        screenModelScope.launchIO {
            updateAnime.awaitUpdateEpisodeFlags(
                SetAnimeEpisodeFlags.EpisodeFlags(
                    animeId = animeId,
                    displayMode = mode,
                ),
            )
        }
    }

    fun setCurrentSettingsAsDefault(anime: Anime) {
        screenModelScope.launchIO {
            libraryPreferences.setAnimeDefaultEpisodeFlags(anime)
            withUIContext {
                context.toast(MR.strings.episode_settings_updated)
            }
        }
    }

    private fun downloadNewEpisodes(episodes: List<Episode>) {
        if (episodes.isEmpty() || !isFavorited || !downloadPreferences.downloadNewEpisodes().get()) return

        val categories = runBlocking { getAnimeCategoryIds(anime!!) }
        val excludedCategories = downloadPreferences.downloadNewEpisodeCategoriesExclude().get()
            .map { it.toLong() }
        val includedCategories = downloadPreferences.downloadNewEpisodeCategories().get()
            .map { it.toLong() }

        if (categories.any { it in excludedCategories }) return
        if (includedCategories.isNotEmpty() && !categories.any { it in includedCategories }) return

        downloadManager.downloadEpisodes(anime!!, episodes)
    }

    // Episodes list - end

    // Track sheet - start

    private fun observeTrackers() {
        val anime = successState?.anime ?: return
        screenModelScope.launchIO {
            getTracksInteractor.subscribe(anime.id)
                .catch { logcat(LogPriority.ERROR, it) }
                .distinctUntilChanged()
                .collectLatest { trackItems ->
                    updateSuccessState { it.copy(trackItems = trackItems) }
                }
        }
    }

    fun bookmarkEpisodes(episodes: List<Episode>, bookmark: Boolean) {
        screenModelScope.launchIO {
            val update = episodes.map { EpisodeUpdate(id = it.id, bookmark = bookmark) }
            updateEpisode.awaitAll(update)
        }
    }

    fun fillermarkEpisodes(episodes: List<Episode>, fillermark: Boolean) {
        screenModelScope.launchIO {
            val update = episodes.map { EpisodeUpdate(id = it.id, fillermark = fillermark) }
            updateEpisode.awaitAll(update)
        }
    }

    fun markEpisodesSeen(episodes: List<Episode>, seen: Boolean) {
        screenModelScope.launchIO {
            val update = episodes.map {
                EpisodeUpdate(
                    id = it.id,
                    seen = seen,
                    lastSecondSeen = if (seen) it.totalSeconds else 0L,
                )
            }
            updateEpisode.awaitAll(update)

            if (seen && autoTrackState) {
                val lastEpisodeSeen = episodes.maxByOrNull { it.episodeNumber }?.episodeNumber?.toInt() ?: 0
                trackEpisode.await(context, animeId, lastEpisodeSeen.toLong())
            }
        }
    }

    fun markPreviousEpisodeSeen(episode: Episode) {
        screenModelScope.launchIO {
            val episodes = successState?.processedEpisodes?.fastMap { it.episode } ?: return@launchIO
            val update = episodes.filter { it.episodeNumber < episode.episodeNumber }
                .map {
                    EpisodeUpdate(
                        id = it.id,
                        seen = true,
                        lastSecondSeen = it.totalSeconds,
                    )
                }
            updateEpisode.awaitAll(update)
        }
    }

    fun runEpisodeDownloadActions(
        items: List<EpisodeList.Item>,
        action: EpisodeDownloadAction,
    ) {
        when (action) {
            EpisodeDownloadAction.START -> {
                val episodes = items.fastMap { it.episode }
                downloadManager.downloadEpisodes(anime!!, episodes)
            }
            EpisodeDownloadAction.START_NOW -> {
                val episodes = items.fastMap { it.episode }
                downloadManager.startDownloadNow(episodes.first())
            }
            EpisodeDownloadAction.CANCEL -> {
                val episodes = items.fastMap { it.episode }
                downloadManager.cancelQueuedDownloads(episodes)
            }
            EpisodeDownloadAction.DELETE -> {
                val episodes = items.fastMap { it.episode }
                downloadManager.deleteEpisodes(anime!!, episodes, source!!)
            }
        }
    }

    fun runDownloadAction(action: DownloadAction) {
        val episodes = successState?.processedEpisodes ?: return
        when (action) {
            DownloadAction.NEXT_1 -> downloadNextEpisodes(1)
            DownloadAction.NEXT_5 -> downloadNextEpisodes(5)
            DownloadAction.NEXT_10 -> downloadNextEpisodes(10)
            DownloadAction.NEXT_25 -> downloadNextEpisodes(25)
            DownloadAction.UNSEEN -> {
                val episodesToDownload = episodes.filter { !it.episode.seen }
                downloadManager.downloadEpisodes(anime!!, episodesToDownload.fastMap { it.episode })
            }
            DownloadAction.BOOKMARKED -> {
                val episodesToDownload = episodes.filter { it.episode.bookmark }
                downloadManager.downloadEpisodes(anime!!, episodesToDownload.fastMap { it.episode })
            }
            DownloadAction.ALL -> {
                downloadManager.downloadEpisodes(anime!!, episodes.fastMap { it.episode })
            }
            DownloadAction.CANCEL -> {
                val episodesToCancel = episodes.fastMap { it.episode }
                downloadManager.cancelQueuedDownloads(episodesToCancel)
            }
        }
    }

    private fun downloadNextEpisodes(count: Int) {
        screenModelScope.launchIO {
            val anime = successState?.anime ?: return@launchIO
            val episodes = successState?.processedEpisodes?.fastMap { it.episode } ?: return@launchIO
            val episodesToDownload = filterEpisodesForDownloadInteractor.await(anime, episodes)
                .take(count)
            downloadManager.downloadEpisodes(anime, episodesToDownload)
        }
    }

    private fun observeTrackItems() {
        val anime = successState?.anime ?: return
        screenModelScope.launchIO {
            getTracksInteractor.subscribe(anime.id)
                .catch { logcat(LogPriority.ERROR, it) }
                .distinctUntilChanged()
                .collectLatest { trackItems ->
                    updateSuccessState { it.copy(trackItems = trackItems) }
                }
        }
    }

    private suspend fun updateAiringTime(
        anime: Anime,
        trackItems: List<TrackItem>,
        manualFetch: Boolean,
    ) {
        val airingEpisodeData = AniChartApi().loadAiringTime(anime, trackItems, manualFetch)
        setAnimeViewerFlags.awaitSetNextEpisodeAiring(anime.id, airingEpisodeData)
        updateSuccessState { it.copy(nextAiringEpisode = airingEpisodeData) }
    }

    // Track sheet - end

    fun fetchAIEpisodeSummary() {
        val state = successState ?: return
        val lastWatchedEpisode = state.episodes.sortedByDescending { it.episode.episodeNumber }
            .find { it.episode.seen } ?: return

        screenModelScope.launchIO {
            val summary = aiManager.getEpisodeSummary(state.anime.title, lastWatchedEpisode.episode.episodeNumber)
            updateSuccessState { it.copy(aiEpisodeSummary = summary) }
        }
    }

    sealed interface Dialog {
        data class ChangeCategory(
            val anime: Anime,
            val initialSelection: ImmutableList<CheckboxState<Category>>,
        ) : Dialog
        data class DeleteEpisodes(val episodes: List<Episode>) : Dialog
        data class DuplicateAnime(val anime: Anime, val duplicate: Anime) : Dialog
        data class Migrate(val newAnime: Anime, val oldAnime: Anime) : Dialog
        data class SetAnimeFetchInterval(val anime: Anime) : Dialog
        data class ShowQualities(val episode: Episode, val anime: Anime, val source: Source) : Dialog

        // SY -->
        data class EditAnimeInfo(val anime: Anime) : Dialog
        // SY <--

        data object ChangeAnimeSkipIntro : Dialog
        data object SettingsSheet : Dialog
        data object TrackSheet : Dialog
        data object FullCover : Dialog
        data class RemoveAnime(val anime: Anime) : Dialog
    }

    fun dismissDialog() {
        updateSuccessState { it.copy(dialog = null) }
    }

    fun showDeleteEpisodeDialog(episodes: List<Episode>) {
        updateSuccessState { it.copy(dialog = Dialog.DeleteEpisodes(episodes)) }
    }

    fun showSettingsDialog() {
        updateSuccessState { it.copy(dialog = Dialog.SettingsSheet) }
    }

    fun showTrackDialog() {
        updateSuccessState { it.copy(dialog = Dialog.TrackSheet) }
    }

    fun showCoverDialog() {
        updateSuccessState { it.copy(dialog = Dialog.FullCover) }
    }

    // SY -->
    fun showEditAnimeInfoDialog() {
        mutableState.update { state ->
            when (state) {
                State.Loading -> state
                is State.Success -> {
                    state.copy(dialog = Dialog.EditAnimeInfo(state.anime))
                }
            }
        }
    }
    // SY <--

    fun showMigrateDialog(duplicate: Anime) {
        val anime = successState?.anime ?: return
        updateSuccessState { it.copy(dialog = Dialog.Migrate(newAnime = anime, oldAnime = duplicate)) }
    }

    fun showAnimeSkipIntroDialog() {
        updateSuccessState { it.copy(dialog = Dialog.ChangeAnimeSkipIntro) }
    }

    private fun showQualitiesDialog(episode: Episode) {
        updateSuccessState { it.copy(dialog = Dialog.ShowQualities(episode, it.anime, it.source)) }
    }

    fun showChangeCategoryDialog() {
        val anime = successState?.anime ?: return
        screenModelScope.launchIO {
            val categories = getCategories()
            val preselectedIds = getAnimeCategoryIds(anime)
            val items = categories.mapAsCheckboxState { it.id in preselectedIds }
            updateSuccessState { it.copy(dialog = Dialog.ChangeCategory(anime, items.toImmutableList())) }
        }
    }

    sealed interface State {
        @Immutable
        data object Loading : State

        @Immutable
        data class Success(
            val anime: Anime,
            val source: Source,
            val isFromSource: Boolean,
            val episodes: List<EpisodeList.Item>,
            val trackingCount: Int = 0,
            val hasLoggedInTrackers: Boolean = false,
            val isRefreshingData: Boolean = false,
            val dialog: Dialog? = null,
            val hasPromptedToAddBefore: Boolean = false,
            val trackItems: List<TrackItem> = emptyList(),
            val nextAiringEpisode: Pair<Int, Long> = Pair(
                anime.nextEpisodeToAir,
                anime.nextEpisodeAiringAt,
            ),
            val aiEpisodeSummary: String? = null,
        ) : State {

            val processedEpisodes by lazy {
                episodes.applyFilters(anime).toList()
            }

            val episodeListItems by lazy {
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
                        calculateChapterGap(higherEpisode.episode, lowerEpisode.episode)
                    }
                        .takeIf { it > 0 }
                        ?.let { missingCount ->
                            EpisodeList.MissingCount(
                                id = "${lowerEpisode?.id}-${higherEpisode.id}",
                                count = missingCount,
                            )
                        }
                }
            }

            val trackingAvailable: Boolean
                get() = trackItems.isNotEmpty()

            val airingEpisodeNumber: Double
                get() = nextAiringEpisode.first.toDouble()

            val airingTime: Long
                get() = nextAiringEpisode.second.times(1000L).minus(
                    Calendar.getInstance().timeInMillis,
                )

            val filterActive: Boolean
                get() = anime.episodesFiltered()

            /**
             * Applies the view filters to the list of episodes obtained from the database.
             * @return an observable of the list of episodes filtered and sorted.
             */
            private fun List<EpisodeList.Item>.applyFilters(anime: Anime): Sequence<EpisodeList.Item> {
                val isLocalAnime = anime.isLocal()
                val unseenFilter = anime.unseenFilter
                val downloadedFilter = anime.downloadedFilter
                val bookmarkedFilter = anime.bookmarkedFilter
                // AM (FILLERMARK) -->
                val fillermarkedFilter = anime.fillermarkedFilter
                // <-- AM (FILLERMARK)
                return asSequence()
                    .filter { (episode) -> applyFilter(unseenFilter) { !episode.seen } }
                    .filter { (episode) -> applyFilter(bookmarkedFilter) { episode.bookmark } }
                    // AM (FILLERMARK) -->
                    .filter { (episode) -> applyFilter(fillermarkedFilter) { episode.fillermark } }
                    // <-- AM (FILLERMARK)
                    .filter { applyFilter(downloadedFilter) { it.isDownloaded || isLocalAnime } }
                    .sortedWith { (episode1), (episode2) ->
                        getEpisodeSort(anime).invoke(
                            episode1,
                            episode2,
                        )
                    }
            }
        }
    }
}

@Immutable
sealed class EpisodeList {
    @Immutable
    data class Item(
        val episode: Episode,
        val downloadState: Download.State,
        val downloadProgress: Int,
        val selected: Boolean = false,
        var fileSize: Long? = null,
    ) : EpisodeList() {
        val id: Long? = episode.id
        val isDownloaded: Boolean = downloadState == Download.State.DOWNLOADED
    }

    @Immutable
    data class MissingCount(
        val id: String,
        val count: Int,
    ) : EpisodeList()
}

private fun List<Episode>.toEpisodeListItems(anime: Anime): List<EpisodeList.Item> {
    return map { episode ->
        EpisodeList.Item(
            episode = episode,
            downloadState = Download.State.NOT_DOWNLOADED,
            downloadProgress = 0,
        )
    }
}
