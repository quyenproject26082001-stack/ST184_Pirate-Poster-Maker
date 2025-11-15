package com.charactor.avatar.maker.pfp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.charactor.avatar.maker.pfp.data.local.dao.UserDao
import com.charactor.avatar.maker.pfp.data.local.entity.User

@Database(entities = [User::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}