package com.sbs.ui;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.sbs.R;
import com.sbs.data.FieldDataStore;

public class NewSightingActivity extends AppCompatActivity {

    private TextInputEditText etTitle;
    private TextInputEditText etLatitude;
    private TextInputEditText etLongitude;
    private TextInputEditText etNotes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_sighting);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        etTitle = findViewById(R.id.etSightingTitle);
        etLatitude = findViewById(R.id.etLatitude);
        etLongitude = findViewById(R.id.etLongitude);
        etNotes = findViewById(R.id.etNotes);
        MaterialButton btnSave = findViewById(R.id.btnSaveSighting);

        toolbar.setNavigationOnClickListener(v -> finish());

        double lat = getIntent().getDoubleExtra("lat", 0.0);
        double lng = getIntent().getDoubleExtra("lng", 0.0);

        if (lat != 0.0 || lng != 0.0) {
            etLatitude.setText(String.valueOf(lat));
            etLongitude.setText(String.valueOf(lng));
        }

        btnSave.setOnClickListener(v -> saveSighting());
    }

    private void saveSighting() {
        String title = valueOf(etTitle);
        String latText = valueOf(etLatitude);
        String lngText = valueOf(etLongitude);
        String notes = valueOf(etNotes);

        if (title.isEmpty() || latText.isEmpty() || lngText.isEmpty()) {
            Toast.makeText(this, "Title, latitude, and longitude are required", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double lat = Double.parseDouble(latText);
            double lng = Double.parseDouble(lngText);

            FieldDataStore.saveSighting(this, title, lat, lng, notes);
            Toast.makeText(this, "Sighting saved locally", Toast.LENGTH_SHORT).show();
            finish();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Latitude/longitude must be valid numbers", Toast.LENGTH_SHORT).show();
        }
    }

    private String valueOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }
}
