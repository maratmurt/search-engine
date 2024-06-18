package searchengine.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import searchengine.dto.indexing.PageDto;
import searchengine.model.PageRowMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PageDao {
    private final JdbcTemplate connection;
    private final PageRowMapper rowMapper = new PageRowMapper();

    public List<PageDto> fetch(int limit, int offset) {
        String sql = "SELECT * FROM page LIMIT " + limit + " OFFSET " + offset;

        return connection.query(sql, rowMapper);
    }

    public Optional<PageDto> findBySiteIdAndPath(int siteId, String path) {
        String sql = "SELECT * FROM page WHERE site_id=" + siteId + " AND path='" + path + "'";

        return connection.query(sql, rowMapper).stream().findAny();
    }

    public void delete(PageDto pageDto) {
        connection.update("DELETE FROM page WHERE id=" + pageDto.getId());
    }

    public PageDto save(PageDto page) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        connection.update(
                con -> {
                    PreparedStatement ps = con.prepareStatement(
                            "INSERT INTO page (code, content, path, site_id) VALUES (?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);

                    ps.setInt(1, page.getCode());
                    ps.setString(2, page.getContent());
                    ps.setString(3, page.getPath());
                    ps.setInt(4, page.getSiteId());

                    return ps;
                },
                keyHolder
        );

        page.setId(Objects.requireNonNull(keyHolder.getKey()).intValue());

        return page;
    }

    public void saveAll(List<PageDto> pages) {
        String sql = "INSERT INTO page(code, content, path, site_id) VALUES(?, ?, ?, ?)";

        connection.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                PageDto item = pages.get(i);

                ps.setInt(1, item.getCode());
                ps.setString(2, item.getContent());
                ps.setString(3, item.getPath());
                ps.setInt(4, item.getSiteId());
            }

            @Override
            public int getBatchSize() {
                return pages.size();
            }
        });
    }

    public int getAllPagesCount() {
        return connection.queryForObject("SELECT COUNT(*) FROM page", Integer.class);
    }

    public int getSitePagesCount(int siteId) {
        return connection.queryForObject("SELECT COUNT(*) FROM page WHERE site_id=" + siteId, Integer.class);
    }
}
