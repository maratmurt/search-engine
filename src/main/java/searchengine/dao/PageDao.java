package searchengine.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.SitesRepository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PageDao {
    @Value("${indexing-settings.batch_size}")
    private int batchSize;

    private final JdbcTemplate jdbcTemplate;
    private final List<Page> pages = new ArrayList<>();
    private final SitesRepository sitesRepository;

    public synchronized void batch(Page page) {
        pages.add(page);

        if (pages.size() >= batchSize) {
            flush();

            List<Site> sites = sitesRepository.findAllByStatus(Status.INDEXING);
            sites.forEach(site -> site.setStatusTime(LocalDateTime.now()));
            sitesRepository.saveAll(sites);
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
