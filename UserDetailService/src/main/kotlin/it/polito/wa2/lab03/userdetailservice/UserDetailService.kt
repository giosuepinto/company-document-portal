package it.polito.wa2.lab03.userdetailservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class UserDetailService

fun main(args: Array<String>) {
    runApplication<UserDetailService>(*args)
}
