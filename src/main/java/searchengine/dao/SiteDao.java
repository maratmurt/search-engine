package searchengine.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import searchengine.dto.indexing.SiteDto;
import searchengine.model.SiteRowMapper;
import searchengine.model.Status;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class SiteDao {
    private final JdbcTemplate connection;
    private final SiteRowMapper rowMapper = new SiteRowMapper();

    public SiteDto save(SiteDto site) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        connection.update(
                con -> {
                    PreparedStatement ps = con.prepareStatement(
                            "INSERT INTO site (name, url, last_error, status, status_time) VALUES (?, ?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);

                    ps.setString(1, site.getName());
                    ps.setString(2, site.getUrl());
                    ps.setString(3, site.getLastError());
                    ps.setString(4, site.getStatus());
                    ps.setTimestamp(5, site.getStatusTime());

                    return ps;
                },
                keyHolder
        );

        site.setId(Objects.requireNonNull(keyHolder.getKey()).intValue());

        return site;
    }

    public Optional<SiteDto> findByUrl(String url) {
        String sql = "SELECT * FROM site WHERE url='" + url + "'";

        return connection.query(sql, rowMapper).stream().findAny();
    }

    public void delete(SiteDto site) {
        connection.update("DELETE FROM site WHERE id=" + site.getId());
    }

    public List<SiteDto> findAllByStatus(Status status) {
        String sql = "SELECT * FROM site WHERE site.status='" + status.toString() + "'";

        return connection.query(sql, rowMapper);
    }

    public void saveAll(List<SiteDto> sites) {
        String sql = "INSERT INTO site (name, url, last_error, status, status_time) VALUES (?, ?, ?, ?, ?)";

        connection.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                SiteDto item = sites.get(i);

                ps.setString(1, item.getName());
                ps.setString(2, item.getUrl());
                ps.setString(3, item.getLastError());
                ps.setString(4, item.getStatus());
                ps.setTimestamp(5, item.getStatusTime());
            }

            @Override
            public int getBatchSize() {
                return sites.size();
            }
        });
    }
}
