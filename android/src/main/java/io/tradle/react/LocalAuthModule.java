
package io.tradle.react;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Intent;
import android.content.Context;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.ReadableMap;
import com.reactnativenavigation.NavigationApplication;

// source for main part from:
// https://github.com/googlesamples/android-ConfirmCredential/blob/master/Application/src/main/java/com/example/android/confirmcredential/MainActivity.java

public class LocalAuthModule extends ReactContextBaseJavaModule {

  private static final int AUTH_REQUEST = 16238;
  private static final String E_ACTIVITY_DOES_NOT_EXIST = "E_ACTIVITY_DOES_NOT_EXIST";
  private static final String E_AUTH_CANCELLED = "LAErrorUserCancel";
  private static final String E_FAILED_TO_SHOW_AUTH = "E_FAILED_TO_SHOW_AUTH";
  private static final String E_ONE_REQ_AT_A_TIME = "E_ONE_REQ_AT_A_TIME";

  private final ReactApplicationContext reactContext;
  private KeyguardManager mKeyguardManager;
  private Promise authPromise;

  public static LocalAuthModule instance;

  public LocalAuthModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
    mKeyguardManager = (KeyguardManager) this.reactContext.getSystemService(Context.KEYGUARD_SERVICE);
    instance = this;
  }

  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode != AUTH_REQUEST || authPromise == null) return;

    if (resultCode == Activity.RESULT_CANCELED) {
      authPromise.reject(E_AUTH_CANCELLED, "User canceled");
    } else if (resultCode == Activity.RESULT_OK) {
      authPromise.resolve(true);
    }

    authPromise = null;
  }

  @Override
  public String getName() {
    return "RNLocalAuth";
  }

  @ReactMethod
  public void isDeviceSecure(final Promise promise) {
    promise.resolve(mKeyguardManager.isDeviceSecure());
  }

  @ReactMethod
  public void authenticate(ReadableMap map, final Promise promise) {
    // Create the Confirm Credentials screen. You can customize the title and description. Or
    // we will provide a generic one for you if you leave it null
    Activity currentActivity = getCurrentActivity();

    if (authPromise != null) {
      promise.reject(E_ONE_REQ_AT_A_TIME, "Activity doesn't exist");
      return;
    }

    if (currentActivity == null) {
      promise.reject(E_ACTIVITY_DOES_NOT_EXIST, "One auth request at a time");
      return;
    }

    // Store the promise to resolve/reject when picker returns data
    authPromise = promise;


    String reason = map.hasKey("reason") ? map.getString("reason") : null;
    String description = map.hasKey("description") ? map.getString("description") : null;
    try {
      final Intent authIntent = mKeyguardManager.createConfirmDeviceCredentialIntent(reason, description);
      currentActivity.startActivityForResult(authIntent, AUTH_REQUEST, null);
    } catch (Exception e) {
      authPromise.reject(E_FAILED_TO_SHOW_AUTH, e);
      authPromise = null;
    }
  }
}
