package dev.aaguirre.pc01.data.remote

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

object FirebaseAuthManager {

    private val auth = FirebaseAuth.getInstance()

    // Iniciar sesión
    suspend fun loginUser(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Registrar usuario
    suspend fun registerUser(email: String, password: String): Result<Unit> {
        return try {
            auth.createUserWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Cerrar sesión
    fun logoutUser() {
        auth.signOut()
    }

    // Obtener usuario actual
    fun getCurrentUser() = auth.currentUser

    // Verificar si está logueado
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }
}