package com.jeremy.lumi

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LumiApp : Application(){
    override fun onCreate() {
        super.onCreate()
        com.jeremy.lumi.notifications.NotificationChannels.ensureAllChannelsExist(this)
    }
}
