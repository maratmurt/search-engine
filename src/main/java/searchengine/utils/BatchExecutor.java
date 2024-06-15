package searchengine.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import searchengine.dao.SiteDao;
import searchengine.dto.indexing.PageDto;
import searchengine.dto.indexing.SiteDto;
import searchengine.model.Status;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
@Component
@RequiredArgsConstructor
public class BatchExecutor {
    @Value("${indexing-settings.batch_size}")
    private int batchSize;

    private int fetchOffset = 0;

    private final List<PageDto> pages = new ArrayList<>();
    private final ApplicationContext context;
    private final SiteDao siteDao;

    public synchronized void add(PageDto page) {
        pages.add(page);

        if (pages.size() >= batchSize) {
            flush();

            List<SiteDto> sites = siteDao.findAllByStatus(Status.INDEXING);
            sites.forEach(site -> site.setStatusTime(Timestamp.valueOf(LocalDateTime.now())));
            siteDao.saveAll(sites);
        }
    }

    public synchronized void flush() {
        BatchProcessor batchProcessor = context.getBean(BatchProcessor.class);
        batchProcessor.setPages(new ArrayList<>(pages));
        batchProcessor.setFetchOffset(fetchOffset);
        batchProcessor.start();

        fetchOffset += pages.size();

        pages.clear();
    }
}
