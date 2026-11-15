package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
    trySend(firebaseAuth.currentUser)
  }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

private fun resolveWebClientId(context: Context): String? {
  val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
  if (resId == 0) return null
  return context.getString(resId).trim().takeIf { it.isNotEmpty() }
}

private fun getFirebaseAuthOrNull(context: Context): FirebaseAuth? {
  return runCatching {
    if (FirebaseApp.getApps(context).isEmpty()) {
      FirebaseApp.initializeApp(context) ?: return null
    }
    Firebase.auth
  }.getOrNull()
}

fun attemptAutoSignIn(
  context: Context,
  credentialManager: CredentialManager?,
  onAuthSuccess: () -> Unit,
  onUnauthenticated: () -> Unit,
  scope: CoroutineScope,
) {
  val auth = getFirebaseAuthOrNull(context)
  if (auth == null) {
    onUnauthenticated()
    return
  }
  if (auth.currentUser != null) {
    onAuthSuccess()
    return
  }
  if (credentialManager == null) {
    onUnauthenticated()
    return
  }
  val clientId = resolveWebClientId(context)
  if (clientId == null) {
    onUnauthenticated()
    return
  }

  val googleIdOption = GetGoogleIdOption.Builder()
    .setFilterByAuthorizedAccounts(true)
    .setServerClientId(clientId)
    .setAutoSelectEnabled(true)
    .build()

  val request = GetCredentialRequest.Builder()
    .addCredentialOption(googleIdOption)
    .build()

  scope.launch {
    try {
      val result = credentialManager.getCredential(context, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        auth.signInWithCredential(authCredential).await()
        onAuthSuccess()
      } else {
        onUnauthenticated()
      }
    } catch (_: Exception) {
      onUnauthenticated()
    }
  }
}

fun onGoogleSignInClicked(
  context: Context,
  credentialManager: CredentialManager?,
  onAuthSuccess: () -> Unit,
  onAuthError: (String) -> Unit,
  scope: CoroutineScope,
  onAuthCancelled: () -> Unit = {},
) {
  val auth = getFirebaseAuthOrNull(context)
  if (auth == null) {
    onAuthError("Firebase कॉन्फ़िगरेशन (google-services.json) उपलब्ध नहीं है।")
    return
  }
  if (credentialManager == null) {
    onAuthError("इस डिवाइस पर Google Credential Manager उपलब्ध नहीं है।")
    return
  }
  val clientId = resolveWebClientId(context)
  if (clientId == null) {
    onAuthError("Google Sign-In कॉन्फ़िगरेशन (google-services.json) उपलब्ध नहीं है।")
    return
  }

  val activity = context as? Activity
  if (activity == null) {
    onAuthError("साइन इन के लिए वैध Activity संदर्भ आवश्यक है।")
    return
  }

  val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
  val request = GetCredentialRequest.Builder()
    .addCredentialOption(signInOption)
    .build()

  scope.launch {
    try {
      val result = credentialManager.getCredential(activity, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        auth.signInWithCredential(authCredential).await()
        onAuthSuccess()
      } else {
        onAuthError("अमान्य क्रेडेंशियल प्रकार प्राप्त हुआ।")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
      onAuthCancelled()
    } catch (e: Exception) {
      Log.e("Auth", "Google Sign-In failed", e)
      onAuthError(e.localizedMessage ?: "Google साइन इन विफल रहा।")
    }
  }
}

fun signOut(
  credentialManager: CredentialManager?,
  scope: CoroutineScope,
  onSignOutComplete: () -> Unit = {},
) {
  scope.launch {
    withContext(NonCancellable) {
      try {
        credentialManager?.clearCredentialState(ClearCredentialStateRequest())
      } catch (e: Exception) {
        Log.w("Auth", "Failed to clear credential state: ${e.message}")
      } finally {
        runCatching { Firebase.auth.signOut() }
        onSignOutComplete()
      }
    }
  }
}
