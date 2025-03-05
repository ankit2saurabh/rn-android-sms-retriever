package com.rnandroidsmsretriever

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.Promise

abstract class RnAndroidSmsRetrieverSpec internal constructor(context: ReactApplicationContext) :
  ReactContextBaseJavaModule(context) {

  abstract fun getOtp(otpLength: Double, phoneNumber: String?, promise: Promise)

  abstract fun getSms(phoneNumber: String?, promise: Promise)
}
