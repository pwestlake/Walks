package com.pwestlake.walks.service

import com.pwestlake.walks.MainActivity
import dagger.Component

@Component(modules = [ServiceModule::class])
interface SettingsComponent {
    fun inject(app: MainActivity)
}