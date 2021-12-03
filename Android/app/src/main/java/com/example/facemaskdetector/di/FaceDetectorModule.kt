package com.example.facemaskdetector.di

import android.app.Application
import com.example.facemaskdetector.ml.FackMaskDetection
import com.example.facemaskdetector.oriclass.BitmapOutputAnalysis
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import org.tensorflow.lite.support.model.Model
import javax.inject.Singleton


@Module
@InstallIn(ApplicationComponent::class)
class FaceDetectorModule {
    private val options: Model.Options by lazy {
        Model.Options.Builder().setDevice(Model.Device.GPU).setNumThreads(5).build()
    }

    @Provides
    @Singleton
    fun provideFaceMaskDetector(context: Application): FackMaskDetection {
        return  FackMaskDetection.newInstance(context, options)
    }

    @Provides
    @Singleton
    fun provideAnalyst(context: Application): BitmapOutputAnalysis {
        return BitmapOutputAnalysis(context)
    }

}