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

    private static final int OUTPUT_SIZE = 1080; // 1:1 square output size in pixels
    private List<Bitmap> images = new ArrayList<>();
    private int edgeColor = Color.parseColor("#FF00FF"); // Default glitch color (magenta)
    private int effectThreshold = 10; // Default threshold (1-50)
    private ImageView outputImageView;
    private EditText stripCountInput;
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

        // Update effect threshold dynamically
        thresholdSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                effectThreshold = progress + 1; // Range 1-50 (progress is 0-49)
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
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
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
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
        int stripCount;
        try {
            stripCount = Integer.parseInt(stripCountInput.getText().toString());
            if (stripCount < 10 || stripCount > 100) {
                Toast.makeText(this, "Strip count must be between 10 and 100", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid strip count", Toast.LENGTH_SHORT).show();
            return;
        }

        // Resize all images to 1080x1080
        List<Bitmap> resizedImages = new ArrayList<>();
        for (Bitmap img : images) {
            float scaleFactor = Math.max((float) OUTPUT_SIZE / img.getWidth(), (float) OUTPUT_SIZE / img.getHeight());
            int newWidth = (int) (img.getWidth() * scaleFactor);
            int newHeight = (int) (img.getHeight() * scaleFactor);
            Bitmap scaled = Bitmap.createScaledBitmap(img, newWidth, newHeight, true);
            Bitmap cropped = Bitmap.createBitmap(scaled, (newWidth - OUTPUT_SIZE) / 2, (newHeight - OUTPUT_SIZE) / 2, OUTPUT_SIZE, OUTPUT_SIZE);
            resizedImages.add(cropped);
        }

        // Calculate strip width
        int stripWidth = OUTPUT_SIZE / stripCount;

        // Create strips from all images
        List<Bitmap> allStrips = new ArrayList<>();
        for (Bitmap img : resizedImages) {
            for (int i = 0; i < stripCount; i++) {
                Bitmap strip = Bitmap.createBitmap(img, i * stripWidth, 0, stripWidth, OUTPUT_SIZE);
                allStrips.add(strip);
            }
        }

        // Randomly select `stripCount` strips from all available strips
        List<Bitmap> selectedStrips = new ArrayList<>();
        Random random = new Random();
        int totalStrips = allStrips.size(); // Total strips = stripCount * number of images
        for (int i = 0; i < stripCount; i++) {
            int randomIndex = random.nextInt(totalStrips);
            selectedStrips.add(allStrips.get(randomIndex));
        }

        // Create final bitmap
        finalBitmap = Bitmap.createBitmap(OUTPUT_SIZE, OUTPUT_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(finalBitmap);
        canvas.drawColor(Color.WHITE); // White background

        // Draw the randomly selected strips
        for (int i = 0; i < stripCount; i++) {
            canvas.drawBitmap(selectedStrips.get(i), i * stripWidth, 0, null);
        }

        // Apply effects with threshold
        applyEffects(finalBitmap);

        // Display result
        outputImageView.setImageBitmap(finalBitmap);
    }

    private void applyEffects(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        // Convert to grayscale
        int[] tempPixels = pixels.clone(); // For edge detection
        for (int i = 0; i < pixels.length; i++) {
            int r = Color.red(pixels[i]);
            int g = Color.green(pixels[i]);
            int b = Color.blue(pixels[i]);
            int brightness = (r + g + b) / 3;
            pixels[i] = Color.rgb(brightness, brightness, brightness);
        }

        // Edge detection and glitch effect with adjustable threshold
        int edgeR = Color.red(edgeColor);
        int edgeG = Color.green(edgeColor);
        int edgeB = Color.blue(edgeColor);

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int index = x + y * width;
                int current = Color.red(tempPixels[index]); // Use red channel for simplicity
                int left = Color.red(tempPixels[(x - 1) + y * width]);
                int right = Color.red(tempPixels[(x + 1) + y * width]);
                int up = Color.red(tempPixels[x + (y - 1) * width]);
                int down = Color.red(tempPixels[x + (y + 1) * width]);

                if (Math.abs(current - left) > effectThreshold ||
                        Math.abs(current - right) > effectThreshold ||
                        Math.abs(current - up) > effectThreshold ||
                        Math.abs(current - down) > effectThreshold) {
                    pixels[index] = Color.rgb(edgeR, edgeG, edgeB);
                }
            }
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
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