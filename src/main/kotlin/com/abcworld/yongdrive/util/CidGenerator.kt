package com.abcworld.yongdrive.util

import org.springframework.stereotype.Component
import java.security.SecureRandom

@Component
class CidGenerator {

    private val random = SecureRandom()

    fun generate(length: Int = 32): String {
        val sb = StringBuilder(length)
        repeat(length) {
            sb.append(ALPHABET[random.nextInt(ALPHABET.size)])
        }
        return sb.toString()
    }

    companion object {
        private val ALPHABET: CharArray =
            (('0'..'9') + ('A'..'Z') + ('a'..'z')).toCharArray()
    }
}
