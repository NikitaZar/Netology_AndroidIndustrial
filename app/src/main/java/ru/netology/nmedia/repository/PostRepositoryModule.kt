package ru.netology.nmedia.repository

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.scopes.ViewModelScoped
import dagger.hilt.components.SingletonComponent
import retrofit2.http.POST
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PostRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindPostRepository(imp: PostRepositoryImpl): PostRepository
}