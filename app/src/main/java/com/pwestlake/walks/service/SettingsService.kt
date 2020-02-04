package com.pwestlake.walks.service

import com.pwestlake.walks.bo.Settings

interface SettingsService {
    fun saveSettings(settings: Settings)
}