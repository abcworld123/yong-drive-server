package com.abcworld.yongdrive.dto.request

data class CreateFolderRequest(
    val bucket: String = "",
    val path: String = "",
    val foldername: String = "",
)
