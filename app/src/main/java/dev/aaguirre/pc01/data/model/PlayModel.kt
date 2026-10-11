package dev.aaguirre.pc01.data.model

data class PlayModel(
    val drawId: String = "",
    val userId: String = "",
    val numbers: List<Int> = emptyList()  // 6 números
)

