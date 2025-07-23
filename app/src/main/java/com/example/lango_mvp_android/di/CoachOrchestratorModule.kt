package com.example.lango_mvp_android.di

import com.example.domain.CoachOrchestrator
import com.example.domain.CoachOrchestratorImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CoachOrchestratorModule {

    @Binds
    @Singleton
    abstract fun bindCoachOrchestrator(coachOrchestratorImpl: CoachOrchestratorImpl): CoachOrchestrator
}