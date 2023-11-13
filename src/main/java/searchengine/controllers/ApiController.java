package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.config.*;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.Page;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.services.*;

import java.util.concurrent.*;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final SitesList sitesList;
    private final RepositoryService repositoryService;

    @Autowired
    public ApiController(StatisticsService statisticsService, SitesList sitesList, RepositoryService repositoryService) {
        this.statisticsService = statisticsService;
        this.sitesList = sitesList;
        this.repositoryService = repositoryService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping(value = "/startIndexing")
    public ResponseEntity<String> startIndexing() {
        Logger logger = Logger.getLogger("CTR-log");

        int taskCount = sitesList.getSites().size();

        ExecutorService executor = Executors.newFixedThreadPool(taskCount);

        for (Site siteConfig : sitesList.getSites()) {
            SiteEntity siteRecord = repositoryService.findSiteByUrl(siteConfig.getUrl());
            if (siteRecord != null) {
                Iterable<Page> pages = repositoryService.findPagesBySite(siteRecord);
                if (pages != null) {
                    repositoryService.deleteAll(pages);
                }
                repositoryService.delete(siteRecord);
            }

            SiteEntity site = new SiteEntity();
            site.setName(siteConfig.getName());
            site.setUrl(siteConfig.getUrl());
            site.setStatus(Status.INDEXING);
            repositoryService.save(site);

            PageCollector collector = new PageCollector(site, "/", repositoryService);

            CompletableFuture<SiteEntity> future = new CompletableFuture<>();
            StatusChecker checker = new StatusChecker(future, repositoryService);
            executor.execute(checker);

            new Thread(()->{
                ForkJoinPool pool = new ForkJoinPool(8);
                pool.invoke(collector);
                future.complete(site);
            }).start();
        }

        executor.shutdown();

        return ResponseEntity.ok().build();
    }
}
