package searchengine.utils;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import searchengine.dao.IndexDao;
import searchengine.dao.LemmaDao;
import searchengine.dto.indexing.IndexDto;
import searchengine.dto.indexing.LemmaDto;
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
    private final LemmaDao lemmaDao;
    private final IndexDao indexDao;

    @Override
    public void run() {
        String fetchSql = "SELECT * FROM page LIMIT " + pagesCount + " OFFSET " + pagesOffset;
        List<PageDto> fetchedPages = jdbcTemplate.query(fetchSql, new PageRowMapper());

        for (PageDto page : fetchedPages) {
            int siteId = page.getSiteId();
            String text = parser.getText(page.getContent());
            Map<String, Double> lemmaRankMap = lemmatizer.buildLemmaRankMap(text);
            List<String> lemmas = lemmaRankMap.keySet().stream().toList();

            // update existing lemmas
            List<LemmaDto> existingLemmas = lemmaDao.findAllByLemmaAndSiteId(lemmas, siteId);
            existingLemmas.forEach(lemma -> lemma.setFrequency(lemma.getFrequency() + 1));
            lemmaDao.updateAll(existingLemmas);

            // create new lemmas
            List<String> existingLemmaWords = existingLemmas.stream().map(LemmaDto::getLemma).toList();
            List<LemmaDto> newLemmas = lemmas.stream()
                    .filter(lemma -> !existingLemmaWords.contains(lemma))
                    .map(lemma -> {
                LemmaDto lemmaDto = new LemmaDto();
                lemmaDto.setLemma(lemma);
                lemmaDto.setSiteId(siteId);
                lemmaDto.setFrequency(1);
                return lemmaDto;
            }).toList();
            lemmaDao.saveAll(newLemmas);

            // create indexes
            List<IndexDto> indexes = lemmaDao.findAllByLemmaAndSiteId(lemmas, siteId).stream().map(lemma -> {
                IndexDto index = new IndexDto();
                index.setPageId(page.getId());
                index.setLemmaId(lemma.getId());
                index.setRank(lemmaRankMap.get(lemma.getLemma()));
                return index;
            }).toList();
            indexDao.saveAll(indexes);
        }
    }
}
