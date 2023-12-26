package searchengine.utils;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
public class HtmlScraper {
    private String html;
    private String text;
    private int statusCode;

    public void initialize(String url) throws IOException, InterruptedException {
        Thread.sleep(1000);

        log.info("CONNECTING TO " + url);

        Connection.Response response = Jsoup.connect(url)
                .userAgent("MySearchEngine")
                .referrer("http://www.google.com")
                .execute();

        html = response.body();
        statusCode = response.statusCode();
        text = Jsoup.parse(html).text();
    }

    public List<String> getLinks() {
        List<String> links = new ArrayList<>();
        Elements elements = Jsoup.parse(html).getElementsByAttribute("href");
        for (Element element : elements) {
            String link = element.attr("href");
            links.add(link);
        }
        return links;
    }
}
