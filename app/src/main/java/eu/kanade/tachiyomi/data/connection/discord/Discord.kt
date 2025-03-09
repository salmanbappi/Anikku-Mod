// AM (DISCORD) -->

// Taken from Animiru. Thank you Quickdev for permission!

package eu.kanade.tachiyomi.data.connection.discord

import android.graphics.Color
import androidx.annotation.StringRes
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.connection.BaseConnection
import eu.kanade.tachiyomi.data.connections.discord.DiscordAccount
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class Discord(id: Long) : BaseConnection(id) {

    @StringRes
    override fun nameRes() = R.string.connections_discord

    override fun getLogo() = R.drawable.ic_discord_24dp

    @Suppress("MagicNumber")
    override fun getLogoColor() = Color.rgb(88, 101, 242)

    override fun logout() {
        super.logout()
        connectionPreferences.connectionsToken(this).delete()
    }

    override suspend fun login(username: String, password: String) {
        // Not Needed
    }

    private val json = Injekt.get<Json>()

    fun getAccounts(): List<DiscordAccount> {
        val accountsJson = connectionPreferences.discordAccounts().get()
        return try {
            if (accountsJson.isNotBlank()) {
                json.decodeFromString<List<DiscordAccount>>(accountsJson)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addAccount(account: DiscordAccount) {
        val accounts = getAccounts().toMutableList()
        logcat(LogPriority.DEBUG) { "Debug: Adding account: $account" }

        if (account.isActive) {
            accounts.replaceAll { it.copy(isActive = false) }
            connectionPreferences.connectionsToken(this).set(account.token)
        }

        val index = accounts.indexOfFirst { it.id == account.id }
        if (index >= 0) {
            accounts[index] = account
        } else {
            accounts.add(account)
        }

        logcat(LogPriority.DEBUG) { "Debug: Updated accounts: $accounts" } // Debug log
        saveAccounts(accounts)
    }

    fun removeAccount(accountId: String) {
        val accounts = getAccounts().toMutableList()
        accounts.removeAll { it.id == accountId }
        saveAccounts(accounts)
    }

    fun setActiveAccount(accountId: String) {
        val accounts = getAccounts().toMutableList()
        accounts.replaceAll { it.copy(isActive = it.id == accountId) }
        saveAccounts(accounts)
        // Update active token and restart RPC
        accounts.find { it.id == accountId }?.let { account ->
            connectionPreferences.connectionsToken(this).set(account.token)
            // Trigger RPC restart
            connectionPreferences.enableDiscordRPC().set(false)
            connectionPreferences.enableDiscordRPC().set(true)
        }
    }

    fun restartRichPresence() {
        // Trigger RPC restart by toggling the preference
        connectionPreferences.enableDiscordRPC().set(false)
        connectionPreferences.enableDiscordRPC().set(true)
    }

    private fun saveAccounts(accounts: List<DiscordAccount>) {
        try {
            val accountsJson = json.encodeToString(accounts)
            connectionPreferences.discordAccounts().set(accountsJson)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
// <-- AM (DISCORD)
