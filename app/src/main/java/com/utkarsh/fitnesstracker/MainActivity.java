package com.utkarsh.fitnesstracker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor stepCounterSensor;

    // Views
    private TextView tvSteps, tvDistance, tvCalories, tvGoal;
    private CircularProgressIndicator progressCircle;
    private BarChart chart;
    private FloatingActionButton fabReset;
    private LottieAnimationView lottieSteps, lottieDistance, lottieCalories;

    // Data
    private int totalSteps = 0;
    private int previousTotalSteps = 0;
    private final int dailyGoal = 10000;
    private final float strideLength = 0.762f; // meters
    private final float weight = 70f; // kg

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupChart();
        setupSensors();
        setupClickListeners();
    }

    private void initializeViews() {
        // Required views
        tvSteps = findViewById(R.id.tvSteps);
        progressCircle = findViewById(R.id.progressCircle);
        chart = findViewById(R.id.chart);
        fabReset = findViewById(R.id.fabReset);
        tvGoal = findViewById(R.id.tvGoal);

        // Optional views with null checks
        tvDistance = findViewById(R.id.tvDistance);
        tvCalories = findViewById(R.id.tvCalories);

        lottieSteps = findViewById(R.id.lottieSteps);
        lottieDistance = findViewById(R.id.lottieDistance);
        lottieCalories = findViewById(R.id.lottieCalories);

        // Initialize progress
        progressCircle.setProgress(0);
        updateGoalText(0);
    }

    private void updateGoalText(int steps) {
        if (tvGoal != null) {
            tvGoal.setText(String.format(Locale.getDefault(), "%,d / %,d steps", steps, dailyGoal));
        }
    }

    private void setupSensors() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        }

        if (stepCounterSensor == null) {
            Log.e("FitnessTracker", "No step counter sensor found");
            // Show appropriate UI message if needed
        }
    }

    private void setupChart() {
        if (chart == null) return;

        chart.getDescription().setEnabled(false);
        chart.setDrawBarShadow(false);
        chart.setDrawValueAboveBar(true);
        chart.setPinchZoom(false);
        chart.setDrawGridBackground(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(7);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"}));

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setLabelCount(8, false);
        leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        leftAxis.setSpaceTop(15f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setGranularity(1000f);

        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setEnabled(false);

        // Sample data
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, 4500));
        entries.add(new BarEntry(1, 7200));
        entries.add(new BarEntry(2, 9800));
        entries.add(new BarEntry(3, 11000));
        entries.add(new BarEntry(4, 8500));
        entries.add(new BarEntry(5, 6500));
        entries.add(new BarEntry(6, 12000));

        BarDataSet dataSet = new BarDataSet(entries, "Steps");
        dataSet.setColor(ContextCompat.getColor(this, R.color.primary));

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.9f);
        chart.setData(data);
        chart.setFitBars(true);
        chart.animateY(1500);
    }

    private void setupClickListeners() {
        if (fabReset != null) {
            fabReset.setOnClickListener(v -> {
                previousTotalSteps = totalSteps;
                updateUI();
                animateReset();
            });
        }
    }

    private void animateReset() {
        if (fabReset != null) {
            fabReset.animate().rotationBy(360).setDuration(500).start();
        }

        if (lottieSteps != null) lottieSteps.playAnimation();
        if (lottieDistance != null) lottieDistance.playAnimation();
        if (lottieCalories != null) lottieCalories.playAnimation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            totalSteps = (int) event.values[0];
            runOnUiThread(this::updateUI);
        }
    }

    private void updateUI() {
        int currentSteps = totalSteps - previousTotalSteps;

        // Update steps
        if (tvSteps != null) {
            tvSteps.setText(String.format(Locale.getDefault(), "%,d", currentSteps));
        }

        // Update distance
        if (tvDistance != null) {
            float distance = currentSteps * strideLength / 1000;
            tvDistance.setText(String.format(Locale.getDefault(), "%.2f km", distance));
        }

        // Update calories
        if (tvCalories != null) {
            float calories = currentSteps * 0.04f * weight;
            tvCalories.setText(String.format(Locale.getDefault(), "%.0f kcal", calories));
        }

        // Update progress (0-100)
        int progress = Math.min(100, (int) (((float) currentSteps / dailyGoal) * 100));
        if (progressCircle != null) {
            progressCircle.setProgress(progress);
        }

        updateGoalText(currentSteps);
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }
}