package hu.elte.resnet18hybridtester;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.*;
import java.util.*;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

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

        // Egyenlőre csak dummy gombok (nem csinálnak semmit)
        cameraButton.setOnClickListener(v -> {
            resultText.setText("Camera: ide jön majd a fotózás logika!");
            imageView.setImageBitmap(null);
        });

        galleryButton.setOnClickListener(v -> {
            resultText.setText("Gallery: ide jön majd a galéria logika!");
            imageView.setImageBitmap(null);
        });
    }

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
