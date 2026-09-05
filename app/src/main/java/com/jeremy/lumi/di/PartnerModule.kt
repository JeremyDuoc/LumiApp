package com.jeremy.lumi.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.messaging.FirebaseMessaging
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
 * IMPORTANTE: nadie más debe llamar a FirebaseFirestore.getInstance()
 * directamente — la configuración de caché solo puede aplicarse ANTES del
 * primer uso de la instancia, y este proveedor es el único punto de entrada.
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        val firestore = FirebaseFirestore.getInstance()
        // Caché offline persistente sin límite de tamaño: los vínculos y el
        // diario siguen visibles sin red, y las escrituras quedan en cola
        // hasta que vuelva la conexión.
        firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(
                PersistentCacheSettings.newBuilder()
                    .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build()
            )
            .build()
        return firestore
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
