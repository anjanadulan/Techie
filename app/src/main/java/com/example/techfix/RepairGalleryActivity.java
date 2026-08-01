package com.example.techfix;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RepairGalleryActivity extends CustomerScreen {
  private CustomerRepository repository;

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    showCustomerLayout(R.layout.activity_repair_gallery);
    repository = new CustomerRepository(this);
    findViewById(R.id.btnGalleryBack).setOnClickListener(view -> finish());
  }

  @Override
  protected void onResume() {
    super.onResume();
    renderGallery();
  }

  private void renderGallery() {
    LinearLayout container = findViewById(R.id.repairGalleryList);
    container.removeAllViews();
    List<CustomerRepository.GalleryItem> images =
        new ArrayList<>(repository.getFeaturedRepairImages());
    renderItems(container, images);
    FirebaseFirestore.getInstance()
        .collection("repairHistory")
        .whereEqualTo("featured", true)
        .get()
        .addOnSuccessListener(snapshot -> {
          Set<String> paths = new HashSet<>();
          for (CustomerRepository.GalleryItem item : images)
            paths.add(item.imagePath);
          for (DocumentSnapshot document : snapshot.getDocuments()) {
            String imagePath = document.getString("imagePath");
            if (imagePath == null || imagePath.isEmpty() ||
                !paths.add(imagePath))
              continue;
            Long localId = document.getLong("localId");
            Long recordedAt = document.getLong("recordedAt");
            images.add(new CustomerRepository.GalleryItem(
                localId == null ? 0 : localId, imagePath,
                text(document, "device", "Repaired device"),
                text(document, "serviceName", "TechFix repair"),
                text(document, "branchName", "TechFix"),
                recordedAt == null ? 0 : recordedAt));
          }
          renderItems(container, images);
        });
  }

  private void renderItems(LinearLayout container,
                           List<CustomerRepository.GalleryItem> images) {
    container.removeAllViews();
    if (images.isEmpty()) {
      TextView empty = new TextView(this);
      empty.setText("Featured TechFix repairs will appear here.");
      empty.setTextColor(getColor(R.color.customer_muted));
      empty.setTextSize(14);
      empty.setPadding(0, dp(36), 0, dp(36));
      container.addView(empty);
      return;
    }

    LayoutInflater inflater = LayoutInflater.from(this);
    for (CustomerRepository.GalleryItem item : images) {
      View card =
          inflater.inflate(R.layout.view_repair_gallery_item, container, false);
      ImageView image = card.findViewById(R.id.ivGalleryRepair);
      if (item.imagePath.startsWith("http://") ||
          item.imagePath.startsWith("https://"))
        RemoteImageLoader.load(image, item.imagePath);
      else
        image.setImageURI(Uri.parse(item.imagePath));
      ((TextView)card.findViewById(R.id.tvGalleryTitle))
          .setText(item.device + " · " + item.service);
      ((TextView)card.findViewById(R.id.tvGalleryMeta))
          .setText(item.branch + " · " +
                   CustomerRepository.formatDate(item.recordedAt));
      container.addView(card);
    }
  }

  private String text(DocumentSnapshot document, String field,
                      String fallback) {
    String value = document.getString(field);
    return value == null || value.trim().isEmpty() ? fallback : value;
  }

  private int dp(int value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }

  @Override
  protected void onDestroy() {
    if (repository != null)
      repository.close();
    super.onDestroy();
  }
}
