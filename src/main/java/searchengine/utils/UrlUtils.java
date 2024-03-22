package searchengine.utils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlUtils {
    public static String endingSlash(String url) {
        return url.endsWith("/") ? url : url.concat("/");
    }

    public static String decode(String url) {
        return URLDecoder.decode(url, StandardCharsets.UTF_8);
    }

    public static String extractRoot(String url) {
        Matcher rootMatch = Pattern.compile("http(s?)://[\\w-.]+/").matcher(url);
        String rootUrl;
        if (rootMatch.find()) {
            rootUrl = rootMatch.group();
        } else {
            throw new IllegalArgumentException();
        }
        return rootUrl;
    }
}
