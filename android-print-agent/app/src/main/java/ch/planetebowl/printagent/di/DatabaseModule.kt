package ch.planetebowl.printagent.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import ch.planetebowl.printagent.data.local.AppDatabase
import ch.planetebowl.printagent.data.local.PrintJobDao
import ch.planetebowl.printagent.data.local.PrintLogDao
import ch.planetebowl.printagent.data.repository.QueueRepositoryImpl
import ch.planetebowl.printagent.data.repository.SettingsRepositoryImpl
import ch.planetebowl.printagent.domain.repository.QueueRepository
import ch.planetebowl.printagent.domain.repository.SettingsRepository
import com.google.gson.Gson
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.printAgentDataStore: DataStore<Preferences> by preferencesDataStore(name = "print_agent_settings")

/** Persistence locale : Room (file d'impression + logs) et DataStore (reglages non
 * sensibles). Le token, lui, ne transite jamais par ici — voir SecurityModule. */
@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {

    @Binds
    @Singleton
    abstract fun bindQueueRepository(impl: QueueRepositoryImpl): QueueRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    companion object {
        @Provides
        @Singleton
        fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
                // Pas de migration destructive silencieuse en production : un changement
                // de schema doit passer par une vraie Migration ecrite explicitement.
                .build()

        @Provides
        fun providePrintJobDao(database: AppDatabase): PrintJobDao = database.printJobDao()

        @Provides
        fun providePrintLogDao(database: AppDatabase): PrintLogDao = database.printLogDao()

        @Provides
        @Singleton
        fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            context.printAgentDataStore

        @Provides
        @Singleton
        fun provideGson(): Gson = Gson()
    }
}
