package searchengine.utils;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import searchengine.dto.indexing.LemmaDto;
import searchengine.dto.indexing.PageDto;
import searchengine.model.LemmaRowMapper;
import searchengine.model.PageRowMapper;

import java.sql.PreparedStatement;
import java.sql.SQLException;
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
            int siteId = page.getSiteId();
            String text = parser.getText(page.getContent());
            Map<String, Double> lemmaRankMap = lemmatizer.buildLemmaRankMap(text);
            List<String> lemmas = lemmaRankMap.keySet().stream().toList();

            // update existing lemmas
            String sql = "SELECT * FROM lemma WHERE lemma.lemma IN ('" + String.join("', '", lemmas) + "') AND lemma.site_id=" + siteId;
            List<LemmaDto> existingLemmaDtos = jdbcTemplate.query(sql, new LemmaRowMapper());
            existingLemmaDtos.forEach(lemma -> lemma.setFrequency(lemma.getFrequency() + 1));
            String updateSql = "UPDATE lemma SET lemma.frequency=? WHERE lemma.lemma=?";
            jdbcTemplate.batchUpdate(updateSql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    LemmaDto lemma = existingLemmaDtos.get(i);
                    ps.setInt(1, lemma.getFrequency());
                    ps.setString(2, lemma.getLemma());
                }

                @Override
                public int getBatchSize() {
                    return existingLemmaDtos.size();
                }
            });

            // insert new lemmas
            List<String> existingLemmas = existingLemmaDtos.stream().map(LemmaDto::getLemma).toList();
            List<String> newLemmas = lemmas.stream().filter(lemma -> !existingLemmas.contains(lemma)).toList();
            sql = "INSERT INTO lemma(lemma, site_id, frequency) VALUES(?, ?, ?)";
            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    ps.setString(1, newLemmas.get(i));
                    ps.setInt(2, siteId);
                    ps.setInt(3, 1);
                }

                @Override
                public int getBatchSize() {
                    return newLemmas.size();
                }
            });

            // insert indexes
            sql = "SELECT * FROM lemma WHERE lemma.lemma IN ('" + String.join("' ,'", lemmas) + "') AND lemma.site_id=" + siteId;
            List<LemmaDto> lemmaDtos = jdbcTemplate.query(sql, new LemmaRowMapper());
            jdbcTemplate.batchUpdate("INSERT INTO search_engine.index(lemma_id, page_id, search_engine.index.rank) VALUES(?, ?, ?)", new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    LemmaDto lemmaDto = lemmaDtos.get(i);
                    double rank = lemmaRankMap.get(lemmaDto.getLemma());

                    ps.setInt(1, lemmaDto.getId());
                    ps.setInt(2, page.getId());
                    ps.setDouble(3, rank);
                }

                @Override
                public int getBatchSize() {
                    return lemmaDtos.size();
                }
            });
        }
    }
}
