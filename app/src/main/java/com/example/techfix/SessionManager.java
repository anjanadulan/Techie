package com.example.techfix;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
  private static final String PREFERENCES_NAME = "techfix_session";
  private static final String KEY_LOGGED_IN = "logged_in";
  private static final String KEY_USER_ID = "user_id";
  private static final String KEY_FULL_NAME = "full_name";
  private static final String KEY_EMAIL = "email";
  private static final String KEY_ROLE = "role";
  public static final String ROLE_CUSTOMER = "customer";
  public static final String ROLE_MANAGER = "manager";

  private final SharedPreferences preferences;

  public SessionManager(Context context) {
    preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
  }

  public void startSession(User user) {
    startSession(user, ROLE_CUSTOMER);
  }

  public void startSession(User user, String role) {
    preferences.edit()
        .putBoolean(KEY_LOGGED_IN, true)
        .putLong(KEY_USER_ID, user.getId())
        .putString(KEY_FULL_NAME, user.getFullName())
        .putString(KEY_EMAIL, user.getEmail())
        .putString(KEY_ROLE, normalizeRole(role))
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

  public String getRole() {
    return preferences.getString(KEY_ROLE, ROLE_CUSTOMER);
  }

  public boolean isManager() {
    return ROLE_MANAGER.equals(getRole());
  }

  public void setRole(String role) {
    preferences.edit().putString(KEY_ROLE, normalizeRole(role)).apply();
  }

  public void clearSession() {
    preferences.edit().clear().apply();
  }

  private String normalizeRole(String role) {
    return ROLE_MANAGER.equals(role) ? ROLE_MANAGER : ROLE_CUSTOMER;
  }
}
