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
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class HtmlScraper {
    private final AgentsList agentsList;
    private Random random = new Random();

    public ResponseEntity<String> getResponse(String url) throws IOException, InterruptedException {
        Thread.sleep(1000);
        List<String> agents = agentsList.getAgents();
        String agent = agents.get(random.nextInt(agents.size()));
        log.info("Agent: " + agent + ". Connecting to " + url);
        Connection.Response response = Jsoup.connect(url)
                .userAgent(agent)
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
        if (header == null) {
            return "Без заголовка";
        } else {
            return header.text();
        }
    }

    public String getText(String body) {
        return Jsoup.parse(body).text();
    }
}
