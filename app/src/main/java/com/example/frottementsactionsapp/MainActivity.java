package com.example.frottementsactionsapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private int frottementCounter = 0;
    private long lastUpdate = 0;
    private static final int SHAKE_THRESHOLD = 800;

    private ListView listView;
    private ActionAdapter adapter;
    private ArrayList<ActionItem> actions;
    private HashMap<Integer, Integer> frottementActions;
    private int frottementCount = 0;
    private Handler handler = new Handler();
    private Runnable frottementRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        frottementActions = new HashMap<>();
        actions = new ArrayList<>();
        actions.add(new ActionItem("Ouvrir la caméra", 0));
        actions.add(new ActionItem("Allumer la lampe torche", 0));
        actions.add(new ActionItem("Répondre à un appel", 0));
        actions.add(new ActionItem("Lire/Arrêter la musique", 0));
        actions.add(new ActionItem("Appeler un numéro d'urgence", 0));

        listView = findViewById(R.id.list_actions);
        adapter = new ActionAdapter(this, actions);
        listView.setAdapter(adapter);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometer != null) {
                sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }

        listView.setOnItemClickListener((parent, view, position, id) -> showEditDialog(position));
        checkPermissions();

    }

    private void checkPermissions() {
        String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.CALL_PHONE};
        ArrayList<String> permissionsToRequest = new ArrayList<>();

        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), 1);
        }
    }

    private void onFrottementDetected() {
        frottementCount++;

        // Supprimez les callbacks en attente
        if (frottementRunnable != null) {
            handler.removeCallbacks(frottementRunnable);
        }

        // Reprogrammez pour exécuter après 1 seconde
        frottementRunnable = () -> {
            executeActionBasedOnFrottementCount();
            frottementCount = 0; // Réinitialisez le compteur
        };

        handler.postDelayed(frottementRunnable, 1000); // Attendre 1 seconde avant de décider
    }


    private void executeActionBasedOnFrottementCount() {
        if (frottementActions.containsKey(frottementCount)) {
            int actionPosition = frottementActions.get(frottementCount);
            performAction(actionPosition);
        } else {
            Toast.makeText(this, "Aucune action assignée pour " + frottementCount + " frottements", Toast.LENGTH_SHORT).show();
        }
    }


    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                long currentTime = System.currentTimeMillis();
                long diffTime = currentTime - lastUpdate;

                if (diffTime > 100) {
                    lastUpdate = currentTime;

                    float x = event.values[0];
                    float y = event.values[1];
                    float z = event.values[2];

                    float acceleration = (x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH;

                    if (acceleration > SHAKE_THRESHOLD) {
                        onFrottementDetected(); // Appeler la fonction pour gérer les frottements
                    }
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    private void performAction(int position) {
        switch (position) {
            case 0:
                openCamera();
                break;
            case 1:
                toggleFlashlight();
                break;
            case 2:
                Toast.makeText(this, "Répondre à un appel : fonctionnalité non implémentée", Toast.LENGTH_SHORT).show();
                break;
            case 3:
                Toast.makeText(this, "Lire/Arrêter la musique : fonctionnalité non implémentée", Toast.LENGTH_SHORT).show();
                break;
            case 4:
                makeEmergencyCall();
                break;
            default:
                Toast.makeText(this, "Action non définie", Toast.LENGTH_SHORT).show();
        }
    }

    private void openCamera() {
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "Aucune application caméra trouvée", Toast.LENGTH_SHORT).show();
        }
    }


    private boolean isTorchOn = false;

    private void toggleFlashlight() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = cameraManager.getCameraIdList()[0];
            isTorchOn = !isTorchOn;
            cameraManager.setTorchMode(cameraId, isTorchOn);
        } catch (Exception e) {
            Toast.makeText(this, "Erreur : Impossible de gérer la lampe torche", Toast.LENGTH_SHORT).show();
        }
    }

    private void makeEmergencyCall() {
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:112"));
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            startActivity(intent);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, 1);
        }
    }

    private boolean isFrottementUsed(int frottements) {
        for (ActionItem action : actions) {
            if (action.getFrottements() == frottements && action.isEnabled()) {
                return true;
            }
        }
        return false;
    }

    private void showEditDialog(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Configurer les frottements pour : " + actions.get(position).getTitle());

        String[] options = {"1 frottement", "2 frottements", "3 frottements", "4 frottements"};
        builder.setItems(options, (dialog, which) -> {
            int selectedFrottements = which + 1;
            if (frottementActions.containsKey(selectedFrottements)) {
                int existingPosition = frottementActions.get(selectedFrottements);
                actions.get(existingPosition).setFrottements(0);
            }
            frottementActions.put(selectedFrottements, position);
            actions.get(position).setFrottements(selectedFrottements);
            adapter.notifyDataSetChanged();
        });

        builder.setNegativeButton("Annuler", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(sensorEventListener);
        }
    }
}
