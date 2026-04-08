package com.example.firebase;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.firebase.adapters.MovieAdapter;
import com.example.firebase.databinding.ActivityMainBinding;
import com.example.firebase.models.Movie;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements MovieAdapter.OnMovieClickListener {

    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private MovieAdapter adapter;
    private List<Movie> movieList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // --- PHẦN LẤY FCM TOKEN ---
        getAndSaveFCMToken();

        binding.btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });

        setupRecyclerView();
        loadMovies();
    }

    private void getAndSaveFCMToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    // Lấy token thành công
                    String token = task.getResult();
                    String userId = mAuth.getCurrentUser().getUid();

                    // Lưu token vào Firestore của User này
                    Map<String, Object> tokenData = new HashMap<>();
                    tokenData.put("fcmToken", token);

                    db.collection("users").document(userId)
                            .update(tokenData)
                            .addOnSuccessListener(aVoid -> Log.d("FCM", "Token updated successfully"));
                });
    }

    private void setupRecyclerView() {
        movieList = new ArrayList<>();
        adapter = new MovieAdapter(movieList, this);
        binding.rvMovies.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMovies.setAdapter(adapter);
    }

    private void loadMovies() {
        db.collection("movies")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        movieList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Movie movie = document.toObject(Movie.class);
                            movie.setId(document.getId());
                            movieList.add(movie);
                        }
                        adapter.notifyDataSetChanged();
                        
                        if (movieList.isEmpty()) {
                            addDummyData();
                        }
                    } else {
                        Toast.makeText(this, "Error getting movies: " + task.getException(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addDummyData() {
        Movie m1 = new Movie(null, "Avengers: Endgame", "The grave course of events set in motion by Thanos...", "", "Action/Sci-Fi", 4.8);
        Movie m2 = new Movie(null, "Joker", "In Gotham City, mentally troubled comedian Arthur Fleck...", "", "Drama/Thriller", 4.5);
        
        db.collection("movies").add(m1);
        db.collection("movies").add(m2).addOnSuccessListener(documentReference -> loadMovies());
    }

    @Override
    public void onBookClick(Movie movie) {
        Intent intent = new Intent(this, BookingActivity.class);
        intent.putExtra("movie", movie);
        startActivity(intent);
    }
}