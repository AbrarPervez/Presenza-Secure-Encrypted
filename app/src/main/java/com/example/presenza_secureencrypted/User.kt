package com.example.presenza_secureencrypted

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val rollNo: String,
    val name: String,
    val section: String,
    val embedding: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as User
        if (id != other.id) return false
        if (rollNo != other.rollNo) return false
        if (name != other.name) return false
        if (section != other.section) return false
        if (!embedding.contentEquals(other.embedding)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + rollNo.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + section.hashCode()
        result = 31 * result + embedding.contentHashCode()
        return result
    }
}
