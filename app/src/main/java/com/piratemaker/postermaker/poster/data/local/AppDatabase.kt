package com.piratemaker.postermaker.poster.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.piratemaker.postermaker.poster.data.local.dao.UserDao
import com.piratemaker.postermaker.poster.data.local.entity.User

@Database(entities = [User::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}