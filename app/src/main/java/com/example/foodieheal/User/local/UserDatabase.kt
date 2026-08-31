package com.example.foodieheal.User.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

//This file is the brain of the local database, it creates and manage database file on user phone
//Entity right here is the folder in the file, version is like the identity card of the database, if add new column need to increase it
@Database(entities = [UserEntity::class, ChefEntity::class, PublicUserEntity::class, FollowEntity::class], version = 3)
abstract class UserDatabase : RoomDatabase() {

    //This connects the dao with the database of our phone, create object function of the dao
    abstract fun userDao(): UserDao

    //Companion object ensure the app only create one single instance of the database
    companion object {

        // Volatile is safety for the multi threading, ensure if one thread make changes other thread can see immediately
        // Private ensure other class cannot access it
        // Create an instance variable hold user database object or null stuff
        @Volatile
        private var INSTANCE: UserDatabase? = null

        //This function is used to get the User Database
        // If the instance not null, return it, else create one and return it
        // Synchronize means only one thread can enter this block at a time, prevent many thread try to create database at the same time
        // this means the current object calling it
        fun getDatabase(context: Context): UserDatabase {
            return INSTANCE ?: synchronized(this) {
                // Create the database instance
                // Give room application level context, so it is not belong to any activity and will not affect once activity stop or terminate
                // Give the room java class object represent user database
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UserDatabase::class.java,
                    "user_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
                //fall back to destructive migration means if database version and structure change, use the new one instead of the old one
                // put the new instance into the old one then return to that line ?:
            }
        }
    }
}