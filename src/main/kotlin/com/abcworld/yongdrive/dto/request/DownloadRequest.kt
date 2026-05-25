package com.abcworld.yongdrive.dto.request

data class DownloadRequest(
    val bucket: String = "",
    val path: String = "",
    val filenames: List<String> = emptyList(),
)
