package com.storyous.contacts

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.tasks.await
import timber.log.Timber

internal class ContactsRepository(private val fireStore: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    companion object {
        const val FLD_PHONE_NUMBER = "phoneNumber"
    }

    suspend fun getContacts(
        merchantId: String,
        placeId: String,
        phoneNumber: String,
        limit: Long = 10
    ): List<Contact> {
        return try {
            fireStore.collection(collectionPath(merchantId, placeId))
                .orderBy(FLD_PHONE_NUMBER)
                .startsWith(FLD_PHONE_NUMBER, phoneNumber)
                .limit(limit)
                .get()
                .await()
                .toObjects()
        } catch (ex: FirebaseFirestoreException) {
            Timber.e(ex, "Firebase error while getting contacts")
            emptyList()
        }
    }

    suspend fun updateContact(
        merchantId: String,
        placeId: String,
        contact: Contact
    ) {
        try {
            fireStore.collection(collectionPath(merchantId, placeId))
                .document(contact.phoneNumber)
                .set(contact).await()
        } catch (ex: FirebaseFirestoreException) {
            Timber.e(ex, "Firebase error while adding contact $contact")
        }
    }

    private fun collectionPath(merchantId: String, placeId: String) =
        "merchants/$merchantId/places/$placeId/contacts"
}
