package com.sincronia.app.core.network

import com.sincronia.app.core.auth.GoogleAuthManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton fun retrofit(auth: GoogleAuthManager): Retrofit = GoogleHttp.retrofit(GoogleHttp.client(auth))
    @Provides @Singleton fun calendarApi(retrofit: Retrofit): CalendarApi = retrofit.create(CalendarApi::class.java)
    @Provides @Singleton fun tasksApi(retrofit: Retrofit): TasksApi = retrofit.create(TasksApi::class.java)
}
