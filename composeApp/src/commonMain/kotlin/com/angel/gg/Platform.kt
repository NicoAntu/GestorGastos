package com.angel.gg

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform