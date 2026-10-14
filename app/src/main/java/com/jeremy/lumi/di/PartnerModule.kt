package com.jeremy.lumi.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging
import com.jeremy.lumi.BuildConfig
import com.jeremy.lumi.data.repository.PartnerRepositoryImpl
import com.jeremy.lumi.domain.repository.PartnerRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Proveedores de Firebase para el Modo Pareja.
 *
 * Configurados para conectarse automáticamente a Firebase Emulator Suite
 * en la variante localDebug (BuildConfig.USE_FIREBASE_EMULATOR = true).
 *
 * IMPORTANTE: nadie más debe llamar a FirebaseFirestore.getInstance()
 * directamente — la configuración de caché y emulador solo puede aplicarse ANTES
 * del primer uso de la instancia, y este proveedor es el único punto de entrada.
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        val auth = FirebaseAuth.getInstance()
        if (BuildConfig.USE_FIREBASE_EMULATOR) {
            auth.useEmulator(BuildConfig.EMULATOR_HOST, 9099)
        }
        return auth
    }

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        val firestore = FirebaseFirestore.getInstance()
        if (BuildConfig.USE_FIREBASE_EMULATOR) {
            firestore.useEmulator(BuildConfig.EMULATOR_HOST, 8080)
            firestore.firestoreSettings = FirebaseFirestoreSettings.Builder(firestore.firestoreSettings)
                .setLocalCacheSettings(
                    PersistentCacheSettings.newBuilder()
                        .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                        .build()
                )
                .build()
        } else {
            firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(
                    PersistentCacheSettings.newBuilder()
                        .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                        .build()
                )
                .build()
        }
        return firestore
    }

    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions {
        val functions = FirebaseFunctions.getInstance("us-central1")
        if (BuildConfig.USE_FIREBASE_EMULATOR) {
            functions.useEmulator(BuildConfig.EMULATOR_HOST, 5001)
        }
        return functions
    }

    @Provides
    @Singleton
    fun provideFirebaseMessaging(): FirebaseMessaging = FirebaseMessaging.getInstance()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PartnerModule {

    @Binds
    @Singleton
    abstract fun bindPartnerRepository(impl: PartnerRepositoryImpl): PartnerRepository
}
