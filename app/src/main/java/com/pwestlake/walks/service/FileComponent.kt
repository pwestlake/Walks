package com.pwestlake.walks.service

import com.pwestlake.walks.activities.WalkListFragment
import dagger.Component

@Component(modules = [ServiceModule::class])
interface FileComponent {
    fun inject(app: WalkListFragment)
}
