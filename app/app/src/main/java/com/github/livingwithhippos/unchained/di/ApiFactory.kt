package com.github.livingwithhippos.unchained.di

import android.content.SharedPreferences
import com.github.livingwithhippos.unchained.BuildConfig
import com.github.livingwithhippos.unchained.data.remote.AuthApiHelper
import com.github.livingwithhippos.unchained.data.remote.AuthApiHelperImpl
import com.github.livingwithhippos.unchained.data.remote.AuthenticationApi
import com.github.livingwithhippos.unchained.data.remote.CustomDownload
import com.github.livingwithhippos.unchained.data.remote.CustomDownloadHelper
import com.github.livingwithhippos.unchained.data.remote.CustomDownloadHelperImpl
import com.github.livingwithhippos.unchained.data.remote.StreamingApi
import com.github.livingwithhippos.unchained.data.remote.StreamingApiHelper
import com.github.livingwithhippos.unchained.data.remote.StreamingApiHelperImpl
import com.github.livingwithhippos.unchained.data.remote.TorrentApiHelper
import com.github.livingwithhippos.unchained.data.remote.TorrentApiHelperImpl
import com.github.livingwithhippos.unchained.data.remote.TorrentsApi
import com.github.livingwithhippos.unchained.data.remote.UpdateApi
import com.github.livingwithhippos.unchained.data.remote.UpdateApiHelper
import com.github.livingwithhippos.unchained.data.remote.UpdateApiHelperImpl
import com.github.livingwithhippos.unchained.data.remote.UserApi
import com.github.livingwithhippos.unchained.data.remote.UserApiHelper
import com.github.livingwithhippos.unchained.data.remote.UserApiHelperImpl
import com.github.livingwithhippos.unchained.data.remote.WebDownloadApi
import com.github.livingwithhippos.unchained.data.remote.WebDownloadApiHelper
import com.github.livingwithhippos.unchained.data.remote.WebDownloadApiHelperImpl
import com.github.livingwithhippos.unchained.plugins.Parser
import com.github.livingwithhippos.unchained.utilities.BASE_URL
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.net.InetAddress
import javax.inject.Singleton
import okhttp3.ConnectionSpec
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

/** This object manages the Dagger-Hilt injection for the OkHttp and Retrofit clients */
@InstallIn(SingletonComponent::class)
@Module
object ApiFactory {

    @Provides
    @Singleton
    @ClassicClient
    fun provideOkHttpClient(): OkHttpClient {
        if (BuildConfig.DEBUG) {
            val logInterceptor: HttpLoggingInterceptor =
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
            // HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.HEADERS }

            return OkHttpClient()
                .newBuilder()
                // should fix the javax.net.ssl.SSLHandshakeException: Failure in SSL library
                .connectionSpecs(
                    listOf(
                        ConnectionSpec.CLEARTEXT,
                        ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                            .allEnabledTlsVersions()
                            .allEnabledCipherSuites()
                            .build(),
                    )
                )
                // logs all the calls, removed in the release channel
                .addInterceptor(logInterceptor)
                .build()
        } else
            return OkHttpClient()
                .newBuilder()
                .connectionSpecs(
                    listOf(
                        ConnectionSpec.CLEARTEXT,
                        ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                            .allEnabledTlsVersions()
                            .allEnabledCipherSuites()
                            .build(),
                    )
                )
                .build()
    }

    /**
     * examples:
     * [https://github.com/square/okhttp/blob/master/okhttp-dnsoverhttps/src/test/java/okhttp3/dnsoverhttps/DohProviders.java]
     * list: [https://github.com/curl/curl/wiki/DNS-over-HTTPS]
     *
     * @return
     */
    @Provides
    @Singleton
    @DOHClient
    fun provideDOHClient(preferences: SharedPreferences): OkHttpClient {

        val bootstrapClient: OkHttpClient =
            if (BuildConfig.DEBUG) {

                val logInterceptor: HttpLoggingInterceptor =
                    HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
                // HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.HEADERS }

                OkHttpClient()
                    .newBuilder()
                    .connectionSpecs(
                        listOf(
                            ConnectionSpec.CLEARTEXT,
                            ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                                .allEnabledTlsVersions()
                                .allEnabledCipherSuites()
                                .build(),
                        )
                    )
                    // logs all the calls, removed in the release channel
                    .addInterceptor(logInterceptor)
                    .build()
            } else {
                OkHttpClient()
                    .newBuilder()
                    .connectionSpecs(
                        listOf(
                            ConnectionSpec.CLEARTEXT,
                            ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                                .allEnabledTlsVersions()
                                .allEnabledCipherSuites()
                                .build(),
                        )
                    )
                    .build()
            }

        val dohProvider = preferences.getString("doh_provider", "quad9") ?: "quad9"

        val dns =
            when (dohProvider) {
                "google" ->
                    DnsOverHttps.Builder()
                        .client(bootstrapClient)
                        .url("https://dns.google/dns-query".toHttpUrl())
                        .bootstrapDnsHosts(
                            InetAddress.getByName("8.8.8.8"),
                            InetAddress.getByName("8.8.4.4"),
                        )
                        // we noticed ipv6 was checked first and then failed for a plugin
                        .includeIPv6(false)
                        .build()
                "cloudflare" ->
                    DnsOverHttps.Builder()
                        .client(bootstrapClient)
                        .url("https://cloudflare-dns.com/dns-query".toHttpUrl())
                        .bootstrapDnsHosts(InetAddress.getByName("1.1.1.1"))
                        .includeIPv6(false)
                        .build()
                "quad9" ->
                    DnsOverHttps.Builder()
                        .client(bootstrapClient)
                        .url("https://dns.quad9.net/dns-query".toHttpUrl())
                        .bootstrapDnsHosts(
                            InetAddress.getByName("9.9.9.9"),
                            InetAddress.getByName("149.112.112.112"),
                        )
                        .includeIPv6(false)
                        .build()
                "mullvad" ->
                    DnsOverHttps.Builder()
                        .client(bootstrapClient)
                        .url("https://dns.mullvad.net/dns-query".toHttpUrl())
                        .bootstrapDnsHosts(InetAddress.getByName("194.242.2.2"))
                        .includeIPv6(false)
                        .build()
                else ->
                    DnsOverHttps.Builder()
                        .client(bootstrapClient)
                        .url("https://dns.quad9.net/dns-query".toHttpUrl())
                        .bootstrapDnsHosts(
                            InetAddress.getByName("9.9.9.9"),
                            InetAddress.getByName("149.112.112.112"),
                        )
                        .includeIPv6(false)
                        .build()
            }

        return bootstrapClient.newBuilder().dns(dns).build()
    }

    @Provides
    @Singleton
    @ApiRetrofit
    fun apiRetrofit(@ClassicClient okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(BASE_URL)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }

    // authentication api injection
    @Provides
    @Singleton
    fun provideAuthenticationApi(@ApiRetrofit retrofit: Retrofit): AuthenticationApi {
        return retrofit.create(AuthenticationApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAuthenticationApiHelper(apiHelper: AuthApiHelperImpl): AuthApiHelper = apiHelper

    // user api injection
    @Provides
    @Singleton
    fun provideUserApi(@ApiRetrofit retrofit: Retrofit): UserApi {
        return retrofit.create(UserApi::class.java)
    }

    @Provides
    @Singleton
    fun provideUserApiHelper(apiHelper: UserApiHelperImpl): UserApiHelper = apiHelper

    // web download (unrestrict) api injection
    @Provides
    @Singleton
    fun provideWebDownloadApi(@ApiRetrofit retrofit: Retrofit): WebDownloadApi {
        return retrofit.create(WebDownloadApi::class.java)
    }

    @Provides
    @Singleton
    fun provideWebDownloadApiHelper(apiHelper: WebDownloadApiHelperImpl): WebDownloadApiHelper =
        apiHelper

    // streaming api injection
    @Provides
    @Singleton
    fun provideStreamingApi(@ApiRetrofit retrofit: Retrofit): StreamingApi {
        return retrofit.create(StreamingApi::class.java)
    }

    @Provides
    @Singleton
    fun provideStreamingApiHelper(apiHelper: StreamingApiHelperImpl): StreamingApiHelper = apiHelper

    // torrent api injection
    @Provides
    @Singleton
    fun provideTorrentsApi(@ApiRetrofit retrofit: Retrofit): TorrentsApi {
        return retrofit.create(TorrentsApi::class.java)
    }

    @Provides
    @Singleton
    fun provideTorrentsApiApiHelper(apiHelper: TorrentApiHelperImpl): TorrentApiHelper = apiHelper

    // update api injection
    @Provides
    @Singleton
    fun provideUpdateApi(@ApiRetrofit retrofit: Retrofit): UpdateApi {
        return retrofit.create(UpdateApi::class.java)
    }

    @Provides
    @Singleton
    fun provideUpdateApiHelper(apiHelper: UpdateApiHelperImpl): UpdateApiHelper = apiHelper

    // custom download injection
    @Provides
    @Singleton
    fun provideCustomDownload(@ApiRetrofit retrofit: Retrofit): CustomDownload {
        return retrofit.create(CustomDownload::class.java)
    }

    @Provides
    @Singleton
    fun provideCustomDownloadHelper(customHelper: CustomDownloadHelperImpl): CustomDownloadHelper =
        customHelper

    /** Search Plugins stuff */
    @Provides
    @Singleton
    fun provideParser(
        preferences: SharedPreferences,
        @ClassicClient classicClient: OkHttpClient,
        @DOHClient dohClient: OkHttpClient,
    ): Parser = Parser(preferences, classicClient, dohClient)
}
