package com.example.facemaskdetector.di

import android.app.Application
import com.example.facemaskdetector.const.ClientSetting
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import org.eclipse.paho.android.service.MqttAndroidClient
import timber.log.Timber
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
class MQTTModule {

    @Provides
    @Singleton
    fun provideClient(context: Application): MqttAndroidClient {
        return MqttAndroidClient(context, ClientSetting.serverURI, ClientSetting.clientID)
    }

}