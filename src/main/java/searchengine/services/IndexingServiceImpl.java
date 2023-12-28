package searchengine.services;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.SiteData;
import searchengine.model.Status;
import searchengine.utils.RecursiveIndexer;
import searchengine.utils.TaskManager;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

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
    private final TaskManager taskManager;
    private ForkJoinPool pool;

    @Override
    public Object startIndexing() {
        HashMap<String, Object> response = new HashMap<>();
        if (taskManager.isIndexing()) {
            response.put("result", false);
            response.put("error", "Индексация уже запущена");
            return response;
        }

        taskManager.setIndexing(true);
        pool = new ForkJoinPool(8);

        for (Site site : sites.getSites()) {
            String name = site.getName();
            String url = site.getUrl();
            url += url.endsWith("/") ? "" : "/";

            try {
                int siteId = siteService.getByUrl(url).getId();
                indexService.deleteAllBySiteId(siteId);
                lemmaService.deleteAllBySiteId(siteId);
                pageService.deleteAllBySiteId(siteId);
                siteService.delete(siteId);
            } catch (NullPointerException e) {
                log.error(e.getMessage());
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
            ForkJoinTask<Void> rootTask = pool.submit(indexer);
//            taskManager.addTask(rootTask);
        }
//        new Thread(taskManager).start();
        pool.shutdown();

        response.put("result", true);
        return response;
    }

    @Override
    public Object stopIndexing() {
        HashMap<String, Object> response = new HashMap<>();
        if (taskManager.isIndexing()) {
            taskManager.setIndexing(false);
            pool.shutdownNow();
//            List<SiteData> sites = siteService.getAllByStatus(Status.INDEXING);
//            for (SiteData site : sites) {
//                site.setStatus(Status.FAILED);
//                site.setStatusTime(LocalDateTime.now());
//                siteService.update(site);
//            }
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
