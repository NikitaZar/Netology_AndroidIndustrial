package ru.netology.nmedia.hiltModules

import com.google.firebase.messaging.FirebaseMessaging
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object FirebaseMessagingModule {
    @Provides
    @Singleton
    fun provideFirebaseMessaging() = FirebaseMessaging.getInstance()
}