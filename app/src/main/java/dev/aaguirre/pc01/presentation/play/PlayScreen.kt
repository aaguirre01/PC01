package dev.aaguirre.pc01.presentation.play

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.aaguirre.pc01.data.model.PlayModel
import dev.aaguirre.pc01.data.remote.FirebaseAuthManager
import dev.aaguirre.pc01.data.remote.FirestoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun PlayScreen(drawId: String, navController: NavController) {
    val selectedNumbers = remember { mutableStateListOf<Int>() }
    var currentNumber by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Registrar Jugada",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            "Selecciona 6 números distintos (1-45)",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Mostrar números seleccionados
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            selectedNumbers.forEach { number ->
                Text(
                    "$number ",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Campo para ingresar número
        OutlinedTextField(
            value = currentNumber,
            onValueChange = { currentNumber = it },
            label = { Text("Número (1-45)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Botón agregar número
        Button(
            onClick = {
                val number = currentNumber.toIntOrNull()

                if (number == null || number < 1 || number > 45) {
                    Toast.makeText(context, "Número inválido (1-45)", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (number in selectedNumbers) {
                    Toast.makeText(context, "Número ya seleccionado", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (selectedNumbers.size >= 6) {
                    Toast.makeText(context, "Ya seleccionaste 6 números", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                selectedNumbers.add(number)
                currentNumber = ""
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = currentNumber.isNotEmpty() && selectedNumbers.size < 6
        ) {
            Text("Agregar Número")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón guardar jugada
        Button(
            onClick = {
                val userId = FirebaseAuthManager.getCurrentUser()?.uid ?: return@Button

                val play = PlayModel(
                    drawId = drawId,
                    userId = userId,
                    numbers = selectedNumbers.toList()
                )

                CoroutineScope(Dispatchers.Main).launch {
                    val result = FirestoreManager.savePlay(play)
                    if (result.isSuccess) {
                        Toast.makeText(context, "Jugada guardada", Toast.LENGTH_SHORT).show()
                        navController.navigate("tracking/$drawId")
                    } else {
                        val exception = result.exceptionOrNull()
                        Toast.makeText(
                            context,
                            exception?.message ?: "Error al guardar",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedNumbers.size == 6
        ) {
            Text("Guardar Jugada e Ir al Seguimiento")
        }
    }
}