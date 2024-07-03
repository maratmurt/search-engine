package searchengine.utils;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import searchengine.dao.PageDao;
import searchengine.dao.SiteDao;
import searchengine.dto.indexing.PageDto;
import searchengine.dto.indexing.SiteDto;
import searchengine.model.Status;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Setter
@Component
@RequiredArgsConstructor
public class BatchProcessor {
    @Value("${indexing-settings.batch_size}")
    private int batchSize;

    private int pagesOffset;

    private final List<PageDto> pages = new ArrayList<>();
    private final ApplicationContext context;
    private final PageDao pageDao;
    private final SiteDao siteDao;

    public synchronized void add(PageDto page) {
        pages.add(page);

        if (pages.size() >= batchSize) {
            flush();

            List<SiteDto> sites = siteDao.findAllByStatus(Status.INDEXING);
            sites.forEach(site -> {
                site.setStatusTime(Timestamp.valueOf(LocalDateTime.now()));
                siteDao.save(site);
            });
        }
    }

    public synchronized void flush() {
        int pagesCount = pages.size();

        pageDao.saveAll(pages);

        List<PageDto> fetchedPages = pageDao.fetch(pagesCount, pagesOffset);

        IndexProcessor indexProcessor = context.getBean(IndexProcessor.class);
        indexProcessor.setPages(fetchedPages);
        indexProcessor.start();

        pagesOffset += pagesCount;

        pages.clear();
    }
}
