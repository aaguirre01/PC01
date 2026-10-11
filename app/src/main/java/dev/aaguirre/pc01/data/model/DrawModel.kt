package dev.aaguirre.pc01.data.model

data class DrawModel(
    val drawId: String = "",
    val name: String = "",
    val date: String = "",
    val status: String = "abierto"  // "abierto", "en_curso", "finalizado"
)
