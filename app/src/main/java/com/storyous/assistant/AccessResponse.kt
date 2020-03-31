package com.storyous.assistant

import java.util.Date

data class Field(
    val stringValue: String
)

data class Fields(
    val token: Field,
    val validUntil: Field
)

data class AccessResponse(
    val fields: Fields,
    val createTime: Date
)
