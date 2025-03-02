package dev.seasnail1.utilities;

import org.json.JSONArray;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Translator {
    private static final String API_URL = "https://translate.googleapis.com/translate_a/single";
    private final String translateFrom;
    private final String translateTo;
    private final Map<String, String[]> cache;

    public Translator(String translateFrom, String translateTo) {
        this.translateFrom = translateFrom;
        this.translateTo = translateTo;
        this.cache = new HashMap<>();
    }

    public String[] translate(String word) throws IOException {
        if (cache.containsKey(word)) {
            return cache.get(word);
        }

        String encodedWord = URLEncoder.encode(word, StandardCharsets.UTF_8);
        String urlStr = String.format("%s?client=gtx&dt=t&sl=%s&tl=%s&q=%s", API_URL, translateFrom, translateTo, encodedWord);
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        if (conn.getResponseCode() == 200) {
            Scanner scanner = new Scanner(url.openStream());
            String response = scanner.useDelimiter("\\A").next();
            scanner.close();

            JSONArray jsonArray = new JSONArray(response);
            String translatedText = jsonArray.getJSONArray(0).getJSONArray(0).getString(0);
            String originalText = jsonArray.getJSONArray(0).getJSONArray(0).getString(1);

            String[] result = new String[]{translatedText, originalText};
            cache.put(word, result);
            return result;
        } else {
            throw new IOException("Error in API call: " + conn.getResponseMessage());
        }
    }
}