package searchengine.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import searchengine.dto.indexing.LemmaDto;
import searchengine.model.LemmaRowMapper;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LemmaDao {
    private final JdbcTemplate jdbcTemplate;

    public List<LemmaDto> findAllByLemmaAndSiteId(List<String> lemmas, int siteId) {
        String sql = "SELECT * FROM lemma WHERE lemma.lemma IN ('" +
                String.join("', '", lemmas) + "') AND site_id=" + siteId;

        return jdbcTemplate.query(sql, new LemmaRowMapper());
    }

    public void updateAll(List<LemmaDto> lemmas) {
        String updateSql = "UPDATE lemma SET lemma.frequency=? WHERE lemma.lemma=?";

        jdbcTemplate.batchUpdate(updateSql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                LemmaDto lemma = lemmas.get(i);

                ps.setInt(1, lemma.getFrequency());
                ps.setString(2, lemma.getLemma());
            }

            @Override
            public int getBatchSize() {
                return lemmas.size();
            }
        });
    }

    public void saveAll(List<LemmaDto> lemmas) {
        String sql = "INSERT INTO lemma(lemma, site_id, frequency) VALUES(?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                LemmaDto lemma = lemmas.get(i);

                ps.setString(1, lemma.getLemma());
                ps.setInt(2, lemma.getSiteId());
                ps.setInt(3, lemma.getFrequency());
            }

            @Override
            public int getBatchSize() {
                return lemmas.size();
            }
        });
    }
}
