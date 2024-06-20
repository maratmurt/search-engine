package searchengine.model;

import org.springframework.jdbc.core.RowMapper;
import searchengine.dto.indexing.IndexDto;

import java.sql.ResultSet;
import java.sql.SQLException;

public class IndexRowMapper implements RowMapper<IndexDto> {
    @Override
    public IndexDto mapRow(ResultSet rs, int rowNum) throws SQLException {
        IndexDto index = new IndexDto();

        index.setId(rs.getInt("id"));
        index.setPageId(rs.getInt("page_id"));
        index.setLemmaId(rs.getInt("lemma_id"));
        index.setRank(rs.getDouble("rank"));

        return index;
    }
}
