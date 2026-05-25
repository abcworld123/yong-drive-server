package com.abcworld.yongdrive.entity

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ObjectInfo(
    val type: String,
    val name: String,
    val size: Long? = null,
) {
    companion object {
        const val TYPE_FILE = "file"
        const val TYPE_FOLDER = "folder"
    }
}
