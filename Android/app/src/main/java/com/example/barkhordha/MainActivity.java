package com.example.barkhordha;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private List<Bitmap> images = new ArrayList<>();
    private int edgeColor = Color.parseColor("#FF00FF"); // Default glitch color (magenta)
    private int effectThreshold = 0; // Threshold (0-100, 0 = no glitch, 100 = max glitch)
    private int outputSize = 1080; // Default output size, will be set by user input
    private ImageView outputImageView;
    private EditText stripCountInput;
    private EditText outputSizeInput; // New input for output size
    private SeekBar thresholdSeekBar;
    private Bitmap finalBitmap; // Store the generated image for saving/sharing

    // Launcher for picking multiple images
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    if (data.getClipData() != null) {
                        images.clear();
                        int count = data.getClipData().getItemCount();
                        for (int i = 0; i < count; i++) {
                            Uri imageUri = data.getClipData().getItemAt(i).getUri();
                            try {
                                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                                images.add(bitmap);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    } else if (data.getData() != null) {
                        Uri imageUri = data.getData();
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                            images.add(bitmap);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    Toast.makeText(this, "Loaded " + images.size() + " images", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); // Force light mode
        setContentView(R.layout.activity_main);

        Button selectImagesButton = findViewById(R.id.selectImagesButton);
        stripCountInput = findViewById(R.id.stripCountInput);
        outputSizeInput = findViewById(R.id.outputSizeInput); // New input field
        Button colorPickerButton = findViewById(R.id.colorPickerButton);
        thresholdSeekBar = findViewById(R.id.thresholdSeekBar);
        Button generateButton = findViewById(R.id.generateButton);
        outputImageView = findViewById(R.id.outputImageView);
        Button saveButton = findViewById(R.id.saveButton);
        Button shareButton = findViewById(R.id.shareButton);

        // Select images
        selectImagesButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            imagePickerLauncher.launch(Intent.createChooser(intent, "Select Images"));
        });

        // Pick color with a spectrum dialog
        colorPickerButton.setOnClickListener(v -> showColorPickerDialog());

        // Update effect threshold dynamically (0-100)
        thresholdSeekBar.setMax(100); // Range 0-100
        thresholdSeekBar.setProgress(0); // Default to 0 (no glitch)
        thresholdSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                effectThreshold = progress; // Range 0-100
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // Generate image
        generateButton.setOnClickListener(v -> {
            if (images.size() < 2) {
                Toast.makeText(this, "Please select at least 2 images", Toast.LENGTH_SHORT).show();
                return;
            }
            generateGlitchedImage();
        });

        // Save image
        saveButton.setOnClickListener(v -> {
            if (finalBitmap == null) {
                Toast.makeText(this, "Generate an image first", Toast.LENGTH_SHORT).show();
                return;
            }
            saveImageToGallery(finalBitmap);
        });

        // Share image
        shareButton.setOnClickListener(v -> {
            if (finalBitmap == null) {
                Toast.makeText(this, "Generate an image first", Toast.LENGTH_SHORT).show();
                return;
            }
            shareImage(finalBitmap);
        });
    }

    // Show a full-spectrum color picker dialog
    private void showColorPickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick Edge Glitch Color");

        // Inflate the custom layout
        final android.view.View dialogView = getLayoutInflater().inflate(R.layout.color_picker_dialog, null);
        builder.setView(dialogView);

        SeekBar redSeekBar = dialogView.findViewById(R.id.redSeekBar);
        SeekBar greenSeekBar = dialogView.findViewById(R.id.greenSeekBar);
        SeekBar blueSeekBar = dialogView.findViewById(R.id.blueSeekBar);
        android.view.View colorPreview = dialogView.findViewById(R.id.colorPreview);

        // Set initial values from edgeColor
        redSeekBar.setProgress(Color.red(edgeColor));
        greenSeekBar.setProgress(Color.green(edgeColor));
        blueSeekBar.setProgress(Color.blue(edgeColor));
        colorPreview.setBackgroundColor(edgeColor);

        // Update preview on slider change
        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int red = redSeekBar.getProgress();
                int green = greenSeekBar.getProgress();
                int blue = blueSeekBar.getProgress();
                colorPreview.setBackgroundColor(Color.rgb(red, green, blue));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };

        redSeekBar.setOnSeekBarChangeListener(listener);
        greenSeekBar.setOnSeekBarChangeListener(listener);
        blueSeekBar.setOnSeekBarChangeListener(listener);

        builder.setPositiveButton("OK", (dialog, which) -> {
            edgeColor = Color.rgb(redSeekBar.getProgress(), greenSeekBar.getProgress(), blueSeekBar.getProgress());
            Toast.makeText(this, "Color selected", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void generateGlitchedImage() {
        // Get output size from user input
        try {
            outputSize = Integer.parseInt(outputSizeInput.getText().toString());
            if (outputSize < 720 || outputSize > 5000) {
                Toast.makeText(this, "Output size must be between 720 and 5000", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid output size", Toast.LENGTH_SHORT).show();
            return;
        }

        int stripCount;
        try {
            stripCount = Integer.parseInt(stripCountInput.getText().toString());
            if (stripCount < 2 || stripCount > 300) {
                Toast.makeText(this, "Strip count must be between 2 and 300", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid strip count", Toast.LENGTH_SHORT).show();
            return;
        }

        // Resize all images to the specified output size
        List<Bitmap> resizedImages = new ArrayList<>();
        for (Bitmap img : images) {
            float scaleFactor = Math.max((float) outputSize / img.getWidth(), (float) outputSize / img.getHeight());
            int newWidth = (int) (img.getWidth() * scaleFactor);
            int newHeight = (int) (img.getHeight() * scaleFactor);
            Bitmap scaled = Bitmap.createScaledBitmap(img, newWidth, newHeight, true);
            Bitmap cropped = Bitmap.createBitmap(scaled, (newWidth - outputSize) / 2, (newHeight - outputSize) / 2, outputSize, outputSize);
            resizedImages.add(cropped);
        }

        // Calculate strip width
        int stripWidth = outputSize / stripCount;

        // Create strips from all images
        List<Bitmap> allStrips = new ArrayList<>();
        for (Bitmap img : resizedImages) {
            for (int i = 0; i < stripCount; i++) {
                Bitmap strip = Bitmap.createBitmap(img, i * stripWidth, 0, stripWidth, outputSize);
                allStrips.add(strip);
            }
        }

        // Check if we have enough strips to avoid repeats
        int totalStrips = allStrips.size(); // Total strips = stripCount * number of images
        if (stripCount > totalStrips) {
            Toast.makeText(this, "Not enough unique strips! Need at least " + stripCount + " strips, but only " + totalStrips + " available.", Toast.LENGTH_LONG).show();
            return;
        }

        // Shuffle the list of strips and select the first `stripCount` strips to ensure no repeats
        List<Bitmap> selectedStrips = new ArrayList<>();
        Collections.shuffle(allStrips, new Random());
        for (int i = 0; i < stripCount; i++) {
            selectedStrips.add(allStrips.get(i));
        }

        // Create final bitmap with the specified output size
        finalBitmap = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(finalBitmap);
        canvas.drawColor(Color.WHITE); // White background

        // Draw the selected strips
        for (int i = 0; i < stripCount; i++) {
            canvas.drawBitmap(selectedStrips.get(i), i * stripWidth, 0, null);
        }

        // Apply glitch effect with threshold
        applyEffects(finalBitmap);

        // Display result
        outputImageView.setImageBitmap(finalBitmap);
    }

    private void applyEffects(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        // Keep original pixels
        int[] originalPixels = pixels.clone();
        int[] tempPixels = pixels.clone(); // For edge detection

        // Convert tempPixels to grayscale for edge detection
        for (int i = 0; i < tempPixels.length; i++) {
            int r = Color.red(tempPixels[i]);
            int g = Color.green(tempPixels[i]);
            int b = Color.blue(tempPixels[i]);
            int brightness = (r + g + b) / 3;
            tempPixels[i] = Color.rgb(brightness, brightness, brightness);
        }

        // Edge detection with adjustable threshold
        int edgeR = Color.red(edgeColor);
        int edgeG = Color.green(edgeColor);
        int edgeB = Color.blue(edgeColor);

        // Map threshold (0-100) to edge sensitivity (e.g., 50 to 5)
        // At threshold 0, sensitivity is high (less glitch); at 100, sensitivity is low (more glitch)
        float edgeSensitivity = 50.0f - (effectThreshold * 0.45f); // Maps 0->50 to 100->5

        // Create a new array for the output pixels
        int[] newPixels = originalPixels.clone();

        // Detect edges and apply glitch effect
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int index = x + y * width;
                int current = Color.red(tempPixels[index]);
                int left = Color.red(tempPixels[(x - 1) + y * width]);
                int right = Color.red(tempPixels[(x + 1) + y * width]);
                int up = Color.red(tempPixels[x + (y - 1) * width]);
                int down = Color.red(tempPixels[x + (y + 1) * width]);

                // Detect edge based on sensitivity
                if (Math.abs(current - left) > edgeSensitivity ||
                        Math.abs(current - right) > edgeSensitivity ||
                        Math.abs(current - up) > edgeSensitivity ||
                        Math.abs(current - down) > edgeSensitivity) {
                    // Apply glitch color to the edge pixel
                    newPixels[index] = Color.rgb(edgeR, edgeG, edgeB);
                    // Optionally, apply to adjacent pixels for a "line" effect
                    if (x > 0)
                        newPixels[(x - 1) + y * width] = Color.rgb(edgeR, edgeG, edgeB); // Left
                    if (x < width - 1)
                        newPixels[(x + 1) + y * width] = Color.rgb(edgeR, edgeG, edgeB); // Right
                    if (y > 0)
                        newPixels[x + (y - 1) * width] = Color.rgb(edgeR, edgeG, edgeB); // Up
                    if (y < height - 1)
                        newPixels[x + (y + 1) * width] = Color.rgb(edgeR, edgeG, edgeB); // Down
                }
            }
        }

        bitmap.setPixels(newPixels, 0, width, 0, 0, width, height);
    }

    private void saveImageToGallery(Bitmap bitmap) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "GlitchedImage_" + System.currentTimeMillis() + ".png");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareImage(Bitmap bitmap) {
        try {
            File cacheDir = getCacheDir();
            File file = new File(cacheDir, "shared_image.png");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();

            Uri contentUri = FileProvider.getUriForFile(this, "com.example.imageglitcher.fileprovider", file);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share Image"));
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to share image", Toast.LENGTH_SHORT).show();
        }
    }
}