package searchengine.utils;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import searchengine.dto.indexing.PageDto;
import searchengine.model.PageRowMapper;

import java.util.List;
import java.util.Map;

@Slf4j
@Setter
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class IndexProcessor extends Thread {
    private int pagesCount;
    private int pagesOffset;

    private final JdbcTemplate jdbcTemplate;
    private final Lemmatizer lemmatizer;
    private final HtmlParser parser;

    @Override
    public void run() {
        String fetchSql = "SELECT * FROM page LIMIT " + pagesCount + " OFFSET " + pagesOffset;
        List<PageDto> fetchedPages = jdbcTemplate.query(fetchSql, new PageRowMapper());

        for (PageDto page : fetchedPages) {
            String text = parser.getText(page.getContent());
            Map<String, Integer> lemmaRankMap = lemmatizer.buildLemmaRankMap(text);

            log.info(page.getSiteId() + " - " + page.getPath() + " - " + String.join(", ", lemmaRankMap.keySet()));
        }
    }
}
