package com.storyous.contacts

import kotlinx.coroutines.withTimeout
import timber.log.Timber

class ContactsManager(
    private val merchantId: String,
    private val placeId: String
) {
    companion object {
        const val FIREBASE_TIMEOUT = 20000L
    }

    private val repository = ContactsRepository()

    suspend fun getContacts(phoneNumber: String): List<Contact> = withTimeout(FIREBASE_TIMEOUT) {
        repository.getContacts(merchantId, placeId, phoneNumber).also {
            Timber.d("ContactsManager get $it")
        }
    }

    suspend fun updateContact(contact: Contact): Unit = withTimeout(FIREBASE_TIMEOUT) {
        repository.updateContact(merchantId, placeId, contact)
    }
}
