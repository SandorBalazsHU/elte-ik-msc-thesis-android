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

    private final String[] modelNames = {"Baseline", "Deep", "Hybrid"};
    private String[] exampleImages = {"test01.jpg", "test02.jpg", "test03.jpg", "test04.jpg", "test05.jpg",
            "test06.jpg", "test07.jpg", "test08.jpg", "test09.jpg", "test10.jpg"};
    private Bitmap currentBitmap = null;

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_GALLERY_IMAGE = 2;

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

        ArrayAdapter<String> modelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, modelNames);
        modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modelSpinner.setAdapter(modelAdapter);

        ArrayAdapter<String> sourceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[]{"Camera", "Gallery", "Examples"});
        sourceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sourceSpinner.setAdapter(sourceAdapter);

        ArrayAdapter<String> exampleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, exampleImages);
        exampleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        exampleSpinner.setAdapter(exampleAdapter);

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

        exampleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadExampleImage(position);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        cameraButton.setOnClickListener(v -> {
            resultText.setText("");
            imageView.setImageBitmap(null);
            Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            } else {
                resultText.setText("Camera not available!");
            }
        });

        galleryButton.setOnClickListener(v -> {
            resultText.setText("");
            imageView.setImageBitmap(null);
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_GALLERY_IMAGE);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            Bitmap scaled = Bitmap.createScaledBitmap(imageBitmap, 224, 224, true);
            imageView.setImageBitmap(scaled);
            currentBitmap = scaled;
            resultText.setText("Kép elkészült, készen áll a predikcióra!");
        }
        else if (requestCode == REQUEST_GALLERY_IMAGE && resultCode == RESULT_OK && data != null) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(data.getData());
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 224, 224, true);
                imageView.setImageBitmap(scaled);
                currentBitmap = scaled;
                resultText.setText("Galériaképet betöltöttük, készen áll a predikcióra!");
                inputStream.close();
            } catch (IOException e) {
                resultText.setText("Galéria hiba: " + e.getMessage());
                imageView.setImageBitmap(null);
                currentBitmap = null;
            }
        }
    }

    private void loadExampleImage(int position) {
        if (exampleImages == null || exampleImages.length == 0) return;
        String imageName = exampleImages[position];
        try {
            AssetManager assetManager = getAssets();
            InputStream istr = assetManager.open(imageName);
            Bitmap bitmap = BitmapFactory.decodeStream(istr);
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
