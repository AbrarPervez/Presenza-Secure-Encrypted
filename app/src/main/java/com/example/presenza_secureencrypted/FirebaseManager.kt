package com.example.presenza_secureencrypted

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class FirebaseManager {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Saves the face embedding for the currently logged-in user.
     */
    suspend fun saveUserEmbedding(embedding: List<Float>): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
        
        return try {
            val data = hashMapOf("face_embedding" to embedding)
            db.collection("users").document(userId)
                .set(data, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Enrolls a new student with their details and face embedding.
     */
    suspend fun enrollStudent(
        rollNo: String,
        firstName: String,
        lastName: String,
        section: String,
        embedding: List<Float>
    ): Result<Unit> {
        return try {
            val studentData = hashMapOf(
                "rollNo" to rollNo,
                "firstName" to firstName,
                "lastName" to lastName,
                "section" to section,
                "face_embedding" to embedding,
                "enrolledAt" to com.google.firebase.Timestamp.now()
            )
            db.collection("students").document(rollNo)
                .set(studentData)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Toggles attendance: If already recorded today, removes it. If not, records it.
     * Returns true if attendance was recorded, false if it was removed.
     */
    suspend fun toggleAttendance(rollNo: String, name: String): Result<Boolean> {
        return try {
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

            val snapshot = db.collection("attendance")
                .whereEqualTo("rollNo", rollNo)
                .whereGreaterThanOrEqualTo("timestamp", today)
                .get()
                .await()

            if (!snapshot.isEmpty) {
                // Already recorded today, remove entries
                for (doc in snapshot.documents) {
                    db.collection("attendance").document(doc.id).delete().await()
                }
                Result.success(false) // Removed
            } else {
                // Not recorded yet, add entry
                val entry = hashMapOf(
                    "rollNo" to rollNo,
                    "name" to name,
                    "timestamp" to com.google.firebase.Timestamp.now(),
                    "status" to "Present",
                    "verified_via" to "FaceRecognition_Liveness"
                )
                db.collection("attendance").add(entry).await()
                Result.success(true) // Added
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllStudents(): Result<List<Map<String, Any>>> {
        return try {
            val snapshot = db.collection("students").get().await()
            val students = snapshot.documents.mapNotNull { it.data?.plus("id" to it.id) }
            Result.success(students)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
