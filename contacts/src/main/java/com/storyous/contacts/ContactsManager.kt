package com.storyous.contacts

import com.auth0.android.jwt.DecodeException
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import timber.log.Timber

class ContactsManager(
    private val repository: ContactsRepository = ContactsRepository(),
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        const val FIREBASE_TIMEOUT = 20000L
    }

    private val autStateListener = FirebaseAuth.AuthStateListener {}
    private var merchantId: String? = null
    private var placeId: String? = null

    init {
        runCatching {
            firebaseAuth.currentUser?.getIdToken(false)?.result?.token?.let {
                val claims = decodeToken(it)

                merchantId = claims.merchantId
                placeId = claims.placeId
                Timber.d("App is authorized for merchantId=$merchantId placeId=$placeId.")
            }
        }.onFailure {
            Timber.d(it, "App is not authorized.")
        }
    }

    @Throws(IllegalArgumentException::class)
    suspend fun getContacts(phoneNumber: String) = withTimeout(FIREBASE_TIMEOUT) {
        repository.getContacts(
            requireNotNull(merchantId),
            requireNotNull(placeId),
            phoneNumber
        ).also { Timber.d("ContactsManager get $it") }
    }

    @Throws(IllegalArgumentException::class)
    suspend fun updateContact(contact: Contact) = withTimeout(FIREBASE_TIMEOUT) {
        repository.updateContact(
            requireNotNull(merchantId),
            requireNotNull(placeId),
            contact
        )
    }

    @Throws(IllegalArgumentException::class)
    suspend fun updateIncomingCalls(incomingCall: IncomingCall) =
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

    @Throws
    suspend fun authenticate(token: String) = withContext(Dispatchers.IO) {
        runCatching {
            firebaseAuth.signInWithCustomToken(token).await()
            decodeToken(token)
        }.onFailure {
            this@ContactsManager.merchantId = null
            this@ContactsManager.placeId = null
            Timber.e(it, "Authorization fail. token=$token")
        }.onSuccess {
            this@ContactsManager.merchantId = it.merchantId
            this@ContactsManager.placeId = it.placeId
            Timber.d("Authorization success. merchantId=$merchantId placeId=$placeId")
        }.getOrThrow()

        Unit
    }

    @Throws(IllegalStateException::class, DecodeException::class)
    private fun decodeToken(token: String): JWTClaims {
        return JWTWrapper.decode(token)
            .takeIf { it.merchantId != null && it.placeId != null }
            ?: throw IllegalStateException("JWT token not contains merchantId or placeId.")
    }

    fun signOut() {
        firebaseAuth.signOut()
    }
}
