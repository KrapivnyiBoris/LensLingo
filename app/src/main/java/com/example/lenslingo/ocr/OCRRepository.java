package com.example.lenslingo.ocr;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.SystemClock;

import com.example.lenslingo.data.model.OcrResult;
import com.example.lenslingo.data.model.OcrWord;
import com.google.mlkit.vision.common.InputImage;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OCRRepository {

    private static final boolean ENHANCE_EXPERIMENT = false;
    private static final boolean USE_ENHANCED_BOXES = false;

    private final Context appContext;
    private final LocalMLDataSource localDataSource;
    private final RemoteDataSource remoteDataSource;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final LocalTranslationDataSource translationSource;

    public OCRRepository(Context context) {
        appContext = context.getApplicationContext();
        localDataSource = new LocalMLDataSource();
        remoteDataSource = new RemoteDataSource();
        translationSource = new LocalTranslationDataSource();
    }

    public interface OcrCallback {
        void onSuccess(OcrResult result, boolean wasOffline);
        void onFailure(Exception e);
    }

    public void processImage(Bitmap bitmap, OcrCallback callback) {
        OcrLog.startRun(bitmap.getWidth(), bitmap.getHeight());
        final long start = SystemClock.elapsedRealtime();

        localDataSource.recognizeText(InputImage.fromBitmap(bitmap, 0),
                new LocalMLDataSource.OcrCallback() {
                    @Override
                    public void onSuccess(OcrResult original) {
                        OcrLog.d("ML Kit finished in " + (SystemClock.elapsedRealtime() - start) + " ms");
                        OcrLog.dumpWords("ML_KIT", original.words);

                        if (!ENHANCE_EXPERIMENT || original.words.isEmpty()) {
                            proceed(bitmap, original, callback);
                            return;
                        }

                        Bitmap enhanced = ImageEnhancer.enhance(bitmap);
                        localDataSource.recognizeText(InputImage.fromBitmap(enhanced, 0),
                                new LocalMLDataSource.OcrCallback() {
                                    @Override
                                    public void onSuccess(OcrResult enh) {
                                        OcrLog.dumpWords("ML_KIT_ENHANCED", enh.words);
                                        compareBoxes(original.words, enh.words);
                                        boolean useEnh = USE_ENHANCED_BOXES && !enh.words.isEmpty();
                                        OcrLog.d("BOXES SOURCE: " + (useEnh ? "enhanced" : "original"));
                                        proceed(bitmap, useEnh ? enh : original, callback);
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        OcrLog.d("Enhanced pass failed: " + e);
                                        proceed(bitmap, original, callback);
                                    }
                                });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        OcrLog.d("ML Kit failed: " + e);
                        callback.onFailure(e);
                    }
                });
    }

    private void proceed(Bitmap bitmap, OcrResult local, OcrCallback callback) {
        if (local.words.isEmpty()) {
            OcrLog.d("RESULT: no text found");
            callback.onFailure(new IllegalStateException("No text found"));
        } else if (isNetworkAvailable()) {
            translateOnline(bitmap, local, callback);
        } else {
            OcrLog.d("MODE offline: no network");
            finishOffline(local, callback);
        }
    }

    private void compareBoxes(List<OcrWord> orig, List<OcrWord> enh) {
        float sumL = 0, sumR = 0;
        int matched = 0;

        for (OcrWord o : orig) {
            float[] ob = o.box();
            if (ob == null) continue;

            OcrWord best = null;
            float bestDist = Float.MAX_VALUE;
            for (OcrWord e : enh) {
                float[] eb = e.box();
                if (eb == null || !norm(e.text).equals(norm(o.text))) continue;
                float dist = Math.abs((eb[0] + eb[2]) - (ob[0] + ob[2]))
                        + Math.abs((eb[1] + eb[3]) - (ob[1] + ob[3]));
                if (dist < bestDist) {
                    bestDist = dist;
                    best = e;
                }
            }

            if (best == null) {
                OcrLog.d("CMP \"" + o.text + "\" not found in enhanced");
                continue;
            }

            float[] eb = best.box();
            float dL = ob[0] - eb[0];
            float dR = eb[2] - ob[2];
            sumL += dL;
            sumR += dR;
            matched++;
            OcrLog.d(String.format(Locale.US, "CMP \"%s\" dLeft=%+.3f dRight=%+.3f conf %.2f->%.2f",
                    o.text, dL, dR, o.confidence, best.confidence));
        }

        if (matched > 0) {
            OcrLog.d(String.format(Locale.US, "CMP SUMMARY matched=%d/%d avgLeft=%+.4f avgRight=%+.4f",
                    matched, orig.size(), sumL / matched, sumR / matched));
        }
    }

    private void translateOnline(Bitmap bitmap, OcrResult local, OcrCallback callback) {
        executor.execute(() -> {
            final long start = SystemClock.elapsedRealtime();
            String base64 = ImageUtils.toBase64(bitmap);

            remoteDataSource.translateWords(base64, local.words, new RemoteDataSource.TranslationCallback() {
                @Override
                public void onSuccess(String sourceLanguage, List<GeminiTranslationPayload.Item> items) {
                    OcrLog.d("Gemini finished in " + (SystemClock.elapsedRealtime() - start)
                            + " ms, items=" + items.size() + ", sent=" + local.words.size());

                    applyTranslations(local.words, items);
                    flagSuspects(local.words);
                    OcrLog.dumpWords("FINAL", local.words);

                    String lang = sourceLanguage != null ? sourceLanguage : "auto";
                    translationSource.prefetch(lang);
                    callback.onSuccess(new OcrResult(local.words, lang, true), false);
                }

                @Override
                public void onFailure(Exception e) {
                    OcrLog.d("MODE offline: Gemini failed: " + e);
                    finishOffline(local, callback);
                }
            });
        });
    }

    private void applyTranslations(List<OcrWord> words, List<GeminiTranslationPayload.Item> items) {
        Map<Integer, GeminiTranslationPayload.Item> byId = new HashMap<>();
        Map<String, ArrayDeque<GeminiTranslationPayload.Item>> byText = new HashMap<>();
        for (GeminiTranslationPayload.Item item : items) {
            byId.put(item.id, item);
            byText.computeIfAbsent(norm(item.w), k -> new ArrayDeque<>()).add(item);
        }

        Set<GeminiTranslationPayload.Item> used = new HashSet<>();

        for (int i = 0; i < words.size(); i++) {
            OcrWord word = words.get(i);
            int id = i + 1;
            GeminiTranslationPayload.Item item = byId.get(id);

            boolean idOk = item != null
                    && (item.w == null || item.w.isEmpty()
                    || norm(item.w).equals(norm(word.text)));

            if (!idOk) {
                if (item != null) {
                    OcrLog.d("MISMATCH id=" + id + " expected=\"" + word.text
                            + "\" got=\"" + item.w + "\"");
                }
                item = null;
                ArrayDeque<GeminiTranslationPayload.Item> candidates = byText.get(norm(word.text));
                if (candidates != null) {
                    for (GeminiTranslationPayload.Item c : candidates) {
                        if (!used.contains(c)) {
                            item = c;
                            OcrLog.d("REMAP id=" + id + " \"" + word.text
                                    + "\" taken from item id=" + c.id);
                            break;
                        }
                    }
                }
            }

            if (item == null) {
                OcrLog.d("MISSING id=" + id + " \"" + word.text + "\"");
                word.translation = null;
                word.highlight = false;
                continue;
            }

            used.add(item);
            apply(word, item);
        }
    }

    private void apply(OcrWord word, GeminiTranslationPayload.Item item) {
        String t = item.t == null ? ""
                : item.t.trim().replaceAll("[\\p{P}&&[^()]]+$", "");
        String kind = item.k == null ? "word" : item.k.trim().toLowerCase(Locale.ROOT);
        String c = item.c == null ? "" : item.c.trim();

        word.kind = kind.equals("name") ? OcrWord.KIND_NAME
                : kind.equals("noise") ? OcrWord.KIND_NOISE
                  : OcrWord.KIND_WORD;

        String base = !c.isEmpty() ? c : word.text;
        String clean = base.replaceAll("^[^\\p{L}\\p{N}]+|[^\\p{L}\\p{N}]+$", "");
        if (!clean.isEmpty() && !clean.equals(word.text)) {
            word.displayText = clean;
        }

        word.translation = t.isEmpty() ? null : t;
        word.highlight = word.kind != OcrWord.KIND_NOISE && !t.isEmpty();
    }

    private String norm(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager)
                appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network network = cm.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private void flagSuspects(List<OcrWord> words) {
        for (int i = 1; i < words.size(); i++) {
            OcrWord a = words.get(i - 1);
            OcrWord b = words.get(i);
            if (a.lineIndex == b.lineIndex && a.translation != null
                    && a.translation.equalsIgnoreCase(b.translation)
                    && !a.text.equalsIgnoreCase(b.text)) {
                OcrLog.d("SUSPECT same translation \"" + a.translation + "\" for \""
                        + a.text + "\" and \"" + b.text + "\"");
            }
        }
    }

    private void finishOffline(OcrResult local, OcrCallback callback) {
        executor.execute(() -> {
            long start = SystemClock.elapsedRealtime();
            LocalTranslationDataSource.Outcome outcome = translationSource.translateWords(local.words);
            OcrLog.d("Offline translation took " + (SystemClock.elapsedRealtime() - start) + " ms");
            OcrLog.dumpWords("OFFLINE_FINAL", local.words);

            OcrResult result = new OcrResult(local.words, outcome.language, false);
            result.notice = outcome.notice;
            callback.onSuccess(result, true);
        });
    }
}