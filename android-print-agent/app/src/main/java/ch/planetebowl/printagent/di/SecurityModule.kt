package ch.planetebowl.printagent.di

import android.content.Context
import androidx.security.crypto.MasterKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    /**
     * Cle maitresse Android Keystore (AES256-GCM), extraite en @Provides plutot que
     * construite dans SecureTokenStore pour rester testable/remplacable independamment.
     */
    @Provides
    @Singleton
    fun provideMasterKey(@ApplicationContext context: Context): MasterKey =
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
}
