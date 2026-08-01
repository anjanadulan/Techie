package com.example.techfix;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public abstract class ManagementScreen extends AppCompatActivity {
  protected boolean showManagementLayout(@LayoutRes int layout) {
    SessionManager sessionManager = new SessionManager(this);
    if (!sessionManager.isLoggedIn() || !sessionManager.isManager() ||
        FirebaseAuth.getInstance().getCurrentUser() == null) {
      openSafeDestination(sessionManager);
      return false;
    }

    EdgeToEdge.enable(this);
    setContentView(layout);
    View root = findViewById(R.id.main);
    ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
      Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
      view.setPadding(0, bars.top, 0, bars.bottom);
      return insets;
    });
    FirebaseRealtimeSync.start(this);
    FirebaseSyncScheduler.enqueueNow(this);
    verifyManagerRole(sessionManager);
    return true;
  }

  protected void logoutManager() {
    FirebaseRealtimeSync.stop();
    FirebaseAuth.getInstance().signOut();
    SessionManager sessionManager = new SessionManager(this);
    sessionManager.clearSession();
    openSafeDestination(sessionManager);
  }

  private void verifyManagerRole(SessionManager sessionManager) {
    FirebaseFirestore.getInstance()
        .collection("users")
        .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
        .get()
        .addOnSuccessListener(profile -> {
          if (!SessionManager.ROLE_MANAGER.equals(profile.getString("role"))) {
            sessionManager.setRole(SessionManager.ROLE_CUSTOMER);
            FirebaseRealtimeSync.stop();
            FirebaseRealtimeSync.start(this);
            openSafeDestination(sessionManager);
          }
        });
  }

  private void openSafeDestination(SessionManager sessionManager) {
    Class<?> destination = sessionManager.isLoggedIn() &&
                                   FirebaseAuth.getInstance().getCurrentUser() !=
                                       null
                               ? CustomerHomeActivity.class
                               : MainActivity.class;
    Intent intent = new Intent(this, destination);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
    finish();
  }
}
