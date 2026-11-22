package com.example.lenslingo;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@RunWith(AndroidJUnit4.class)
public class GeminiFallbackTest {

    private static final String TAG = "GeminiFallback";
    private static final int TRIALS = 40;
    private static final long PAUSE_MS = 8000;       // 2 запити на спробу, тримаємо темп нижче 15/хв
    private static final long TIMEOUT_MS = 30000;    // міряємо справжню затримку
    private static final long MIN_ATTEMPT_MS = 2500; // як в OCRRepository
    private static final long BASE_BUDGET = 10000;   // поточний бюджет застосунку
    private static final long INF = Long.MAX_VALUE / 4;

    private static final String[][] PRESETS = {
            {"I see something", "Russian burning press like"},
            {"Your body will last", "longer than job handbag", "car or expensive house",
                    "Invest in your health"},
            {"das Brot das Fleisch die Milch", "das Gemüse das Obst das Brötchen",
                    "die Wurst die Butter die Tomate", "der Apfel der Kuchen das Würstchen",
                    "der Joghurt die Kartoffel die Weintraube", "der Keks der Fisch der Käse",
                    "die Zwiebel die Orange das Ei", "der Reis das Wasser der Saft"}
    };
    private static final String[] NAMES = {"small", "medium", "large"};

    private static class Call {
        final CountDownLatch latch = new CountDownLatch(1);
        volatile boolean ok;
        volatile long ms;
        volatile String reason = "";
    }

    private static class Pair {
        final String preset;
        final Call a;
        final Call b;

        Pair(String preset, Call a, Call b) {
            this.preset = preset;
            this.a = a;
            this.b = b;
        }
    }

    private static class Outcome {
        boolean online;     // користувач отримав онлайн-результат
        long wait;          // скільки він чекав до результату (онлайн або переходу в офлайн)
        boolean secondUsed; // друга модель взагалі запускалась
        boolean secondOk;   // і вона принесла результат
    }

    @Test
    public void measureFallbackBenefit() throws Exception {
        assertFalse("GEMINI_API_KEY is empty", BuildConfig.GEMINI_API_KEY.isEmpty());
        String[] models = BuildConfig.GEMINI_MODELS.split("\\s*,\\s*");
        assertTrue("Need at least 2 models in GEMINI_MODELS", models.length >= 2);

        final String a = models[0];
        final String b = models[1];
        RemoteDataSource remote = new RemoteDataSource();
        String stamp = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date());
        Log.d(TAG, "===== START " + stamp + " A=" + a + " B=" + b + " trials=" + TRIALS + " =====");

        List<List<OcrWord>> words = new ArrayList<>();
        List<String> images = new ArrayList<>();
        for (String[] preset : PRESETS) {
            words.add(buildWords(preset));
            images.add(ImageUtils.toBase64(drawImage(preset)));
        }

        List<Pair> pairs = new ArrayList<>();
        for (int i = 0; i < TRIALS; i++) {
            int p = i % PRESETS.length;
            Call ca = new Call();
            Call cb = new Call();
            start(remote, a, images.get(p), words.get(p), ca);
            start(remote, b, images.get(p), words.get(p), cb);
            ca.latch.await(TIMEOUT_MS + 5000, TimeUnit.MILLISECONDS);
            cb.latch.await(TIMEOUT_MS + 5000, TimeUnit.MILLISECONDS);

            pairs.add(new Pair(NAMES[p], ca, cb));
            Log.d(TAG, String.format(Locale.US, "PAIR %d %s A=%b,%d B=%b,%d %s %s",
                    i + 1, NAMES[p], ca.ok, ca.ms, cb.ok, cb.ms, ca.reason, cb.reason));
            Thread.sleep(PAUSE_MS);
        }

        report(stamp, a, b, pairs);
    }

    // ---------- вимірювання ----------

    private void start(RemoteDataSource remote, String model, String base64,
                       List<OcrWord> words, Call out) {
        final long t0 = SystemClock.elapsedRealtime();
        remote.translateWords(model, TIMEOUT_MS, base64, words,
                new RemoteDataSource.TranslationCallback() {
                    @Override
                    public void onSuccess(String lang, List<GeminiTranslationPayload.Item> items,
                                          List<GeminiTranslationPayload.Line> lines) {
                        out.ms = SystemClock.elapsedRealtime() - t0;
                        out.ok = true;
                        out.latch.countDown();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        out.ms = SystemClock.elapsedRealtime() - t0;
                        out.ok = false;
                        String m = String.valueOf(e);
                        int nl = m.indexOf('\n');
                        out.reason = (nl > 0 ? m.substring(0, nl) : m).replace(' ', '_');
                        if (out.reason.length() > 40) out.reason = out.reason.substring(0, 40);
                        out.latch.countDown();
                    }
                });
    }

    // ---------- політики (чисті функції над записаними парами) ----------

    // Одна модель, весь бюджет
    private static Outcome single(Call a, long budget) {
        Outcome o = new Outcome();
        if (a.ok && a.ms <= budget) {
            o.online = true;
            o.wait = a.ms;
        } else {
            o.wait = Math.min(a.ms, budget);
        }
        return o;
    }

    // Каскад як в OCRRepository: спроба обмежена, друга модель отримує залишок бюджету
    private static Outcome cascade(Call a, Call b, long budget, long attempt) {
        Outcome o = new Outcome();
        if (a.ok && a.ms <= attempt) {
            o.online = true;
            o.wait = a.ms;
            return o;
        }
        long t1 = a.ok ? attempt : Math.min(a.ms, attempt);
        long remaining = budget - t1;
        if (remaining < MIN_ATTEMPT_MS) {
            o.wait = t1;
            return o;
        }
        o.secondUsed = true;
        if (b.ok && b.ms <= remaining) {
            o.online = true;
            o.secondOk = true;
            o.wait = t1 + b.ms;
        } else {
            o.wait = t1 + Math.min(b.ms, remaining);
        }
        return o;
    }

    // Паралельний резерв: якщо A мовчить delay мс, запускаємо B і беремо ту, що відповіла першою
    private static Outcome hedge(Call a, Call b, long budget, long delay) {
        Outcome o = new Outcome();
        long aDone = (a.ok && a.ms <= budget) ? a.ms : INF;
        if (aDone <= delay) {
            o.online = true;
            o.wait = aDone;
            return o;
        }
        long startB = a.ok ? delay : Math.min(a.ms, delay);
        o.secondUsed = true;
        long bDone = (b.ok && startB + b.ms <= budget) ? startB + b.ms : INF;
        long best = Math.min(aDone, bDone);
        if (best < INF) {
            o.online = true;
            o.secondOk = bDone < aDone;
            o.wait = best;
        } else {
            o.wait = Math.max(Math.min(a.ms, budget), Math.min(startB + b.ms, budget));
        }
        return o;
    }

    // ---------- звіт ----------

    private void report(String stamp, String a, String b, List<Pair> pairs) {
        int n = pairs.size();
        Log.d(TAG, "===== SUMMARY " + stamp + " A=" + a + " B=" + b + " n=" + n + " =====");

        // Чи доповнюють моделі одна одну (поріг 6 с)
        int both = 0, onlyA = 0, onlyB = 0, neither = 0;
        for (Pair p : pairs) {
            boolean okA = p.a.ok && p.a.ms <= 6000;
            boolean okB = p.b.ok && p.b.ms <= 6000;
            if (okA && okB) both++;
            else if (okA) onlyA++;
            else if (okB) onlyB++;
            else neither++;
        }
        Log.d(TAG, String.format(Locale.US,
                "COMPLEMENT (answer within 6 s): both=%d only_A=%d only_B=%d neither=%d",
                both, onlyA, onlyB, neither));

        List<Outcome> base = apply(pairs, p -> single(p.a, BASE_BUDGET));
        Log.d(TAG, describe("A alone, 10 s (reference)", base, base));
        Log.d(TAG, describe("B alone, 10 s", apply(pairs, p -> single(p.b, BASE_BUDGET)), base));

        long[][] cascades = {{10000, 6000}, {10000, 5000}, {12000, 6000}, {8000, 5000}, {10000, 4000}};
        for (long[] c : cascades) {
            final long budget = c[0];
            final long attempt = c[1];
            Log.d(TAG, describe("A->B cascade " + budget / 1000 + "/" + attempt / 1000,
                    apply(pairs, p -> cascade(p.a, p.b, budget, attempt)), base));
        }
        Log.d(TAG, describe("B->A cascade 10/6",
                apply(pairs, p -> cascade(p.b, p.a, 10000, 6000)), base));

        for (long delay : new long[]{3000, 4000}) {
            Log.d(TAG, describe("hedge A+B after " + delay / 1000 + " s (budget 10)",
                    apply(pairs, p -> hedge(p.a, p.b, 10000, delay)), base));
        }
        Log.d(TAG, "===== END =====");
    }

    private List<Outcome> apply(List<Pair> pairs, Function<Pair, Outcome> policy) {
        List<Outcome> out = new ArrayList<>();
        for (Pair p : pairs) out.add(policy.apply(p));
        return out;
    }

    private String describe(String name, List<Outcome> outs, List<Outcome> base) {
        int n = outs.size();
        int online = 0, used = 0, usedOk = 0, gain = 0, lost = 0, offline = 0;
        long sumOnline = 0, sumOffline = 0;

        for (int i = 0; i < n; i++) {
            Outcome o = outs.get(i);
            Outcome r = base.get(i);
            if (o.online) {
                online++;
                sumOnline += o.wait;
            } else {
                offline++;
                sumOffline += o.wait;
            }
            if (o.secondUsed) used++;
            if (o.secondUsed && o.secondOk) usedOk++;
            if (!r.online && o.online) gain++;
            if (r.online && !o.online) lost++;
        }

        return String.format(Locale.US,
                "%-30s online=%d/%d | 2nd_used=%d 2nd_ok=%d | vs_A_alone: gain=%d lost=%d net=%+d"
                        + " | avg_wait_online=%dms avg_wait_to_offline=%dms",
                name, online, n, used, usedOk, gain, lost, gain - lost,
                online == 0 ? 0 : sumOnline / online,
                offline == 0 ? 0 : sumOffline / offline);
    }

    // ---------- синтетичний запит ----------

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