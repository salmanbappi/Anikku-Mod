package eu.kanade.tachiyomi.data.backup.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class LegacyBackup(
    @ProtoNumber(3) val backupAnime: List<BackupAnime>,
    @ProtoNumber(4) var backupCategories: List<BackupCategory> = emptyList(),
    @ProtoNumber(103) var backupSources: List<BackupSource> = emptyList(),
    @ProtoNumber(104) var backupPreferences: List<BackupPreference> = emptyList(),
    @ProtoNumber(105) var backupSourcePreferences: List<BackupSourcePreferences> = emptyList(),
    @ProtoNumber(106) var backupExtensions: List<BackupExtension> = emptyList(),
    @ProtoNumber(107) var backupExtensionRepo: List<BackupExtensionRepos> = emptyList(),
    @ProtoNumber(109) var backupCustomButton: List<BackupCustomButtons> = emptyList(),
) {
    fun toBackup(): Backup {
        return Backup(
            isLegacy = false, // Only used for detection
            backupAnimeModern = backupAnime,
            backupCategoriesModern = backupCategories,
            backupSourcesModern = backupSources,
            backupPreferences = backupPreferences,
            backupSourcePreferences = backupSourcePreferences,
            backupExtensionsModern = backupExtensions,
            backupExtensionRepoModern = backupExtensionRepo,
            backupCustomButtonModern = backupCustomButton,
        )
    }
}

@Serializable
data class Backup(
    @ProtoNumber(1) val backupManga: List<BackupAnime> = emptyList(),
    @ProtoNumber(2) var backupCategories: List<BackupCategory> = emptyList(),
    @ProtoNumber(3) val backupAnime: List<BackupAnime> = emptyList(),
    @ProtoNumber(4) var backupAnimeCategories: List<BackupCategory> = emptyList(),
    // Bump by 100 to specify this is a 0.x value
    @ProtoNumber(100) var backupBrokenMangaSources: List<BrokenBackupAnimeSource> = emptyList(),
    @ProtoNumber(101) var backupMangaSources: List<BackupSource> = emptyList(),
    @ProtoNumber(102) var backupBrokenAnimeSources: List<BrokenBackupAnimeSource> = emptyList(),
    @ProtoNumber(103) var backupSources: List<BackupSource> = emptyList(),
    @ProtoNumber(104) var backupPreferences: List<BackupPreference> = emptyList(),
    @ProtoNumber(105) var backupSourcePreferences: List<BackupSourcePreferences> = emptyList(),
    @ProtoNumber(106) var backupExtensions: List<BackupExtension> = emptyList(),
    @ProtoNumber(107) var backupAnimeExtensionRepo: List<BackupExtensionRepos> = emptyList(),
    @ProtoNumber(109) var backupCustomButton: List<BackupCustomButtons> = emptyList(),

    // Aniyomi/Animiru specific values
    @ProtoNumber(500) val isLegacy: Boolean = true,
    @ProtoNumber(501) val backupAnimeModern: List<BackupAnime> = emptyList(),
    @ProtoNumber(502) var backupCategoriesModern: List<BackupCategory> = emptyList(),
    @ProtoNumber(503) var backupSourcesModern: List<BackupSource> = emptyList(),
    @ProtoNumber(504) var backupExtensionsModern: List<BackupExtension> = emptyList(),
    @ProtoNumber(505) var backupExtensionRepoModern: List<BackupExtensionRepos> = emptyList(),
    @ProtoNumber(506) var backupCustomButtonModern: List<BackupCustomButtons> = emptyList(),
)
