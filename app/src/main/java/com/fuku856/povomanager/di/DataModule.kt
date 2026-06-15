package com.fuku856.povomanager.di

import android.content.Context
import androidx.room.Room
import com.fuku856.povomanager.data.db.LineDao
import com.fuku856.povomanager.data.db.MIGRATION_1_2
import com.fuku856.povomanager.data.db.PovoDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun providePovoDatabase(@ApplicationContext context: Context): PovoDatabase =
        Room.databaseBuilder(context, PovoDatabase::class.java, "povo-manager.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun provideLineDao(database: PovoDatabase): LineDao = database.lineDao()
}
