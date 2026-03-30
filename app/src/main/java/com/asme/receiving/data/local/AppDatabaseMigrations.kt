package com.asme.receiving.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object AppDatabaseMigrations {
    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE materials ADD COLUMN surfaceFinishCode TEXT NOT NULL DEFAULT ''"
            )
            database.execSQL(
                "ALTER TABLE materials ADD COLUMN surfaceFinishReading TEXT NOT NULL DEFAULT ''"
            )
            database.execSQL(
                "ALTER TABLE materials ADD COLUMN surfaceFinishUnit TEXT NOT NULL DEFAULT ''"
            )
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_7_8)
}
