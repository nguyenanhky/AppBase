package com.viettel.appbase.core.di

import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.viettel.appbase.core.playstore.InAppReviewController
import com.viettel.appbase.core.playstore.InAppUpdateController
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val playStoreModule = module {
    single<ReviewManager> { ReviewManagerFactory.create(androidContext()) }
    single<AppUpdateManager> { AppUpdateManagerFactory.create(androidContext()) }
    single { InAppReviewController(get(), get()) }
    single { InAppUpdateController(get()) }
}
