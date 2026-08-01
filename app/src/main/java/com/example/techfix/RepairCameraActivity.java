package com.example.techfix;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;

public class RepairCameraActivity extends AppCompatActivity {
  public static final String RESULT_IMAGE_URI = "repair_camera_image_uri";

  private PreviewView previewView;
  private ImageCapture imageCapture;
  private ProcessCameraProvider cameraProvider;
  private int lensFacing = CameraSelector.LENS_FACING_BACK;
  private TextView captureButton;

  private final ActivityResultLauncher<String> cameraPermission =
      registerForActivityResult(
          new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted)
              startCamera();
            else {
              Toast
                  .makeText(this, "Camera permission is required.",
                            Toast.LENGTH_LONG)
                  .show();
              finish();
            }
          });

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_repair_camera);
    previewView = findViewById(R.id.repairCameraPreview);
    captureButton = findViewById(R.id.btnCaptureRepairImage);
    findViewById(R.id.btnCloseRepairCamera)
        .setOnClickListener(view -> finish());
    captureButton.setOnClickListener(view -> capturePhoto());
    findViewById(R.id.btnFlipRepairCamera)
        .setOnClickListener(view -> flipCamera());

    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED)
      startCamera();
    else
      cameraPermission.launch(Manifest.permission.CAMERA);
  }

  private void startCamera() {
    ListenableFuture<ProcessCameraProvider> providerFuture =
        ProcessCameraProvider.getInstance(this);
    providerFuture.addListener(() -> {
      try {
        cameraProvider = providerFuture.get();
        bindCamera();
      } catch (Exception exception) {
        showCameraError("Unable to start the camera.");
      }
    }, ContextCompat.getMainExecutor(this));
  }

  private void bindCamera() {
    if (cameraProvider == null)
      return;
    CameraSelector selector =
        new CameraSelector.Builder().requireLensFacing(lensFacing).build();
    Preview preview = new Preview.Builder().build();
    preview.setSurfaceProvider(previewView.getSurfaceProvider());
    imageCapture =
        new ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setTargetRotation(previewView.getDisplay().getRotation())
            .build();
    cameraProvider.unbindAll();
    cameraProvider.bindToLifecycle(this, selector, preview, imageCapture);
    captureButton.setEnabled(true);
  }

  private void flipCamera() {
    if (cameraProvider == null)
      return;
    int requested = lensFacing == CameraSelector.LENS_FACING_BACK
                        ? CameraSelector.LENS_FACING_FRONT
                        : CameraSelector.LENS_FACING_BACK;
    try {
      if (cameraProvider.hasCamera(new CameraSelector.Builder()
                                       .requireLensFacing(requested)
                                       .build())) {
        lensFacing = requested;
        bindCamera();
      } else {
        Toast
            .makeText(this, "This camera is not available.", Toast.LENGTH_SHORT)
            .show();
      }
    } catch (Exception exception) {
      showCameraError("Unable to switch cameras.");
    }
  }

  private void capturePhoto() {
    if (imageCapture == null)
      return;
    File directory = new File(getFilesDir(), "repair-images");
    if (!directory.exists() && !directory.mkdirs()) {
      showCameraError("Unable to create image storage.");
      return;
    }
    File photo =
        new File(directory, "repair-" + System.currentTimeMillis() + ".jpg");
    ImageCapture.OutputFileOptions output =
        new ImageCapture.OutputFileOptions.Builder(photo).build();
    captureButton.setEnabled(false);
    imageCapture.takePicture(
        output, ContextCompat.getMainExecutor(this),
        new ImageCapture.OnImageSavedCallback() {
          @Override
          public void onImageSaved(ImageCapture.OutputFileResults result) {
            Intent data = new Intent();
            data.putExtra(RESULT_IMAGE_URI, Uri.fromFile(photo).toString());
            setResult(RESULT_OK, data);
            finish();
          }

          @Override
          public void onError(ImageCaptureException exception) {
            captureButton.setEnabled(true);
            showCameraError("Could not capture the photo. Try again.");
          }
        });
  }

  private void showCameraError(String message) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
  }
}
