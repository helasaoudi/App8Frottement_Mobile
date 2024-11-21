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
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private int frottementCounter = 0; // Compteur de frottements
    private long lastUpdate = 0; // Temps du dernier frottement
    private static final int SHAKE_THRESHOLD = 800; // Sensibilité
    private int targetFrottements = 3; // Nombre de frottements par défaut

    private ListView listView;
    private ArrayAdapter<String> adapter;
    private String[] actions = {
            "Ouvrir la caméra",
            "Allumer la lampe torche",
            "Répondre à un appel",
            "Lire/Arrêter la musique",
            "Appeler un numéro d'urgence"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.list_actions);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, actions);
        listView.setAdapter(adapter);

        // Initialiser l'accéléromètre
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometer != null) {
                sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                Toast.makeText(this, "Aucun accéléromètre détecté", Toast.LENGTH_SHORT).show();
            }
        }

        // Gérer les clics sur les éléments de la liste
        listView.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> showEditDialog(position));
    }

    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                long currentTime = System.currentTimeMillis();
                long diffTime = currentTime - lastUpdate;

                if (diffTime > 100) { // Limite de temps pour détecter les frottements
                    lastUpdate = currentTime;

                    float x = event.values[0];
                    float y = event.values[1];
                    float z = event.values[2];

                    float acceleration = (x * x + y * y + z * z)
                            / SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH;

                    if (acceleration > SHAKE_THRESHOLD) {
                        frottementCounter++;
                        Toast.makeText(MainActivity.this, "Frottement détecté : " + frottementCounter, Toast.LENGTH_SHORT).show();

                        // Vérifiez si le nombre de frottements est atteint
                        if (frottementCounter >= targetFrottements) {
                            performAction();
                            frottementCounter = 0; // Réinitialiser le compteur
                        }
                    }
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    private void performAction() {
        switch (targetFrottements) {
            case 1: // Exemple : Ouvrir la caméra
                openCamera();
                break;

            case 2: // Exemple : Allumer la lampe torche
                toggleFlashlight();
                break;

            case 3: // Exemple : Appeler un numéro d'urgence
                makeEmergencyCall();
                break;

            default:
                Toast.makeText(this, "Aucune action définie pour ce nombre de frottements.", Toast.LENGTH_SHORT).show();
        }
    }

    private void openCamera() {
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "Impossible d'ouvrir la caméra", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleFlashlight() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = cameraManager.getCameraIdList()[0];
            cameraManager.setTorchMode(cameraId, true); // Allume la lampe torche
            Toast.makeText(this, "Lampe torche activée", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Erreur : Impossible d'activer la lampe torche", Toast.LENGTH_SHORT).show();
        }
    }

    private MediaPlayer mediaPlayer;

    /* private void toggleMusic() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.sample_music); // Assurez-vous d'avoir un fichier MP3 dans `res/raw`
            mediaPlayer.start();
            Toast.makeText(this, "Musique démarrée", Toast.LENGTH_SHORT).show();
        } else {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            Toast.makeText(this, "Musique arrêtée", Toast.LENGTH_SHORT).show();
        }
    } */

    private void makeEmergencyCall() {
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:112")); // Exemple : numéro d'urgence
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            startActivity(intent);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, 1);
        }
    }

    private void showEditDialog(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Configurer les frottements pour : " + actions[position]);

        String[] options = {"1 frottement", "2 frottements", "3 frottements", "4 frottements"};
        builder.setItems(options, (dialog, which) -> {
            targetFrottements = which + 1;
            Toast.makeText(this, "Action '" + actions[position] + "' configurée pour " + targetFrottements + " frottements", Toast.LENGTH_SHORT).show();
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
