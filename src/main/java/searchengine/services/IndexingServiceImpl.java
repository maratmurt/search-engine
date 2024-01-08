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

import java.io.IOException;
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
            url += url.endsWith("/") ? "" : "/";

            try {
                SiteEntity existingSite = siteRepository.findByUrl(url).orElseThrow();
                indexRepository.deleteAllByPage_Site(existingSite);
                lemmaRepository.deleteAllBySite(existingSite);
                pageRepository.deleteAllBySite(existingSite);
                siteRepository.delete(existingSite);
            } catch (NoSuchElementException e) {
                log.error(e.getMessage());
            }

            SiteEntity site = new SiteEntity();
            site.setName(name);
            site.setUrl(url);
            site.setStatus(Status.INDEXING);
            site.setStatusTime(LocalDateTime.now());
            site = siteRepository.save(site);

            SiteData siteData = new SiteData();
            siteData.setSiteId(site.getId());
            Set<String> paths = ConcurrentHashMap.newKeySet();
            String path = "/";
            paths.add(path);
            siteData.setPaths(paths);
            Set<String> lemmas = ConcurrentHashMap.newKeySet();
            siteData.setLemmas(lemmas);
            Indexer indexer = context.getBean(Indexer.class);
            indexer.setSourcePath(path);
            indexer.setSiteData(siteData);
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
        // https://300lux.ru/stonks
        IndexingResponse response = new IndexingResponse();

        Matcher rootMatch = Pattern.compile("http(s?)://[\\w-.]+/").matcher(url);
        String rootUrl;
        if (rootMatch.find()) {
            rootUrl = rootMatch.group();
        } else {
            throw new IllegalArgumentException();
        }
        String siteUrl = null;
        String siteName = null;
        for (Site siteConfig : sites.getSites()) {
            if (rootUrl.equals(siteConfig.getUrl())) {
                siteUrl = siteConfig.getUrl();
                siteName = siteConfig.getName();
                break;
            }
        }

        if (siteUrl == null) {
            response.setError("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
            response.setResult(false);
            return response;
        }

        String path = url.substring(rootMatch.end() - 1);;
        path += path.endsWith("/") ? "" : "/";
        SiteEntity site;
        log.info(path);
        try {
            List<IndexEntity> pageIndices = indexRepository.findAllByPage_Path(path);
            for (IndexEntity index : pageIndices) {
                LemmaEntity lemma = index.getLemma();
                lemma.setFrequency(lemma.getFrequency() - 1);
            }
            indexRepository.deleteAll(pageIndices);
            site = siteRepository.findByUrl(rootUrl).orElseThrow();
            pageRepository.deleteBySiteAndPath(site, path);
        } catch (NoSuchElementException e) {
            site = new SiteEntity();
            site.setName(siteName);
            site.setUrl(siteUrl);
            site.setStatusTime(LocalDateTime.now());
            site = siteRepository.save(site);
        }

        Indexer indexer = context.getBean(Indexer.class);
        SiteData siteData = new SiteData();
        Set<String> paths = ConcurrentHashMap.newKeySet();
        paths.add(path);
        siteData.setPaths(paths);
        Set<String> lemmas = ConcurrentHashMap.newKeySet();
        siteData.setLemmas(lemmas);
        siteData.setSiteId(site.getId());
        indexer.setSiteData(siteData);
        indexer.setSourcePath(path);
        PageData pageData;
        try {
            pageData = indexer.fetchData(url);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        PageEntity page = indexer.savePage(pageData);
        indexer.saveLemmasAndIndices(pageData.getText(), page);

        response.setResult(true);
        return response;
    }
}
