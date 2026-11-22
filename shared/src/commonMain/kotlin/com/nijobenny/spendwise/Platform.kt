package com.nijobenny.spendwise

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform