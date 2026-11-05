package com.example.lenslingo.ocr;

import com.example.lenslingo.BuildConfig;
import com.example.lenslingo.data.model.OcrResult;
import com.example.lenslingo.data.model.OcrWord;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

    private final OkHttpClient client;
    private final Gson gson = new Gson();

    public RemoteDataSource() {
        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    public interface OcrCallback {
        void onSuccess(OcrResult result);
        void onFailure(Exception e);
    }

    public void recognizeAndTranslate(String base64Image, OcrCallback callback) {
        android.util.Log.d("LensLingo", "API key empty: " + BuildConfig.GEMINI_API_KEY.isEmpty());
        try {
            JSONObject requestJson = buildRequestBody(base64Image);

            RequestBody body = RequestBody.create(
                    requestJson.toString(),
                    MediaType.parse("application/json"));

            HttpUrl url = HttpUrl.parse(ENDPOINT).newBuilder()
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
                    if (!response.isSuccessful() || response.body() == null) {
                        String errorBody = response.body() != null ? response.body().string() : "no body";
                        callback.onFailure(new IOException("Gemini API error " + response.code() + ": " + errorBody));
                        return;
                    }
                    try {
                        OcrResult result = parseGeminiResponse(response.body().string());
                        callback.onSuccess(result);
                    } catch (Exception e) {
                        callback.onFailure(e);
                    }
                }
            });
        } catch (JSONException e) {
            callback.onFailure(e);
        }
    }

    private JSONObject buildRequestBody(String base64Image) throws JSONException {
        String prompt = "You will receive a photo containing text in a foreign language. "
                + "Detect every distinct word or short phrase, translate each into Ukrainian, "
                + "and return ONLY a raw JSON object, no markdown formatting, no explanation, "
                + "in exactly this shape:\n"
                + "{\"sourceLanguage\":\"<ISO 639-1 code>\","
                + "\"words\":[{\"text\":\"<original word>\",\"translation\":\"<Ukrainian translation>\","
                + "\"box\":[ymin,xmin,ymax,xmax]}]}\n"
                + "Coordinates in box are normalized to a 0-1000 scale relative to image dimensions.";

        JSONObject textPart = new JSONObject().put("text", prompt);

        JSONObject inlineData = new JSONObject()
                .put("mime_type", "image/jpeg")
                .put("data", base64Image);
        JSONObject imagePart = new JSONObject().put("inline_data", inlineData);

        JSONArray parts = new JSONArray().put(textPart).put(imagePart);
        JSONObject content = new JSONObject().put("parts", parts);
        JSONArray contents = new JSONArray().put(content);

        return new JSONObject().put("contents", contents);
    }

    private OcrResult parseGeminiResponse(String rawBody) throws JSONException {
        JSONObject root = new JSONObject(rawBody);

        String modelText = root
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text");

        String cleanJson = modelText.trim()
                .replaceAll("^```json", "")
                .replaceAll("^```", "")
                .replaceAll("```$", "")
                .trim();

        GeminiOcrPayload payload = gson.fromJson(cleanJson, GeminiOcrPayload.class);

        List<OcrWord> words = new ArrayList<>();
        for (GeminiOcrPayload.WordDto dto : payload.words) {
            words.add(new OcrWord(dto.text, dto.translation, dto.box));
        }

        return new OcrResult(words, payload.sourceLanguage, true);
    }
}