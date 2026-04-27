package com.yofi.dailyquotegallery;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private final int[] allImages = {R.drawable.i1, R.drawable.i2, R.drawable.i3, R.drawable.i4,
            R.drawable.i5, R.drawable.i6, R.drawable.i7, R.drawable.i8};

    private final String[] initialCategories = {
            "Business Quotes", "Inspirational Quotes", "Life Quotes",
            "Love Quotes", "Motivational Quotes", "Success Quotes",
            "Spiritual Quotes", "Travel Quotes"
    };

    private List<String> categoriesList;
    private Set<String> favoriteImageIds;
    private final List<Integer> currentImages = new ArrayList<>();
    private ArrayAdapter<String> categoryAdapter;
    private RecyclerView.Adapter<ViewHolder> gridAdapter;
    private SharedPreferences prefs;
    private boolean isFavoritesMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Load theme before super.onCreate
        SharedPreferences themePrefs = getSharedPreferences("theme_prefs", MODE_PRIVATE);
        boolean isDarkMode = themePrefs.getBoolean("isDarkMode", false);
        applyTheme(isDarkMode);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("favorites", Context.MODE_PRIVATE);
        favoriteImageIds = new HashSet<>(prefs.getStringSet("fav_list", new HashSet<>()));

        categoriesList = new ArrayList<>(Arrays.asList(initialCategories));

        for (int img : allImages) {
            currentImages.add(img);
        }

        setupRecyclerView();
        setupCategoryList();
        setupBottomNavigation();
        setupThemeToggle(isDarkMode);
        
        // Restore active tab
        int lastTab = themePrefs.getInt("last_tab", R.id.nav_quotes);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(lastTab);
    }

    private void applyTheme(boolean isDarkMode) {
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void setupThemeToggle(boolean isDarkMode) {
        ImageButton btnToggle = findViewById(R.id.btnToggleTheme);
        btnToggle.setImageResource(isDarkMode ? R.drawable.ic_sun : R.drawable.ic_moon);
        
        btnToggle.setOnClickListener(v -> {
            SharedPreferences themePrefs = getSharedPreferences("theme_prefs", MODE_PRIVATE);
            boolean currentlyDark = themePrefs.getBoolean("isDarkMode", false);
            boolean newValue = !currentlyDark;
            
            BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
            int currentTab = bottomNav.getSelectedItemId();
            
            themePrefs.edit()
                    .putBoolean("isDarkMode", newValue)
                    .putInt("last_tab", currentTab)
                    .apply();
            
            applyTheme(newValue);
            recreate();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        favoriteImageIds = new HashSet<>(prefs.getStringSet("fav_list", new HashSet<>()));
        
        if (isFavoritesMode) {
            showOnlyFavorites();
        } else {
            if (gridAdapter != null) gridAdapter.notifyDataSetChanged();
        }

        updateCategoryListVisibility();
    }

    private void updateCategoryListVisibility() {
        boolean hasFav = !favoriteImageIds.isEmpty();
        boolean listHasFav = categoriesList.contains("Favorite Images");

        if (hasFav && !listHasFav) {
            categoriesList.add(0, "Favorite Images");
            categoryAdapter.notifyDataSetChanged();
        } else if (!hasFav && listHasFav) {
            categoriesList.remove("Favorite Images");
            categoryAdapter.notifyDataSetChanged();
        }
    }

    private void setupRecyclerView() {
        RecyclerView rv = findViewById(R.id.myGrid);
        rv.setLayoutManager(new GridLayoutManager(this, 2));
        
        gridAdapter = new RecyclerView.Adapter<ViewHolder>() {
            @NonNull
            @Override
            public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
                View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_quote, p, false);
                return new ViewHolder(v);
            }

            @Override
            public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
                int imgResId = currentImages.get(pos);
                Glide.with(MainActivity.this).load(imgResId).into(h.img);

                h.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(MainActivity.this, DetailActivity.class);
                    intent.putExtra("img_id", imgResId);
                    startActivity(intent);
                });
            }

            @Override
            public int getItemCount() {
                return currentImages.size();
            }
        };
        
        rv.setAdapter(gridAdapter);
    }

    private void setupCategoryList() {
        boolean isDark = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES;
        
        categoryAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, categoriesList) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = view.findViewById(android.R.id.text1);
                text.setTextColor(isDark ? Color.WHITE : Color.BLACK);
                return view;
            }
        };

        ListView listView = findViewById(R.id.categoryListView);
        listView.setAdapter(categoryAdapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selected = categoriesList.get(position);
            if (selected.equals("Favorite Images")) {
                BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
                bottomNav.setSelectedItemId(R.id.nav_favorites);
            }
        });

        SearchView searchView = findViewById(R.id.searchCategories);
        EditText searchEditText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        if (searchEditText != null) {
            int textColor = isDark ? Color.WHITE : Color.BLACK;
            searchEditText.setTextColor(textColor);
            searchEditText.setHintTextColor(Color.GRAY);
            
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    searchEditText.setTextCursorDrawable(null);
                } else {
                    Field f = TextView.class.getDeclaredField("mCursorDrawableRes");
                    f.setAccessible(true);
                    f.set(searchEditText, 0);
                }
            } catch (Exception ignored) {}
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                categoryAdapter.getFilter().filter(newText);
                return false;
            }
        });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setItemIconTintList(null);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            getSharedPreferences("theme_prefs", MODE_PRIVATE).edit().putInt("last_tab", id).apply();

            if (id == R.id.nav_quotes) {
                isFavoritesMode = false;
                showAllImages();
                findViewById(R.id.myGrid).setVisibility(View.VISIBLE);
                findViewById(R.id.categoryLayout).setVisibility(View.GONE);
            } else if (id == R.id.nav_categories) {
                isFavoritesMode = false;
                findViewById(R.id.myGrid).setVisibility(View.GONE);
                findViewById(R.id.categoryLayout).setVisibility(View.VISIBLE);
            } else if (id == R.id.nav_favorites) {
                isFavoritesMode = true;
                showOnlyFavorites();
                findViewById(R.id.myGrid).setVisibility(View.VISIBLE);
                findViewById(R.id.categoryLayout).setVisibility(View.GONE);
            } else if (id == R.id.nav_more) {
                showMoreInfo();
            }
            return true;
        });
    }

    private void showAllImages() {
        currentImages.clear();
        for (int img : allImages) {
            currentImages.add(img);
        }
        if (gridAdapter != null) gridAdapter.notifyDataSetChanged();
    }

    private void showOnlyFavorites() {
        currentImages.clear();
        for (int img : allImages) {
            if (favoriteImageIds.contains(String.valueOf(img))) {
                currentImages.add(img);
            }
        }
        if (gridAdapter != null) gridAdapter.notifyDataSetChanged();
    }

    private void showMoreInfo() {
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Developer Information")
                .setMessage("Name: Yoftahe Ayiso\nID:RMD2607\nDepartment: IT\nEmail: yoftijephthah@gmail.com")
                .setPositiveButton("OK", null)
                .create();
        
        dialog.show();

        // Fix: Make "OK" button visible by setting its color explicitly
        // Since primary color is white in Light Mode, we force it to Black for visibility.
        // In Dark Mode, we can use White.
        boolean isDark = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES;
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTextColor(isDark ? Color.WHITE : Color.BLACK);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView img;

        public ViewHolder(View v) {
            super(v);
            img = v.findViewById(R.id.imgQuote);
        }
    }
}
