package com.storyous.assistant

import java.util.Date

data class IntField(
    val integerValue: Long
)

data class StringField(
    val stringValue: String
)

data class Fields(
    val token: StringField,
    val validUntil: IntField
)

data class AccessResponse(
    val fields: Fields,
    val createTime: Date
)
