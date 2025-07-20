package com.example.lango_mvp_android.di

import android.app.Application
import android.content.res.AssetManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.io.File

@Module
@InstallIn(SingletonComponent::class)
object PathModule {

    @Provides
    fun provideAssetManager(application: Application): AssetManager {
        return application.assets
    }

    @Provides
    fun provideBaseDir(application: Application): File {
        return application.filesDir
    }
}