package dev.aaguirre.pc01.presentation.tracking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.aaguirre.pc01.data.model.BallModel
import dev.aaguirre.pc01.data.model.DrawModel
import dev.aaguirre.pc01.data.model.PlayModel
import dev.aaguirre.pc01.data.remote.FirebaseAuthManager
import dev.aaguirre.pc01.data.remote.FirestoreManager
import kotlinx.coroutines.flow.collectLatest

@Composable
fun TrackingScreen(drawId: String) {
    var draw by remember { mutableStateOf<DrawModel?>(null) }
    var balls by remember { mutableStateOf<List<BallModel>>(emptyList()) }
    var play by remember { mutableStateOf<PlayModel?>(null) }
    var matches by remember { mutableStateOf(0) }
    var result by remember { mutableStateOf("") }

    val userId = FirebaseAuthManager.getCurrentUser()?.uid ?: ""

    // Cargar jugada del usuario
    LaunchedEffect(drawId, userId) {
        if (userId.isNotEmpty()) {
            val result = FirestoreManager.getUserPlay(drawId, userId)
            if (result.isSuccess) {
                play = result.getOrNull()
            }
        }
    }

    // Escuchar cambios en el sorteo (tiempo real)
    LaunchedEffect(drawId) {
        FirestoreManager.listenToDraw(drawId).collectLatest { drawData ->
            draw = drawData
        }
    }

    // Escuchar bolillas publicadas (tiempo real)
    LaunchedEffect(drawId) {
        FirestoreManager.listenToBalls(drawId).collectLatest { ballList ->
            balls = ballList

            // Recalcular aciertos cuando llegan nuevas bolillas
            play?.let { currentPlay ->
                matches = FirestoreManager.calculateMatches(currentPlay, ballList)

                // Mostrar resultado solo cuando el sorteo esté finalizado y haya 6 bolillas
                if (draw?.status == "finalizado" && ballList.size == 6) {
                    result = FirestoreManager.determineResult(matches)
                } else {
                    result = ""
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Seguimiento del Sorteo",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Estado del sorteo
        draw?.let { currentDraw ->
            Text(
                "Estado: ${currentDraw.status}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Números de la jugada del usuario
        play?.let { currentPlay ->
            Text(
                "Tu jugada:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                currentPlay.numbers.forEach { number ->
                    val isMatch = balls.any { it.ballNumber == number }
                    Text(
                        "$number ",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isMatch) Color.Green else Color.Black,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
        } ?: Text("No has registrado una jugada")

        Spacer(modifier = Modifier.height(24.dp))

        // Bolillas publicadas
        Text(
            "Bolillas publicadas: ${balls.size}/6",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            balls.sortedBy { it.order }.forEach { ball ->
                Text(
                    "${ball.ballNumber} ",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Aciertos
        if (play != null) {
            Text(
                "Aciertos: $matches",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Resultado final
        if (result.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        result,
                        style = MaterialTheme.typography.headlineLarge,
                        color = if (result == "GANADOR") Color.Green else Color.Red
                    )
                }
            }
        } else if (draw?.status == "en_curso") {
            Text(
                "Sorteo en curso...",
                style = MaterialTheme.typography.bodyLarge
            )
        } else if (draw?.status == "abierto") {
            Text(
                "Esperando inicio del sorteo...",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}