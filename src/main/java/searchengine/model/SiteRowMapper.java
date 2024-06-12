package searchengine.model;

import org.springframework.jdbc.core.RowMapper;
import searchengine.dto.indexing.SiteDto;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SiteRowMapper implements RowMapper<SiteDto> {
    @Override
    public SiteDto mapRow(ResultSet rs, int rowNum) throws SQLException {
        SiteDto site = new SiteDto();

        site.setId(rs.getInt("id"));
        site.setName(rs.getString("name"));
        site.setUrl(rs.getString("url"));
        site.setStatus(rs.getString("status"));
        site.setLastError(rs.getString("last_error"));
        site.setStatusTime(rs.getTimestamp("status_time"));

        return site;
    }
}
