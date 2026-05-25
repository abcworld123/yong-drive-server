package com.abcworld.yongdrive.controller

import com.abcworld.yongdrive.dto.response.ApiResponse
import com.abcworld.yongdrive.service.BucketService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/s3/bucket")
class BucketController(
    private val bucketService: BucketService,
) {

    @PostMapping("/get")
    fun get(): Mono<ApiResponse> =
        bucketService.listBuckets().map { ApiResponse.buckets(it) }
}
