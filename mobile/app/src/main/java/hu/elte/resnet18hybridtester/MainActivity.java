package hu.elte.resnet18hybridtester;

import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.*;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    private Spinner modelSpinner, sourceSpinner, exampleSpinner;
    private Button cameraButton, galleryButton, runButton, resetButton;
    private ImageView imageView;
    private TextView resultText;

    private final String[] modelNames = {"Baseline", "Deep", "Hybrid", "Hybrid v2"};
    private final String[] modelFiles = {"resnet18_baseline_scripted.pt", "resnet18_deep_scripted.pt", "resnet18_hybrid_scripted.pt", "resnet18_hybrid_v2_scripted.pt"};
    private String[] exampleImages = {
            "test01.jpg", "test02.jpg", "test03.jpg", "test04.jpg", "test05.jpg",
            "test06.jpg", "test07.jpg", "test08.jpg", "test09.jpg", "test10.jpg",
            "test11.jpg", "test12.jpg", "test13.jpg", "test14.jpg", "test15.jpg",
            "test16.jpg", "test17.jpg", "test18.jpg", "test19.jpg", "test20.jpg",
            "test21.jpg", "test22.jpg", "test23.jpg", "test24.jpg", "test25.jpg",
            "test26.jpg", "test27.jpg", "test28.jpg", "test29.jpg", "test30.jpg",
            "test31.jpg", "test32.jpg", "test33.jpg", "test34.jpg", "test35.jpg",
            "test36.jpg", "test37.jpg", "test38.jpg", "test39.jpg", "test40.jpg"
    };

    private Bitmap currentBitmap = null;
    private List<String> classNames = null;

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
        runButton = findViewById(R.id.runButton);
        resetButton = findViewById(R.id.resetButton);
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
                    int sel = exampleSpinner.getSelectedItemPosition();
                    loadExampleImage(sel);
                    imageView.setImageBitmap(currentBitmap);
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
            // Próbáljuk a Camera/DCIM mappát megnyitni, ha támogatja a galéria
            intent.setData(MediaStore.Images.Media.INTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQUEST_GALLERY_IMAGE);
        });

        // RUN GOMB – predikció és időmérés
        runButton.setOnClickListener(v -> {
            if (currentBitmap == null) {
                resultText.setText("Nincs kép kiválasztva!");
                return;
            }

            resultText.setText("Modell betöltése, predikció fut...");
            new Thread(() -> {
                try {
                    // 1. Modell kiválasztása
                    int modelIdx = modelSpinner.getSelectedItemPosition();
                    String modelFile = modelFiles[modelIdx];
                    Module model = Module.load(assetFilePath(modelFile));

                    // 2. Osztálynevek betöltése (csak egyszer)
                    if (classNames == null) classNames = loadClassNames("class_labels.txt");

                    // 3. Időmérés indítása
                    long startTime = System.currentTimeMillis();

                    // 4. Predikció
                    float[] prediction = predict(model, currentBitmap);

                    // 5. Időmérés vége
                    long endTime = System.currentTimeMillis();
                    long elapsedMs = endTime - startTime;

                    // 6. Top5 eredmény szöveggé alakítása
                    String predResult = getPredictionResult(prediction);
                    String msg = predResult + "\nIdő: " + elapsedMs + " ms";

                    // 7. Kiírás a UI-ra (főszálon)
                    runOnUiThread(() -> resultText.setText(msg));
                } catch (Exception e) {
                    runOnUiThread(() -> resultText.setText("Hiba: " + e.getMessage()));
                    e.printStackTrace();
                }
            }).start();
        });

        // RESET GOMB
        resetButton.setOnClickListener(v -> {
            currentBitmap = null;
            imageView.setImageBitmap(null);
            resultText.setText("");
            sourceSpinner.setSelection(0);
            modelSpinner.setSelection(0);
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

    // Assetből fájl elérési útvonalat ad vissza (PyTorch Mobile ezt kéri)
    private String assetFilePath(String assetName) throws IOException {
        File file = new File(getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }
        AssetManager assetManager = getAssets();
        try (InputStream is = assetManager.open(assetName);
             FileOutputStream os = new FileOutputStream(file)) {
            byte[] buffer = new byte[4 * 1024];
            int read;
            while ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
            os.flush();
        }
        return file.getAbsolutePath();
    }

    // Osztálylista betöltése (soronként egy class)
    private List<String> loadClassNames(String assetName) throws IOException {
        List<String> names = new ArrayList<>();
        AssetManager assetManager = getAssets();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(assetManager.open(assetName)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                names.add(line.trim());
            }
        }
        return names;
    }

    // Predikció végrehajtása
    private float[] predict(Module model, Bitmap bitmap) {
        final float[] MEAN = {0.485f, 0.456f, 0.406f};
        final float[] STD = {0.229f, 0.224f, 0.225f};

        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true);
        Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(resizedBitmap, MEAN, STD);
        IValue inputs = IValue.from(inputTensor);
        Tensor outputTensor = model.forward(inputs).toTensor();

        return outputTensor.getDataAsFloatArray();
    }

    // Top5 eredmény formázása
    private String getPredictionResult(float[] scores) {
        int n = scores.length;

        // Softmax kiszámítása százalékokhoz
        double[] expScores = new double[n];
        double sum = 0.0;
        for (int i = 0; i < n; i++) {
            expScores[i] = Math.exp(scores[i]);
            sum += expScores[i];
        }

        List<Integer> idxList = new ArrayList<>();
        for (int i = 0; i < n; i++) idxList.add(i);
        idxList.sort((a, b) -> Float.compare(scores[b], scores[a]));

        int top1Idx = idxList.get(0);
        StringBuilder sb = new StringBuilder();
        sb.append("Predikált osztály: ").append(classNames.get(top1Idx)).append("\n\n");
        sb.append("Top 5 tipp:\n");
        for (int i = 0; i < 5; i++) {
            int idx = idxList.get(i);
            double prob = expScores[idx] / sum * 100.0;
            sb.append(String.format("%d. %s - %.1f%%\n", i + 1, classNames.get(idx), prob));
        }
        return sb.toString();
    }
}
