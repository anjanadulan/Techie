package com.example.techfix;

import android.content.Intent;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    SessionManager sessionManager = new SessionManager(this);
    if (sessionManager.isLoggedIn() &&
        FirebaseAuth.getInstance().getCurrentUser() != null) {
      FirebaseRealtimeSync.start(this);
      FirebaseSyncScheduler.enqueueNow(this);
      FirebaseSyncScheduler.schedulePeriodic(this);
      Class<?> destination = sessionManager.isManager()
                                 ? ManagementDashboardActivity.class
                                 : CustomerHomeActivity.class;
      startActivity(new Intent(this, destination));
      finish();
      return;
    }

    EdgeToEdge.enable(this);
    setContentView(R.layout.activity_main);

    ViewCompat.setOnApplyWindowInsetsListener(
        findViewById(R.id.main), (v, insets) -> {
          Insets systemBars =
              insets.getInsets(WindowInsetsCompat.Type.systemBars());
          v.setPadding(0, systemBars.top, 0, systemBars.bottom);
          return insets;
        });

    // Navigate to Sign In Activity
    findViewById(R.id.btnSplashSignIn).setOnClickListener(v -> {
      Intent intent = new Intent(MainActivity.this, Signin.class);
      startActivity(intent);
    });

    // Navigate to Sign Up Activity
    findViewById(R.id.btnSplashSignUp).setOnClickListener(v -> {
      Intent intent = new Intent(MainActivity.this, Signup.class);
      startActivity(intent);
    });
  }
}
