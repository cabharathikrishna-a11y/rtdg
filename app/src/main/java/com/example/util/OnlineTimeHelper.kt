package com.example.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object OnlineTimeHelper {
    private const val NTP_PORT = 123
    private const val NTP_PACKET_SIZE = 48
    private const val NTP_MODE_CLIENT = 3
    private const val NTP_VERSION = 3
    private const val OFFSET_1900_TO_1970 = 2208988800L
    private const val TRANSMIT_TIME_OFFSET = 40

    /**
     * Attempts to resolve network time from multiple high-availability NTP servers.
     * Returns a Pair of:
     * - Long: Verified/Synced Timestamp (in milliseconds)
     * - Boolean: Verification Status (true if time is network-verified, false if offline fallback)
     */
    suspend fun getOnlineTimeOrLocal(): Pair<Long, Boolean> {
        val servers = listOf("time.google.com", "pool.ntp.org", "time.windows.com")
        for (server in servers) {
            val verifiedTime = getNetworkTime(server, timeoutMs = 2500)
            if (verifiedTime != null) {
                return Pair(verifiedTime, true)
            }
        }
        // If all NTP lookups fail, gracefully fall back to local system time but flag as not verified
        return Pair(System.currentTimeMillis(), false)
    }

    private suspend fun getNetworkTime(host: String, timeoutMs: Int): Long? = withContext(Dispatchers.IO) {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            socket.soTimeout = timeoutMs
            val address = InetAddress.getByName(host)
            val buffer = ByteArray(NTP_PACKET_SIZE)

            // LI = 0 (no warning), VN = 3 (version 3), Mode = 3 (client) -> 0x1B
            buffer[0] = ((0 and 3) shl 6 or (NTP_VERSION and 7) shl 3 or (NTP_MODE_CLIENT and 7)).toByte()

            // Transmit timestamp is set to current system time
            val requestTime = System.currentTimeMillis()
            writeTimestamp(buffer, TRANSMIT_TIME_OFFSET, requestTime)

            val packet = DatagramPacket(buffer, buffer.size, address, NTP_PORT)
            socket.send(packet)

            val receivePacket = DatagramPacket(buffer, buffer.size)
            socket.receive(receivePacket)

            // Parse transmit timestamp from server response (offset 40)
            readTimestamp(buffer, TRANSMIT_TIME_OFFSET)
        } catch (e: Exception) {
            null
        } finally {
            socket?.close()
        }
    }

    private fun writeTimestamp(buffer: ByteArray, offset: Int, time: Long) {
        var seconds = time / 1000L
        val milliseconds = time % 1000L
        seconds += OFFSET_1900_TO_1970

        buffer[offset] = (seconds ushr 24).toByte()
        buffer[offset + 1] = (seconds ushr 16).toByte()
        buffer[offset + 2] = (seconds ushr 8).toByte()
        buffer[offset + 3] = seconds.toByte()

        val fraction = milliseconds * 0x100000000L / 1000L
        buffer[offset + 4] = (fraction ushr 24).toByte()
        buffer[offset + 5] = (fraction ushr 16).toByte()
        buffer[offset + 6] = (fraction ushr 8).toByte()
        buffer[offset + 7] = fraction.toByte()
    }

    private fun readTimestamp(buffer: ByteArray, offset: Int): Long {
        val seconds = read32(buffer, offset)
        val fraction = read32(buffer, offset + 4)
        return (seconds - OFFSET_1900_TO_1970) * 1000L + (fraction * 1000L ushr 32)
    }

    private fun read32(buffer: ByteArray, offset: Int): Long {
        val b0 = buffer[offset].toLong() and 0xff
        val b1 = buffer[offset + 1].toLong() and 0xff
        val b2 = buffer[offset + 2].toLong() and 0xff
        val b3 = buffer[offset + 3].toLong() and 0xff
        return (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
    }
}
