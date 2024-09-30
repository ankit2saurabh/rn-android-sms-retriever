package com.rnandroidsmsretriever

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.facebook.react.bridge.ActivityEventListener
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactMethod
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.rnandroidsmsretriever.utils.ConsentError
import com.rnandroidsmsretriever.utils.ConsentRequest
import com.rnandroidsmsretriever.utils.Logger
import com.rnandroidsmsretriever.utils.OTPRequest
import com.rnandroidsmsretriever.utils.SmsRequest
import java.util.regex.Matcher
import java.util.regex.Pattern

class RnAndroidSmsRetrieverModule internal constructor(
  private val reactContext: ReactApplicationContext
) : RnAndroidSmsRetrieverSpec(reactContext), ActivityEventListener {
  var TAG = "SMS_RETRIEVER"
  private lateinit var consentRequest: ConsentRequest
  private val SMS_CONSENT_REQUEST = 22071998

  override fun getName(): String {
    return NAME
  }

  init {
    reactContext.addActivityEventListener(this)
  }

  private val smsVerificationReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      if (SmsRetriever.SMS_RETRIEVED_ACTION == intent.action) {
        val extras = intent.extras
        val smsRetrieverStatus = extras?.get(SmsRetriever.EXTRA_STATUS) as Status

        when (smsRetrieverStatus.statusCode) {
          CommonStatusCodes.SUCCESS -> {
            // Get consent intent
            val consentIntent = extras.getParcelable<Intent>(SmsRetriever.EXTRA_CONSENT_INTENT)
            try {
              reactContext.startActivityForResult(
                consentIntent,
                SMS_CONSENT_REQUEST,
                null,
              )
            } catch (e: ActivityNotFoundException) {
              onError(
                e.localizedMessage,
                ConsentError.ActivityNotFound.code,
                Throwable(ConsentError.ActivityNotFound.code)
              )
            }
          }

          CommonStatusCodes.TIMEOUT -> {
            // Time out occurred, handle the error.
            onError(
              "Timeout, no message received!",
              ConsentError.Timeout.code,
              Throwable(ConsentError.Timeout.code)
            )
          }
        }
      }
    }
  }

  override fun onActivityResult(
    activity: Activity?, requestCode: Int, resultCode: Int, intent: Intent?
  ) {
    when (requestCode) {
      SMS_CONSENT_REQUEST -> if (resultCode == Activity.RESULT_OK && intent != null) {
        // Get SMS message content
        val message = intent.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)
        when (consentRequest) {
          is OTPRequest -> {
            val requiredOtpLength = consentRequest.otpLength
            val p: Pattern = Pattern.compile("(\\d{$requiredOtpLength})")
            val m: Matcher? = message?.let { p.matcher(it) }
            Logger.debug(TAG, "onActivityResult!: $message")
            m?.find()?.let {
              if (it) {
                Logger.debug(TAG, "onActivityResult_match: $m")
                onSuccess(m.group(0) as String)
              }
              else {
                Logger.debug(TAG, "onActivityResult_no_match: $m")
                onError(
                  "The message received doesn't include the otp length requested",
                  ConsentError.RegexMismatch.code,
                  Throwable(ConsentError.RegexMismatch.code),
                )
              }
            }
          }

          is SmsRequest -> {
            Logger.debug(TAG, "onActivityResult_SmsRequest: $message")
            message?.let {
              onSuccess(it)
            } ?: run {
              onError(
                "The sms body is null",
                ConsentError.NullSmsException.code,
                Throwable(ConsentError.NullSmsException.code),
              )
            }
          }
        }
      }
      else {
        Logger.debug(TAG, "onActivityResult_match: kkk")

        onError(
          "Consent denied by user",
          ConsentError.Denied.code,
          Throwable(ConsentError.Denied.code),
        )
      }
    }
  }

  override fun onNewIntent(p0: Intent?) {

  }

  /**
   * Retrieves the OTP (One Time Password) sent by the provided phone number(or any phone number in case it's null) and OTP length.
   *
   * @param otpLength The desired length of the OTP to be retrieved.
   * @param phoneNumber The phone number from which the OTP is being sent. Can be null if not provided.
   * @param promise A Promise object that is used to resolve or reject the result of the OTP retrieval process.
   *
   */
  @ReactMethod
  override fun getOtp(otpLength: Int, phoneNumber: String?, promise: Promise) {
    // We firstly assign our consent request
    this.consentRequest = OTPRequest(
      promise = promise,
      otpLength = otpLength,
    )
    // Then initialize the UserConsent
    initializeConsent(phoneNumber = phoneNumber)
  }

  /**
   * Retrieves the full SMS message sent by the provided phone number(or any phone number in case it's null).
   *
   * @param phoneNumber The phone number from which the OTP is being sent. Can be null if not provided.
   * @param promise A Promise object that is used to resolve or reject the result of the OTP retrieval process.
   *
   */
  @ReactMethod
  override fun getSms(phoneNumber: String?, promise: Promise) {
    // We firstly assign our consent request
    this.consentRequest = SmsRequest(
      promise = promise,
    )
    // Then initialize the UserConsent
    initializeConsent(phoneNumber = phoneNumber)
  }

  /**
   * A function that resolve the promise with the retrieved response and unregister the current receiver.
   *
   * @param response The response we got from the receiver (OTP or the SMS)
   */
  private fun onSuccess(response: String) {
    consentRequest.promise.resolve(response)
    unregisterReceiver()
  }

  /**
   * A function that reject the promise with the caught error and unregister the current receiver.
   *
   * @param errorBody The error body to be sent to the caller
   * @param errorCode The error's code to be sent to the caller
   * @param throwable The exception thrown to be sent to the caller
   */
  private fun onError(errorBody: String, errorCode: String, throwable: Throwable) {
    consentRequest.promise.reject(errorBody, errorCode, throwable)
    unregisterReceiver()
  }

  /**
   * Initialize consent client with the provided mobile number(in case not null) and register the receiver
   *
   * @param phoneNumber The phone number from which the OTP is being sent.
   */
  private fun initializeConsent(phoneNumber: String? = null) {
    Logger.debug("SmsRetrieverModule", "initializeConsent with phone number $phoneNumber")
    val client = SmsRetriever.getClient(reactContext)
    client.startSmsUserConsent(phoneNumber)
    registerReceiver()
  }

  @SuppressLint("UnspecifiedRegisterReceiverFlag")
  fun registerReceiver() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      try {
        val intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
        reactContext.registerReceiver(
          smsVerificationReceiver,
          intentFilter,
          SmsRetriever.SEND_PERMISSION,
          Handler(Looper.getMainLooper())
        )
      } catch (e: Exception) {
        var msg = e.message
        if (!msg!!.contains("registered with differing handler", ignoreCase = true)) {
          Logger.debug(TAG, "initializeConsent_exception_unhandled: $msg ")
          consentRequest.promise.reject(
            "Receiver Exception",
            ConsentError.ReceiverException.code,
            Throwable(ConsentError.ReceiverException.code),
          )
        }
      }
    }
  }

  fun unregisterReceiver() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      try {
        reactContext.unregisterReceiver(smsVerificationReceiver)
      } catch (e: Exception) {
        val msg = e.message
        Logger.debug(TAG, "initializeConsent_exception_unhandled: $msg ")
        consentRequest.promise.reject(
          "Receiver Exception",
          ConsentError.ReceiverException.code,
          Throwable(ConsentError.ReceiverException.code),
        )
      }
    }
  }

  companion object {
    const val NAME = "RnAndroidSmsRetriever"
  }
}
