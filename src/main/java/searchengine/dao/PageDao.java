package searchengine.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import searchengine.dto.indexing.PageDto;
import searchengine.model.PageRowMapper;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.SitesRepository;
import searchengine.utils.IndexProcessor;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PageDao {
    @Value("${indexing-settings.batch_size}")
    private int batchSize;

    private int pagesOffset = 0;

    private final JdbcTemplate jdbcTemplate;
    private final List<PageDto> pages = new ArrayList<>();
    private final SitesRepository sitesRepository;
    private final ApplicationContext context;

    public synchronized void batch(PageDto page) {
        pages.add(page);

        if (pages.size() >= batchSize) {
            flush();

            List<Site> sites = sitesRepository.findAllByStatus(Status.INDEXING);
            sites.forEach(site -> site.setStatusTime(LocalDateTime.now()));
            sitesRepository.saveAll(sites);
        }
    }

    public synchronized void flush() {
        int pagesCount = pages.size();

        String sql = "INSERT INTO page(code, content, path, site_id) VALUES(?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
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
                return pagesCount;
            }
        });

        List<PageDto> fetchedPages = fetch(pagesCount, pagesOffset);

        IndexProcessor indexProcessor = context.getBean(IndexProcessor.class);
        indexProcessor.setPages(fetchedPages);
        indexProcessor.start();

        pagesOffset += pagesCount;

        pages.clear();
    }

    public List<PageDto> fetch(int limit, int offset) {
        String sql = "SELECT * FROM page LIMIT " + limit + " OFFSET " + offset;

        return jdbcTemplate.query(sql, new PageRowMapper());
    }
}
