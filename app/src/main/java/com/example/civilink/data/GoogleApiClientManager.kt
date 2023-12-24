package com.example.civilink.data

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.example.civilink.R
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.GoogleApiClient

class GoogleApiClientManager private constructor(context: Context) {
    private val googleApiClient: GoogleApiClient

    init {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleApiClient = GoogleApiClient.Builder(context)
            .enableAutoManage(context as AppCompatActivity) {}
            .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
            .build()
    }

    companion object {
        @Volatile
        private var instance: GoogleApiClientManager? = null

        fun getInstance(context: Context): GoogleApiClientManager =
            instance ?: synchronized(this) {
                instance ?: GoogleApiClientManager(context).also { instance = it }
            }
    }

    fun getGoogleApiClient(): GoogleApiClient {
        return googleApiClient
    }
}
