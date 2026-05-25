package com.abcworld.yongdrive.entity

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class BucketInfo(
    @field:JsonProperty("Name")
    val name: String,
    @field:JsonProperty("CreationDate")
    val creationDate: Instant,
)
