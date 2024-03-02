package io.romix.chat.network

import android.content.Context
import com.haroldadmin.cnradapter.NetworkResponseAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.romix.chat.R
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

@Module
@InstallIn(SingletonComponent::class)
class NetworkModule {

    companion object {
        const val CLUSTER_IP_ADDRESS = "192.168.0.106"
    }

//    @Provides
//    fun provideAuthInterceptorOkHttpClient(): OkHttpClient {
//        return OkHttpClient.Builder()
//            .addInterceptor(HttpLoggingInterceptor()
//                .apply { level = HttpLoggingInterceptor.Level.BODY })
//            .build()
//    }

    @Provides
    fun createOkHttpWithTrustManager(@ApplicationContext context: Context): OkHttpClient {
        val certificateFactory = CertificateFactory.getInstance("X.509")

        // Replace R.raw.your_certificate with your certificate resource ID
        val certificateInputStream = context.resources.openRawResource(R.raw.certificate)
        val certificate = certificateInputStream.use {
            certificateFactory.generateCertificate(it)
        }

        val keyStoreType = KeyStore.getDefaultType()
        val keyStore = KeyStore.getInstance(keyStoreType).apply {
            load(null, null)
            setCertificateEntry("ca", certificate)
        }

        val trustManagerFactoryAlgorithm = TrustManagerFactory.getDefaultAlgorithm()
        val trustManagerFactory = TrustManagerFactory.getInstance(trustManagerFactoryAlgorithm).apply {
            init(keyStore)
        }

        val trustManagers = trustManagerFactory.trustManagers
        if (trustManagers.size != 1 || trustManagers[0] !is X509TrustManager) {
            throw IllegalStateException("Unexpected default trust managers:" + trustManagers.contentToString())
        }

        val trustManager = trustManagers[0] as X509TrustManager
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(trustManager), null)
        }

        return OkHttpClient.Builder()
            .hostnameVerifier { _, _ -> true } // For development only!
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .addInterceptor(HttpLoggingInterceptor()
                .apply { level = HttpLoggingInterceptor.Level.BODY })
            .build()
    }

    @Provides
    fun provideAnalyticsService(
        okHttpClient: OkHttpClient,
    ): BackendService {
        return Retrofit.Builder()
            .baseUrl("https://$CLUSTER_IP_ADDRESS/")
            .client(okHttpClient)
            .addCallAdapterFactory(NetworkResponseAdapterFactory())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create()
    }
}