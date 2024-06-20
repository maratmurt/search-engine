package searchengine.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import searchengine.config.AgentsList;

import java.io.IOException;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class HtmlParser {
    private final AgentsList agentsList;
    private final Random random = new Random();

    public ResponseEntity<String> getResponse(String url) throws IOException, InterruptedException {
        Thread.sleep(1000);
        List<String> agents = agentsList.getAgents();
        String agent = agents.get(random.nextInt(agents.size()));
        log.info("Connecting to " + url);
        Connection.Response response = Jsoup.connect(url)
                .userAgent(agent)
                .referrer("http://www.google.com")
                .execute();
        return ResponseEntity.ok(response.body());
    }

    public List<String> getLinks(String body) {
        Set<String> links = new HashSet<>();
        Elements elements = Jsoup.parse(body).getElementsByAttribute("href");
        elements.forEach(element -> links.add(element.attr("href")));
        return new ArrayList<>(links);
    }

    public String getText(String body) {
        return Jsoup.parse(body).text();
    }

    public String getTitle(String body) {
        Element header = Jsoup.parse(body).selectFirst("title");
        if (header == null) {
            return "Без заголовка";
        } else {
            return header.text();
        }
    }
}
