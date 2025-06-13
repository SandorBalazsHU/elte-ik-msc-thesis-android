package hu.elte.resnet18hybridtester;

import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private Spinner modelSpinner, sourceSpinner, exampleSpinner;
    private Button cameraButton, galleryButton;
    private ImageView imageView;
    private TextView resultText;

    // A három modell
    private final String[] modelNames = {"Baseline", "Deep", "Hybrid"};
    // Példaképek az assets-ben (állítsd be a tényleges nevet, ha más)
    private String[] exampleImages = {"test01.jpg", "test02.jpg", "test03.jpg", "test04.jpg", "test05.jpg",
            "test06.jpg", "test07.jpg", "test08.jpg", "test09.jpg", "test10.jpg"};
    private Bitmap currentBitmap = null;

    // Kamera request code
    private static final int REQUEST_IMAGE_CAPTURE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        modelSpinner = findViewById(R.id.modelSpinner);
        sourceSpinner = findViewById(R.id.sourceSpinner);
        exampleSpinner = findViewById(R.id.exampleSpinner);
        cameraButton = findViewById(R.id.cameraButton);
        galleryButton = findViewById(R.id.galleryButton);
        imageView = findViewById(R.id.imageView);
        resultText = findViewById(R.id.resultText);

        // Modell spinner
        ArrayAdapter<String> modelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, modelNames);
        modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modelSpinner.setAdapter(modelAdapter);

        // Source spinner
        ArrayAdapter<String> sourceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[]{"Camera", "Gallery", "Examples"});
        sourceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sourceSpinner.setAdapter(sourceAdapter);

        // Példaképek spinner
        ArrayAdapter<String> exampleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, exampleImages);
        exampleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        exampleSpinner.setAdapter(exampleAdapter);

        // Source választó logika
        sourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                cameraButton.setVisibility(View.GONE);
                galleryButton.setVisibility(View.GONE);
                exampleSpinner.setVisibility(View.GONE);

                if (position == 0) { // Camera
                    cameraButton.setVisibility(View.VISIBLE);
                    imageView.setImageBitmap(null);
                    resultText.setText("");
                } else if (position == 1) { // Gallery
                    galleryButton.setVisibility(View.VISIBLE);
                    imageView.setImageBitmap(null);
                    resultText.setText("");
                } else if (position == 2) { // Examples
                    exampleSpinner.setVisibility(View.VISIBLE);
                    imageView.setImageBitmap(null);
                    resultText.setText("");
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        // Példaképek választása esetén
        exampleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadExampleImage(position);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        // KAMERA gomb logikája:
        cameraButton.setOnClickListener(v -> {
            resultText.setText(""); // töröld az előző üzenetet
            imageView.setImageBitmap(null);

            Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            } else {
                resultText.setText("Camera not available!");
            }
        });

        // Galéria gomb logika majd később jön!
        galleryButton.setOnClickListener(v -> {
            resultText.setText("Gallery: ide jön majd a galéria logika!");
            imageView.setImageBitmap(null);
        });
    }

    // Kamera eredmény feldolgozása (Android 11-ig működik így!)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            // Átméretezzük 224x224-re
            Bitmap scaled = Bitmap.createScaledBitmap(imageBitmap, 224, 224, true);
            imageView.setImageBitmap(scaled);
            currentBitmap = scaled;
            resultText.setText("Kép elkészült, készen áll a predikcióra!");
        }
    }

    // Példakép betöltő függvény
    private void loadExampleImage(int position) {
        if (exampleImages == null || exampleImages.length == 0) return;
        String imageName = exampleImages[position];
        try {
            AssetManager assetManager = getAssets();
            InputStream istr = assetManager.open(imageName);
            Bitmap bitmap = BitmapFactory.decodeStream(istr);
            // 224x224-re átméretezve
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 224, 224, true);
            imageView.setImageBitmap(scaled);
            currentBitmap = scaled;
            resultText.setText("Loaded: " + imageName);
        } catch (IOException e) {
            resultText.setText("Hiba: " + e.getMessage());
            imageView.setImageBitmap(null);
            currentBitmap = null;
        }
    }
}
