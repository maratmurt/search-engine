package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.LemmaData;
import searchengine.dto.indexing.PageData;
import searchengine.dto.indexing.SiteData;
import searchengine.model.Status;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private final SitesList sites;
    private final SiteCrudService siteService;
    private final PageCrudService pageService;
    private final LemmaCrudService lemmaService;
    private final IndexCrudService indexService;
    private final ForkJoinPool pool = new ForkJoinPool(8);

    @Override
    public Object startIndexing() {
        HashMap<String, Object> response = new HashMap<>();

        for (Site site : sites.getSites()) {
            String name = site.getName();
            String url = site.getUrl();

            try {
                int siteId = siteService.getByUrl(url).getId();
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

            RecursiveIndexer indexer = new RecursiveIndexer();
            indexer.setSiteId(siteData.getId());
            indexer.setSourcePath("/");
            indexer.setPaths(new HashSet<>());
            indexer.setSiteService(siteService);
            indexer.setPageService(pageService);
            indexer.setLemmaService(lemmaService);
            indexer.setIndexService(indexService);
            pool.submit(indexer);
        }
        pool.shutdown();
        response.put("result", true);
        return response;
    }

    @Override
    public Object stopIndexing() {
        HashMap<String, Object> response = new HashMap<>();
        response.put("result", true);
        return response;
    }

    @Override
    public Object indexPage(String url) {
        HashMap<String, Object> response = new HashMap<>();
        response.put("result", true);
        return response;
    }
}
