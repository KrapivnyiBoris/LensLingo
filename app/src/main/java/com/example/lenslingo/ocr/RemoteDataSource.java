package com.example.lenslingo.ocr;

import com.example.lenslingo.BuildConfig;
import com.example.lenslingo.data.model.OcrWord;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RemoteDataSource {

    private static final String BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    private final OkHttpClient client;
    private final Gson gson = new Gson();

    public RemoteDataSource() {
        client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(12, TimeUnit.SECONDS)
                .build();
    }

    public interface TranslationCallback {
        void onSuccess(String sourceLanguage, List<GeminiTranslationPayload.Item> items,
                       List<GeminiTranslationPayload.Line> lines);
        void onFailure(Exception e);
    }

    public void translateWords(String model, long timeoutMs, String base64Image,
                               List<OcrWord> words, TranslationCallback callback) {
        try {
            JSONObject requestJson = buildRequestBody(base64Image, words);
            RequestBody body = RequestBody.create(
                    requestJson.toString(), MediaType.parse("application/json"));

            HttpUrl url = HttpUrl.parse(BASE_URL + model + ":generateContent")
                    .newBuilder()
                    .addQueryParameter("key", BuildConfig.GEMINI_API_KEY)
                    .build();

            Request request = new Request.Builder().url(url).post(body).build();

            OkHttpClient perCallClient = client.newBuilder()
                    .callTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                    .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                    .build();

            perCallClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onFailure(e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : null;
                    if (!response.isSuccessful() || responseBody == null) {
                        callback.onFailure(new IOException(
                                "Gemini API error " + response.code() + ": " + responseBody));
                        return;
                    }
                    try {
                        parseResponse(responseBody, callback);
                    } catch (Exception e) {
                        callback.onFailure(e);
                    }
                }
            });
        } catch (JSONException e) {
            callback.onFailure(e);
        }
    }

    private JSONObject buildRequestBody(String base64Image, List<OcrWord> words) throws JSONException {
        StringBuilder list = new StringBuilder();
        int currentLine = -1;
        for (int i = 0; i < words.size(); i++) {
            OcrWord w = words.get(i);
            if (w.lineIndex != currentLine) {
                if (currentLine != -1) list.append("\n");
                list.append("Line ").append(w.lineIndex).append(": ");
                currentLine = w.lineIndex;
            }
            list.append("[").append(i + 1).append("] ").append(w.text).append(" ");
        }

        String prompt = "You are given a photo and the words an OCR engine found on it, grouped by "
                + "text line (Line N: [id] word ...). Words inside a line are in reading order. "
                + "The OCR may have lost or glued letters ('eptember' = 'September', 'hadto' = 'had to').\n"
                + "Return one item for EVERY id, in the same order.\n"
                + "Rules:\n"
                + "- \"t\" is the Ukrainian meaning of THAT ONE word only, as used in this context. "
                + "Never translate the line as a sentence and never spread a sentence translation "
                + "over the words. The translation of the whole line goes ONLY into \"lines\".\n"
                + "- Articles and particles with no standalone Ukrainian meaning (the, a, an, to) get "
                + "a short grammatical note in brackets, e.g. \"(артикль)\". Never give them the "
                + "translation of a neighbouring word.\n"
                + "- Example: Line 1: [1] The [2] dog [3] is [4] big -> The=(артикль), dog=собака, "
                + "is=є, big=великий; line 1 = \"Собака велика\".\n"
                + "- \"w\" repeats the id's word exactly as listed.\n"
                + "- \"c\": the corrected word if the OCR word is clearly damaged or glued and you are "
                + "sure, otherwise an empty string. If the OCR glued several words together "
                + "('uswith' = 'us with'), \"c\" holds them separated by a space and \"t\" must translate "
                + "EVERY part, joined with ' + ' (for example \"нас + з\"). Never drop a part.\n"
                + "- \"k\": \"word\" = ordinary word (a capital letter at the start of a line or in a "
                + "heading does NOT make a word a name, 'Banana' is a word); \"name\" = person name, "
                + "place, event, title or brand that is part of the content the reader is meant to "
                + "read; its \"t\" must be written in Ukrainian letters (Фортнайт, Леклер), never the "
                + "Latin original; \"noise\" = sponsor logos printed on objects, interface elements, "
                + "usernames, numbers, codes, meaningless fragments and text already in Ukrainian "
                + "(then \"t\" is an empty string).\n"
                + "- No punctuation marks in \"t\".\n"
                + "- Also detect the main language of the photo text as an ISO 639-1 code.\n"
                + "- The photo may mix languages (for example a Ukrainian app interface around "
                + "English content). Translate every non-Ukrainian word and line INTO Ukrainian. "
                + "Text that is already Ukrainian gets kind \"noise\" and an empty \"t\". "
                + "\"sourceLanguage\" is the language of the content that needs translating, never "
                + "Ukrainian unless the whole photo is Ukrainian.\n"
                + "- \"lines\": translate ONLY the words listed in that line, never add words that are "
                + "not in the list, never copy the original text back. If a line is not worth "
                + "translating, use an empty string.\n"
                + "Return ONLY compact JSON on a single line, no indentation, no markdown:\n"
                + "{\"sourceLanguage\":\"en\",\"items\":[{\"id\":1,\"w\":\"The\",\"t\":\"(артикль)\","
                + "\"k\":\"word\",\"c\":\"\"}],\"lines\":[{\"n\":1,\"t\":\"Собака велика\"}]}\n\n"
                + "Words:\n" + list;

        JSONObject textPart = new JSONObject().put("text", prompt);
        JSONObject imagePart = new JSONObject().put("inline_data", new JSONObject()
                .put("mime_type", "image/jpeg")
                .put("data", base64Image));

        JSONObject content = new JSONObject().put("parts", new JSONArray().put(textPart).put(imagePart));
        JSONObject generationConfig = new JSONObject()
                .put("temperature", 0)
                .put("responseMimeType", "application/json");

        return new JSONObject()
                .put("contents", new JSONArray().put(content))
                .put("generationConfig", generationConfig);
    }

    private void parseResponse(String rawBody, TranslationCallback callback) throws JSONException {
        JSONArray parts = new JSONObject(rawBody)
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts");

        StringBuilder text = new StringBuilder();
        for (int i = 0; i < parts.length(); i++) {
            JSONObject part = parts.getJSONObject(i);
            if (!part.optBoolean("thought", false)) {
                text.append(part.optString("text", ""));
            }
        }

        String clean = text.toString().trim()
                .replaceAll("^```json", "")
                .replaceAll("^```", "")
                .replaceAll("```$", "")
                .trim();

        OcrLog.raw("GEMINI_RAW", clean);

        GeminiTranslationPayload payload = gson.fromJson(clean, GeminiTranslationPayload.class);
        if (payload == null || payload.items == null) {
            throw new JSONException("Empty translation payload");
        }
        if (payload.lines != null) {
            for (GeminiTranslationPayload.Line line : payload.lines) {
                OcrLog.d("LINE " + line.n + ": " + line.t);
            }
        }
        callback.onSuccess(payload.sourceLanguage, payload.items, payload.lines);
    }
}