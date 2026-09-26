package com.example.notification

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.util.Log

data class SoundPreset(
    val id: String,
    val name: String,
    val description: String,
    val uriString: String
)

object NotificationSoundHelper {
    private var currentPlayingRingtone: Ringtone? = null
    private var currentlyPlayingUri: String? = null

    const val ID_SYSTEM_DEFAULT = "SYSTEM_DEFAULT"
    const val ID_SYSTEM_ALARM = "SYSTEM_ALARM"
    const val ID_SYSTEM_RINGTONE = "SYSTEM_RINGTONE"

    fun getPresetSounds(context: Context): List<SoundPreset> {
        val list = mutableListOf<SoundPreset>()

        val defaultNotifUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)?.toString() ?: ""
        val defaultAlarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.toString() ?: ""
        val defaultRingtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)?.toString() ?: ""

        list.add(
            SoundPreset(
                id = ID_SYSTEM_DEFAULT,
                name = "Chuông thông báo mặc định",
                description = "Âm thanh ngắn gọn, chuẩn của hệ điều hành",
                uriString = defaultNotifUri
            )
        )
        list.add(
            SoundPreset(
                id = ID_SYSTEM_ALARM,
                name = "Chuông báo thức rõ ràng",
                description = "Âm lượng lớn vang, thích hợp việc quan trọng",
                uriString = defaultAlarmUri
            )
        )
        list.add(
            SoundPreset(
                id = ID_SYSTEM_RINGTONE,
                name = "Giai điệu chuông gọi",
                description = "Giai điệu vui tai, du dương dễ nhận biết",
                uriString = defaultRingtoneUri
            )
        )

        // Read up to 5 actual system notification tones from device
        try {
            val rm = RingtoneManager(context).apply {
                setType(RingtoneManager.TYPE_NOTIFICATION)
            }
            val cursor = rm.cursor
            var added = 0
            while (cursor.moveToNext() && added < 5) {
                val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                val uri = rm.getRingtoneUri(cursor.position)
                if (uri != null && uri.toString() != defaultNotifUri) {
                    list.add(
                        SoundPreset(
                            id = "SYS_${cursor.position}",
                            name = title,
                            description = "Âm thanh có sẵn trên điện thoại",
                            uriString = uri.toString()
                        )
                    )
                    added++
                }
            }
        } catch (e: Exception) {
            Log.w("NotificationSoundHelper", "Could not query system ringtones: ${e.message}")
        }

        return list
    }

    fun isPlaying(uriString: String?): Boolean {
        return currentPlayingRingtone?.isPlaying == true && currentlyPlayingUri == uriString
    }

    fun playSound(context: Context, uriString: String?) {
        stopSound()
        try {
            val targetUri = if (!uriString.isNullOrBlank()) {
                Uri.parse(uriString)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            val ringtone = RingtoneManager.getRingtone(context, targetUri)
            if (ringtone != null) {
                ringtone.audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .build()
                currentPlayingRingtone = ringtone
                currentlyPlayingUri = uriString
                ringtone.play()
            } else {
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
            }
        } catch (e: Exception) {
            Log.e("NotificationSoundHelper", "Error playing sound: ${e.message}")
        }
    }

    fun stopSound() {
        try {
            currentPlayingRingtone?.let {
                if (it.isPlaying) it.stop()
            }
            currentPlayingRingtone = null
            currentlyPlayingUri = null
        } catch (e: Exception) {
            Log.w("NotificationSoundHelper", "Error stopping sound: ${e.message}")
        }
    }

    fun getSoundTitle(context: Context, uriString: String?): String {
        if (uriString.isNullOrBlank()) return "Chuông mặc định hệ thống"
        return try {
            val uri = Uri.parse(uriString)
            val defaultNotifUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val defaultAlarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val defaultRingtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            if (uri == defaultNotifUri) {
                "Chuông thông báo mặc định"
            } else if (uri == defaultAlarmUri) {
                "Chuông báo thức rõ ràng"
            } else if (uri == defaultRingtoneUri) {
                "Giai điệu chuông gọi"
            } else {
                val ringtone = RingtoneManager.getRingtone(context, uri)
                ringtone?.getTitle(context) ?: "Chuông tùy chọn"
            }
        } catch (_: Exception) {
            "Chuông tùy chọn"
        }
    }
}
