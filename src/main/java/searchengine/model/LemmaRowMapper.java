package searchengine.model;

import org.springframework.jdbc.core.RowMapper;
import searchengine.dto.indexing.LemmaDto;

import java.sql.ResultSet;
import java.sql.SQLException;

public class LemmaRowMapper implements RowMapper<LemmaDto> {
    @Override
    public LemmaDto mapRow(ResultSet rs, int rowNum) throws SQLException {
        LemmaDto lemma = new LemmaDto();

        lemma.setId(rs.getInt("id"));
        lemma.setLemma(rs.getString("lemma"));
        lemma.setSiteId(rs.getInt("site_id"));
        lemma.setFrequency(rs.getInt("frequency"));

        return lemma;
    }
}
