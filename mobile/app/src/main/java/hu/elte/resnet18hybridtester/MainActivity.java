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

public class MainActivity extends AppCompatActivity {

    private Module model;
    private List<String> classNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView imageView = findViewById(R.id.imageView);
        TextView textResult = findViewById(R.id.textResult);

        try {
            // 1. Modell betöltése
            model = Module.load(assetFilePath("resnet18_baseline_scripted.pt"));

            // 2. Osztálynevek betöltése
            classNames = loadClassNames("class_labels.txt");

            // 3. Mintakép betöltése az assets-ből
            Bitmap bitmap = getBitmapFromAsset("test01.jpg");
            imageView.setImageBitmap(bitmap);

            // 4. Predikció futtatása
            float[] prediction = predict(bitmap);

            // 5. Top5 eredmény kiírása
            String resultText = getPredictionResult(prediction);
            textResult.setText(resultText);

        } catch (Exception e) {
            textResult.setText("Hiba: " + e.getMessage());
            e.printStackTrace();
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

    // Kép betöltése assets-ből
    private Bitmap getBitmapFromAsset(String filePath) throws IOException {
        AssetManager assetManager = getAssets();
        try (InputStream istr = assetManager.open(filePath)) {
            return BitmapFactory.decodeStream(istr);
        }
    }

    // Predikció végrehajtása
    private float[] predict(Bitmap bitmap) {
        // Ugyanazzal a normalizálással, mint tanításkor!
        final float[] MEAN = {0.485f, 0.456f, 0.406f};
        final float[] STD = {0.229f, 0.224f, 0.225f};

        // Ha a bemeneti kép mérete eltér 224x224-től, átméretezzük
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

        // Párosítsuk index-szel
        List<Integer> idxList = new ArrayList<>();
        for (int i = 0; i < n; i++) idxList.add(i);

        // Top5 index keresése
        idxList.sort((a, b) -> Float.compare(scores[b], scores[a]));

        // Top1 predikció
        int top1Idx = idxList.get(0);
        StringBuilder sb = new StringBuilder();
        sb.append("Predikált osztály: ").append(classNames.get(top1Idx)).append("\n\n");
        sb.append("Top 5 tipp:\n");
        for (int i = 0; i < 5; i++) {
            int idx = idxList.get(i);
            double prob = expScores[idx] / sum * 100.0;
            sb.append(String.format("%d. %s - %.1f%%\n", i+1, classNames.get(idx), prob));
        }
        return sb.toString();
    }
}
