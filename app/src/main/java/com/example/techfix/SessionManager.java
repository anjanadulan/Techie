package com.example.techfix;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
  private static final String PREFERENCES_NAME = "techfix_session";
  private static final String KEY_LOGGED_IN = "logged_in";
  private static final String KEY_USER_ID = "user_id";
  private static final String KEY_FULL_NAME = "full_name";
  private static final String KEY_EMAIL = "email";

  private final SharedPreferences preferences;

  public SessionManager(Context context) {
    preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
  }

  public void startSession(User user) {
    preferences.edit()
        .putBoolean(KEY_LOGGED_IN, true)
        .putLong(KEY_USER_ID, user.getId())
        .putString(KEY_FULL_NAME, user.getFullName())
        .putString(KEY_EMAIL, user.getEmail())
        .apply();
  }

  public boolean isLoggedIn() {
    return preferences.getBoolean(KEY_LOGGED_IN, false);
  }

  public long getUserId() {
    return preferences.getLong(KEY_USER_ID, -1);
  }

  public String getFullName() {
    return preferences.getString(KEY_FULL_NAME, "");
  }

  public String getEmail() {
    return preferences.getString(KEY_EMAIL, "");
  }

  public void clearSession() {
    preferences.edit().clear().apply();
  }
}
