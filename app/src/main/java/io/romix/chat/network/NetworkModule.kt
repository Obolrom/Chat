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
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

@Module
@InstallIn(SingletonComponent::class)
class NetworkModule {

    companion object {
        private const val CLUSTER_IP_ADDRESS = "192.168.0.106"

        // note: for plain http should be used `ws` not `wss`
        private const val WEB_SOCKET_ENDPOINT = "wss://$CLUSTER_IP_ADDRESS/gs-guide-websocket"
    }

    // for plain HTTP
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

        // Put certificate.pem in R.raw.certificate
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
    fun provideChatClient(okHttpClient: OkHttpClient): StompClient {
        return Stomp.over(
            Stomp.ConnectionProvider.OKHTTP,
            WEB_SOCKET_ENDPOINT,
            mapOf(),
            okHttpClient
        )
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