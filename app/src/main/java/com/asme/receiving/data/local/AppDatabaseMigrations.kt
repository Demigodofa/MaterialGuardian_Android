package com.asme.receiving.data.local

import androidx.room.migration.Migration

object AppDatabaseMigrations {
    // Version 7 is the production baseline on disk. Future schema changes
    // must add explicit migrations here instead of wiping local data.
    val ALL: Array<Migration> = emptyArray()
}
