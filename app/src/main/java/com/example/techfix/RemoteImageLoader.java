package com.example.techfix;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class RemoteImageLoader {
  private static final ExecutorService executor = Executors.newFixedThreadPool(2);

  private RemoteImageLoader() {}

  public static void load(ImageView imageView, String imageUrl) {
    imageView.setTag(imageUrl);
    executor.execute(() -> {
      HttpURLConnection connection = null;
      try {
        connection = (HttpURLConnection) new URL(imageUrl).openConnection();
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(10_000);
        connection.setInstanceFollowRedirects(true);
        try (InputStream stream = connection.getInputStream()) {
          Bitmap bitmap = BitmapFactory.decodeStream(stream);
          if (bitmap != null) {
            imageView.post(() -> {
              if (imageUrl.equals(imageView.getTag()))
                imageView.setImageBitmap(bitmap);
            });
          }
        }
      } catch (Exception ignored) {
        // Keep the layout placeholder when a remote image is temporarily unavailable.
      } finally {
        if (connection != null)
          connection.disconnect();
      }
    });
  }
}
