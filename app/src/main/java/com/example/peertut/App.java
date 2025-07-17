package com.example.peertut;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.FirebaseDatabase;

/**
 * Application subclass to force all dynamic registerReceiver calls
 * on Android 14+ to supply RECEIVER_NOT_EXPORTED.
 */
public class App extends Application {
  @Override
  public void onCreate() {
    super.onCreate();
    // Enable Firebase offline persistence before any other usage
    FirebaseDatabase.getInstance().setPersistenceEnabled(true);
  }
  @Override
  public Intent registerReceiver(
          @Nullable BroadcastReceiver receiver,
          IntentFilter filter) {

    if (Build.VERSION.SDK_INT >= 34) {
      // mark every dynamic receiver as NOT_EXPORTED on Android 14+
      return super.registerReceiver(
              receiver,
              filter,
              Context.RECEIVER_NOT_EXPORTED
      );
    }
    return super.registerReceiver(receiver, filter);
  }
}
