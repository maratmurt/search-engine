package searchengine.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import searchengine.dto.indexing.IndexDto;
import searchengine.model.IndexRowMapper;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class IndexDao {
    private final JdbcTemplate connection;
    private final IndexRowMapper rowMapper = new IndexRowMapper();

    public void saveAll(List<IndexDto> indexes) {
        String sql = "INSERT INTO search_engine.index(lemma_id, page_id, search_engine.index.rank) VALUES(?, ?, ?)";

        connection.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                IndexDto index = indexes.get(i);

                ps.setInt(1, index.getLemmaId());
                ps.setInt(2, index.getPageId());
                ps.setDouble(3, index.getRank());
            }

            @Override
            public int getBatchSize() {
                return indexes.size();
            }
        });
    }

    public List<IndexDto> findAllByLemmaIds(List<Integer> lemmaIds) {
        String idsString = lemmaIds.stream().map(String::valueOf).collect(Collectors.joining(", "));
        return connection.query("SELECT * FROM search_engine.index WHERE lemma_id IN (" + idsString + ")", rowMapper);
    }
}
