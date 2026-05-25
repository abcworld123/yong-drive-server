package com.abcworld.yongdrive.dto.request

data class DeleteRequest(
    val bucket: String = "",
    val path: String = "",
    val objects: List<String> = emptyList(),
)
