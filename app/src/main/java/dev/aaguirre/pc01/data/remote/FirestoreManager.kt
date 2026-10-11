package dev.aaguirre.pc01.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dev.aaguirre.pc01.data.model.BallModel
import dev.aaguirre.pc01.data.model.DrawModel
import dev.aaguirre.pc01.data.model.PlayModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object FirestoreManager {

    private val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    // Guardar la cabecera del sorteo.
    suspend fun saveDraw(draw: DrawModel): Result<Unit> {
        return try {
            require(draw.drawId.isNotBlank()) {
                "El identificador del sorteo es obligatorio"
            }

            db.collection("draws")
                .document(draw.drawId)
                .set(draw)
                .await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Guardar una jugada por usuario mientras el sorteo esté abierto.
    suspend fun savePlay(play: PlayModel): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid
                ?: throw IllegalStateException("Debes iniciar sesión")

            require(play.userId == uid) {
                "La jugada debe pertenecer al usuario autenticado"
            }

            require(play.drawId.isNotBlank()) {
                "El identificador del sorteo es obligatorio"
            }

            require(play.numbers.size == 6) {
                "Debes seleccionar exactamente seis números"
            }

            require(play.numbers.toSet().size == 6) {
                "Los seis números deben ser distintos"
            }

            require(play.numbers.all { it in 1..45 }) {
                "Los números deben estar entre 1 y 45"
            }

            val drawRef = db.collection("draws")
                .document(play.drawId)

            val playRef = db.collection("plays")
                .document(play.drawId)
                .collection("userPlays")
                .document(uid)

            db.runTransaction { transaction ->
                val drawDoc = transaction.get(drawRef)

                check(drawDoc.exists()) {
                    "El sorteo no existe"
                }

                check(drawDoc.getString("status") == "abierto") {
                    "El sorteo ya no está abierto. No puedes registrar ni modificar la jugada"
                }

                transaction.set(playRef, play)
                Unit
            }.await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Recuperar únicamente la jugada del usuario autenticado.
    suspend fun getUserPlay(
        drawId: String,
        userId: String
    ): Result<PlayModel?> {
        return try {
            val uid = auth.currentUser?.uid
                ?: throw IllegalStateException("Debes iniciar sesión")

            require(userId == uid) {
                "Solo puedes consultar tu propia jugada"
            }

            require(drawId.isNotBlank()) {
                "El identificador del sorteo es obligatorio"
            }

            val document = db.collection("plays")
                .document(drawId)
                .collection("userPlays")
                .document(uid)
                .get()
                .await()

            Result.success(
                document.toObject(PlayModel::class.java)
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Escuchar la cabecera y el estado del sorteo en tiempo real.
    fun listenToDraw(drawId: String): Flow<DrawModel?> = callbackFlow {
        val listener = db.collection("draws")
            .document(drawId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                try {
                    val draw = snapshot?.toObject(DrawModel::class.java)
                    trySend(draw)
                } catch (e: Exception) {
                    close(e)
                }
            }

        // Quitar la escucha cuando se cancela la colección del Flow.
        awaitClose {
            listener.remove()
        }
    }

    // Escuchar las bolillas y ordenarlas por su orden de salida.
    fun listenToBalls(drawId: String): Flow<List<BallModel>> = callbackFlow {
        val listener = db.collection("balls")
            .whereEqualTo("drawId", drawId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                try {
                    val balls = snapshot?.documents
                        ?.mapNotNull { document ->
                            document.toObject(BallModel::class.java)
                        }
                        ?.sortedBy { it.order }
                        ?: emptyList()

                    trySend(balls)
                } catch (e: Exception) {
                    close(e)
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    // Contar coincidencias distintas, sin importar el orden.
    fun calculateMatches(
        play: PlayModel,
        balls: List<BallModel>
    ): Int {
        val ballNumbers = balls
            .map { it.ballNumber }
            .toSet()

        return play.numbers
            .toSet()
            .count { it in ballNumbers }
    }

    // Mantener compatibilidad con las llamadas existentes.
    // Usar solo cuando el sorteo terminó y llegaron las seis bolillas.
    fun determineResult(matches: Int): String {
        return if (matches == 6) {
            "GANADOR"
        } else {
            "SIGUE INTENTANDO"
        }
    }

    // Usar esta versión en la pantalla para controlar cuándo
    // se permite mostrar el resultado definitivo.
    fun determineResult(
        draw: DrawModel?,
        play: PlayModel?,
        balls: List<BallModel>
    ): String {
        if (draw == null) {
            return "Esperando datos del sorteo"
        }

        return when (draw.status) {
            "abierto" -> "Esperando el inicio del sorteo"

            "en curso", "en_curso" -> "Sorteo en curso"

            "finalizado" -> {
                val completeBalls =
                    balls.size == 6 &&
                            balls.map { it.ballNumber }.toSet().size == 6 &&
                            balls.all { it.ballNumber in 1..45 } &&
                            balls.map { it.order }.toSet() == (1..6).toSet()

                val validPlay =
                    play != null &&
                            play.drawId == draw.drawId &&
                            play.numbers.size == 6 &&
                            play.numbers.toSet().size == 6 &&
                            play.numbers.all { it in 1..45 }

                if (!completeBalls) {
                    "Esperando resultados completos"
                } else if (!validPlay || play == null) {
                    "No tienes una jugada válida para este sorteo"
                } else {
                    determineResult(calculateMatches(play, balls))
                }
            }

            else -> "Estado del sorteo desconocido"
        }
    }
}