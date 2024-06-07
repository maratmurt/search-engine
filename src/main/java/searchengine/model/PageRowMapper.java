package searchengine.model;

import org.springframework.jdbc.core.RowMapper;
import searchengine.dto.indexing.PageDto;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PageRowMapper implements RowMapper<PageDto> {
    @Override
    public PageDto mapRow(ResultSet rs, int rowNum) throws SQLException {
        PageDto page = new PageDto();

        page.setId(rs.getInt("id"));
        page.setCode(rs.getInt("code"));
        page.setSiteId(rs.getInt("site_id"));
        page.setPath(rs.getString("path"));
        page.setContent(rs.getString("content"));

        return page;
    }
}
