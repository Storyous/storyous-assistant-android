package com.storyous.contacts

class Contact(
    val phoneNumber: String,
    val name: String,
    val streetAddress: String,
    val city: String
) {
    constructor() : this("", "", "", "")

    override fun toString(): String {
        return "Contact(telNumber='$phoneNumber', name='$name', streetAddress='$streetAddress', city='$city')"
    }
}
