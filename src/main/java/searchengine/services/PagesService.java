package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import searchengine.model.Page;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PagesService {
    @Value("${indexing-settings.batch_size}")
    private int batchSize;

    private final JdbcTemplate jdbcTemplate;
    private final List<Page> pages = new ArrayList<>();

    public synchronized void batch(Page page) {
        pages.add(page);

        if (pages.size() >= batchSize) {
            flush();
        }
    }

    public synchronized void flush() {
        String sql = "INSERT INTO page(code, content, path, site_id) VALUES(?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Page item = pages.get(i);

                ps.setInt(1, item.getCode());
                ps.setString(2, item.getContent());
                ps.setString(3, item.getPath());
                ps.setInt(4, item.getSite().getId());
            }

            @Override
            public int getBatchSize() {
                return pages.size();
            }
        });

        pages.clear();
    }
}
