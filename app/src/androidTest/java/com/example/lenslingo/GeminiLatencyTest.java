package com.example.lenslingo;

import static org.junit.Assert.assertFalse;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.SystemClock;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.lenslingo.data.model.OcrWord;
import com.example.lenslingo.ocr.GeminiTranslationPayload;
import com.example.lenslingo.ocr.ImageUtils;
import com.example.lenslingo.ocr.RemoteDataSource;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class GeminiLatencyTest {

    private static final String TAG = "GeminiBench";
    private static final int REPEATS = 5;
    private static final long PAUSE_MS = 5000;     // пауза між запитами, щоб не впертись у ліміт 15/хв
    private static final long TIMEOUT_MS = 30000;
    private static final long[] THRESHOLDS = {3000, 5000, 6000, 10000, 12000};

    private static final String[][] PRESETS = {
            // ~7 слів
            {"I see something", "Russian burning press like"},
            // ~16 слів
            {"Your body will last", "longer than job handbag", "car or expensive house",
                    "Invest in your health"},
            // ~40 слів
            {"das Brot das Fleisch die Milch", "das Gemüse das Obst das Brötchen",
                    "die Wurst die Butter die Tomate", "der Apfel der Kuchen das Würstchen",
                    "der Joghurt die Kartoffel die Weintraube", "der Keks der Fisch der Käse",
                    "die Zwiebel die Orange das Ei", "der Reis das Wasser der Saft"}
    };
    private static final String[] NAMES = {"small", "medium", "large"};

    private static class Sample {
        long ms;
        boolean ok;
        String reason = "";
    }

    @Test
    public void measureGeminiLatency() throws Exception {
        assertFalse("GEMINI_API_KEY is empty", BuildConfig.GEMINI_API_KEY.isEmpty());

        String[] models = BuildConfig.GEMINI_MODELS.split("\\s*,\\s*");
        RemoteDataSource remote = new RemoteDataSource();
        String stamp = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date());
        Log.d(TAG, "===== START " + stamp + " models=" + String.join(",", models)
                + " repeats=" + REPEATS + " =====");

        // ключ: preset|model -> вибірки
        Map<String, List<Sample>> results = new LinkedHashMap<>();

        for (int p = 0; p < PRESETS.length; p++) {
            List<OcrWord> words = buildWords(PRESETS[p]);
            String base64 = ImageUtils.toBase64(drawImage(PRESETS[p]));
            String label = NAMES[p] + "(" + words.size() + "w)";

            for (int rep = 1; rep <= REPEATS; rep++) {
                for (String model : models) {
                    Sample s = call(remote, model, base64, words);
                    results.computeIfAbsent(label + "|" + model, k -> new ArrayList<>()).add(s);
                    Log.d(TAG, String.format(Locale.US, "CALL preset=%s model=%s rep=%d ok=%b ms=%d %s",
                            label, model, rep, s.ok, s.ms, s.reason));
                    Thread.sleep(PAUSE_MS);
                }
            }
        }

        Log.d(TAG, "===== SUMMARY " + stamp + " =====");
        for (Map.Entry<String, List<Sample>> e : results.entrySet()) {
            Log.d(TAG, summarize(e.getKey(), e.getValue(), stamp));
        }
        Log.d(TAG, "===== END =====");
    }

    private Sample call(RemoteDataSource remote, String model, String base64, List<OcrWord> words)
            throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> error = new AtomicReference<>();
        long start = SystemClock.elapsedRealtime();

        remote.translateWords(model, TIMEOUT_MS, base64, words, new RemoteDataSource.TranslationCallback() {
            @Override
            public void onSuccess(String lang, List<GeminiTranslationPayload.Item> items,
                                  List<GeminiTranslationPayload.Line> lines) {
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                String m = String.valueOf(e);
                int nl = m.indexOf('\n');
                error.set(nl > 0 ? m.substring(0, nl) : m);
                latch.countDown();
            }
        });

        boolean finished = latch.await(TIMEOUT_MS + 5000, TimeUnit.MILLISECONDS);
        Sample s = new Sample();
        s.ms = SystemClock.elapsedRealtime() - start;
        s.ok = finished && error.get() == null;
        s.reason = finished ? (error.get() == null ? "" : error.get()) : "no callback";
        return s;
    }

    private String summarize(String key, List<Sample> samples, String stamp) {
        List<Long> okTimes = new ArrayList<>();
        Map<String, Integer> reasons = new LinkedHashMap<>();
        for (Sample s : samples) {
            if (s.ok) okTimes.add(s.ms);
            else reasons.merge(s.reason.length() > 70 ? s.reason.substring(0, 70) : s.reason, 1, Integer::sum);
        }
        Collections.sort(okTimes);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.US, "%s | %s | n=%d ok=%d fail=%d",
                stamp, key, samples.size(), okTimes.size(), samples.size() - okTimes.size()));

        if (!okTimes.isEmpty()) {
            long sum = 0;
            for (long t : okTimes) sum += t;
            sb.append(String.format(Locale.US, " | min=%d med=%d avg=%d max=%d",
                    okTimes.get(0), okTimes.get(okTimes.size() / 2),
                    sum / okTimes.size(), okTimes.get(okTimes.size() - 1)));

            sb.append(" | within(all calls):");
            for (long th : THRESHOLDS) {
                int within = 0;
                for (Sample s : samples) if (s.ok && s.ms <= th) within++;
                sb.append(String.format(Locale.US, " %ds=%d%%", th / 1000, 100 * within / samples.size()));
            }
        }
        if (!reasons.isEmpty()) sb.append(" | errors=").append(reasons);
        return sb.toString();
    }

    private List<OcrWord> buildWords(String[] lines) {
        List<OcrWord> words = new ArrayList<>();
        float[] dummy = {0, 0, 1, 0, 1, 1, 0, 1};
        for (int i = 0; i < lines.length; i++) {
            for (String w : lines[i].split(" ")) {
                words.add(new OcrWord(w, null, dummy, 0.9f, i + 1));
            }
        }
        return words;
    }

    private Bitmap drawImage(String[] lines) {
        Bitmap bmp = Bitmap.createBitmap(1088, 1344, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(Color.WHITE);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLACK);
        paint.setTextSize(44);
        for (int i = 0; i < lines.length; i++) {
            canvas.drawText(lines[i], 40, 120 + i * 100, paint);
        }
        return bmp;
    }
}