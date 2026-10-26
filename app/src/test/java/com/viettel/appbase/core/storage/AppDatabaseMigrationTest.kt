package com.viettel.appbase.core.storage

import android.app.Application
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Guards users' existing data across schema changes. Runs on the JVM (Robolectric), so CI covers it.
 * Each test starts from an old exported schema (app/schemas) and migrates to [AppDatabase.VERSION].
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class) // Skip AppApplication: no Koin/Firebase needed here.
class AppDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun `every exported version migrates to the latest schema`() {
        for (version in FIRST_VERSION..AppDatabase.VERSION) {
            val name = "migration-from-$version"
            helper.createDatabase(name, version).close()
            // Validates the migrated schema against the latest exported JSON.
            helper.runMigrationsAndValidate(name, AppDatabase.VERSION, true, *AppDatabase.MIGRATIONS).close()
        }
    }

    @Test
    fun `cached user from version 1 survives migration`() {
        helper.createDatabase(TEST_DB, FIRST_VERSION).use { db ->
            // Written against the version 1 schema on purpose: this SQL must never change.
            db.execSQL(
                "INSERT INTO users (id, email, displayName, updatedAtEpochMillis) " +
                    "VALUES ('42', 'user@example.com', 'User', 1000)",
            )
        }

        helper.runMigrationsAndValidate(TEST_DB, AppDatabase.VERSION, true, *AppDatabase.MIGRATIONS).use { db ->
            db.query("SELECT id, email, displayName FROM users").use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                assertEquals("42", cursor.getString(0))
                assertEquals("user@example.com", cursor.getString(1))
                assertEquals("User", cursor.getString(2))
            }
        }
    }

    @Test
    fun `production builder opens the database with registered migrations`() {
        val database = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
            TEST_DB,
        ).addMigrations(*AppDatabase.MIGRATIONS).allowMainThreadQueries().build()

        database.openHelper.writableDatabase
        database.close()
    }

    private companion object {
        const val FIRST_VERSION = 1
        const val TEST_DB = "migration-test"
    }
}
