package searchengine;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestPaths {
    public static void main(String[] args) {
        String siteUrl = "https://triko.agency/";

        Connection.Response connectionResponse;
        try {
            connectionResponse = Jsoup.connect(siteUrl)
                    .userAgent("MySearchEngine")
                    .referrer("http://www.google.com")
                    .execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String html = connectionResponse.body();
        Document document = Jsoup.parse(html);
        Elements elements = document.getElementsByAttribute("href");
        List<String> pathList = new ArrayList<>();

        String urlRegex = "(" + siteUrl + ")[\\w-/]+$";
        String pathRegex = "^/[\\w-]+[\\w-/]*/?$";
        for (Element element : elements) {
            String href = element.attr("href");
            System.out.println("new href: " + href);

            String path = "";

            if (href.matches(urlRegex)) {
                path = href.substring(siteUrl.length() - 1);
                System.out.println("TRIMMED to path: " + path);
            } else if (href.matches(pathRegex)) {
                System.out.println("PATH match: " + href);
                path = href;
            }

            if (path.isEmpty()) {
                continue;
            } else if (!path.endsWith("/")) {
                path += "/";
            }

            pathList.add(path);
        }

        pathList.forEach(System.out::println);

        System.out.println("ELEMENTS COUNT: " + elements.size());
        System.out.println("PATHS COUNT: " + pathList.size());
    }
}
