package com.example.techfix;

import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public abstract class ManagementScreen extends AppCompatActivity {
  protected void showManagementLayout(@LayoutRes int layout) {
    EdgeToEdge.enable(this);
    setContentView(layout);
    View root = findViewById(R.id.main);
    ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
      Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
      view.setPadding(0, bars.top, 0, bars.bottom);
      return insets;
    });
  }
}
