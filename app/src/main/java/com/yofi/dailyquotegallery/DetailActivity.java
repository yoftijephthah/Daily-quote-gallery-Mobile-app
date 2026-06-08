package com.yofi.dailyquotegallery;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

public class DetailActivity extends AppCompatActivity {
    private SharedPreferences prefs;
    private Set<String> favoriteImages;
    private String imageIdStr;
    private int currentImageId;
    private static final int REQUEST_STORAGE_PERMISSION = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences themePrefs = getSharedPreferences("theme_prefs", MODE_PRIVATE);
        boolean isDarkMode = themePrefs.getBoolean("isDarkMode", false);
        applyTheme(isDarkMode);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        prefs = getSharedPreferences("favorites", Context.MODE_PRIVATE);
        favoriteImages = new HashSet<>(prefs.getStringSet("fav_list", new HashSet<>()));

        ImageView fullImage = findViewById(R.id.fullImage);
        ImageButton btnFav = findViewById(R.id.btnFavoriteDetail);
        ImageButton btnToggleTheme = findViewById(R.id.btnToggleThemeDetail);
        ImageButton btnBack = findViewById(R.id.btnBackDetail);

        currentImageId = getIntent().getIntExtra("img_id", 0);
        imageIdStr = String.valueOf(currentImageId);

        if (currentImageId != 0) {
            fullImage.setImageResource(currentImageId);
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (favoriteImages.contains(imageIdStr)) {
            btnFav.setImageResource(android.R.drawable.btn_star_big_on);
        } else {
            btnFav.setImageResource(android.R.drawable.btn_star_big_off);
        }

        btnToggleTheme.setImageResource(isDarkMode ? R.drawable.ic_sun : R.drawable.ic_moon);
        btnToggleTheme.setOnClickListener(v -> {
            boolean newValue = !themePrefs.getBoolean("isDarkMode", false);
            themePrefs.edit().putBoolean("isDarkMode", newValue).apply();
            applyTheme(newValue);
            recreate();
        });

        btnFav.setOnClickListener(v -> toggleFavorite(btnFav));

        findViewById(R.id.btnDownload).setOnClickListener(v -> checkPermissionAndSave());
        findViewById(R.id.btnShare).setOnClickListener(v -> shareImage());
        findViewById(R.id.btnFb).setOnClickListener(v -> openUrl("https://www.facebook.com/login"));
        findViewById(R.id.btnIg).setOnClickListener(v -> openUrl("https://www.instagram.com/accounts/login"));
    }

    private void applyTheme(boolean isDarkMode) {
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    private void toggleFavorite(ImageButton btnFav) {
        favoriteImages = new HashSet<>(prefs.getStringSet("fav_list", new HashSet<>()));
        if (favoriteImages.contains(imageIdStr)) {
            favoriteImages.remove(imageIdStr);
            btnFav.setImageResource(android.R.drawable.btn_star_big_off);
            Toast.makeText(this, "Removed from Favorites", Toast.LENGTH_SHORT).show();
        } else {
            favoriteImages.add(imageIdStr);
            btnFav.setImageResource(android.R.drawable.btn_star_big_on);
            Toast.makeText(this, "Added to Favorites", Toast.LENGTH_SHORT).show();
        }
        prefs.edit().putStringSet("fav_list", favoriteImages).apply();
    }

    private void checkPermissionAndSave() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
            } else {
                saveImageToGallery();
            }
        } else {
            // Android 10+ doesn't need WRITE_EXTERNAL_STORAGE for saving to public folders via MediaStore
            saveImageToGallery();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            saveImageToGallery();
        } else if (requestCode == REQUEST_STORAGE_PERMISSION) {
            Toast.makeText(this, "Permission denied to save image", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveImageToGallery() {
        if (currentImageId == 0) return;

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), currentImageId);
        if (bitmap == null) {
            Toast.makeText(this, "Error decoding image", Toast.LENGTH_SHORT).show();
            return;
        }

        String filename = "Quote_" + System.currentTimeMillis() + ".jpg";

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/DailyQuotes");
                values.put(MediaStore.MediaColumns.IS_PENDING, 1);

                Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (imageUri != null) {
                    try (OutputStream fos = getContentResolver().openOutputStream(imageUri)) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    }
                    values.clear();
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0);
                    getContentResolver().update(imageUri, values, null, null);
                    Toast.makeText(this, "Image saved to Gallery (DailyQuotes folder)", Toast.LENGTH_LONG).show();
                }
            } else {
                File imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                File myDir = new File(imagesDir, "DailyQuotes");
                if (!myDir.exists()) myDir.mkdirs();
                File imageFile = new File(myDir, filename);
                try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                }
                // Notify the system that a new file exists so it appears in the gallery app
                MediaScannerConnection.scanFile(this, new String[]{imageFile.getAbsolutePath()}, new String[]{"image/jpeg"}, null);
                Toast.makeText(this, "Image saved to Gallery", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error saving image: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void shareImage() {
        if (currentImageId == 0) return;
        try {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), currentImageId);
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs();
            File file = new File(cachePath, "shared_image.png");
            try (FileOutputStream stream = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            }
            Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            if (contentUri != null) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                shareIntent.setDataAndType(contentUri, getContentResolver().getType(contentUri));
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                startActivity(Intent.createChooser(shareIntent, "Share Image via"));
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error sharing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
