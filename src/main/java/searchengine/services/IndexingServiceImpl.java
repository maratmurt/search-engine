package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.PageData;
import searchengine.dto.indexing.SiteData;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utils.Indexer;
import searchengine.utils.TaskManager;
import searchengine.utils.UrlUtils;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private final SitesList sites;
    private final ApplicationContext context;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final TaskManager taskManager;

    @Override
    public IndexingResponse startIndexing() {
        IndexingResponse response = new IndexingResponse();
        if (!taskManager.getTaskMap().isEmpty()) {
            response.setError("Индексация уже запущена");
            response.setResult(false);
            return response;
        }

        taskManager.initialize();
        for (Site siteConfig : sites.getSites()) {
            String name = siteConfig.getName();
            String url = siteConfig.getUrl();
            url = UrlUtils.endingSlash(url);

            deleteExistingEntities(url);

            SiteEntity site = new SiteEntity();
            site.setName(name);
            site.setUrl(url);
            site.setStatus(Status.INDEXING);
            site.setStatusTime(LocalDateTime.now());
            site = siteRepository.save(site);

            Indexer indexer = createIndexer(site, "/");
            taskManager.addTask(indexer, site.getId());
        }
        new Thread(taskManager).start();

        response.setResult(true);
        return response;
    }

    @Override
    public IndexingResponse stopIndexing() {
        IndexingResponse response = new IndexingResponse();
        if (taskManager.getTaskMap().isEmpty()) {
            response.setError("Индексация не запущена");
            response.setResult(false);
        } else {
            taskManager.stopProcess();
            response.setResult(true);
        }
        return response;
    }

    @Override
    public IndexingResponse indexPage(String url) {
        IndexingResponse response = new IndexingResponse();

        url = UrlUtils.decode(url);
        url = UrlUtils.endingSlash(url);
        String rootUrl;
        try {
            rootUrl = UrlUtils.extractRoot(url);
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage());
            response.setError("Введён некорректный адрес страницы");
            response.setResult(false);
            return response;
        }

        Site siteConfig = findSiteConfig(rootUrl);
        if (siteConfig == null) {
            response.setError("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
            response.setResult(false);
            return response;
        }

        String path = url.substring(rootUrl.length() - 1);
        SiteEntity site = findOrCreateSite(rootUrl, siteConfig.getName());
        updateLemmasAndIndices(site, path);

        Indexer indexer = createIndexer(site, path);
        PageData pageData;
        try {
            pageData = indexer.fetchData(url);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        PageEntity page = indexer.storePage(pageData);
        indexer.saveLemmasAndIndices(pageData.getText(), page);

        response.setResult(true);
        return response;
    }

    private SiteEntity findOrCreateSite(String url, String name) {
        SiteEntity site;
        try {
            site = siteRepository.findByUrl(url).orElseThrow();
        } catch (NoSuchElementException e) {
            site = new SiteEntity();
            site.setUrl(url);
            site.setName(name);
            site.setStatusTime(LocalDateTime.now());
            site = siteRepository.save(site);
        }
        return site;
    }

    private void updateLemmasAndIndices(SiteEntity site, String path) {
        try {
            PageEntity page = pageRepository.findBySiteAndPath(site, path).orElseThrow();
            List<IndexEntity> pageIndices = indexRepository.findAllByPage(page);
            for (IndexEntity index : pageIndices) {
                LemmaEntity lemma = index.getLemma();
                lemma.setFrequency(lemma.getFrequency() - 1);
                lemmaRepository.save(lemma);
            }
            indexRepository.deleteAll(pageIndices);
            pageRepository.delete(page);
        } catch (NoSuchElementException e) {
            log.error(e.getMessage());
        }
    }

    private Indexer createIndexer(SiteEntity site, String path) {
        Indexer indexer = context.getBean(Indexer.class);
        SiteData siteData = new SiteData();
        Set<String> paths = ConcurrentHashMap.newKeySet();
        paths.add(path);
        siteData.setPaths(paths);
        Set<String> lemmas = ConcurrentHashMap.newKeySet();
        siteData.setLemmas(lemmas);
        siteData.setSite(site);
        indexer.setSiteData(siteData);
        indexer.setSourcePath(path);
        return indexer;
    }

    private void deleteExistingEntities(String url) {
        try {
            SiteEntity existingSite = siteRepository.findByUrl(url).orElseThrow();
            indexRepository.deleteAllByPage_Site(existingSite);
            lemmaRepository.deleteAllBySite(existingSite);
            pageRepository.deleteAllBySite(existingSite);
            siteRepository.delete(existingSite);
        } catch (NoSuchElementException e) {
            log.error(e.getMessage());
        }
    }

    private Site findSiteConfig(String url) {
        Site site = null;
        for (Site siteConfig : sites.getSites()) {
            if (url.contains(siteConfig.getUrl())) {
                site = siteConfig;
                break;
            }
        }
        return site;
    }
}
