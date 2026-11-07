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
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public interface TranslationCallback {
        void onSuccess(String sourceLanguage, Map<Integer, String> translations);
        void onFailure(Exception e);
    }

    public void translateWords(String base64Image, List<OcrWord> words, TranslationCallback callback) {
        try {
            JSONObject requestJson = buildRequestBody(base64Image, words);

            RequestBody body = RequestBody.create(
                    requestJson.toString(), MediaType.parse("application/json"));

            HttpUrl url = HttpUrl.parse(BASE_URL + BuildConfig.GEMINI_MODEL + ":generateContent")
                    .newBuilder()
                    .addQueryParameter("key", BuildConfig.GEMINI_API_KEY)
                    .build();

            Request request = new Request.Builder().url(url).post(body).build();

            client.newCall(request).enqueue(new Callback() {
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
        for (int i = 0; i < words.size(); i++) {
            list.append(i + 1).append(": ").append(words.get(i).text).append("\n");
        }

        String prompt = "You are given a photo and a numbered list of words that an OCR engine "
                + "found on it. Translate each word into Ukrainian, choosing the meaning that fits "
                + "the context of the photo: the surrounding text, the kind of object, the scene.\n"
                + "Rules:\n"
                + "- Translate the single word only, not the whole sentence.\n"
                + "- If an item is a brand name, logo, abbreviation, code, number, a fragment "
                + "without meaning, or anything that should not be translated, return an empty "
                + "string for it.\n"
                + "- Also detect the main language of the text on the photo as an ISO 639-1 code.\n"
                + "Return ONLY a raw JSON object, no markdown, exactly in this shape:\n"
                + "{\"sourceLanguage\":\"es\",\"translations\":[{\"id\":1,\"t\":\"переклад\"}]}\n"
                + "Include every id from the list.\n\n"
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

        android.util.Log.d("LensLingo", "Model raw text: " + clean);

        GeminiTranslationPayload payload = gson.fromJson(clean, GeminiTranslationPayload.class);
        if (payload == null || payload.translations == null) {
            throw new JSONException("Empty translation payload");
        }

        Map<Integer, String> map = new HashMap<>();
        for (GeminiTranslationPayload.Item item : payload.translations) {
            map.put(item.id, item.t);
        }
        callback.onSuccess(payload.sourceLanguage, map);
    }
}