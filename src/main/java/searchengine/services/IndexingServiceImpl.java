package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.SitesRepository;
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

    @Override
    public IndexingResponse startIndexing() {
        for (SiteConfig siteConfig : sitesList.getSites()) {
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
            crawler.fork();
        }

        return new IndexingResponse();
    }
}
