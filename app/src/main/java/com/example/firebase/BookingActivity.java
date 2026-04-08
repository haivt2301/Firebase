package com.example.firebase;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.firebase.databinding.ActivityBookingBinding;
import com.example.firebase.models.Movie;
import com.example.firebase.models.Showtime;
import com.example.firebase.models.Ticket;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BookingActivity extends AppCompatActivity {

    private ActivityBookingBinding binding;
    private Movie movie;
    private List<Showtime> showtimes;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBookingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        movie = (Movie) getIntent().getSerializableExtra("movie");
        if (movie != null) {
            binding.tvMovieTitle.setText(movie.getTitle());
            loadShowtimes();
        }

        binding.btnConfirmBooking.setOnClickListener(v -> confirmBooking());
    }

    private void loadShowtimes() {
        showtimes = new ArrayList<>();
        db.collection("showtimes")
                .whereEqualTo("movieId", movie.getId())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> timeLabels = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Showtime st = doc.toObject(Showtime.class);
                        st.setId(doc.getId());
                        showtimes.add(st);
                        timeLabels.add(st.getTime() + " ($" + st.getPrice() + ")");
                    }

                    if (showtimes.isEmpty()) {
                        // Dummy showtime if none exist
                        addDummyShowtime();
                    } else {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                                android.R.layout.simple_spinner_item, timeLabels);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        binding.spShowtimes.setAdapter(adapter);
                    }
                });
    }

    private void addDummyShowtime() {
        Showtime st = new Showtime(null, movie.getId(), "theater1", "Tonight 20:00", 12.0);
        db.collection("showtimes").add(st).addOnSuccessListener(ref -> loadShowtimes());
    }

    private void confirmBooking() {
        if (showtimes.isEmpty()) return;

        String seat = binding.etSeat.getText().toString().trim();
        if (TextUtils.isEmpty(seat)) {
            binding.etSeat.setError("Enter seat number");
            return;
        }

        Showtime selectedShowtime = showtimes.get(binding.spShowtimes.getSelectedItemPosition());
        String userId = mAuth.getCurrentUser().getUid();

        Ticket ticket = new Ticket(null, userId, selectedShowtime.getId(), seat, 
                new Date().toString(), selectedShowtime.getPrice());

        db.collection("tickets").add(ticket)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Booking Successful!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Booking Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}