package com.colswe.groupbtwo.data.repository

import com.colswe.groupbtwo.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ProfileRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val usersCollection = firestore.collection("users")

    suspend fun getUserProfile(): Result<UserProfile> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("Usuario no autenticado"))

            try {
                val document = usersCollection.document(currentUser.uid).get().await()

                if (document.exists()) {
                    val profile = document.toObject(UserProfile::class.java)
                    Result.success(profile ?: UserProfile())
                } else {
                    // Si no existe el perfil, crear uno básico
                    val newProfile = UserProfile(
                        id = currentUser.uid,
                        email = currentUser.email ?: "",
                        alias = ""
                    )
                    usersCollection.document(currentUser.uid).set(newProfile).await()
                    Result.success(newProfile)
                }
            } catch (firestoreException: Exception) {
                // Si Firestore no está habilitado o hay error de conexión,
                // retornar un perfil básico con los datos de Firebase Auth
                val basicProfile = UserProfile(
                    id = currentUser.uid,
                    email = currentUser.email ?: "",
                    alias = ""
                )
                Result.success(basicProfile)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(profile: UserProfile): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("Usuario no autenticado"))

            val updatedProfile = profile.copy(
                id = currentUser.uid,
                updatedAt = System.currentTimeMillis()
            )

            try {
                usersCollection.document(currentUser.uid).set(updatedProfile).await()
                Result.success(Unit)
            } catch (firestoreException: Exception) {
                // Si Firestore no está disponible, retornar error amigable
                Result.failure(Exception("Firestore no está habilitado. Por favor, habilítalo en Firebase Console."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentUserEmail(): String? {
        return auth.currentUser?.email
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
}