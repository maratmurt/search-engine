package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.dto.ApiResponse;
import searchengine.dto.ErrorResponse;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.SitesRepository;
import searchengine.utils.IndexingTasksManager;
import searchengine.utils.SiteCrawler;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService{

    private final SitesList sitesList;
    private final SitesRepository sitesRepository;
    private final ApplicationContext context;
    private final IndexingTasksManager tasksManager;

    @Override
    public ApiResponse startIndexing() {
        if (tasksManager.isRunning())
            return new ErrorResponse("Индексация уже запущена");

        List<SiteConfig> configList = sitesList.getSites();

        tasksManager.initialize(configList.size());

        for (SiteConfig siteConfig : configList) {
            sitesRepository.findByUrl(siteConfig.getUrl()).ifPresent(sitesRepository::delete);

            Site site = new Site();

            site.setName(siteConfig.getName());
            site.setUrl(siteConfig.getUrl());
            site.setStatus(Status.INDEXING);
            site.setStatusTime(LocalDateTime.now());

            site = sitesRepository.save(site);

            SiteCrawler crawler = context.getBean(SiteCrawler.class);
            String path = "/";
            List<String> visited = new ArrayList<>();
            visited.add(path);
            crawler.setPath(path);
            crawler.setVisited(visited);
            crawler.setSite(site);
            tasksManager.queueTask(crawler);
        }
        new Thread(tasksManager).start();

        IndexingResponse response = new IndexingResponse();
        response.setResult(true);
        return response;
    }

    @Override
    public ApiResponse stopIndexing() {
        if (!tasksManager.isRunning())
            return new ErrorResponse("Индексация не запущена");

        tasksManager.abort();

        IndexingResponse response = new IndexingResponse();
        response.setResult(true);
        return response;
    }
}
