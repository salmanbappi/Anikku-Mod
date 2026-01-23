package eu.kanade.tachiyomi.data.cache

import android.content.Context
import android.text.format.Formatter
import com.jakewharton.disklrucache.DiskLruCache
import eu.kanade.tachiyomi.util.storage.DiskUtil
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.File

/**
 * Class used to create chapter cache
 * For each image in a chapter a file is created
 * For each chapter a Json list is created and converted to a file.
 * The files are in format *md5key*.0
 *
 * @param context the application context.
 * @constructor creates an instance of the chapter cache.
 */
class ChapterCache(
    private val context: Context,
) {

    companion object {
        /** Application cache version.  */
        const val PARAMETER_APP_VERSION = 1

        /** The number of values per cache entry. Must be positive.  */
        const val PARAMETER_VALUE_COUNT = 1

        /** The maximum number of bytes this cache should use to store.  */
        const val PARAMETER_CACHE_SIZE = 50L * 1024 * 1024
    }

    /** Cache class used for cache management. */
    private val diskCache = DiskLruCache.open(
        File(context.cacheDir, "chapter_disk_cache"),
        PARAMETER_APP_VERSION,
        PARAMETER_VALUE_COUNT,
        PARAMETER_CACHE_SIZE,
    )

    /**
     * Returns directory of cache.
     */
    private val cacheDir: File
        get() = diskCache.directory

    /**
     * Returns real size of directory.
     */
    private val realSize: Long
        get() = DiskUtil.getDirectorySize(cacheDir)

    /**
     * Returns real size of directory in human readable format.
     */
    val readableSize: String
        get() = Formatter.formatFileSize(context, realSize)

    /**
     * Remove file from cache.
     *
     * @param file name of file "md5.0".
     * @return status of deletion for the file.
     */
    private fun removeFileFromCache(file: String): Boolean {
        // Make sure we don't delete the journal file (keeps track of cache)
        if (file == "journal" || file.startsWith("journal.")) {
            return false
        }

        return try {
            // Remove the extension from the file to get the key of the cache
            val key = file.substringBeforeLast(".")
            // Remove file from cache
            diskCache.remove(key)
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e) { "Failed to remove file from cache" }
            false
        }
    }

    fun clear(): Int {
        var deletedFiles = 0
        cacheDir.listFiles()?.forEach {
            if (removeFileFromCache(it.name)) {
                deletedFiles++
            }
        }
        return deletedFiles
    }
}
