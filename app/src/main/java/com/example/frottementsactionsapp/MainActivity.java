package com.example.frottementsactionsapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.telephony.SmsManager;
import android.widget.ListView;
import android.widget.Toast;
import android.net.wifi.WifiManager;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
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
    private static final int SCREENSHOT_REQUEST_CODE = 1000;
    private MediaProjectionManager mProjectionManager;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private ImageReader mImageReader;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        frottementActions = new HashMap<>();
        actions = new ArrayList<>();
        actions.add(new ActionItem("Ouvrir la caméra", 0));
        actions.add(new ActionItem("Allumer la lampe torche", 0));
        actions.add(new ActionItem("Prendre une capture d'écran", 0));
        actions.add(new ActionItem("Vibrer le téléphone", 0));
        actions.add(new ActionItem("Allumer/Éteindre le Wi-Fi", 0));
        actions.add(new ActionItem("Enregistrer un audio", 0));
        actions.add(new ActionItem("Ouvrir la boîte email", 0));
        actions.add(new ActionItem("Éteindre l'écran", 0));
        actions.add(new ActionItem("Envoyer un SMS", 0));
        actions.add(new ActionItem("Lancer une application", 0));

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
        String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.CALL_PHONE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.SEND_SMS, Manifest.permission.READ_EXTERNAL_STORAGE};
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

        if (frottementRunnable != null) {
            handler.removeCallbacks(frottementRunnable);
        }

        frottementRunnable = () -> {
            executeActionBasedOnFrottementCount();
            frottementCount = 0;
        };

        handler.postDelayed(frottementRunnable, 1000);
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
                        onFrottementDetected();
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
                takeScreenshot();
                break;
            case 3:
                vibratePhone();
                break;
            case 4:
                toggleWiFi();
                break;
            case 5:
                recordAudio();
                break;
            case 6:
                openEmail();
                break;
            case 7:
                turnOffScreen();
                break;
            case 8:
                sendSMS();
                break;
            case 9:
                launchApp();
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

    private void takeScreenshot() {
        if (mProjectionManager == null) {
            mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        }

        if (mProjectionManager != null) {
            Intent captureIntent = mProjectionManager.createScreenCaptureIntent();
            startActivityForResult(captureIntent, SCREENSHOT_REQUEST_CODE);
        } else {
            Toast.makeText(this, "Capture d'écran non supportée", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SCREENSHOT_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
                mImageReader = ImageReader.newInstance(
                        getResources().getDisplayMetrics().widthPixels,
                        getResources().getDisplayMetrics().heightPixels,
                        PixelFormat.RGBA_8888, 2);
                mVirtualDisplay = mMediaProjection.createVirtualDisplay("Screenshot",
                        getResources().getDisplayMetrics().widthPixels,
                        getResources().getDisplayMetrics().heightPixels,
                        getResources().getDisplayMetrics().densityDpi,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        mImageReader.getSurface(), null, null);

                mImageReader.setOnImageAvailableListener(reader -> {
                    try (Image image = reader.acquireNextImage()) {
                        saveScreenshot(image);
                    }
                }, handler);
            } else {
                Toast.makeText(this, "Capture d'écran refusée", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveScreenshot(Image image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        File screenshotFile = new File(getExternalFilesDir(null), "screenshot.png");
        try (FileOutputStream fos = new FileOutputStream(screenshotFile)) {
            fos.write(bytes);
            Toast.makeText(this, "Capture d'écran enregistrée", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Erreur lors de la capture d'écran", Toast.LENGTH_SHORT).show();
        }
    }

    private void vibratePhone() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    private void toggleWiFi() {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            boolean isEnabled = wifiManager.isWifiEnabled();
            wifiManager.setWifiEnabled(!isEnabled);
            Toast.makeText(this, "Wi-Fi " + (isEnabled ? "désactivé" : "activé"), Toast.LENGTH_SHORT).show();
        }
    }

    private void recordAudio() {
        // Ajoutez le code pour enregistrer l'audio ici
    }

    private void openEmail() {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"));
        startActivity(Intent.createChooser(emailIntent, "Choisir une application email"));
    }

    private void turnOffScreen() {
        // Logique pour éteindre l'écran
    }

    private void sendSMS() {
        String phoneNumber = "1234567890";
        String message = "Test de l'application SMS";

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Toast.makeText(this, "SMS envoyé", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Permission SMS non accordée", Toast.LENGTH_SHORT).show();
        }
    }

    private void launchApp() {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.example.app");
        if (launchIntent != null) {
            startActivity(launchIntent);
        } else {
            Toast.makeText(this, "Application non trouvée", Toast.LENGTH_SHORT).show();
        }
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
