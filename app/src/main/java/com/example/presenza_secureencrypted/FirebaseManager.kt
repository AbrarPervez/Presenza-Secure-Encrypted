package com.example.presenza_secureencrypted

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirebaseManager {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Saves the face embedding for the currently logged-in user.
     * @param embedding A list of floats representing the face features.
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
            // Use rollNo as the document ID for easy lookup
            db.collection("students").document(rollNo)
                .set(studentData)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieves a student's embedding by their Roll Number.
     */
    suspend fun getStudentEmbedding(rollNo: String): Result<List<Float>?> {
        return try {
            val document = db.collection("students").document(rollNo).get().await()
            val embedding = document.get("face_embedding") as? List<*>
            val floatEmbedding = embedding?.map { (it as Number).toFloat() }
            Result.success(floatEmbedding)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieves all enrolled students.
     */
    suspend fun getAllStudents(): Result<List<Map<String, Any>>> {
        return try {
            val snapshot = db.collection("students").get().await()
            val students = snapshot.documents.mapNotNull { it.data?.plus("id" to it.id) }
            Result.success(students)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Records an attendance entry after successful verification.
     */
    suspend fun recordAttendance(rollNo: String, name: String, status: String): Result<Unit> {
        return try {
            val entry = hashMapOf(
                "rollNo" to rollNo,
                "name" to name,
                "timestamp" to com.google.firebase.Timestamp.now(),
                "status" to status,
                "verified_via" to "FaceRecognition_Liveness"
            )
            db.collection("attendance").add(entry).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
