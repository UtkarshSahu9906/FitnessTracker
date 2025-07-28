package com.utkarsh.fitnesstracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private TextView tvSteps, tvDistance, tvCalories;
    private Button btnReset;
    private LineChart chart;

    private int totalSteps = 0;
    private int previousTotalSteps = 0;
    private float strideLength = 0.762f; // Average stride length in meters (can be made configurable)
    private float weight = 70f; // User weight in kg (can be made configurable)

    private ArrayList<Entry> stepEntries = new ArrayList<>();
    private ArrayList<String> labels = new ArrayList<>();
    private int dataPoints = 0;
    private final int MAX_DATA_POINTS = 7; // Show last 7 data points

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvSteps = findViewById(R.id.tvSteps);
        tvDistance = findViewById(R.id.tvDistance);
        tvCalories = findViewById(R.id.tvCalories);
        btnReset = findViewById(R.id.btnReset);
        chart = findViewById(R.id.chart);

        setupChart();
        loadData();
        resetSteps();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        if (stepCounterSensor == null) {
            Toast.makeText(this, "No step counter sensor detected on this device", Toast.LENGTH_SHORT).show();
        }

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetSteps();
                saveData();
                updateChart();
            }
        });

        // Check and request permission
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                    1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        saveData();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            totalSteps = (int) event.values[0];
            int currentSteps = totalSteps - previousTotalSteps;

            // Update UI
            tvSteps.setText("Steps: " + currentSteps);

            // Calculate distance (steps * stride length in km)
            float distance = currentSteps * strideLength / 1000;
            tvDistance.setText(String.format(Locale.getDefault(), "Distance: %.2f km", distance));

            // Calculate calories (steps * 0.04 * weight in kg)
            float calories = currentSteps * 0.04f * weight;
            tvCalories.setText(String.format(Locale.getDefault(), "Calories: %.1f kcal", calories));

            // Update chart data periodically
            if (dataPoints % 10 == 0) { // Update every 10 sensor updates to reduce frequency
                updateChartData(currentSteps);
            }
            dataPoints++;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed for this implementation
    }

    private void resetSteps() {
        previousTotalSteps = totalSteps;
        tvSteps.setText("Steps: 0");
        tvDistance.setText("Distance: 0.0 km");
        tvCalories.setText("Calories: 0 kcal");
    }

    private void saveData() {
        getSharedPreferences("FitnessPrefs", MODE_PRIVATE)
                .edit()
                .putInt("previousTotalSteps", previousTotalSteps)
                .putInt("totalSteps", totalSteps)
                .apply();
    }

    private void loadData() {
        previousTotalSteps = getSharedPreferences("FitnessPrefs", MODE_PRIVATE)
                .getInt("previousTotalSteps", 0);
        totalSteps = getSharedPreferences("FitnessPrefs", MODE_PRIVATE)
                .getInt("totalSteps", 0);
    }

    private void setupChart() {
        Description description = new Description();
        description.setText("Step Tracking History");
        chart.setDescription(description);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));

        chart.getAxisRight().setEnabled(false);
    }

    private void updateChartData(int currentSteps) {
        // Get current time for label
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String currentTime = sdf.format(new Date());

        // Add new entry
        stepEntries.add(new Entry(stepEntries.size(), currentSteps));
        labels.add(currentTime);

        // Limit the number of data points
        if (stepEntries.size() > MAX_DATA_POINTS) {
            stepEntries.remove(0);
            labels.remove(0);
        }

        updateChart();
    }

    private void updateChart() {
        LineDataSet dataSet = new LineDataSet(stepEntries, "Steps");
        dataSet.setColor(getResources().getColor(android.R.color.holo_blue_dark));
        dataSet.setValueTextColor(getResources().getColor(android.R.color.black));
        dataSet.setCircleColor(getResources().getColor(android.R.color.holo_blue_dark));

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);

        // Update X-axis labels
        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));

        chart.invalidate(); // refresh
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission denied - step counting may not work", Toast.LENGTH_SHORT).show();
            }
        }
    }
}