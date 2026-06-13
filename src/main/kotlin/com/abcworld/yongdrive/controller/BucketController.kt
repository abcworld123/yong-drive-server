package com.abcworld.yongdrive.controller

import com.abcworld.yongdrive.dto.response.ApiResponse
import com.abcworld.yongdrive.service.BucketService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/bucket")
@PreAuthorize("isAuthenticated()")
class BucketController(
    private val bucketService: BucketService,
) {

    @PostMapping("/get")
    suspend fun get(): ApiResponse = ApiResponse.buckets(bucketService.listBuckets())
}
