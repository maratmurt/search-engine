package searchengine.utils;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Getter
@Component
public class HtmlScraper {
    private String body;
    private String text;
    private int statusCode;

    public void initialize(String url) throws IOException, InterruptedException {
        Thread.sleep(3000);

        log.info("CONNECTING TO " + url);
        Connection.Response response = Jsoup.connect(url)
                .timeout(5000)
                .userAgent("MySearchEngine")
                .referrer("http://www.google.com")
                .execute();

        body = response.body();
        statusCode = response.statusCode();
        text = Jsoup.parse(body).text();
    }

    public Set<String> getLinks() {
        Set<String> links = new HashSet<>();
        Elements elements = Jsoup.parse(body).getElementsByAttribute("href");
        for (Element element : elements) {
            String link = element.attr("href");
            links.add(link);
        }
        return links;
    }
}
