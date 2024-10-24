package de.murmelmeister.murmelapi.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;

// Thanks Minestom: https://github.com/Minestom/Minestom/blob/master/src/main/java/net/minestom/server/utils/url/URLUtils.java
public final class URLUtils {
    public static String getText(String url) throws IOException, URISyntaxException {
        HttpURLConnection connection = (HttpURLConnection) new URI(url).toURL().openConnection();

        final int responseCode = connection.getResponseCode();
        final InputStream inputStream;
        if (200 <= responseCode && responseCode <= 299)
            inputStream = connection.getInputStream();
        else inputStream = connection.getErrorStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder response = new StringBuilder();
        String currentLine;
        while ((currentLine = reader.readLine()) != null) response.append(currentLine);
        reader.close();
        return response.toString();
    }
}
