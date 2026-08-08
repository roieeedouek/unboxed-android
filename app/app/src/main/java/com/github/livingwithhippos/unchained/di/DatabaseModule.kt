package com.github.livingwithhippos.unchained.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.github.livingwithhippos.unchained.data.local.CompleteRemoteServiceDao
import com.github.livingwithhippos.unchained.data.local.HostRegexDao
import com.github.livingwithhippos.unchained.data.local.KodiDeviceDao
import com.github.livingwithhippos.unchained.data.local.RemoteDeviceDao
import com.github.livingwithhippos.unchained.data.local.RepositoryDataDao
import com.github.livingwithhippos.unchained.data.local.UnchaineDB
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides the database injected with Dagger Hilt */
@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext appContext: Context): UnchaineDB {
        return Room.databaseBuilder(appContext, UnchaineDB::class.java, "unchained_db")
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_8_9,
                MIGRATION_9_10,
                MIGRATION_10_11,
            )
            .build()
    }

    @Provides
    fun provideHostRegexDao(database: UnchaineDB): HostRegexDao {
        return database.hostRegexDao()
    }

    @Provides
    fun provideKodiDeviceDao(database: UnchaineDB): KodiDeviceDao {
        return database.kodiDeviceDao()
    }

    @Provides
    fun provideRemoteDeviceDao(database: UnchaineDB): RemoteDeviceDao {
        return database.remoteDeviceDao()
    }

    @Provides
    fun providePluginRepositoryDao(database: UnchaineDB): RepositoryDataDao {
        return database.pluginRepositoryDao()
    }

    @Provides
    fun provideCompleteServiceDao(database: UnchaineDB): CompleteRemoteServiceDao {
        return database.completeRemoteServiceDao()
    }

    private val MIGRATION_1_2 =
        object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE `host_regex` (`regex` TEXT NOT NULL, " + "PRIMARY KEY(`regex`))"
                )
            }
        }

    private val MIGRATION_2_3 =
        object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // historical migration: "type" distinguished RD's host vs folder regexes, and no
                // longer exists on the entity (see MIGRATION_10_11) - kept as a literal since the
                // REGEX_TYPE_HOST constant it originally referenced is gone.
                db.execSQL("ALTER TABLE host_regex ADD COLUMN type INTEGER NOT NULL DEFAULT 0")
            }
        }

    private val MIGRATION_3_4 =
        object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE credentials")
            }
        }

    private val MIGRATION_8_9 =
        object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE plugin_version ADD COLUMN disabled INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

    private val MIGRATION_9_10 =
        object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE complete_remote_service ADD COLUMN enabled INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

    private val MIGRATION_10_11 =
        object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // host_regex is a disposable cache of hoster-matching regexes (re-fetched from
                // TorBox's webdl/hosters endpoint), so it's safe to rebuild it from scratch now
                // that RD's host-vs-folder regex distinction ("type" column) no longer applies.
                db.execSQL("DROP TABLE IF EXISTS host_regex")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `host_regex` (`regex` TEXT NOT NULL, PRIMARY KEY(`regex`))"
                )
            }
        }
}
