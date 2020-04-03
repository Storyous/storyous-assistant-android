package com.storyous.contacts

import com.auth0.jwt.JWT
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import timber.log.Timber

class ContactsManager(
    private val repository: ContactsRepository = ContactsRepository()
) {
    companion object {
        const val FIREBASE_TIMEOUT = 20000L
    }

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val autStateListener = FirebaseAuth.AuthStateListener {}
    private var merchantId: String? = null
    private var placeId: String? = null

    init {
        runCatching {
            firebaseAuth.currentUser?.getIdToken(false)?.result?.token?.let {
                val (merchantId, placeId) = decodeToken(it)

                this.merchantId = merchantId
                this.placeId = placeId
                Timber.d("App is authorized for merchantId=$merchantId placeId=$placeId.")
            }
        }.onFailure {
            Timber.d(it, "App is not authorized.")
        }
    }

    @Throws(IllegalArgumentException::class)
    suspend fun getContacts(phoneNumber: String): List<Contact> = withTimeout(FIREBASE_TIMEOUT) {
        repository.getContacts(
            requireNotNull(merchantId),
            requireNotNull(placeId),
            phoneNumber
        ).also { Timber.d("ContactsManager get $it") }
    }

    @Throws(IllegalArgumentException::class)
    suspend fun updateContact(contact: Contact): Unit = withTimeout(FIREBASE_TIMEOUT) {
        repository.updateContact(
            requireNotNull(merchantId),
            requireNotNull(placeId),
            contact
        )
    }

    @Throws(IllegalArgumentException::class)
    suspend fun updateIncomingCalls(incomingCall: IncomingCall): Unit =
        withTimeout(FIREBASE_TIMEOUT) {
            repository.updateIncomingCalls(
                requireNotNull(merchantId),
                requireNotNull(placeId),
                incomingCall
            )
        }

    fun setAuthListener(listener: (Boolean) -> Unit) = firebaseAuth.addAuthStateListener {
        listener(it.currentUser != null)
    }

    fun removeAuthListener() = firebaseAuth.removeAuthStateListener(autStateListener)

    fun isAuthenticated() = firebaseAuth.currentUser != null

    @Throws(IllegalStateException::class)
    suspend fun authenticate(token: String) = withContext(Dispatchers.IO) {
        val (merchantId, placeId) = decodeToken(token)

        runCatching {
            firebaseAuth.signInWithCustomToken(token).await()
        }.onFailure {
            this@ContactsManager.merchantId = null
            this@ContactsManager.placeId = null
            Timber.e(it, "Authorization fail. merchantId=$merchantId placeId=$placeId")
        }.onSuccess {
            this@ContactsManager.merchantId = merchantId
            this@ContactsManager.placeId = placeId
            Timber.d("Authorization success. merchantId=$merchantId placeId=$placeId")
        }.getOrThrow()

        Unit
    }

    @Throws(IllegalStateException::class)
    private fun decodeToken(token: String): Pair<String, String> {
        val decode = JWT.decode(token)
        val claims: Map<String, Any>? = decode.getClaim("claims").asMap()
        val merchantId = claims?.get("merchantId") as? String
            ?: decode.getClaim("merchantId").asString()
        val placeId = claims?.get("placeId") as? String
            ?: decode.getClaim("placeId").asString()

        if (merchantId == null || placeId == null) {
            throw IllegalStateException("Some of properties are null. merchantId=$merchantId placeId=$placeId")
        }

        return merchantId to placeId
    }

    fun signOut() {
        firebaseAuth.signOut()
    }
}
