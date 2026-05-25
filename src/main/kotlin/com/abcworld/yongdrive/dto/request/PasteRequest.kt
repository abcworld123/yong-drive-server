package com.abcworld.yongdrive.dto.request

data class PasteRequest(
    val bucket: String = "",
    val pathFrom: String = "",
    val pathTo: String = "",
    val objects: List<String> = emptyList(),
    val mode: String = "copy",
) {
    companion object {
        const val MODE_COPY = "copy"
        const val MODE_CUT = "cut"
    }
}
