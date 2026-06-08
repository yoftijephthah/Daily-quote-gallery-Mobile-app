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

    private final int[] allImages = {
            // Inspirational / Life / Love Quotes (i prefix)
            R.drawable.i2, R.drawable.i3, R.drawable.i4, R.drawable.i5,
            R.drawable.i6, R.drawable.i7, R.drawable.i8, R.drawable.i9, R.drawable.i10,
            R.drawable.i11, R.drawable.i12, R.drawable.i13, R.drawable.i14, R.drawable.i16,
            R.drawable.i17, R.drawable.i18, R.drawable.i19, R.drawable.i20, R.drawable.i21,
            R.drawable.i22, R.drawable.i23, R.drawable.i24, R.drawable.i25,

            // Motivational / Business / Success Quotes (m prefix)
            R.drawable.m1, R.drawable.m2, R.drawable.m3, R.drawable.m4, R.drawable.m5,
            R.drawable.m6, R.drawable.m7, R.drawable.m8, R.drawable.m9, R.drawable.m10,
            R.drawable.m11, R.drawable.m12, R.drawable.m13, R.drawable.m15,

            // Travel Quotes (t prefix)
            R.drawable.t1, R.drawable.t2, R.drawable.t3, R.drawable.t4, R.drawable.t6,
            R.drawable.t7, R.drawable.t8, R.drawable.t9, R.drawable.t10, R.drawable.t11,
            R.drawable.t12, R.drawable.t14, R.drawable.t15,

            // Spiritual Quotes (isp prefix)
            R.drawable.isp1, R.drawable.isp2, R.drawable.isp3, R.drawable.isp4, R.drawable.isp5,
            R.drawable.isp6, R.drawable.isp7, R.drawable.isp8, R.drawable.isp9, R.drawable.isp10,
            R.drawable.isp11, R.drawable.isp12, R.drawable.isp13, R.drawable.isp14, R.drawable.isp15,



    };

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
    private boolean skipReset = false; // Prevents reloading all images when switching tabs after filtering
    private boolean isFromCategory = false; // Tracks if we are currently in a filtered category view

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
        } else if (!skipReset) {
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
            } else {
                showImagesForCategory(selected);
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
                if (!skipReset) {
                    showAllImages();
                    isFromCategory = false;
                }
                skipReset = false;
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

    private void showImagesForCategory(String category) {
        currentImages.clear();
        String prefix = "";

        // Map category names to resource prefixes
        if (category.equals("Travel Quotes")) prefix = "t";
        else if (category.equals("Motivational Quotes") || category.equals("Business Quotes") || category.equals("Success Quotes")) prefix = "m";
        else if (category.equals("Spiritual Quotes")) prefix = "isp";
        else if (category.equals("Inspirational Quotes") || category.equals("Life Quotes") || category.equals("Love Quotes")) prefix = "i";

        if (!prefix.isEmpty()) {
            for (int img : allImages) {
                try {
                    String name = getResources().getResourceEntryName(img);
                    if (name.startsWith(prefix)) {
                        currentImages.add(img);
                    }
                } catch (Exception ignored) {}
            }
        } else {
            for (int img : allImages) currentImages.add(img);
        }

        if (gridAdapter != null) gridAdapter.notifyDataSetChanged();
        
        // Switch to the Quotes tab (Grid) to show the filtered results
        isFromCategory = true;
        skipReset = true;
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_quotes);
    }

    @Override
    public void onBackPressed() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        int currentId = bottomNav.getSelectedItemId();

        if (currentId == R.id.nav_quotes) {
            if (isFromCategory) {
                // If we came from categories and are viewing filtered results, go back to Categories
                bottomNav.setSelectedItemId(R.id.nav_categories);
                isFromCategory = false;
            } else {
                // Standard app exit
                super.onBackPressed();
            }
        } else {
            // If on any other tab (Favorites, Categories, More), return to the primary Home tab
            bottomNav.setSelectedItemId(R.id.nav_quotes);
        }
    }

    private void showMoreInfo() {
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Developer Information")
                .setMessage("Name: Yoftahe Ayiso\nID:RMD2607\nDepartment: IT\nEmail: yoftijephthah@gmail.com")
                .setPositiveButton("OK", null)
                .create();
        
        dialog.show();

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
