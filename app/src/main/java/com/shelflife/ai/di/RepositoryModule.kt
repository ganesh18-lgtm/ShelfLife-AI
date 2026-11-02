package com.shelflife.ai.di

import com.shelflife.ai.data.repository.PantryRepositoryImpl
import com.shelflife.ai.domain.repository.PantryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module binding domain repository contracts to data layer implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPantryRepository(
        impl: PantryRepositoryImpl
    ): PantryRepository
}
