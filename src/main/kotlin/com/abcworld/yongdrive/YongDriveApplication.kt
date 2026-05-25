package com.abcworld.yongdrive

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class YongDriveApplication

fun main(args: Array<String>) {
    runApplication<YongDriveApplication>(*args)
}
