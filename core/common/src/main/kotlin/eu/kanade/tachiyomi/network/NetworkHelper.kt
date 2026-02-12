package eu.kanade.tachiyomi.network

import android.content.Context
import eu.kanade.tachiyomi.network.interceptor.CloudflareInterceptor
import eu.kanade.tachiyomi.network.interceptor.IgnoreGzipInterceptor
import eu.kanade.tachiyomi.network.interceptor.UncaughtExceptionInterceptor
import eu.kanade.tachiyomi.network.interceptor.UserAgentInterceptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.brotli.BrotliInterceptor
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext
import kotlin.math.pow
import kotlin.random.Random

/* SY --> */
open /* SY <-- */ class NetworkHelper(
    private val context: Context,
    private val preferences: NetworkPreferences,
    // SY -->
    val isDebugBuild: Boolean,
    // SY <--
) {

    /* SY --> */
    open /* SY <-- */val cookieJar = AndroidCookieJar()

    /* SY --> */
    open /* SY <-- */val client: OkHttpClient =
        // KMK -->
        clientWithTimeOut()

    /**
     * Specialized client for high-performance downloading.
     * Mimics 1DM+/ADM by allowing many concurrent connections to the same host.
     */
    val downloadClient: OkHttpClient by lazy {
        client.newBuilder()
            .dispatcher(
                Dispatcher().apply {
                    maxRequests = 256
                    maxRequestsPerHost = 32 // Professional level concurrency
                },
            )
            .connectionPool(ConnectionPool(64, 5, TimeUnit.MINUTES))
            .build()
    }

    /**
     * Timeout in unit of seconds.
     */
    fun clientWithTimeOut(
        connectTimeout: Long = 30,
        readTimeout: Long = 30,
        callTimeout: Long = 120,
        // KMK <--
    ): OkHttpClient = run {
        val builder = OkHttpClient.Builder()
            .dispatcher(
                Dispatcher().apply {
                    maxRequests = 128
                    maxRequestsPerHost = 16
                },
            )
            .connectionPool(ConnectionPool(32, 2, TimeUnit.MINUTES))
            .cookieJar(cookieJar)
            // KMK -->
            .connectTimeout(connectTimeout, TimeUnit.SECONDS)
            .readTimeout(readTimeout, TimeUnit.SECONDS)
            .callTimeout(callTimeout, TimeUnit.SECONDS)
            // KMK <--
            .cache(
                Cache(
                    directory = File(context.cacheDir, "network_cache"),
                    maxSize = 5L * 1024 * 1024, // 5 MiB
                ),
            )
            .addInterceptor(UncaughtExceptionInterceptor())
            .addInterceptor(UserAgentInterceptor(::defaultUserAgentProvider))
            .addNetworkInterceptor(IgnoreGzipInterceptor())
            .addNetworkInterceptor(BrotliInterceptor)

        if (isDebugBuild) {
            val httpLoggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.HEADERS
            }
            builder.addNetworkInterceptor(httpLoggingInterceptor)
        }

        builder.addInterceptor(
            CloudflareInterceptor(context, cookieJar, ::defaultUserAgentProvider),
        )

        when (preferences.dohProvider().get()) {
            PREF_DOH_CLOUDFLARE -> builder.dohCloudflare()
            PREF_DOH_GOOGLE -> builder.dohGoogle()
            PREF_DOH_ADGUARD -> builder.dohAdGuard()
            PREF_DOH_QUAD9 -> builder.dohQuad9()
            PREF_DOH_ALIDNS -> builder.dohAliDNS()
            PREF_DOH_DNSPOD -> builder.dohDNSPod()
            PREF_DOH_360 -> builder.doh360()
            PREF_DOH_QUAD101 -> builder.dohQuad101()
            PREF_DOH_MULLVAD -> builder.dohMullvad()
            PREF_DOH_CONTROLD -> builder.dohControlD()
            PREF_DOH_NJALLA -> builder.dohNajalla()
            PREF_DOH_SHECAN -> builder.dohShecan()
            PREF_DOH_LIBREDNS -> builder.dohLibreDNS()
        }

        builder.build()
    }

    // KMK -->
    /**
     * Allow to download a big file with retry & resume capability because
     * normally it would get a Timeout exception.
     */
    fun downloadFileWithResume(url: String, outputFile: File, progressListener: ProgressListener) {
        val client = clientWithTimeOut(
            callTimeout = 120,
        )

        var downloadedBytes: Long

        var attempt = 0

        while (attempt < MAX_RETRY) {
            try {
                // Check how much has already been downloaded
                downloadedBytes = outputFile.length()
                // Set up request with Range header to resume from the last byte
                val request = GET(
                    url = url,
                    headers = Headers.Builder()
                        .add("Range", "bytes=$downloadedBytes-")
                        .build(),
                )

                var failed = false
                client.newCachelessCallWithProgress(request, progressListener).execute().use { response ->
                    if (response.isSuccessful || response.code == 206) { // 206 indicates partial content
                        saveResponseToFile(response, outputFile, downloadedBytes)
                        return
                    } else {
                        attempt++
                        android.util.Log.e("NetworkHelper", "Unexpected response code: ${response.code}. Retrying...")
                        if (response.code == 416) {
                            // 416: Range Not Satisfiable
                            outputFile.delete()
                        }
                        failed = true
                    }
                }
                if (failed) exponentialBackoff(attempt - 1)
            } catch (e: IOException) {
                android.util.Log.e("NetworkHelper", "Download interrupted: ${e.message}. Retrying...")
                // Wait or handle as needed before retrying
                attempt++
                exponentialBackoff(attempt - 1)
            }
        }
        throw IOException("Max retry attempts reached.")
    }

    // Helper function to save data incrementally
    private fun saveResponseToFile(response: Response, outputFile: File, startPosition: Long) {
        val body = response.body

        // Use RandomAccessFile to write from specific position
        RandomAccessFile(outputFile, "rw").use { file ->
            file.seek(startPosition)
            body.byteStream().use { input ->
                val buffer = ByteArray(8 * 1024)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    file.write(buffer, 0, bytesRead)
                }
                file.fd.sync()
            }
        }
    }

    // Increment attempt and apply exponential backoff
    private fun exponentialBackoff(attempt: Int) {
        val backoffDelay = calculateExponentialBackoff(attempt)
        Thread.sleep(backoffDelay)
    }

    // Helper function to calculate exponential backoff with jitter
    internal fun calculateExponentialBackoff(attempt: Int, baseDelay: Long = 1000L, maxDelay: Long = 32000L): Long {
        // Calculate the exponential delay
        val delay = baseDelay * 2.0.pow(attempt).toLong()
        // Apply jitter by adding a random value to avoid synchronized retries in distributed systems
        return (delay + Random.nextLong(0, 1000)).coerceAtMost(maxDelay)
    }

    data class DownloadChunk(val start: Long, val end: Long)

    internal fun calculateChunks(totalSize: Long, threadCount: Int): List<DownloadChunk> {
        val chunkSize = totalSize / threadCount
        return (0 until threadCount).map { i ->
            val start = i * chunkSize
            val end = if (i == threadCount - 1) totalSize - 1 else (i + 1) * chunkSize - 1
            DownloadChunk(start, end)
        }
    }

    /**
     * Downloads a file using multiple threads/connections in parallel.
     * 
     * Rationale: Standard sequential downloads often fail to saturate bandwidth on high-latency 
     * or throttled connections (common in BDIX environments). This implementation employs 
     * HTTP Range requests to divide the file into independent chunks, allowing for parallel 
     * I/O and exponential backoff on a per-chunk basis to ensure resilience.
     */
    suspend fun multiThreadedDownload(
        url: String,
        outputFile: File,
        headers: Headers = Headers.Builder().build(),
        threadCount: Int = 4,
        onProgress: (Long, Long) -> Unit = { _, _ -> },
    ) {
        val client = clientWithTimeOut(callTimeout = 0) // No timeout for big downloads

        // 1. Get total file size
        val headRequest = Request.Builder().url(url).headers(headers).head().build()
        val totalSize = client.newCall(headRequest).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Failed to get file size: ${response.code}")
            response.header("Content-Length")?.toLong() ?: throw IOException("Content-Length missing")
        }

        if (totalSize <= 0) throw IOException("Invalid content length")

        // 2. Prepare file
        val randomAccessFile = RandomAccessFile(outputFile, "rw")
        randomAccessFile.setLength(totalSize)
        randomAccessFile.close()

        val chunks = calculateChunks(totalSize, threadCount)
        val progressMap = mutableMapOf<Int, Long>()
        var totalDownloaded = 0L

        coroutineScope {
            chunks.forEachIndexed { i, chunk ->
                launch(Dispatchers.IO) {
                    var attempt = 0
                    var currentStart = chunk.start
                    while (attempt < MAX_RETRY && currentStart <= chunk.end) {
                        try {
                            val request = Request.Builder()
                                .url(url)
                                .headers(headers)
                                .addHeader("Range", "bytes=$currentStart-${chunk.end}")
                                .build()

                            client.newCall(request).execute().use { response ->
                                if (response.code != 206 && response.code != 200) {
                                    throw IOException("Unexpected response code: ${response.code}")
                                }

                                val body = response.body ?: throw IOException("Empty body")
                                RandomAccessFile(outputFile, "rw").use { file ->
                                    file.seek(currentStart)
                                    val buffer = ByteArray(1024 * 1024)
                                    var bytesRead: Int
                                    val bis = body.byteStream()
                                    while (bis.read(buffer).also { bytesRead = it } != -1) {
                                        coroutineContext.ensureActive()
                                        file.write(buffer, 0, bytesRead)
                                        currentStart += bytesRead
                                        
                                        val currentDownloaded = currentStart - chunk.start
                                        synchronized(progressMap) {
                                            progressMap[i] = currentDownloaded
                                            totalDownloaded = progressMap.values.sum()
                                        }
                                        onProgress(totalDownloaded, totalSize)
                                    }
                                }
                                return@launch // Success
                            }
                        } catch (e: Exception) {
                            attempt++
                            if (attempt >= MAX_RETRY) throw e
                            delay(1000L * attempt)
                        }
                    }
                }
            }
        }
    }
    // KMK <--

    /**
     * @deprecated Since extension-lib 1.5
     */
    @Deprecated("The regular client handles Cloudflare by default", ReplaceWith("client"))
    @Suppress("UNUSED")
    /* SY --> */
    open /* SY <-- */val cloudflareClient: OkHttpClient
        get() = client

    fun defaultUserAgentProvider() = preferences.defaultUserAgent().get().trim()

    companion object {
        // KMK -->
        private const val MAX_RETRY = 5
        // KMK <--
    }
}
