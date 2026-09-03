package com.example.foodieheal.User.Repo

import android.util.Log
import com.example.foodieheal.MainActivity
import com.example.foodieheal.SupabaseClient
import com.example.foodieheal.User.Model.Follow
import com.example.foodieheal.User.local.UserDatabase
import com.example.foodieheal.User.local.toEntity
import com.example.foodieheal.User.local.toDomain
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class FollowRepository {

    private fun getDao() = MainActivity.appContext?.let { UserDatabase.getDatabase(it).userDao() }

    suspend fun getFollowStatus(followerId: String, followingId: String): String? = withContext(Dispatchers.IO) {
        try {
            val follow = SupabaseClient.client.from("follows")
                .select {
                    filter {
                        eq("follower_id", followerId)
                        eq("following_id", followingId)
                    }
                }.decodeSingleOrNull<Follow>()
            
            follow?.let { getDao()?.insertFollows(listOf(it.toEntity())) }
            follow?.status
        } catch (e: Exception) {
            Log.w("FollowRepository", "Error getting follow status from network: ${e.message}")
            getDao()?.getFollowStatus(followerId, followingId)
        }
    }

    suspend fun sendFollowRequest(followerId: String, followingId: String) = withContext(Dispatchers.IO) {
        try {
            val nextId = getNextFollowId()
            val payload = buildJsonObject {
                put("id", nextId)
                put("follower_id", followerId)
                put("following_id", followingId)
                put("status", "PENDING")
            }
            SupabaseClient.client.from("follows").insert(payload)
            val follow = Follow(
                id = nextId,
                followerId = followerId,
                followingId = followingId,
                status = "PENDING"
            )
            getDao()?.insertFollows(listOf(follow.toEntity()))
        } catch (e: Exception) {
            Log.e("FollowRepository", "Error sending follow request", e)
        }
    }

    suspend fun cancelFollowRequest(followerId: String, followingId: String) = withContext(Dispatchers.IO) {
        try {
            SupabaseClient.client.from("follows").delete {
                filter {
                    eq("follower_id", followerId)
                    eq("following_id", followingId)
                }
            }
            getDao()?.deleteFollow(followerId, followingId)
        } catch (e: Exception) {
            Log.e("FollowRepository", "Error cancelling follow request", e)
        }
    }

    suspend fun acceptFollowRequest(followerId: String, followingId: String) = withContext(Dispatchers.IO) {
        try {
            val payload = buildJsonObject {
                put("status", "ACCEPTED")
            }
            SupabaseClient.client.from("follows").update(payload) {
                filter {
                    eq("follower_id", followerId)
                    eq("following_id", followingId)
                }
            }
            // Refresh from network to get latest
            getFollowStatus(followerId, followingId)
        } catch (e: Exception) {
            Log.e("FollowRepository", "Error accepting follow request", e)
        }
    }

    suspend fun unfollowUser(followerId: String, followingId: String) = withContext(Dispatchers.IO) {
        try {
            SupabaseClient.client.from("follows").delete {
                filter {
                    eq("follower_id", followerId)
                    eq("following_id", followingId)
                }
            }
            getDao()?.deleteFollow(followerId, followingId)
        } catch (e: Exception) {
            Log.e("FollowRepository", "Error unfollowing user", e)
        }
    }

    suspend fun getFollowing(userId: String): List<Follow> = withContext(Dispatchers.IO) {
        try {
            val list = SupabaseClient.client.from("follows")
                .select { filter { eq("follower_id", userId) } }
                .decodeList<Follow>()
            
            getDao()?.insertFollows(list.map { it.toEntity() })
            list
        } catch (e: Exception) {
            Log.w("FollowRepository", "Offline: getting following from cache for $userId")
            getDao()?.getFollowing(userId)?.map { it.toDomain() } ?: emptyList()
        }
    }

    suspend fun getFollowers(userId: String): List<Follow> = withContext(Dispatchers.IO) {
        try {
            val list = SupabaseClient.client.from("follows")
                .select { filter { eq("following_id", userId) } }
                .decodeList<Follow>()
            
            getDao()?.insertFollows(list.map { it.toEntity() })
            list
        } catch (e: Exception) {
            Log.w("FollowRepository", "Offline: getting followers from cache for $userId")
            getDao()?.getFollowers(userId)?.map { it.toDomain() } ?: emptyList()
        }
    }

    private suspend fun getNextFollowId(): String = withContext(Dispatchers.IO) {
        try {
            val existing = SupabaseClient.client.from("follows")
                .select { order("id", Order.DESCENDING) }
                .decodeList<Follow>()

            val maxNum = existing
                .mapNotNull { it.id?.removePrefix("F")?.toIntOrNull() }
                .maxOrNull() ?: 0

            "F${(maxNum + 1).toString().padStart(3, '0')}"
        } catch (e: Exception) {
            "F${System.currentTimeMillis().toString().takeLast(3)}"
        }
    }
}
