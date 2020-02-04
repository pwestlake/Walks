package com.pwestlake.walks.service

import dagger.Module
import dagger.Provides

@Module
class ServiceModule {
    @Provides
    fun createSettingsService(): SettingsService {
        return SettingsServiceImpl()
    }

    @Provides
    fun createFileService(): FileService {
        return FileServiceImpl()
    }
}