package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.ResponseObj;
import searchengine.dto.indexing.PageData;
import searchengine.dto.indexing.SiteData;
import searchengine.dto.statistics.StatisticsData;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utils.Indexer;
import searchengine.utils.TaskManager;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
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
    public Object startIndexing() {
        ResponseObj response = new ResponseObj();
        if (!taskManager.getTaskMap().isEmpty()) {
            response.setResult(false);
            response.setError("Индексация уже запущена");
            return response.getResponse();
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
        return response.getResponse();
    }

    @Override
    public Object stopIndexing() {
        ResponseObj response = new ResponseObj();
        if (taskManager.getTaskMap().isEmpty()) {
            response.setResult(false);
            response.setError("Индексация не запущена");
        } else {
            taskManager.stopProcess();
            response.setResult(true);
        }
        return response.getResponse();
    }

    @Override
    public Object indexPage(String url) {
        // https://300lux.ru/stonks
        ResponseObj response = new ResponseObj();

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
            response.setResult(false);
            response.setError("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
            return response.getResponse();
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
        return response.getResponse();
    }
}
