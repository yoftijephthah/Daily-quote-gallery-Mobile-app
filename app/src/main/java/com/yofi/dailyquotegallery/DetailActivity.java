package com.yofi.dailyquotegallery;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import java.util.HashSet;
import java.util.Set;

public class DetailActivity extends AppCompatActivity {
    private SharedPreferences prefs;
    private Set<String> favoriteImages;
    private String imageIdStr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. Load theme before super.onCreate to ensure consistency
        SharedPreferences themePrefs = getSharedPreferences("theme_prefs", MODE_PRIVATE);
        boolean isDarkMode = themePrefs.getBoolean("isDarkMode", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        prefs = getSharedPreferences("favorites", Context.MODE_PRIVATE);
        favoriteImages = new HashSet<>(prefs.getStringSet("fav_list", new HashSet<>()));

        ImageView fullImage = findViewById(R.id.fullImage);
        ImageButton btnFav = findViewById(R.id.btnFavoriteDetail);
        ImageButton btnToggleTheme = findViewById(R.id.btnToggleThemeDetail);

        int imageId = getIntent().getIntExtra("img_id", 0);
        imageIdStr = String.valueOf(imageId);

        if (imageId != 0) {
            fullImage.setImageResource(imageId);
        }

        // Set initial star state
        if (favoriteImages.contains(imageIdStr)) {
            btnFav.setImageResource(android.R.drawable.btn_star_big_on);
        } else {
            btnFav.setImageResource(android.R.drawable.btn_star_big_off);
        }

        // --- Theme Toggle Logic ---
        btnToggleTheme.setImageResource(isDarkMode ? R.drawable.ic_sun : R.drawable.ic_moon);
        btnToggleTheme.setOnClickListener(v -> {
            boolean newValue = !themePrefs.getBoolean("isDarkMode", false);
            themePrefs.edit().putBoolean("isDarkMode", newValue).apply();
            
            if (newValue) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
            recreate();
        });

        // Favorite Logic
        btnFav.setOnClickListener(v -> {
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
        });

        // Other Buttons
        findViewById(R.id.btnDownload).setOnClickListener(v -> Toast.makeText(this, "Downloading...", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnShare).setOnClickListener(v -> Toast.makeText(this, "Opening Share...", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnFb).setOnClickListener(v -> Toast.makeText(this, "Sharing to Facebook...", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnIg).setOnClickListener(v -> Toast.makeText(this, "Sharing to Instagram...", Toast.LENGTH_SHORT).show());
    }
}
