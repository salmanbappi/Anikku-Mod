package eu.kanade.tachiyomi.data.backup

object BackupDetector {
    /**
     * Check if the backup is a legacy format (e.g. from older Aniyomi/Animiru versions).
     * Legacy backups often have anime at field 3, while modern ones use 501.
     */
    fun isLegacyBackup(bytes: ByteArray): Boolean {
        // Very basic heuristic: check if any of the first few hundred bytes match the tag for ID 3 (0x1A)
        // or ID 1 (0x0A - manga). Modern backups would have ID 501 (0xAA 0x1F) or 500.
        // For simplicity, we can also just try to decode a small portion or check the header.
        // A more robust way is to check for the absence of field 500/501 tags.
        
        // Let's check the first 100 bytes for 0x1A (ID 3) or 0x0A (ID 1)
        for (i in 0 until minOf(bytes.size, 100)) {
            if (bytes[i].toInt() == 0x1A || bytes[i].toInt() == 0x0A) return true
        }
        return false
    }
}
