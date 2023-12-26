package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.SiteData;
import searchengine.model.Status;
import searchengine.utils.RecursiveIndexer;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private final SitesList sites;
    private final ApplicationContext context;
    private final SiteCrudService siteService;
    private final PageCrudService pageService;
    private final LemmaCrudService lemmaService;
    private final IndexCrudService indexService;
    private boolean running = false;
    private final ForkJoinPool pool = new ForkJoinPool(8);

    @Override
    public Object startIndexing() {
        HashMap<String, Object> response = new HashMap<>();
        if (running) {
            response.put("result", false);
            response.put("error", "Индексация уже запущена");
            return response;
        }

        running = true;
        for (Site site : sites.getSites()) {
            String name = site.getName();
            String url = site.getUrl();

            try {
                int siteId = siteService.getByUrl(url).getId();
                indexService.deleteAllBySiteId(siteId);
                lemmaService.deleteAllBySiteId(siteId);
                pageService.deleteAllBySiteId(siteId);
                siteService.delete(siteId);
            } catch (NullPointerException e) {
                log.info(e.getMessage());
            }

            SiteData siteData = new SiteData();
            siteData.setName(name);
            siteData.setUrl(url);
            siteData.setStatus(Status.INDEXING);
            siteData.setStatusTime(LocalDateTime.now());
            siteData = siteService.create(siteData);

            String path = "/";
            Set<String> paths = new HashSet<>();
            paths.add(path);
            RecursiveIndexer indexer = context.getBean(RecursiveIndexer.class);
            indexer.setSiteId(siteData.getId());
            indexer.setSourcePath(path);
            indexer.setPaths(paths);
            pool.submit(indexer);
        }
        pool.shutdown();
        response.put("result", true);
        return response;
    }

    @Override
    public Object stopIndexing() {
        HashMap<String, Object> response = new HashMap<>();
        if (running) {
            running = false;
            pool.shutdownNow();
            List<SiteData> sites = siteService.getAllByStatus(Status.INDEXING);
            for (SiteData site : sites) {
                site.setStatus(Status.FAILED);
                site.setStatusTime(LocalDateTime.now());
                siteService.update(site);
            }
            response.put("result", true);
        } else {
            response.put("result", false);
            response.put("error", "Индексация не запущена");
        }
        return response;
    }

    @Override
    public Object indexPage(String url) {
        HashMap<String, Object> response = new HashMap<>();
        response.put("result", true);
        return response;
    }
}
