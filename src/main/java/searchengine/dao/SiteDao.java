package searchengine.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import searchengine.dto.indexing.SiteDto;
import searchengine.model.SiteRowMapper;
import searchengine.model.Status;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class SiteDao {
    private final JdbcTemplate connection;
    private final SiteRowMapper rowMapper = new SiteRowMapper();

    public SiteDto save(SiteDto site) {
        Optional<SiteDto> existingSite = findByUrl(site.getUrl());
        if (existingSite.isPresent()) {
            site.setId(existingSite.get().getId());

            connection.update(con -> {
                PreparedStatement ps = con.prepareStatement(
                        "UPDATE site SET (status, status_time, last_error) VALUES (?, ?, ?) WHERE id=?");

                ps.setString(1, site.getStatus());
                ps.setTimestamp(2, site.getStatusTime());
                ps.setString(3, site.getLastError());
                ps.setInt(4, site.getId());

                return ps;
            });
        } else {
            KeyHolder keyHolder = new GeneratedKeyHolder();

            connection.update(con -> {
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
        }
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

    public List<SiteDto> findAllById(List<Integer> siteIds) {
        String idsString = siteIds.stream().map(String::valueOf).collect(Collectors.joining(", "));
        String sql = "SELECT * FROM site WHERE id IN(" + idsString + ")";
        return connection.query(sql, rowMapper);
    }
}
