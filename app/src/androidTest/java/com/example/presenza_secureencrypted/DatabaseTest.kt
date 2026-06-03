package com.example.presenza_secureencrypted

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    private lateinit var db: AppDatabase
    private lateinit var userDao: UserDao

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        userDao = db.userDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun writeUserAndReadInList() = runBlocking {
        val user = User(
            rollNo = "123",
            name = "Test User",
            section = "AIML",
            embedding = floatArrayOf(0.1f, 0.2f, 0.3f)
        )
        userDao.insert(user)
        val allUsers = userDao.getAll()
        assertEquals(allUsers[0].name, "Test User")
        assertEquals(allUsers[0].rollNo, "123")
        assert(allUsers[0].embedding.contentEquals(floatArrayOf(0.1f, 0.2f, 0.3f)))
    }
}
