package searchengine.utils;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
public class HtmlScraper {
    public ResponseEntity<String> getResponse(String url) throws IOException, InterruptedException {
        Thread.sleep(1000);
        log.info("CONNECTING TO " + url);
        Connection.Response response = Jsoup.connect(url)
                .userAgent("MySearchEngine")
                .referrer("http://www.google.com")
                .execute();
        return ResponseEntity.ok(response.body());
    }

    public Set<String> getLinks(String body) {
        Set<String> links = new HashSet<>();
        Elements elements = Jsoup.parse(body).getElementsByAttribute("href");
        for (Element element : elements) {
            String link = element.attr("href");
            links.add(link);
        }
        return links;
    }

    public String getTitle(String body) {
        Element header = Jsoup.parse(body).selectFirst("title");
        return header.text();
    }

    public String getText(String body) {
        return Jsoup.parse(body).text();
    }
}
