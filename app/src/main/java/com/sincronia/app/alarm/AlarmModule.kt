package com.sincronia.app.alarm

import com.sincronia.app.core.alarm.AlarmClock
import com.sincronia.app.core.alarm.AlarmNotices
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AlarmModule {
    @Binds abstract fun clock(impl: AndroidAlarmClock): AlarmClock
    @Binds abstract fun notices(impl: AlarmNotifier): AlarmNotices
}
